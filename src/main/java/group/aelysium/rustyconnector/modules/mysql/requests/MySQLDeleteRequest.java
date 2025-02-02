package group.aelysium.rustyconnector.modules.mysql.requests;

import group.aelysium.rustyconnector.modules.mysql.MySQLDatabase;
import group.aelysium.rustyconnector.modules.mysql.lib.MySQLFilterable;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.exceptions.HazeException;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.Filterable;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.query.DeleteRequest;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MySQLDeleteRequest extends DeleteRequest {
    protected MySQLFilterable filters = new MySQLFilterable();

    public MySQLDeleteRequest(
            @NotNull MySQLDatabase database,
            @NotNull String target
    ) {
        super(database, target);
    }

    public Filterable filters() {
        return this.filters;
    }

    @Override
    public void execute() throws Exception {
        String query = "DELETE FROM " + target + filters.toWhereClause();

        try (
                Connection connection = ((MySQLDatabase) this.database).dataSource().getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            if (!filters.filterBy().isEmpty()) {
                int index = 1;
                for (Filterable.KeyValue<String, Filterable.FilterValue> filter : filters.filterBy())
                    preparedStatement.setObject(index++, filter.value().value());
            }

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new HazeException("Error executing delete request. " + e.getMessage());
        }
    }
}
