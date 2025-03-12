package group.aelysium.rustyconnector.modules.mysql.lib;

import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.Filter;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.KeyValue;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.Orderable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Converter {
    static @NotNull String convert(@NotNull Filter.Operator operator) {
        return switch (operator) {
            case AND -> "AND";
            case OR -> "OR";
            case AND_NOT -> "AND NOT";
            case OR_NOT -> "OR NOT";
            case EXCLUSIVE_OR -> "XOR";
        };
    }
    
    static @NotNull String convert(@NotNull Filter.Qualifier qualifier) {
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
    
    static @NotNull String convert(@NotNull Filter filter) {
        filter.resetPointer();
        
        StringBuilder clause = new StringBuilder();
        
        while (filter.next()) {
            KeyValue<Filter.Operator, KeyValue<String, Filter.Value>> entry = filter.get();
            String columnName = entry.value().key();
            Filter.Value filterWith = entry.value().value();
            
            boolean isFirst = entry.key() == null;
            
            if (!isFirst)
                clause
                    .append(" ")
                    .append(convert(entry.key()))
                    .append(" ");
            
            clause.append(columnName)
                .append(" ")
                .append(convert(filterWith.equality()))
                .append(" ");
            
            if (filterWith.equality() == Filter.Qualifier.CONTAINS || filterWith.equality() == Filter.Qualifier.NOT_CONTAINS) {
                clause.append("'%")
                    .append(filterWith.value())
                    .append("%'");
                continue;
            }
            
            clause.append("?");
        }
        
        if (clause.isEmpty()) return "";
        
        return " WHERE " + clause;
    }
    
    static @NotNull String convert(int startAt, int endAt) {
        StringBuilder queryBuilder = new StringBuilder();
        
        if (startAt != -1) queryBuilder.append(" OFFSET ").append(startAt);
        if (endAt != -1) queryBuilder.append(" LIMIT ").append(endAt - startAt + 1);
        
        return queryBuilder.toString();
    }
    
    static @NotNull String convert(List<KeyValue<String, Orderable.Ordering>> orderBy) {
        if (orderBy == null) return "";
        if (orderBy.isEmpty()) return "";
        
        StringBuilder clause = new StringBuilder();
        clause.append(" ORDER BY ");
        int count = 0;
        for (KeyValue<String, Orderable.Ordering> order : orderBy) {
            if (count > 0) {
                clause.append(", ");
            }
            clause.append(order.key())
                .append(" ")
                .append(order.value() == Orderable.Ordering.ASCENDING ? "ASC" : "DESC");
            count++;
        }
        
        return clause.toString();
    }
}
