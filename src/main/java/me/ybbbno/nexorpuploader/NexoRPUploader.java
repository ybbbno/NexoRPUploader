package me.ybbbno.nexorpuploader;

import com.nexomc.nexo.pack.server.NexoPackServer;
import com.nexomc.nexo.pack.server.PackServerRegistry;
import kotlin.jvm.functions.Function0;
import org.bukkit.plugin.java.JavaPlugin;

public final class NexoRPUploader extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        PackServerRegistry.register("DROPBOX", () -> ((Function0<NexoPackServer>) RPProvider::new).invoke());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
