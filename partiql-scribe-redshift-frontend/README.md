# PartiQL Scribe Redshift front end

A front end that reads Amazon Redshift SQL for translation to PartiQL. This module currently contains the Redshift
grammar and its tests; the translation follows in later changes.

## Grammar

`src/main/antlr/Redshift.g4` is an ANTLR 4 grammar of the Redshift *query* language: `SELECT` with its clauses,
`WITH`, and the set operations. It is case-insensitive, follows PostgreSQL operator precedence (from which Redshift
derives), and reports a syntax error with a line and column for anything else. See the grammar's header comment and
the pull request that introduced it for the list of what is and is not recognized.

A few constructs are recognized without being analyzed, so that the translation can report them as unsupported with a
location instead of a syntax error: `PIVOT`, `UNPIVOT`, `CONNECT BY`, `ROLLUP`, `CUBE`, `GROUPING SETS`,
`OBJECT_TRANSFORM`, and `INTO`. Their contents are matched as balanced token sequences.

The generated parser is in the package `org.partiql.scribe.frontend.redshift.antlr`; it is an implementation detail
and not part of the module's API.

## Tests

- `src/test/resources/grammar/accept/*.sql`: statements the grammar parses, one file per area of the grammar, each
  statement under a `--#[name]` marker.
- `src/test/resources/grammar/reject/*.sql`: statements the grammar refuses, each marker carrying the expected
  `line:column` of the first syntax error: `--#[name] 1:17`.
- `RedshiftParseTreeTest`: the shape of the parse tree where the grammar has to choose (operator precedence, set
  operation precedence, keywords that are also identifiers, literal prefixes).

```
./gradlew :partiql-scribe-redshift-frontend:test
```

The build needs JDK 17.
