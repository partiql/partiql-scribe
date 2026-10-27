-- Navigation of SUPER values: field access with a dot and element access with brackets.
-- https://docs.aws.amazon.com/redshift/latest/dg/query-super.html

--#[field]
SELECT s.a, s.a.b, s.a.b.c FROM t;

--#[field-qualified-column]
SELECT t.s.a, t.s.a.b FROM t;

--#[element]
SELECT s[0], s[1][2], s[n], s[n + 1] FROM t;

--#[element-string-key]
SELECT s['key'], s['a']['b'] FROM t;

--#[mixed]
SELECT s.items[0].name, s[0].a[1].b, t.s.list[i].value FROM t;

--#[quoted-field]
SELECT s."Mixed Case".a, t."col"."Field" FROM t;

--#[keyword-field]
SELECT s.date, s.time, s.count, s.year FROM t;

--#[path-in-condition]
SELECT * FROM t WHERE s.a.b = 1 AND s.items[0] IS NOT NULL AND s.tags[1] IN ('x', 'y');

--#[path-in-function]
SELECT UPPER(s.name), COUNT(s.items[0]), COALESCE(s.a.b, s.c) FROM t;

--#[path-with-cast]
SELECT s.a::INT, s.items[0].price::DECIMAL(10, 2), CAST(s.a.b AS VARCHAR) FROM t;

--#[path-with-operators]
SELECT s.a + s.b, s.items[0].qty * s.items[0].price, s.first || ' ' || s.last FROM t;

--#[path-in-group-order]
SELECT s.a, COUNT(*) FROM t GROUP BY s.a ORDER BY s.a;

--#[path-of-unnested]
SELECT i.name, i.tags[0] FROM t, t.items AS i WHERE i.price > 0;
