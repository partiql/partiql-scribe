# agents.md

This file provides guidance to AI coding agents when working with code in this repository.

## Project Overview

PartiQL Scribe is a Kotlin/JVM transpiler framework. It takes a resolved, typed PartiQL logical plan from [partiql-lang-kotlin](https://github.com/partiql/partiql-lang-kotlin) and compiles it for a _target_. The built-in targets produce SQL text for PartiQL, Redshift, Spark SQL, Trino, and DuckDB (experimental). It is published to Maven Central as `org.partiql:scribe` and `org.partiql:partiql-scribe-function-extensions`; both share the version in `gradle.properties`.

## Build Commands

**Requires JDK 17** (CI uses Corretto 17). With JDK 21 the build fails in the `partiql-lang-kotlin` submodule ("Unknown Kotlin JVM target: 21"); set `JAVA_HOME` to a JDK 17.

```bash
# Initialize the partiql-lang-kotlin submodule (required; the build includes it)
git submodule update --init --recursive

# Build everything: compile, ktlint, API check, and all tests
./gradlew build

# Run all tests
./gradlew test

# Run one target's suite (use the root-project path `:test`; a bare `test --tests ...`
# also runs in the function-extensions subproject and fails with "No tests found")
./gradlew :test --tests "org.partiql.scribe.targets.redshift.RedshiftTargetSuite"

# Run the function-extensions module's tests
./gradlew :partiql-scribe-function-extensions:test

# Lint
./gradlew ktlintCheck
./gradlew ktlintFormat

# Public API check against api/*.api; after an intentional public API change, update the files with apiDump
./gradlew apiCheck
./gradlew apiDump
```

Before opening a PR, make sure `./gradlew apiCheck ktlintCheck` and the tests pass.

## Architecture

### Compilation pipeline

`Scribe.compile(plan, session, target)` hands the plan to a `ScribeTarget<T>`. For SQL targets, `SqlTarget.compile` runs:

1. **Feature validation**: `SqlFeatures.validate` reports unsupported plan operators. `SqlFeatures.Defensive` denies everything not on its allow list; the built-in targets currently use `SqlFeatures.Permissive`.
2. **Plan rewrite**: `SqlTarget.rewrite` applies target-specific plan-to-plan rewrites, usually a `<Target>Rewriter` built on partiql-lang-kotlin's `OperatorRewriter`.
3. **Plan to AST**: `PlanToAst` uses a `RelConverter` (relational operators to `ExprQuerySet`) and a `RexConverter` (row expressions to `Expr`), with `SqlCalls` rewriting scalar function calls and operators.
4. **AST to text**: `AstToSql` prints the AST as SQL in the target dialect.

Errors are reported through `ScribeContext.getProblemListener()` as `ScribeProblem`s and thrown as `ScribeException`. The codes are `UNSUPPORTED_OPERATION`, `UNSUPPORTED_PLAN_TO_AST_CONVERSION`, `UNSUPPORTED_AST_TO_TEXT_CONVERSION`, `INVALID_PLAN`, `INTERNAL_ERROR`, and `TRANSLATION_INFO`.

### Source layout (`src/main/kotlin/org/partiql/scribe/`)

- Top level: the public entry points (`Scribe`, `ScribeTarget`, `ScribeContext`, `ScribeOutput`).
- `problems/`: `ScribeProblem`, `ScribeException`, `ScribeProblemListener`, and `Severity`.
- `sql/`: the shared SQL pipeline (`SqlTarget`, `SqlFeatures`, `PlanToAst`, `RelConverter`, `RexConverter`, `SqlCalls`, `AstToSql`) and `sql/utils/` (identifier and backslash escaping, conversion helpers, window-subquery checks).
- `targets/<target>/`: one package per dialect, each with `<Target>Target`, `<Target>Features`, `<Target>Rewriter`, `<Target>PlanToAst`, `<Target>RelConverter`, `<Target>RexConverter`, `<Target>Calls`, and `<Target>AstToSql`.
  - `targets/partiql/` is a single `PartiQLTarget`.
  - The Redshift, Spark, Trino, and DuckDB packages each also have an `<Target>ExcludeTranspilation.md` design note for the `EXCLUDE` rewrite.
- A target-specific change belongs in that target's classes. Change the shared `sql/` classes only for behavior every SQL target should get.

### Other modules and directories

- `partiql-scribe-function-extensions/`: an optional artifact with per-target translations (`<Target>FunctionExtensionCalls`) of partiql-lang-kotlin's `partiql-function-extensions` functions.
- `partiql-lang-kotlin/`: a git submodule that `settings.gradle.kts` adds with `includeBuild`, so the `org.partiql:*` dependencies are built from the submodule's commit, not downloaded. The published POMs depend on `partiqlVersion` from `gradle.properties`, so keep it equal to the submodule's release: when upgrading partiql-lang-kotlin, check out its release tag in the submodule and update `partiqlVersion` in the same change.
- `docs/`: `support.adoc`, `taxonomy.adoc`, and per-target docs in `docs/targets/` (Redshift, Spark, Trino).
- `catalogs/tpc_ds/`: TPC-DS table schemas (Ion).

## Testing

Tests are JUnit 5. Each target has a suite (`src/test/kotlin/org/partiql/scribe/targets/<target>/<Target>TargetSuite.kt`, built on `SqlTargetSuite`) that transpiles shared PartiQL inputs and compares the result with expected SQL:

- **Inputs**: `src/test/resources/inputs/<group>/*.sql`. Each test is named with `--#[test-name]` and ends with `;`. A test is identified by its group (the directory name) and name, so names must be unique within a group; one file can hold many tests.
- **Expected outputs**: `src/test/resources/outputs/<target>/<group>/*.sql`, with the same group directory and `--#[test-name]` keys. A target gets a test only for the keys in its output files. The comparison (`SqlEqualsNaive`) trims each line and ignores blank lines, `--` comment lines, and a trailing `;`, so expected files can keep notes in `--` comments; the generated SQL itself must not contain comments.
- **Expected errors**: write the exception instead of SQL, e.g. `[ScribeException{code=UNSUPPORTED_OPERATION}];` or with a message, `[ScribeException{code=UNSUPPORTED_PLAN_TO_AST_CONVERSION, message="..."}];`.
- **Table schemas**: `src/test/resources/catalogs/default/` holds the test tables as Ion schemas (see the README appendix); the file name is the table name.
- **Sandbox**: `SandboxTargetSuite` transpiles `inputs/sandbox/sandbox.sql` with `PartiQLTarget` for quick experiments.
- **Function extensions**: `FunctionExtensionTargetSuite` uses `inputs/function-extensions/` and `outputs/function-extensions/<target>/`.
- Focused unit tests for converters and utilities are in `src/test/kotlin/org/partiql/scribe/sql/`.

To add a test, add the input under `inputs/<group>/`, then add the expected SQL (or expected error) for each target under `outputs/<target>/<group>/`.

## Key Conventions

- **Explicit API mode** is strict for both modules: public declarations need explicit visibility and return types. Changes to the public API must be reflected in `api/Scribe.api` and `partiql-scribe-function-extensions/api/*.api` (`./gradlew apiDump`).
- **JVM target is 1.8**, compiled with Kotlin 2.3 and `-jvm-default=enable`.
- The Redshift, Spark, Trino, and DuckDB targets are open classes with a `STANDARD` instance (e.g. `TrinoTarget.STANDARD`); `PartiQLTarget` is an `object`. Their rulesets (`SqlCalls`, `AstToSql`, `RexConverter`, …) are open so users can subclass them to override behavior; keep new extension points `open` and avoid binary-incompatible changes.
- Commit messages and PR titles follow conventional commits, with an optional target or module scope: `fix(redshift): …`, `feat(duckdb): …`, `feat(function-extensions): …`, `chore: …`.

## Changelog

The changelog is managed with [changie](https://changie.dev). `CHANGELOG.md` and the release files `.changes/<version>.md` are generated at release time; never edit them by hand.

### Adding an entry for a new change

1. Decide whether the change needs an entry. Add one for any user-facing change: new or changed public API, generated SQL, supported features, errors, or a bug fix. Changes to tests, CI, build scripts, or internal refactoring with no visible effect don't need one.
2. Pick the kind, matching the case exactly: `Added`, `Changed`, `Deprecated`, `Fixed`, `Removed`, or `Security`. Use `Removed` only to remove stable public API. It marks the release as a major version, so changes to behavior or to experimental features go under `Changed`.
3. Add one file under `.changes/unreleased/` for each change. A PR with several changes adds several files. Either run:
   ```bash
   changie new -k Fixed -b "Fixed ..." -m Author=<github-username>
   ```
   or write the file directly, with any name ending in `.yaml`:
   ```yaml
   kind: Fixed
   body: "Redshift target: `UPPER` and `LOWER` now cast strings read from SUPER values to VARCHAR."
   time: 2026-11-23T10:00:00Z
   custom:
     Author: <github-username>
   ```
4. Keep the `body` concise, preferably one line: say what changed for users, not how it was implemented. Implementation details, examples, and edge cases belong in the PR description. Use past tense ("Added …", "Fixed …"), start with the affected targets when the change is target-specific ("Redshift target: …", "Spark and Trino targets: …", "All SQL targets: …"), name the affected API or SQL in backticks, and link issues as `([#1234](https://github.com/partiql/partiql-scribe/issues/1234))`. Start the entry with `**BREAKING**:` for a backward-incompatible change and `**EXPERIMENTAL**:` for an experimental feature.
   - Good: ``Redshift target: `UPPER` and `LOWER` now cast strings read from SUPER values to VARCHAR.``
   - Too long: a paragraph listing every affected target, function, rewrite rule, and edge case.
5. Check the output with `changie batch auto --dry-run`, which prints the next release notes without changing any files.

changie doesn't validate these rules when it reads a hand-written file, so follow them exactly:
- Quote the `body` value. Most entries contain `: ` ("Redshift target: …", "**BREAKING**: …"), which is a YAML error when unquoted; `changie new` quotes it for you.
- Put exactly one change in each file. Don't use `---` to put several YAML documents in one file; changie silently drops every document after the first.
- Use a non-empty `body` and a non-empty `custom.Author` (spelled exactly). A missing author shows up as a bare `- @` under Contributors.
- Avoid multi-line bodies. If a migration note is really needed, add it as a sub-bullet indented by two spaces; a line starting with `- ` that isn't indented renders as a separate entry.
