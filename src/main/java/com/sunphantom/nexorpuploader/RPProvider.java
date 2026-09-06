package com.sunphantom.nexorpuploader;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.sharing.CreateSharedLinkWithSettingsErrorException;
import com.dropbox.core.v2.sharing.ListSharedLinksResult;
import com.dropbox.core.v2.sharing.SharedLinkMetadata;
import com.nexomc.nexo.NexoPlugin;
import com.nexomc.nexo.api.NexoPack;
import com.nexomc.nexo.pack.server.NexoPackServer;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import team.unnamed.creative.BuiltResourcePack;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RPProvider implements NexoPackServer {
    private final NexoPlugin plugin;

    private final DbxClientV2 client;
    private final String dropboxPath;

    private volatile String packURL = "";
    private volatile String hash = "";
    private volatile UUID packUUID = UUID.randomUUID();

    public RPProvider() {
        plugin = (NexoPlugin) Bukkit.getPluginManager().getPlugin("Nexo");
        if (plugin == null) {
            throw new NoSuchElementException("No nexo plugin!");
        }

        File file = new File(plugin.getDataFolder(), "settings.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("Pack.server.dropbox");
        if (section == null) {
            throw new NoSuchElementException("No dropbox section!");
        }

        String appKey = section.getString("app_key", "");
        String appSecret = section.getString("app_secret", "");
        String refreshToken = section.getString("refresh_token", "");
        String filename = section.getString("filename", "pack.zip");
        this.dropboxPath = "/" + filename;

        DbxRequestConfig configdbx = DbxRequestConfig.newBuilder("NexoRPProvider/0.1").build();
        DbxCredential credential = new DbxCredential(
                "",
                -1L,
                refreshToken,
                appKey,
                appSecret
        );
        this.client = new DbxClientV2(configdbx, credential);
    }

    @Override
    public @NonNull CompletableFuture<Void> uploadPack() {
        return CompletableFuture.runAsync(() -> {
            try {
                BuiltResourcePack builtPack = NexoPack.builtResourcePack();
                if (builtPack == null) {
                    throw new IllegalStateException("Nexo has not built a resource pack yet");
                }

                byte[] packBytes = builtPack.data().toByteArray();
                hash = builtPack.hash();

                try (ByteArrayInputStream in = new ByteArrayInputStream(packBytes)) {
                    FileMetadata metadata = client.files().uploadBuilder(dropboxPath)
                            .withMode(WriteMode.OVERWRITE)
                            .uploadAndFinish(in);

                    if (metadata == null) {
                        throw new IllegalStateException("Dropbox upload returned no metadata");
                    }
                }

                String shareUrl = getOrCreateSharedLink(dropboxPath);
                packURL = toDirectDownloadLink(shareUrl);
                packUUID = UUID.randomUUID();

                plugin.getLogger().info(packURL);
            } catch (DbxException | IOException e) {
                throw new RuntimeException("Failed to upload resource pack to Dropbox", e);
            }
        });
    }

    @Override
    public void sendPack(@NonNull Player player) {
        ResourcePackInfo info = packInfo();
        if (info == null) {
            return;
        }
        player.sendResourcePacks(ResourcePackRequest.resourcePackRequest()
                .packs(info)
                .build());
    }

    @Override
    public @NonNull String packUrl() {
        return packURL;
    }

    @Override
    @Nullable
    public ResourcePackInfo packInfo() {
        if (packURL.isEmpty() || hash.isEmpty()) {
            return null;
        }
        return ResourcePackInfo.resourcePackInfo(packUUID, URI.create(packURL), hash);
    }

    private @NonNull String toDirectDownloadLink(@NonNull String shareUrl) {
        if (shareUrl.contains("?dl=0")) {
            return shareUrl.replace("?dl=0", "?dl=1");
        }
        if (shareUrl.contains("&dl=0")) {
            return shareUrl.replace("&dl=0", "&dl=1");
        }
        return shareUrl + (shareUrl.contains("?") ? "&dl=1" : "?dl=1");
    }

    private @NonNull String getOrCreateSharedLink(String path) throws DbxException {
        try {
            SharedLinkMetadata linkMetadata = client.sharing().createSharedLinkWithSettings(path);
            return linkMetadata.getUrl();
        } catch (CreateSharedLinkWithSettingsErrorException e) {
            // A shared link for this path already exists - fetch it instead.
            ListSharedLinksResult result = client.sharing()
                    .listSharedLinksBuilder()
                    .withPath(path)
                    .withDirectOnly(true)
                    .start();

            if (!result.getLinks().isEmpty()) {
                return result.getLinks().getFirst().getUrl();
            }
            throw e;
        }
    }
}
