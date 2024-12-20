# Scribe

## Features

* [ ] feature allow-list
* [ ] error handling mechanisms (I just panic)
* [ ] tag with version/commit metadata
* [ ] smithy ast model
* [ ] repl needs persisting DDL

## Commands

```
# generate support docs
./gradlew docs

# use JDK 17
./gradlew run

# install bundles
./gradlew install
```

## Design Notes

* Using sealed kotlin classes because Scribe core is internalized.

## Scribe IR

This section attempts to clarify the mapping the from PartiQL and SQL grammar to Scribe's generalized IR; this is
tricky in practice – any feedback to improve clarity or add missing details is greatly appreciated!

### SqlNode

The abstract base class for Scribe IR is "SqlNode". In code, it's easy to alias an import, but in docs it's precise to
say SqlNode and AstNode when talking about the Scribe IR and PartiQL AST respectively. So we've chosen this to reduce
confusion; this term is also more generic and is borrowed from Calcite.

### SqlStatement

| Statement     | Category | Class            |
|---------------|----------|------------------|
| [WITH] SELECT | DQL      | `SqlSelect`      |
| INSERT        | DML      | `SqlInsert`      |
| UPDATE        | DML      | `SqlUpdate`      |
| DELETE        | DML      | `SqlDelete`      |
| CREATE TABLE  | DDL      | `SqlCreateTable` |
| CREATE VIEW   | DDL      | `SqlCreateView`  |

> SQL-99 does not define a SELECT statement under SQL-data statements (DQL), only `<select statement: single row>`.

### SqlSelect

> For now, the Scribe SELECT statement does not support a top-level expression since this is not a requirement.

### SqlQuery

* SqlQuerySpec
* SqlQuerySetOp
