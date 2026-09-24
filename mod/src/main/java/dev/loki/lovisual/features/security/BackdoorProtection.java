package dev.loki.lovisual.features.security;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import dev.loki.lovisual.config.i18n.MainConfig;

import java.net.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BackdoorProtection {

    public static final BackdoorProtection INSTANCE = new BackdoorProtection();

    private final Map<String, Set<String>> translationKeyPacks = new ConcurrentHashMap<>();
    private final Set<String> vanillaKeybinds = ConcurrentHashMap.newKeySet();

    private volatile String currentServerAddress;
    private volatile boolean currentServerLocal;

    private BackdoorProtection() {
    }

    public void resetLoadedTranslations() {
        translationKeyPacks.clear();
    }

    public void registerTranslationKey(String key, String packId) {
        if (key == null || key.isBlank() || packId == null || packId.isBlank()) return;
        translationKeyPacks.computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet()).add(packId);
    }

    public void resetVanillaKeybinds() {
        vanillaKeybinds.clear();
    }

    public void registerVanillaKeybind(String key) {
        if (key == null || key.isBlank()) return;
        vanillaKeybinds.add(key);
    }

    public void updateServerConnection(Connection connection) {
        currentServerAddress = null;
        currentServerLocal = true;
        if (connection == null || connection.isMemoryConnection()) return;
        SocketAddress address = connection.getRemoteAddress();
        if (!(address instanceof InetSocketAddress inetSocketAddress)) return;
        InetAddress inetAddress = inetSocketAddress.getAddress();
        String host = inetAddress != null ? inetAddress.getHostAddress() : inetSocketAddress.getHostString();
        currentServerAddress = host;
        currentServerLocal = isLocalAddressQuiet(host);
    }

    public Component filterText(Component input) {
        if (input == null || !MainConfig.get().isBackdoorTranslationFilterEnabled()) return input;

        ComponentContents content = filterContent(input.getContents());
        MutableComponent filtered = MutableComponent.create(content);
        filtered.setStyle(input.getStyle());
        for (Component sibling : input.getSiblings()) {
            filtered.append(filterText(sibling));
        }
        return filtered;
    }

    private ComponentContents filterContent(ComponentContents content) {
        if (content instanceof FilteredTranslatableTextContent) {
            return content;
        }
        if (content instanceof TranslatableContents translatable) {
            Object[] args = translatable.getArgs();
            Object[] filteredArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                filteredArgs[i] = arg instanceof Component text ? filterText(text) : arg;
            }
            String resolved = isTranslationAllowed(translatable.getKey())
                    ? null
                    : translatable.getFallback() == null ? translatable.getKey() : translatable.getFallback();
            return new FilteredTranslatableTextContent(translatable.getKey(), translatable.getFallback(), filteredArgs, resolved);
        }
        if (content instanceof KeybindContents keybind && MainConfig.get().isBackdoorKeybindFilterEnabled() && !isKeybindAllowed(keybind.getName())) {
            return PlainTextContents.create(keybind.getName());
        }
        return content;
    }

    public boolean isTranslationAllowed(String key) {
        MainConfig cfg = MainConfig.get();
        if (!cfg.isBackdoorTranslationFilterEnabled()) return true;
        if (key == null || key.isBlank()) return true;

        Set<String> packIds = translationKeyPacks.get(key);
        if (packIds == null || packIds.isEmpty()) {
            return translationKeyPacks.isEmpty();
        }

        Set<String> allowedPacks = cfg.getBackdoorAllowedTranslationPacks();
        for (String packId : packIds) {
            if (isBuiltInPack(packId) || allowedPacks.contains(packId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isKeybindAllowed(String key) {
        MainConfig cfg = MainConfig.get();
        if (!cfg.isBackdoorKeybindFilterEnabled()) return true;
        if (key == null || key.isBlank()) return true;
        return vanillaKeybinds.contains(key) || cfg.getBackdoorAllowedKeybinds().contains(key);
    }

    public void assertRemoteUrlAllowed(URL url) throws UnknownHostException {
        MainConfig cfg = MainConfig.get();
        if (!cfg.isBackdoorLocalHttpGuardEnabled() || url == null) return;
        String host = url.getHost();
        if (host == null || host.isBlank()) return;
        boolean targetLocal = isLocalAddress(host);
        if (!targetLocal) return;
        if (currentServerAddress == null) return;
        if (currentServerLocal && cfg.isBackdoorAllowLocalHttpWhenServerLocal()) return;
        throw new IllegalStateException("Blocked local/private HTTP request to " + host + " while connected to a remote server");
    }

    public java.nio.file.Path isolatePackCachePath(java.nio.file.Path original) {
        MainConfig cfg = MainConfig.get();
        if (!cfg.isBackdoorPackCacheIsolationEnabled() || original == null) return original;
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getUser() == null) return original;
        java.util.UUID uuid = client.getUser().getProfileId();
        if (uuid == null) return original;
        java.nio.file.Path fileName = original.getFileName();
        java.nio.file.Path parent = original.getParent();
        if (fileName == null || parent == null) return original;
        return parent.resolve(uuid.toString()).resolve(fileName.toString());
    }

    private static boolean isBuiltInPack(String packId) {
        if (packId == null || packId.isBlank()) return false;
        String normalized = packId.toLowerCase();
        return normalized.equals("vanilla") || normalized.equals("high_contrast");
    }

    private static boolean isLocalAddressQuiet(String host) {
        try {
            return isLocalAddress(host);
        } catch (UnknownHostException ignored) {
            return false;
        }
    }

    private static boolean isLocalAddress(String host) throws UnknownHostException {
        for (InetAddress address : InetAddress.getAllByName(host)) {
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()) {
                return true;
            }
        }
        return false;
    }
}
