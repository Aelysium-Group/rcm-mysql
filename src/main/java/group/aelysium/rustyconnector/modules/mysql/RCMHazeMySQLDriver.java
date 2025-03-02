package group.aelysium.rustyconnector.modules.mysql;

import group.aelysium.rustyconnector.common.haze.HazeDatabase;
import group.aelysium.rustyconnector.common.haze.HazeProvider;
import group.aelysium.rustyconnector.common.modules.ExternalModuleBuilder;
import group.aelysium.rustyconnector.common.modules.Module;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;

public class RCMHazeMySQLDriver extends HazeProvider {
    public RCMHazeMySQLDriver() {
        try {
            File directory = new File("rc-modules/rcm-mysql");
            if(!directory.exists()) directory.mkdirs();
            
            {
                File[] files = directory.listFiles();
                if (files == null || files.length == 0)
                    DatabaseConfig.New("default");
            }
            
            File[] files = directory.listFiles();
            if (files == null) return;
            if (files.length == 0) return;
            
            for (File file : files) {
                if (!(file.getName().endsWith(".yml") || file.getName().endsWith(".yaml"))) continue;
                int extensionIndex = file.getName().lastIndexOf(".");
                String name = file.getName().substring(0, extensionIndex);
                this.registerDatabase(DatabaseConfig.New(name).builder());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void registerDatabase(@NotNull Module.Builder<HazeDatabase> database) throws Exception {
        this.databases.registerModule(database);
    }

    @Override
    public @Nullable Component details() {
        return null;
    }
    
    public static class Builder extends ExternalModuleBuilder<RCMHazeMySQLDriver> {
        @NotNull
        @Override
        public RCMHazeMySQLDriver onStart(@NotNull Path dataDirectory) throws Exception {
            return new RCMHazeMySQLDriver();
        }
    }
}