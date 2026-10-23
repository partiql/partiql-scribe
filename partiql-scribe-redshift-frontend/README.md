# PartiQL Scribe Redshift Frontend

Translates Amazon Redshift queries to PartiQL. Scribe targets translate PartiQL to other dialects; this module goes
the other way for Redshift, and reuses the PartiQL planner and the Scribe PartiQL target to do it.

This is the first part of the module: the Redshift grammar (`src/main/antlr/Redshift.g4`), the parser with its syntax
errors, `RedshiftTranspiler.tables(sql)`, which lists the tables a query reads so that a caller can fetch their
schemas and build the session of the translation, and `RedshiftTranspiler.transpile(sql, session)` with its contract,
which refuses every well-formed query as unsupported until the translation follows. The conformance suite that the
translation is built against is complete; see [Tests](#tests).

```kotlin
RedshiftTranspiler().tables("SELECT c FROM andes.provider.orders o JOIN other.t ON o.id = t.id")
// [[andes, provider, orders], [other, t]]
```

## Identifiers

By default, quoted and unquoted identifiers fold to lower case, as with the Redshift default
`enable_case_sensitive_identifier = false`. Use `RedshiftTranspiler(caseSensitiveIdentifiers = true)` for sources that
set it to true: then only unquoted identifiers fold, and names match exactly. Only the ASCII letters A to Z fold, as in
Redshift.

## Tests

- `RedshiftTranspilerSuite` is the conformance suite: each case is a Redshift input in
  `src/test/resources/redshift-frontend/inputs` and the expected result of the same name in `outputs`. An expected
  output is PartiQL text, or a status with a message for a failed translation. A case whose marker is prefixed
  `-- --#[name] TODO(redshift)` waits for the change that translates its construct, which enables it; the change that
  enables a case is reviewed against its expected output. Whatever its marker, every input is parsed, so the grammar
  covers the whole suite.

```shell
./gradlew :partiql-scribe-redshift-frontend:test
```
