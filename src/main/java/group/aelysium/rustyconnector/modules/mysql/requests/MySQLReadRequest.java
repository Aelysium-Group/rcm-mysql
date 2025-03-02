package group.aelysium.rustyconnector.modules.mysql.requests;

import group.aelysium.rustyconnector.modules.mysql.MySQLDatabase;
import group.aelysium.rustyconnector.modules.mysql.lib.MySQLFilterable;
import group.aelysium.rustyconnector.shaded.com.google.code.gson.gson.JsonSyntaxException;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.exceptions.HazeCastingException;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.exceptions.HazeException;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.Filterable;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.query.ReadRequest;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MySQLReadRequest extends ReadRequest {
    protected MySQLFilterable filters = new MySQLFilterable();

    public MySQLReadRequest(
            @NotNull MySQLDatabase database,
            @NotNull String target
    ) {
        super(database, target);
    }

    public Filterable filters() {
        return this.filters;
    }

    @Override
    public <T> @NotNull Set<T> execute(Class<T> clazz) throws Exception {
        StringBuilder query = new StringBuilder("SELECT * FROM ").append(target);

        query.append(this.filters.toWhereClause());
        query.append(this.filters.toGroupByClause());
        query.append(this.filters.toOrderByClause());

        try (
                Connection connection = ((MySQLDatabase) this.database).dataSource().getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(query.toString())
        ) {
            if (!filters.filterBy().isEmpty()) {
                int index = 1;
                for (Filterable.KeyValue<String, Filterable.FilterValue> filter : filters.filterBy())
                    preparedStatement.setObject(index++, filter.value().value());
            }

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                Set<T> response = new HashSet<>();
                while (resultSet.next()) {
                    ResultSetMetaData metadata = resultSet.getMetaData();
                    Map<String, Object> rows = new HashMap<>();
                    for (int i = 1; i <= metadata.getColumnCount(); i++) {
                        String columnName = metadata.getColumnName(i);
                        Object value = resultSet.getObject(i);
                        rows.put(columnName, value);
                    }
                    String jsonString = MySQLDatabase.gson.toJson(rows);

                    try {
                        response.add(MySQLDatabase.gson.fromJson(jsonString, clazz));
                    } catch (JsonSyntaxException e) {
                        throw new HazeCastingException("Unable to deserialize the response from "+this.target+". "+e.getMessage());
                    }
                }
                return response;
            }
        } catch (SQLException e) {
            throw new HazeException("Error executing read request. " + e.getMessage());
        }
    }
}
