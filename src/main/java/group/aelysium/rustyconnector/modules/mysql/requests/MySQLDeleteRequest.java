package group.aelysium.rustyconnector.modules.mysql.requests;

import group.aelysium.rustyconnector.modules.mysql.MySQLDatabase;
import group.aelysium.rustyconnector.modules.mysql.lib.Converter;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.exceptions.HazeException;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.Filter;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.KeyValue;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.requests.DeleteRequest;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MySQLDeleteRequest extends DeleteRequest {
    public MySQLDeleteRequest(
            @NotNull MySQLDatabase database,
            @NotNull String target
    ) {
        super(database, target);
    }

    @Override
    public void execute() throws Exception {
        String query = "DELETE FROM " + target + " " + Converter.convert(this.filter);

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

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new HazeException("Error executing delete request. " + e.getMessage());
        }
    }
}
