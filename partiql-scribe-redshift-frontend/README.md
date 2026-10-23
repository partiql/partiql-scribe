# PartiQL Scribe Redshift Frontend

Translates Amazon Redshift queries to PartiQL. Scribe targets translate PartiQL to other dialects; this module goes
the other way for Redshift, and reuses the PartiQL planner and the Scribe PartiQL target to do it.

This is the first part of the module: the Redshift grammar (`src/main/antlr/Redshift.g4`), the parser with its syntax
errors, and `RedshiftTranspiler.tables(sql)`, which lists the tables a query reads so that a caller can fetch their
schemas and build the session of the translation. The translation itself follows.

```kotlin
RedshiftTranspiler().tables("SELECT c FROM andes.provider.orders o JOIN other.t ON o.id = t.id")
// [[andes, provider, orders], [other, t]]
```

## Identifiers

By default, quoted and unquoted identifiers fold to lower case, as with the Redshift default
`enable_case_sensitive_identifier = false`. Use `RedshiftTranspiler(caseSensitiveIdentifiers = true)` for sources that
set it to true: then only unquoted identifiers fold, and names match exactly. Only the ASCII letters A to Z fold, as in
Redshift.
