package group.aelysium.rustyconnector.modules.mysql.requests;

import group.aelysium.rustyconnector.modules.mysql.MySQLDatabase;
import group.aelysium.rustyconnector.modules.mysql.lib.Converter;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.exceptions.HazeException;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.Filter;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.KeyValue;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.requests.UpdateRequest;
import org.jetbrains.annotations.NotNull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MySQLUpdateRequest extends UpdateRequest {
    public MySQLUpdateRequest(
            @NotNull MySQLDatabase database,
            @NotNull String target
    ) {
        super(database, target);
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

        query.append(Converter.convert(this.filter));

        try (
                Connection connection = ((MySQLDatabase) this.database).dataSource().getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(query.toString())
        ) {
            int index = 1;

            for (Object value : parameters.values())
                preparedStatement.setObject(index++, value);
            
            if (this.filter != null) {
                this.filter.resetPointer();
                
                while (this.filter.next()) {
                    KeyValue<Filter.Operator, KeyValue<String, Filter.Value>> entry = this.filter.get();
                    preparedStatement.setObject(index++, entry.value().value().value());
                }
            }

            return preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new HazeException("Error executing update request. " + e.getMessage());
        }
    }
}
