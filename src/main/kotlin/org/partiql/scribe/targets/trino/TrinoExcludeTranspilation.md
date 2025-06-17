This document goes over one of the more complicated rewrites for PartiQL's `EXCLUDE` to Trino.

### Trino Background
#### Trino ROW
Trino has a [ROW type](https://trino.io/docs/current/language/types.html#row) that is most similar to PartiQL's 
struct/row data type. There are a couple ways to create a ROW value in Trino:
1. Using `CAST(ROW(...) AS ROW(...))` syntax

E.g.
```sql
CAST(ROW(1, 2e0) AS ROW(x BIGINT, y DOUBLE))
```

Somewhat annoyingly, to provide column names, we must provide the types with each of the ROW fields.

2. Using a SELECT projection

E.g.
```sql
SELECT 1 AS x, 2e0 AS y
```

Above is equivalent to the `CAST(ROW(...)` construction syntax and is significantly less verbose. However, there are a
couple issues we've noticed with how the `SELECT` projection differs
- If there is a single `ROW` field, the output value will be a single value rather than a `ROW` containing that single 
value. E.g. `(SELECT 1 AS x)` will return back just the value `1`
- The `SELECT` syntax will give an error when used within a `transform` function

Based on the above limitations, the `EXCLUDE` transpilation will use the `CAST(ROW)` syntax to recreate ROWs with
excluded attributes.

#### Trino Array
Trino supports an array type similar to PartiQL's array/list data type. The `transform` [function](https://trino.io/docs/current/functions/array.html#transform)
is the same as SparkSQL's transform function. It allows us to reconstruct arrays that contain ROWs with excluded
attributes:

```sql
SELECT transform(ARRAY[], x -> x + 1);
-- []

SELECT transform(ARRAY[5, 6], x -> x + 1);
-- [6, 7]

SELECT transform(ARRAY[5, NULL, 6], x -> coalesce(x, 0) + 1);
-- [6, 1, 7]

SELECT transform(ARRAY['x', 'abc', 'z'], x -> x || '0');
-- ['x0', 'abc0', 'z0']

SELECT transform(ARRAY[ARRAY[1, NULL, 2], ARRAY[3, NULL]],
                 a -> filter(a, x -> x IS NOT NULL));
-- [[1, 2], [3]]
```

### PartiQL Rewrite
Trino's rewrite of `EXCLUDE` follows the same approach as SparkSQL in how it
1. Depends on [partiql-lang-kotlin#1764](https://github.com/partiql/partiql-lang-kotlin/pull/1764) which denotes any
ROWs and collections that have an excluded field
2. If the input to `RelExclude` is `RelProject`, remove the `RelExclude`
3. Recreates `PType` ROWs containing excluded fields with the equivalent way to create ROWs in Trino
4. Reconstructs `PType` collections containing nested excluded fields using the `transform` function.

See `SparkExcludeTranspilation`'s section on `PartiQL Plan Rewrite`.

Where Trino differs is for creating the ROWs with the excluded fields omitted. Within `TrinoExcludeUtils`, we hard-code
the type to the `CAST` (i.e. everything after the `AS` in the CAST) as a hard-coded string that we reconstruct based 
on the `PType.ROW`'s fields.

### Other Limitations
Similar to SparkSQL, there are a couple limitations
- We only currently support `EXCLUDE` transpilation of excluding struct field and struct fields within collections
- Trino does not allow for creating empty `ROW`s, so we throw an error whenever a `PType.ROW` used in the exclude
rewrite has only one field.
