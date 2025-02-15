package group.aelysium.rustyconnector.modules.mysql;

import group.aelysium.declarative_yaml.DeclarativeYAML;
import group.aelysium.declarative_yaml.annotations.*;
import group.aelysium.declarative_yaml.lib.Printer;
import group.aelysium.rustyconnector.common.modules.ModuleTinder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Namespace("rustyconnector-modules")
@Config("/rcm-mysql/{name}.yml")
@Comment({
        "############################################################",
        "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
        "#                    MySQL Haze Database                   #",
        "#                                                          #",
        "#               ---------------------------                #",
        "#                                                          #",
        "# | Setup your database! The target database name          #",
        "# | is the same as the name you give this file!            #",
        "#                                                          #",
        "# | To make a new database connection, just duplicate this #",
        "# | template, rename it, and configure it how you'd like!  #",
        "#                                                          #",
        "#               ---------------------------                #",
        "#                                                          #",
        "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
        "############################################################",
})
public class DatabaseConfig {
    @PathParameter("name")
    private String name;

    @Node(0)
    private String address = "127.0.0.1";
    @Node(1)
    private int port = 3306;
    @Node(2)
    private String username = "root";
    @Node(3)
    private String password = "admin";
    @Node(4)
    private int maxPoolSize = 100;
    @Node(5)
    private boolean ssl = false;

    public @NotNull ModuleTinder<? extends MySQLDatabase> tinder() {
        MySQLDatabase.Tinder tinder = new MySQLDatabase.Tinder(
                name,
                this.address,
                this.port,
                this.username,
                this.password
        );
        tinder.poolSize(this.maxPoolSize);

        return tinder;
    }

    public static DatabaseConfig New(@NotNull String name) {
        return DeclarativeYAML.From(
                DatabaseConfig.class,
                new Printer().pathReplacements(Map.of(
                        "name", name
                ))
        );
    }
}