package group.aelysium.rustyconnector.modules.mysql.lib;

import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.Filterable;
import org.jetbrains.annotations.NotNull;

public class MySQLFilterable extends Filterable {
    public @NotNull String toWhereClause() {
        if (this.filterBy().isEmpty()) return "";

        StringBuilder clause = new StringBuilder();
        clause.append(" WHERE ");
        boolean isFirst = true;
        for (KeyValue<String, FilterValue> e : this.filterBy()) {
            if (!isFirst) clause.append(" AND ");
            if(isFirst) isFirst = false;

            clause.append(e.key())
                    .append(" ")
                    .append(getSQLQualifier(e.value().equality()))
                    .append(" ");

            if (e.value().equality() == Qualifier.CONTAINS || e.value().equality() == Qualifier.NOT_CONTAINS) {
                clause.append("'%")
                        .append(e.value().value())
                        .append("%'");
                continue;
            }

            clause.append("?");
        }

        return clause.toString();
    }

    public @NotNull String toGroupByClause() {
        if (this.groupBy().isEmpty()) return "";

        StringBuilder clause = new StringBuilder();
        clause.append(" GROUP BY ");
        int count = 0;
        for (String column : this.groupBy()) {
            if (count > 0) clause.append(", ");
            clause.append(column);
            count++;
        }

        return clause.toString();
    }

    public @NotNull String toOrderByClause() {
        if (this.orderBy().isEmpty()) return "";

        StringBuilder clause = new StringBuilder();
        clause.append(" ORDER BY ");
        int count = 0;
        for (KeyValue<String, Ordering> order : this.orderBy()) {
            if (count > 0) {
                clause.append(", ");
            }
            clause.append(order.key())
                    .append(" ")
                    .append(order.value() == Ordering.ASCENDING ? "ASC" : "DESC");
            count++;
        }

        return clause.toString();
    }

    private String getSQLQualifier(Qualifier qualifier) {
        return switch (qualifier) {
            case EQUALS -> "=";
            case NOT_EQUALS -> "!=";
            case CONTAINS -> "LIKE";
            case NOT_CONTAINS -> "NOT LIKE";
            case GREATER_THAN -> ">";
            case LESS_THAN -> "<";
            case GREATER_THAN_OR_EQUAL -> ">=";
            case LESS_THAN_OR_EQUAL -> "<=";
            case IS_NULL -> "IS NULL";
            case IS_NOT_NULL -> "IS NOT NULL";
        };
    }
}
