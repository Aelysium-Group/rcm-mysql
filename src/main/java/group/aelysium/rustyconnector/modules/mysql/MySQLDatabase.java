package group.aelysium.rustyconnector.modules.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import group.aelysium.rustyconnector.common.haze.HazeDatabase;
import group.aelysium.rustyconnector.common.modules.ModuleTinder;
import group.aelysium.rustyconnector.modules.mysql.requests.*;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.exceptions.HazeCastingException;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.exceptions.HazeException;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.DataHolder;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.lib.DataKey;
import group.aelysium.rustyconnector.shaded.group.aelysium.haze.query.*;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringJoiner;

public class MySQLDatabase extends HazeDatabase {
    protected final HikariDataSource dataSource;
    protected final String address;
    protected final int port;
    protected final String username;
    protected final String password;
    protected final int poolSize;

    protected MySQLDatabase(
        @NotNull String database,
        @NotNull String address,
        int port,
        @NotNull String username,
        @NotNull String password,
        int poolSize
    ) {
        super(database, Type.TABULAR);
        this.address = address;
        this.port = port;
        this.username = username;
        this.password = password;
        this.poolSize = poolSize;

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://"+this.address+":"+this.port+"/"+this.name);
        config.setUsername(this.username);
        config.setPassword(this.password);
        config.setMaximumPoolSize(this.poolSize);
        this.dataSource = new HikariDataSource(config);
    }

    public HikariDataSource dataSource() {
        return this.dataSource;
    }

    @Override
    public CreateRequest newCreateRequest(@NotNull String target) {
        return new MySQLCreateRequest(this, target);
    }

    @Override
    public ReadRequest newReadRequest(@NotNull String target) {
        return new MySQLReadRequest(this, target);
    }

    @Override
    public UpdateRequest newUpdateRequest(@NotNull String target) {
        return new MySQLUpdateRequest(this, target);
    }

    @Override
    public DeleteRequest newDeleteRequest(@NotNull String target) {
        return new MySQLDeleteRequest(this, target);
    }

    @Override
    public void createDataHolder(@NotNull DataHolder dataHolder) throws HazeException {
        String tableName = dataHolder.name();
        StringJoiner columnDefinitions = new StringJoiner(", ");

        // Add default primary key column
        columnDefinitions.add("id INT AUTO_INCREMENT PRIMARY KEY");

        dataHolder.keys().forEach(d -> {
            StringBuilder columnDefinition = new StringBuilder();
            columnDefinition.append(d.name())
                    .append(" ")
                    .append(getMySQLType(d));

            if (!d.nullable()) columnDefinition.append(" NOT NULL");
            if (d.unique()) columnDefinition.append(" UNIQUE");

            columnDefinitions.add(columnDefinition.toString());
        });

        String createTableSQL = "CREATE TABLE IF NOT EXISTS " + tableName + " (" + columnDefinitions + ")";

        try (
                Connection connection = this.dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(createTableSQL)
        ) {
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new HazeException("Error creating table: `" + tableName + "`. "+e.getMessage());
        }
    }

    private String getMySQLType(DataKey key) {
        return switch (key.type()) {
            case STRING -> {
                if (key.length() == -1) yield "VARCHAR(n)";  // Use 255 for unlimited length varchar
                if (key.length() > 0) yield "VARCHAR(" + key.length() + ")";
                yield "TEXT";
            }
            case INTEGER -> {
                if (key.length() == 1) yield "TINYINT";
                if (key.length() <= 3) yield "SMALLINT";
                if (key.length() <= 5) yield "MEDIUMINT";
                if (key.length() <= 8) yield "BIGINT";
                yield "INT";
            }
            case DECIMAL -> {
                if (key.length() > 0) yield "DECIMAL(" + key.length() + ", 2)";
                yield "DECIMAL(10, 2)";
            }
            case BOOLEAN -> "TINYINT(1)";
            case DATE -> "DATE";
            case DATETIME -> "DATETIME";
            case TIME -> "TIME";
            case BINARY -> {
                if (key.length() > 0) yield "VARBINARY(" + key.length() + ")";
                yield "BLOB";
            }
            case ARRAY, OBJECT -> "varchar(n)";
            default -> throw new IllegalArgumentException("Unsupported DataType: " + key.type());
        };
    }

    @Override
    public boolean doesDataHolderExist(@NotNull String target) throws HazeException {
        try(Connection connection = this.dataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT CASE WHEN EXISTS ( SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ? ) THEN 1 ELSE 0 END AS [exists];");
            statement.setString(1, target);
            try(ResultSet result = statement.executeQuery()) {
                if(!result.next()) throw new HazeException("Unable to tell if the table `"+target+"` exists. No response was received.");
                return result.getBoolean(1);
            }
        } catch (ClassCastException e) {
            throw new HazeCastingException(e.getMessage());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteDataHolder(@NotNull String target) throws HazeException {
        try(Connection connection = this.dataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("DROP TABLE ?;");
            statement.setString(1, target);
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        this.dataSource.close();
    }

    public static class Tinder extends ModuleTinder<MySQLDatabase> {
        protected final String database;
        protected final String address;
        protected final int port;
        protected final String username;
        protected final String password;
        protected int poolSize;

        public Tinder(
                @NotNull String database,
                @NotNull String address,
                int port,
                @NotNull String username,
                @NotNull String password
        ) {
            super(database, "A MySQL Database.", "rustyconnector-haze-databaseDetails");
            this.database = database;
            this.address = address;
            this.port = port;
            this.username = username;
            this.password = password;
        }

        public Tinder poolSize(int poolSize) {
            this.poolSize = poolSize;
            return this;
        }

        @NotNull
        @Override
        public MySQLDatabase ignite() throws Exception {
            return new MySQLDatabase(
                    this.database,
                    this.address,
                    this.port,
                    this.username,
                    this.password,
                    this.poolSize
            );
        }
    }
}
