package group.aelysium.rustyconnector.modules.mysql.requests;

import group.aelysium.rustyconnector.modules.mysql.MySQLDatabase;
import group.aelysium.rustyconnector.modules.mysql.lib.Converter;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.exceptions.HazeException;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.Filter;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.KeyValue;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.requests.UpdateRequest;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.requests.UpsertRequest;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MySQLUpsertRequest extends UpsertRequest {
    public MySQLUpsertRequest(
        @NotNull MySQLDatabase database,
        @NotNull String target
    ) {
        super(database, target);
    }
    
    @Override
    public long execute() throws Exception {
        if (parameters.isEmpty()) throw new HazeException("Parameters cannot be empty for an UPSERT operation.");
        
        List<String> columns = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (String column : parameters.keySet()) {
            columns.add(column);
            values.add("?");
        }
        
        List<String> updateClause = parameters.keySet().stream()
            .map(k -> k+" = VALUES("+k+")")
            .toList();
        
        String query = "INSERT INTO " + target + " (" + String.join(", ", columns) + ") VALUES (" + String.join(", ", values) + ") " +
            "ON DUPLICATE KEY UPDATE " + String.join(", ", updateClause);
        
        try (
            Connection connection = ((MySQLDatabase) this.database).dataSource().getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)
        ) {
            int index = 1;
            for (Object value : parameters.values())
                preparedStatement.setObject(index++, value);
            
            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows < 0) return 0;
            
            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (!generatedKeys.next()) return 0;
                return generatedKeys.getLong(1);
            }
        } catch (SQLException e) {
            throw new HazeException("Error executing upsert request. " + e.getMessage());
        }
    }
}
