package org.oddlama.vane.proxycore.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ManagedServer {

    public String displayName;
    public ServerStart start;

    private final String id;
    private final StatefulConfiguration onlineConfig;
    private final StatefulConfiguration offlineConfig;

    public ManagedServer(
        String id,
        String displayName,
        CommentedConfig onlineConfigSection,
        CommentedConfig offlineConfigSection,
        CommentedConfig start
    ) throws IOException {
        this.id = id;
        this.displayName = displayName;

        this.onlineConfig = new StatefulConfiguration(id, displayName, onlineConfigSection);
        this.offlineConfig = new StatefulConfiguration(id, displayName, offlineConfigSection);
        this.start = new ServerStart(id, displayName, start);
    }

    public @NotNull String id() {
        return this.id;
    }

    public @Nullable String favicon(ConfigItemSource source) {
        switch (source) {
            case ONLINE -> {
                return onlineConfig.encodedFavicon;
            }
            case OFFLINE -> {
                return offlineConfig.encodedFavicon;
            }
            default -> {
                return null;
            }
        }
    }

    public String[] startCmd() {
        return start.cmd;
    }

    public String startKickMsg() {
        return start.kickMsg;
    }

    private String randomQuote(ConfigItemSource source) {
        final String[] quoteSet;
        switch (source) {
            case ONLINE -> quoteSet = onlineConfig.quotes;
            case OFFLINE -> quoteSet = offlineConfig.quotes;
            default -> {
                return "";
            }
        }

        if (quoteSet == null || quoteSet.length == 0) {
            return "";
        }
        return quoteSet[new Random().nextInt(quoteSet.length)];
    }

    public String motd(ConfigItemSource source) {
        final String sourcedMotd;
        final ConfigItemSource quoteSource;
        switch (source) {
            case ONLINE -> {
                sourcedMotd = onlineConfig.motd;
                quoteSource = ConfigItemSource.ONLINE;
            }
            case OFFLINE -> {
                sourcedMotd = offlineConfig.motd;
                quoteSource = ConfigItemSource.OFFLINE;
            }
            default -> {
                return "";
            }
        }

        if (sourcedMotd == null) {
            return "";
        }
        return sourcedMotd.replace("{QUOTE}", randomQuote(quoteSource));
    }

    public Integer commandTimeout() {
        return start.timeout;
    }

    public enum ConfigItemSource {
        ONLINE,
        OFFLINE,
    }

    private static class StatefulConfiguration {

        public String[] quotes = null;
        public String motd = null;
        private @Nullable String encodedFavicon;

        public StatefulConfiguration(String id, String displayName, CommentedConfig config) throws IOException {
            // [managedServers.my_server.state]
            if (config == null) {
                // The whole section is missing
                return;
            }

            // quotes = ["", ...]
            List<String> quotes = config.get("quotes");

            if (quotes != null) this.quotes = quotes
                .stream()
                .filter(s -> !s.isBlank())
                .map(s -> s.replace("{SERVER}", id).replace("{SERVER_DISPLAY_NAME}", displayName))
                .toArray(String[]::new);

            // motd = "..."
            var motd = config.get("motd");

            if (!(motd == null || motd instanceof String)) throw new IllegalArgumentException(
                "Managed server '" + id + "' has a non-string MOTD!"
            );

            if (motd != null && !((String) motd).isEmpty()) this.motd = ((String) motd).replace(
                    "{SERVER_DISPLAY_NAME}",
                    displayName
                );

            // favicon = "..."
            var faviconPath = config.get("favicon");

            if (!(faviconPath == null || faviconPath instanceof String)) throw new IllegalArgumentException(
                "Managed server '" + id + "' has a non-string favicon path!"
            );

            if (faviconPath != null && !((String) faviconPath).isEmpty()) this.encodedFavicon = encodeFavicon(
                id,
                (String) faviconPath
            );
        }

        private static String encodeFavicon(String id, String faviconPath) throws IOException {
            File faviconFile = new File(faviconPath.replace("{SERVER}", id));
            BufferedImage image;
            try {
                image = ImageIO.read(faviconFile);
            } catch (IOException e) {
                throw new IOException("Failed to read favicon file: " + e.getMessage());
            }

            if (image.getWidth() != 64 || image.getHeight() != 64) {
                throw new IllegalArgumentException("Favicon has invalid dimensions (must be 64x64)");
            }

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", stream);
            byte[] faviconBytes = stream.toByteArray();

            String encodedFavicon = "data:image/png;base64," + Base64.getEncoder().encodeToString(faviconBytes);

            if (encodedFavicon.length() > Short.MAX_VALUE) {
                throw new IllegalArgumentException("Favicon file too large for server to process");
            }

            return encodedFavicon;
        }
    }

    public static class ServerStart {

        private static final int DEFAULT_TIMEOUT_SECONDS = 10;
        public String[] cmd;
        public Integer timeout;
        public String kickMsg;
        public boolean allowAnyone;

        public ServerStart(String id, String displayName, CommentedConfig config) {
            List<String> cmd = config.get("cmd");
            var timeout = config.get("timeout");
            var kickMsg = config.get("kickMsg");
            var allowAnyone = config.get("allowAnyone");

            if (cmd != null) this.cmd = cmd.stream().map(s -> s.replace("{SERVER}", id)).toArray(String[]::new);
            else this.cmd = null;

            if (!(kickMsg == null || kickMsg instanceof String)) throw new IllegalArgumentException(
                "Managed server '" + id + "' has an invalid kick message!"
            );

            if (kickMsg != null) this.kickMsg = ((String) kickMsg).replace("{SERVER}", id).replace(
                    "{SERVER_DISPLAY_NAME}",
                    displayName
                );
            else this.kickMsg = null;

            if (allowAnyone != null) this.allowAnyone = (boolean) allowAnyone;
            else this.allowAnyone = false;

            if (timeout == null) {
                this.timeout = DEFAULT_TIMEOUT_SECONDS;
                return;
            }

            if (!(timeout instanceof Integer)) throw new IllegalArgumentException(
                "Managed server '" + id + "' has an invalid command timeout!"
            );

            this.timeout = (Integer) timeout;
        }
    }
}
