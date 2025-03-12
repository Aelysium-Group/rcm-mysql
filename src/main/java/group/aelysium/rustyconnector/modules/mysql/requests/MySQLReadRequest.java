package group.aelysium.rustyconnector.modules.mysql.requests;

import group.aelysium.rustyconnector.modules.mysql.MySQLDatabase;
import group.aelysium.rustyconnector.modules.mysql.lib.Converter;
import group.aelysium.rustyconnector.shaded.com.google.code.gson.gson.JsonSyntaxException;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.exceptions.HazeCastingException;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.exceptions.HazeException;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.Filter;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.KeyValue;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.requests.ReadRequest;
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
    public MySQLReadRequest(
            @NotNull MySQLDatabase database,
            @NotNull String target
    ) {
        super(database, target);
    }

    @Override
    public <T> @NotNull Set<T> execute(Class<T> clazz) throws Exception {
        String query = "SELECT * FROM " + target +
            Converter.convert(this.filter) +
            Converter.convert(this.orderBy) +
            Converter.convert(this.startAt, this.endAt);

        try (
                Connection connection = ((MySQLDatabase) this.database).dataSource().getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            if (this.filter != null) {
                this.filter.resetPointer();
                
                int index = 1;
                while (this.filter.next()) {
                    KeyValue<Filter.Operator, KeyValue<String, Filter.Value>> entry = this.filter.get();
                    preparedStatement.setObject(index++, entry.value().value().value());
                }
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
