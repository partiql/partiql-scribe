This document goes over one of the more complicated rewrites for PartiQL's `EXCLUDE` to Redshift.

### Redshift Background

#### SUPER OBJECT
The [Redshift `OBJECT` type](https://docs.aws.amazon.com/redshift/latest/dg/r_SUPER_type.html) is what is most similar
to PartiQL's struct type. From the docs, there are a few ways to create an `OBJECT`.

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
reconstructing an `OBJECT` using the `OBJECT` function proves to have worse performance than the following approach.

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

As brought up in [partiql-scribe#52](https://github.com/partiql/partiql-scribe/issues/52), modifying `OBJECT`s using
`OBJECT_TRANSFORM` has shown to have a lot better performance than reconstructing `OBJECT`s using the `OBJECT` function.
So in the rewrite that follows, we will primarily be using `OBJECT_TRANSFORM`.

### PartiQL Rewrite
This section will go over a PartiQL rewrite of nested `EXCLUDE` paths (e.g. `EXCLUDE t.a.b.c.`) to Redshift.

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

We get the following typed plan from partiql-lang-kotlin: (with simplified syntax)
```
-- other parts not relevant for this rewrite
   RexSelect(
      constructor = RexVar(0),
      rel = RelProject(
         type = <relation `t` with exclude paths applied>
         projections = RexTupleUnion(RexVar(0)),
         input = RelExclude(
            type = <relation `t` with exclude paths applied>
            excludePaths = listOf(<representation of t.flds.b>, <representation of t.flds.c.field_x>),
            input = RelScan(
               type = <`EXCLUDE_T`'s type from catalog as relation `t`>
               ... -- not relevant for this rewrite
            )
         )
      )
   )
```

The `PlanTyper` pass in partiql-lang-kotlin will alter `Rel.Type` after the `Rel.Op.Exclude` to omit those nested 
fields from the static type. In the Scribe rewrite, we will perform a similar pass but add a meta to all struct types 
with an excluded field or subfield. When the struct is projected back out, this meta will indicate that we should 
recursively list out each field.

In the above example, the StructType will be
```
Struct(  -- for `flds`
   fields = {
      'a': Struct(
         fields = {
            'field_x': INT4,
            'field_y': VARCHAR
         },
         metas = {}  -- empty metas since no field or nested field is excluded
      ),
      -- b is excluded so include meta "EXPAND" in all encompasing structs
      'c': Struct(
         fields = {
            -- field_x is excluded so include meta "EXPAND" in its struct and encompasing structs
            'field_y': VARCHAR
         },
         metas = { 'EXPAND' = true }
      ),
   },
   metas = { 'EXPAND' = true }
)
```

We will no longer will have the `RelExclude` in the plan and will use the struct type with updated metas. Rewritten plan

```
-- other parts not relevant for this rewrite
   RexSelect(
      constructor = RexVar(0),
      rel = RelProject(
         type = <relation `t` with exclude paths applied AND struct types with 'EXPAND' meta>
         projections = RexTupleUnion(RexVar(0)),
         input =  RelScan(
            type = <`EXCLUDE_T`'s type from catalog as relation `t`>
            ... -- not relevant for this rewrite
         )
      )
   )
```

When we project back out any `Rex.Op.Var` and `Rex.Op.Path`, we will expand any structs that have this "EXPAND" meta
to use `OBJECT_TRANSFORM` to explicitly include any fields and nested fields we wish to keep. Taking the `StructType`
example from above with the "EXPAND" metas, we will transform that `StructType` into the following `OBJECT_TRANSFORM`
call

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
