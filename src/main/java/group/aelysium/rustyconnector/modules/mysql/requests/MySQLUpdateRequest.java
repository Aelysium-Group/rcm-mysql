package group.aelysium.rustyconnector.modules.mysql.requests;

import group.aelysium.rustyconnector.modules.mysql.MySQLDatabase;
import group.aelysium.rustyconnector.modules.mysql.lib.MySQLFilterable;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.exceptions.HazeException;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.Filterable;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.query.UpdateRequest;
import org.jetbrains.annotations.NotNull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MySQLUpdateRequest extends UpdateRequest {
    protected MySQLFilterable filters = new MySQLFilterable();

    public MySQLUpdateRequest(
            @NotNull MySQLDatabase database,
            @NotNull String target
    ) {
        super(database, target);
    }

    public Filterable filters() {
        return this.filters;
    }

    @Override
    public long execute() throws Exception {
        StringBuilder query = new StringBuilder("UPDATE ").append(target).append(" SET ");

        boolean first = true;
        for (String column : parameters.keySet()) {
            if (!first) query.append(", ");
            query.append(column).append(" = ?");
            first = false;
        }

        query.append(filters.toWhereClause());

        try (
                Connection connection = ((MySQLDatabase) this.database).dataSource().getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(query.toString())
        ) {
            int index = 1;

            for (Object value : parameters.values())
                preparedStatement.setObject(index++, value);

            if (!filters.filterBy().isEmpty())
                for (Filterable.KeyValue<String, Filterable.FilterValue> filter : filters.filterBy())
                    preparedStatement.setObject(index++, filter.value().value());

            return preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new HazeException("Error executing update request. " + e.getMessage());
        }
    }
}
