# agents.md

This file provides guidance to AI coding agents when working with code in this repository.

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
