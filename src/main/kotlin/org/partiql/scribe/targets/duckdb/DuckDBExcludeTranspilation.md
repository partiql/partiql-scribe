This document goes over one of the more complicated rewrites for PartiQL's `EXCLUDE` to DuckDB.

### DuckDB Background
#### DuckDB STRUCT
DuckDB has a [STRUCT type](https://duckdb.org/docs/current/sql/data_types/struct) that is most similar to PartiQL's
struct/row data type. There are a few ways to create a struct value in DuckDB:

1. Struct literal syntax `{'name': value, ...}`

E.g.
```sql
{'x': 1, 'y': 2e0}
-- struct(x integer, y double)
```

This preserves field names directly and infers each field's type from its value. It is the form the `EXCLUDE`
transpilation uses.

2. `ROW(...)` constructor (optionally with a `CAST` to name/type the fields)

E.g.
```sql
CAST(ROW(1, 2e0) AS ROW(x BIGINT, y DOUBLE))
```

`ROW(...)` is a synonym for `STRUCT` in DuckDB and also works, but it is more verbose (names and types must be
restated in the `CAST`), so we prefer the struct-literal form.

3. A `SELECT` projection

E.g. `SELECT 1 AS x, 2e0 AS y` — equivalent but has edge cases (a single field collapses to a scalar; it is awkward
inside a lambda), so it is not used.

The struct literal `{'name': value}` is verified to preserve names/types and to work inside a `list_transform`
lambda on DuckDB v1.5.6, which is why the rewrite uses it.

#### DuckDB LIST
DuckDB supports a list type similar to PartiQL's array/list data type. The
[`list_transform`](https://duckdb.org/docs/current/sql/functions/lambda#list_transformlist-lambda) function (DuckDB's
name for the higher-order map; SparkSQL calls it `transform`) lets us reconstruct lists whose elements are structs with
excluded fields:

```sql
SELECT list_transform([], x -> x + 1);
-- []

SELECT list_transform([5, 6], x -> x + 1);
-- [6, 7]

SELECT list_transform([{'a': 1, 'b': 2}], x -> {'a': x.a});
-- [{'a': 1}]
```

### PartiQL Rewrite
DuckDB's rewrite of `EXCLUDE` follows the same approach as SparkSQL in how it
1. Depends on [partiql-lang-kotlin#1764](https://github.com/partiql/partiql-lang-kotlin/pull/1764) which denotes any
ROWs and collections that have an excluded field
2. If the input to `RelExclude` is `RelProject`, remove the `RelExclude`
3. Reconstructs `PType` ROWs containing excluded fields as a `RexStruct` (rendered as a `{'name': value}` struct
literal by `DuckDBAstToSql.visitExprStruct`)
4. Reconstructs `PType` collections containing nested excluded fields using the `list_transform` function.

See `SparkExcludeTranspilation`'s section on `PartiQL Plan Rewrite` — `DuckDBExcludeUtils.toRexDuckDB` mirrors
`SparkExcludeUtils.toRexSpark`, building a `RexStruct` for ROW types and a `list_transform` call for collections.

### Other Limitations
Similar to SparkSQL, there are a couple limitations
- We only currently support `EXCLUDE` transpilation of excluding struct fields and struct fields within collections.
- DuckDB cannot represent an empty struct (`{}`, `ROW()`, and `struct_pack()` all error), so we throw an error
whenever a `PType.ROW` used in the exclude rewrite would have zero fields (i.e. all of its fields were excluded).
Exclude the containing field instead.
