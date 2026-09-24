package dev.loki.lovisual.util.session;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public record SessionLoginData(String name, UUID uuid, String accessToken, boolean online) {

    public SessionLoginData {
        name = name == null ? "" : name.trim();
        uuid = uuid == null ? offlineUuid(name) : uuid;
        accessToken = accessToken == null ? "" : accessToken;
    }

    public static SessionLoginData offline(String name) {
        String cleanName = name == null ? "" : name.trim();
        return new SessionLoginData(cleanName, offlineUuid(cleanName), "lovisual:offline", false);
    }

    public static UUID offlineUuid(String name) {
        String cleanName = name == null ? "" : name.trim();
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + cleanName).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String toString() {
        return "SessionLoginData{" +
                "name='" + name + '\'' +
                ", uuid=" + uuid +
                ", token=[TOKEN]" +
                ", online=" + online +
                '}';
    }
}
