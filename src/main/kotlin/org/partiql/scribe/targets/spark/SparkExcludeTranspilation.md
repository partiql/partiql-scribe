This document goes over one of the more complicated rewrites for PartiQL's `EXCLUDE` to SparkSQL.

### Spark Background

#### Spark Struct
Spark has a struct data type that is most similar to PartiQL's struct/row data type. From the docs, we can
construct a Spark struct using the `STRUCT` [function](https://spark.apache.org/docs/latest/api/sql/#struct).

E.g.
```
SELECT STRUCT(1 AS a, 2 AS b, 3 AS c);
```

```
{"a":1,"b":2,"c":3}
```

We use this `STRUCT` function to model a PartiQL ROW with any excluded attributes omitted from the output Spark `STRUCT`
fields.

E.g.
```SQL
SELECT STRUCT(
   t.a AS a, 
   -- omit field `t.b` from the struct
   t.c AS c
);
```

#### Spark Array
Spark supports an array type that is similar to PartiQL's array/list data type. The `transform` 
[function](https://spark.apache.org/docs/latest/api/sql/#transform) is useful for reconstructing arrays that contain
ROWs with excluded attributes.

Suppose we have an array of structs, each containing three fields - `a`, `b`, `c`. This can be represented in SparkSQL
as below:

```SQL
ARRAY(
    STRUCT(1 AS a, 2 AS b, 3 AS c), -- array[0]
    STRUCT(4 AS a, 5 AS b, 6 AS c)  -- array[1]
)
```

To omit all of the `b`s from the nested structs, we can use the `transform` to reconstruct all the arrays with the
structs that omit the field `c`.

```SQL
SELECT TRANSFORM(
    ARRAY(
        STRUCT(1 AS a, 2 AS b, 3 AS c), 
        STRUCT(4 AS a, 5 AS b, 6 AS c)
    ), 
    struct_var -> STRUCT(struct_var.a AS a, struct_var.c AS c)
);
```
which outputs the following
```
[{"a":1,"c":3},{"a":4,"c":6}]
```

### PartiQL Rewrite
#### PartiQL Plan Rewrite

Similar to the Redshift rewrite, we will rely on [partiql-lang-kotlin#1764](https://github.com/partiql/partiql-lang-kotlin/pull/1764)
which denotes whether certain `PType.ROW`s and collection types have an excluded attribute. This rewrite differs from 
Redshift such that for Spark (as well as Trino) we also rewrite any collections (i.e. `PType.ARRAY` and `PType.BAG`) 
that have this added meta to support `EXCLUDE` collection wildcards.

The entry point for the `EXCLUDE` rewrite is in `SparkRewriter`. Within `SparkRewriter`, we remove any `EXCLUDE` `Rel`
that are inputs to `RelProject`s. Then, we rewrite the projection's `RexStruct` fields based on which fields include
the `CONTAINS_EXCLUDED_FIELD` meta.

Taking a look at `SparkExcludeUtils`, the functions provide ways to reconstruct both ROWs and collections that 
contain excluded fields. For `PType.ROW`s, the fields that are excluded will no longer be in the `PType`'s list of 
fields due the partiql-lang-kotlin typing pass. Hence, we can create a `RexStruct` from all the `PType` ROW's fields.
Collections are a bit trickier since we need to represent the collection of ROWs with omitted fields using the 
`transform` function. To do this, we define a `RexCall` that takes three arguments:
1. the input collection of ROWs
2. the variable used in the `transform` lambda function
3. the expression reconstructing each of the elements based on the nested excluded fields

#### AST -> SQL Rewrite
`SparkAstToSql` performs some additional changes when converting the `ExprStruct` and `transform` `ExprCall`
into valid SparkSQL.

For `ExprStruct`, we transform the `ExprStruct` into the `STRUCT` function call where each `ExprStruct.Field` name gets
used as an alias for the field value. The `transform` `ExprCall` is originally three arguments with the second argument
denoting the variable used in the third argument's element reconstruction. SparkSQL follows a different syntax, so we
alter the default function printing from

```sql
transform(arg1, arg2, arg3)
```

to 
```sql
transform(arg1, arg2 -> arg)
```

### Other Limitations
- We only currently support `EXCLUDE` transpilation of excluding struct field and struct fields within collections
- SparkSQL varies in behavior across versions when creating an empty `STRUCT`. To ensure syntactically valid SparkSQL,
we just error when creating a `STRUCT` with no fields. If we are to stabilize the output Spark to a version that 
supports empty structs, we can remove that error condition.
