This document goes over one of the more complicated rewrites for PartiQL's `EXCLUDE` to Redshift.

### Redshift Background

#### SUPER OBJECT
The [Redshift `OBJECT` value](https://docs.aws.amazon.com/redshift/latest/dg/r_SUPER_type.html) is what is most similar
to PartiQL's struct/row value. From the docs, there are a few ways to create an `OBJECT`.

1. [JSON_PARSE function](https://docs.aws.amazon.com/redshift/latest/dg/JSON_PARSE.html) -- takes in a JSON string and
   returns back an `OBJECT`.

E.g.
```Redshift
JSON_PARSE(
    '{
        "a": { "field_x": 1, "field_y": "one" },
        "b": { "field_x": 2, "field_y": "two" },
        "c": { "field_x": 3, "field_y": "three" }
    }'
)
```
Above will output a struct similar to PartiQL's struct:
```PartiQL
{
    'a': { 'field_x': 1, 'field_y': 'one' },
    'b': { 'field_x': 2, 'field_y': 'two' },
    'c': { 'field_x': 3, 'field_y': 'three' }
}
```


2. [OBJECT function](https://docs.aws.amazon.com/redshift/latest/dg/r_object_function.html) -- takes in a variable
   number of key (string) and value expressions (any Redshift data type except datetime types).

Taking the above example,
```Redshift
OBJECT(
    'a', OBJECT(
        'field_x', 1,
        'field_y', 'one'
    ),
    'b', OBJECT(
        'field_x', 2,
        'field_y', 'two'
    ),
    'c', OBJECT(
        'field_x', 3,
        'field_y', 'three'
    )
)
```
The `OBJECT` function also allows us to create empty `OBJECT`s/structs -- `OBJECT()`. One limitation is that
reconstructing an `OBJECT` using the `OBJECT` function proves to have worse performance than using the following 
approach for `OBJECT_TRANSFORM`.

3. [OBJECT_TRANSFORM function](https://docs.aws.amazon.com/redshift/latest/dg/r_object_transform_function.html) --
   takes three arguments
    1. input SUPER OBJECT
    2. set of paths to `KEEP` (represented as a string literal with double-quoted path components)
    3. set of paths to modify with corresponding replacement value

`OBJECT_TRANSFORM` allows us to modify an existing `OBJECT` values and omit and/or reassign certain fields.
```Redshift
-- Assuming the `OBJECT` defined from above is a `SUPER` column `flds` in some table `t`.
OBJECT_TRANSFORM(
    t.flds
    KEEP
        '"a"."field_x"',    -- leave out `a.field_y`
        -- '"b"', omit b
        '"c"'
    SET
        '"c"', OBJECT()     -- reassign path '"c"' to an empty `OBJECT()`
)
```
The above will output a struct (written in PartiQL struct syntax) of:
```PartiQL
{
    'a': { 'field_y': 'one' },
    'c': { }
}
```

As brought up in [partiql-scribe#52](https://github.com/partiql/partiql-scribe/issues/52), modifying `OBJECT`s to use
`OBJECT_TRANSFORM` has shown to have a lot better performance than reconstructing `OBJECT`s using the `OBJECT` function.
So in the rewrite that follows, we will primarily be using `OBJECT_TRANSFORM`.

### PartiQL Rewrite
This section will go over a PartiQL rewrite of nested `EXCLUDE` paths (e.g. `EXCLUDE t.flds.a.field_x`) to Redshift.

For demonstration purposes, let's assume an input environment created by the following statements in Redshift:
```Redshift
CREATE TABLE EXCLUDE_T (
    flds SUPER,
    foo VARCHAR
);

INSERT INTO EXCLUDE_T VALUES(
    JSON_PARSE(
        '{
            "a": { "field_x": 0, "field_y": "zero" },
            "b": { "field_x": 1, "field_y": "one" },
            "c": { "field_x": 2, "field_y": "two" }
        }'
    ),
    'bar'
);
```

Let's say we want to exclude a couple nested fields
```PartiQL
-- In PartiQL
SELECT * EXCLUDE t.flds.b, t.flds.c.field_x FROM EXCLUDE_T AS t
```

After parsing and planning, partiql-lang-kotlin will provide a typed plan like the following:

```Kotlin
// Types omitted in this code block
RexSelect(
   input = RelProject(
      input = RelExclude(
         input = RelScan( // scan table `EXCLUDE_T`
            // ...
         ),
         exclusions = listOf(
             // plan representation of `t.flds.b` and `t.flds.c.field_x`
         )
      ),
      projections = listOf(
          rexStruct(
              "flds" to rexPathKey(
                  operand = rexVarRef(...),
                  key = "flds"
              ),
              "foo" to // Rex version of "foo"
          )
      )
   )
)
```

Inside the Redshift-specific Rel -> Rel conversion, `RedshiftRewriter`, we remove the `RelExclude` and replace the 
input to the `RelProject` with the `RelExclude`'s input.

```Kotlin
RexSelect(
   input = RelProject( // RelProject's input now points to the RelExclude's input, RelScan
      input = RelScan( // scan table `EXCLUDE_T`
         // ...
      ),
   ),
   projections = listOf(
       rexStruct(
           "flds" to rexPathKey(
               operand = rexVarRef(...),
               key = "flds"
           ),
           "foo" to // Rex version of "foo"
       )
   )
)
```

Looking at the types of the projected nodes, [partiql-lang-kotlin#1764](https://github.com/partiql/partiql-lang-kotlin/pull/1764),
introduced an additional meta on `PType`s that denotes whether a certain `PType.ROW` or collection type has an excluded
attribute. For our example, this results in a projection type for the `flds` struct field like the following:
```Kotlin
// `flds`'s type
ROW(
    fields = listOf(
        "a": ROW(
            fields = listOf(
                "field_x": INTEGER,
                "field_y": STRING
            ),
            metas = emptyMap() // no omitted fields hence no meta
        ),
        "c": ROW(
            fields = listOf(
                "field_y": STRING
            ),
            metas = mapOf("CONTAINS_EXCLUDED_FIELD" = true)
        )
    ),
    metas = mapOf("CONTAINS_EXCLUDED_FIELD" = true)
),
```

When the `RedshiftRewriter` then visits a variable reference or path plan node that references a `ROW` field with this
meta, it will call the `RedshiftExcludeUtils` function `rewriteToObjectTransform`. This utility basically rewrites
any variable references and paths to use `OBJECT_TRANSFORM` to explicitly include any fields and nested fields we wish
to keep. Taking the `ROW` type from above from the projection, we will transform that `ROW` type into the following
`OBJECT_TRANSFORM` call:

```Redshift
OBJECT_TRANSFORM(
    t.flds
    KEEP
        '"a"',
        '"c.field_y"'
)
```
Notice how we `KEEP` the path `'"c.field_y"'` rather than `'"c"'` since `c.field_x` was excluded from the query. Also,
notice how we do not expand `'"a"'` since none of its subfields were excluded. This can help eliminate some redundant
`KEEP` paths in the query.

The overall query would look something like:
```Redshift
SELECT
    OBJECT_TRANSFORM(
        "t"."flds" 
        KEEP
            '"a"',
            '"c"."field_y"'
    ) AS "flds", 
    "t"."foo" AS "foo"
FROM "EXCLUDE_T" AS "t"
```

### Other Edge Cases
#### Omitting every field within a nested struct

Taking the Redshift catalog defined above, if we were to explicitly omit every nested field within `flds.c` but retain
`flds.c` in the output. The PartiQL query would look something like

```PartiQL
SELECT * EXCLUDE t.flds.c.field_x, t.flds.c.field_y FROM EXCLUDE_T AS t
```

The simplest way to create an empty `OBJECT` would be to call the `OBJECT` function with no arguments. So for any struct
where all its subfields are empty, we can use the `SET` argument of `OBJECT_TRANSFORM`. The rewrite in Redshift would
be:

```Redshift
SELECT
    OBJECT_TRANSFORM(
        "t"."flds"
         KEEP
            '"a"',
            '"b"',
            '"c"'
         SET
            '"c"', OBJECT()
    ) AS "flds",
    "t"."foo" AS "foo"
FROM "EXCLUDE_T" AS "t"
```

#### Omitting every field of a top-level SUPER OBJECT column
If a user were to omit every field of a top-level SUPER OBJECT column, then the simplest query would be to just return
back an empty `OBJECT` created through a function call.

For example with the same catalog from above,
```PartiQL
SELECT * EXCLUDE t.flds.a, t.flds.b, t.flds.c FROM EXCLUDE_T AS t;
```

Would get transpiled to:
```Redshift
SELECT
    OBJECT() AS "flds", 
    "t"."foo" AS "foo"
FROM "default"."EXCLUDE_T" AS "t"
```
