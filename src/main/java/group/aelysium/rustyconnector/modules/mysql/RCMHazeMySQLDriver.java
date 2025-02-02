package group.aelysium.rustyconnector.modules.mysql;

import group.aelysium.rustyconnector.common.haze.HazeProvider;
import group.aelysium.rustyconnector.common.modules.ExternalModuleTinder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Optional;

public class RCMHazeMySQLDriver extends HazeProvider {
    public RCMHazeMySQLDriver() {
        try {
            DatabaseConfig.New("default");
            File directory = new File("rc-modules/rcm-mysql");
            if(!directory.exists()) directory.mkdirs();
            for (File file : Optional.ofNullable(directory.listFiles()).orElse(new File[0])) {
                if (!(file.getName().endsWith(".yml") || file.getName().endsWith(".yaml"))) continue;
                int extensionIndex = file.getName().lastIndexOf(".");
                String name = file.getName().substring(0, extensionIndex);
                this.registerDatabase(DatabaseConfig.New(name).tinder());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Tinder extends ExternalModuleTinder<RCMHazeMySQLDriver> {
        @NotNull
        @Override
        public RCMHazeMySQLDriver onStart() throws Exception {
            return new RCMHazeMySQLDriver();
        }
    }
}