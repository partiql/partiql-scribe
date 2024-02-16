package org.partiql.scribe.targets.trino

import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.scribe.sql.SqlFeatures

public open class TrinoFeatures : SqlFeatures.Defensive() {

    override val allow: Set<Class<*>> = setOf(
        //
        // Rel
        //
        Rel.Op.Scan::class.java,
        Rel.Op.Project::class.java,
        Rel.Op.Sort::class.java,
        Rel.Op.Filter::class.java,
        Rel.Op.Join::class.java,
        Rel.Op.Limit::class.java,
        Rel.Op.Offset::class.java,
        Rel.Op.Exclude::class.java,
        Rel.Op.Exclude.Item::class.java,
        Rel.Op.Exclude.Step.StructField::class.java,
        Rel.Op.Exclude.Step.CollWildcard::class.java,
        //
        // Rex
        //
        Rex.Op.Call::class.java,
        Rex.Op.Call.Static::class.java,
        Rex.Op.Case::class.java,
        Rex.Op.Case.Branch::class.java,
        Rex.Op.Collection::class.java,
        Rex.Op.Global::class.java,
        Rex.Op.Lit::class.java,
        Rex.Op.Path::class.java,
        Rex.Op.Path.Index::class.java,
        Rex.Op.Path.Key::class.java,
        Rex.Op.Path.Symbol::class.java,
        Rex.Op.Select::class.java,
        Rex.Op.Subquery::class.java,
        Rex.Op.Struct::class.java,
        Rex.Op.Struct.Field::class.java,
        Rex.Op.TupleUnion::class.java,
        Rex.Op.Var::class.java,
    )
}
