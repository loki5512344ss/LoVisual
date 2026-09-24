package dev.loki.lovisual.features.account;

import dev.loki.lovisual.config.values.collection.ConfigValue;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The {@code accounts} config entry: a {@link ConfigValue} holding the list of {@link AccountEntry}
 * records and their JSON (de)serialization. Microsoft refresh tokens are intentionally never
 * written to disk ({@code microsoftRefreshToken} is always persisted empty). Shares the
 * {@code features.account} package (lives in a subfolder) so it can reuse {@link
 * AccountConfig}'s parse helpers and {@link AccountEntry}'s package-private constructors.
 */
final class AccountsValue extends ConfigValue<List<AccountEntry>> {

    AccountsValue(String name) {
        super(name, new ArrayList<>());
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    private static boolean boolValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string) {
            return Boolean.parseBoolean(string);
        }
        return false;
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        if (value instanceof String string) {
            try {
                return Math.max(0, Integer.parseInt(string.trim()));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    @Override
    public Object toJson() {
        List<Object> out = new ArrayList<>();
        for (AccountEntry entry : value) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", entry.getName());
            row.put("date", entry.getDate());
            row.put("skin", entry.getSkin() == null ? "" : entry.getSkin().toString());
            row.put("pinned", entry.isPinned());
            row.put("originalIndex", entry.getOriginalIndex());
            row.put("authType", entry.getAuthType().id());
            row.put("onlineUuid", entry.getOnlineUuid() == null ? "" : entry.getOnlineUuid().toString());
            row.put("microsoftRefreshToken", "");
            out.add(row);
        }
        return out;
    }

    @Override
    public void fromJson(Object json) {
        List<AccountEntry> loaded = new ArrayList<>();
        if (json instanceof Iterable<?> iterable) {
            int fallbackIndex = 0;
            for (Object item : iterable) {
                if (!(item instanceof Map<?, ?> row)) {
                    continue;
                }

                String name = stringValue(row.get("name"));
                if (name.isBlank()) {
                    continue;
                }

                String date = stringValue(row.get("date"));
                Identifier skin = AccountConfig.parseIdentifier(stringValue(row.get("skin")));
                boolean pinned = boolValue(row.get("pinned"));
                int originalIndex = intValue(row.get("originalIndex"), fallbackIndex);
                AccountAuthType authType = AccountAuthType.byId(stringValue(row.get("authType")));
                UUID onlineUuid = AccountConfig.parseUuid(stringValue(row.get("onlineUuid")));
                loaded.add(new AccountEntry(name, date, skin, pinned, originalIndex, authType, onlineUuid, ""));
                fallbackIndex++;
            }
        }

        loaded.sort(Comparator.comparingInt(AccountEntry::getOriginalIndex));
        value = loaded;
    }
}
