package group.aelysium.rustyconnector.modules.mysql;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.RCKernel;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.haze.HazeProvider;
import group.aelysium.rustyconnector.common.modules.ExternalModuleBuilder;
import group.aelysium.rustyconnector.common.modules.Module;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import group.aelysium.rustyconnector.server.ServerKernel;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Vector;

public class MySQLHazeProvider implements Module {
    private final List<String> trackedDatabases = new Vector<>();
    
    private MySQLHazeProvider() {}
    
    protected void trackNewDatabase(@NotNull String name) {
        this.trackedDatabases.add(name);
    }
    
    @Override
    public @Nullable Component details() {
        return Component.text("This module simply registers configurations into Haze. It doesn't contain any details.");
    }
    
    @Override
    public void close() {
        try {
            HazeProvider provider = RC.Haze();
            trackedDatabases.forEach(provider::unregisterDatabase);
        } catch (Exception ignore) {}
    }
    
    public static class Builder extends ExternalModuleBuilder<MySQLHazeProvider> {
        private void bind(@NotNull RCKernel<?> kernel, @NotNull MySQLHazeProvider instance) {
            kernel.fetchModule("Haze").onStart(h -> {
                HazeProvider provider = (HazeProvider) h;
                
                File directory = new File("rc-modules/rcm-mysql");
                if(!directory.exists()) directory.mkdirs();
                
                File[] files = directory.listFiles();
                if (files == null) return;
                
                for (File file : files) {
                    if (!(file.getName().endsWith(".yml") || file.getName().endsWith(".yaml"))) continue;
                    int extensionIndex = file.getName().lastIndexOf(".");
                    String name = file.getName().substring(0, extensionIndex);
                    try {
                        provider.registerDatabase(DatabaseConfig.New(name).builder());
                        instance.trackNewDatabase(name);
                    } catch (Exception e) {
                        RC.Error(Error.from(e).whileAttempting("To register a new database to Haze."));
                    }
                }
            });
        }
        
        @Override
        public void bind(@NotNull ServerKernel kernel, @NotNull MySQLHazeProvider instance) {
            this.bind((RCKernel<?>) kernel, instance);
        }
        
        @Override
        public void bind(@NotNull ProxyKernel kernel, @NotNull MySQLHazeProvider instance) {
            this.bind((RCKernel<?>) kernel, instance);
        }
        
        @NotNull
        @Override
        public MySQLHazeProvider onStart(@NotNull Context context) throws Exception {
            MySQLHazeProvider provider = new MySQLHazeProvider();
            try {
                File directory = new File("rc-modules/rcm-mysql");
                if(!directory.exists()) directory.mkdirs();
                
                {
                    File[] files = directory.listFiles();
                    if (files == null || files.length == 0)
                        DatabaseConfig.New("default");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return provider;
        }
    }
}