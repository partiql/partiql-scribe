# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

<!-- Template: after a release, copy and paste out below
## [Unreleased](https://TODO.com) - YYYY-MM-DD

### Added

### Changed

### Deprecated

### Fixed

### Removed

### Security

### Contributors
Thank you to all who have contributed!
-->

## [Unreleased](https://TODO.com) - YYYY-MM-DD

### Added
- Adds an experimental DuckDB SQL transpilation target, with the same PartiQL feature coverage as the Redshift, Spark, and Trino targets.

### Changed

### Deprecated

### Fixed
- Integer division truncates toward zero like PartiQL on Spark (`DIV`) and DuckDB (`//`) instead of returning a fraction.
- Correlated join `ON` predicates are combined with an existing `WHERE` instead of overwriting it (Trino, Spark, DuckDB).
- Nested array indexes (`a[0].b[0]`) are all shifted to 1-based on Trino and DuckDB, not just the outermost one.
- `EXISTS (<subquery>)` is rendered as a native `EXISTS` predicate on every target instead of invalid SQL.
- SQL targets reject a `SELECT VALUE` that can't be rewritten to a SELECT list instead of emitting `SELECT VALUE`.
- DuckDB target: `IN` / `NOT IN` keep SQL's NULL result when there is no match and the list or array contains a NULL.
- Redshift target: `IN` / `NOT IN` keep SQL's NULL result for a list with a NULL literal and for SUPER array membership (`array_contains`).

### Removed

### Security

### Contributors
Thank you to all who have contributed!
- @xd1313113

## [2.0.2](https://github.com/partiql/partiql-scribe/releases/tag/v2.0.2) - 2026-09-14

### Added

### Changed

### Deprecated

### Fixed
- Transpile array-membership `<value> IN <array-expr>` to `contains` (Trino) / `array_contains` (Spark, Redshift) instead of the SQL `IN` operator, which those engines reject for a non-list/subquery right side.

### Removed

### Security

### Contributors
Thank you to all who have contributed!
- @xd1313113

## [2.0.1](https://github.com/partiql/partiql-scribe/releases/tag/v2.0.1) - 2026-09-10

### Added

### Changed

- Updated the build to Kotlin 2.3.21.
- Changed `ScribeProblemListener.reportAndThrow` from an abstract method to a JVM default method to retain binary compatibility.

### Deprecated

### Fixed

### Removed

### Security

### Contributors
Thank you to all who have contributed!
- @XuechunHHH

## [2.0.0](https://github.com/partiql/partiql-scribe/releases/tag/v2.0.0) - 2026-09-09

### Added

- Added the initial Maven Central distribution of the Scribe transpiler framework, including PartiQL, Redshift, Spark, and Trino SQL targets.
- Added the optional `partiql-scribe-function-extensions` artifact with Redshift, Spark, and Trino translations for PartiQL function extensions.

### Changed

- Established `2.0.0` as the first Maven release line, with `scribe` and `partiql-scribe-function-extensions` sharing one version and release lifecycle.

### Deprecated

### Fixed

### Removed

### Security

### Contributors
Thank you to all who have contributed!
- @alancai98
- @johnedquinn
- @rchowell
- @xd1313113
- @XuechunHHH
- @yliuuuu
