
package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.relations;

import com.mojang.authlib.GameProfile;
import dev.loki.lovisual.features.relations.PlayerRelations;
import dev.loki.lovisual.features.relations.category.CategoryService;
import dev.loki.lovisual.features.relations.category.CategoryType;
import dev.loki.lovisual.util.player.health.PlayerSkinResolver;
import dev.loki.lovisual.util.text.format.LegacyTextUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;
import java.util.*;

final class OnlinePickerData {
    private static final long STRUCTURE_REFRESH_MS = 350L;
    private static final long DYNAMIC_REFRESH_MS = 250L;
    final List<PlayerRow> rows = new ArrayList<>();
    final Map<UUID, Identifier> skinCache = new HashMap<>();
    String search = "";
    int rowsVersion;
    int filteredRowsVersion = -1;
    String filteredSearchKey = "";
    List<PlayerRow> filteredRowsCache = List.of();
    long lastStructureCheckMs;
    long lastDynamicRefreshMs;
    long lastStructureSignature;
    int lastOnlineCount = -1;
    String searchText() { return search == null ? "" : search; }
    String normalizedSearch() { return search == null ? "" : LegacyTextUtil.stripLegacy(search).trim().toLowerCase(Locale.ROOT); }
    void invalidateFilter() { filteredRowsVersion = -1; }
    List<PlayerRow> filteredRows() {
        String q = normalizedSearch();
        if (filteredRowsVersion == rowsVersion && filteredSearchKey.equals(q)) return filteredRowsCache;
        if (q.isBlank()) filteredRowsCache = rows;
        else {
            List<PlayerRow> out = new ArrayList<>();
            for (PlayerRow r : rows) if (r.searchText().contains(q)) out.add(r);
            filteredRowsCache = out;
        }
        filteredRowsVersion = rowsVersion;
        filteredSearchKey = q;
        return filteredRowsCache;
    }
    Identifier resolveSkin(PlayerRow p) {
        if (p == null || p.id() == null) return null;
        Identifier c = skinCache.get(p.id());
        if (c != null) return c;
        Identifier s = PlayerSkinResolver.resolveProfileSkin(p.profile());
        if (s != null) skinCache.put(p.id(), s);
        return s;
    }
    void refreshIfNeeded() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getConnection() == null) return;
        long now = System.currentTimeMillis();
        int online = onlineCount();
        boolean check = rows.isEmpty() || online != lastOnlineCount || now - lastStructureCheckMs >= STRUCTURE_REFRESH_MS;
        if (check) {
            long sig = structureSignature();
            lastStructureCheckMs = now;
            if (rows.isEmpty() || online != lastOnlineCount || sig != lastStructureSignature) {
                collectRows();
                lastOnlineCount = online;
                lastStructureSignature = sig;
                lastDynamicRefreshMs = now;
                return;
            }
        }
        if (now - lastDynamicRefreshMs >= DYNAMIC_REFRESH_MS) {
            refreshDynamicRows();
            lastDynamicRefreshMs = now;
        }
    }
    void refreshNow() {
        collectRows();
        lastOnlineCount = onlineCount();
        lastStructureSignature = structureSignature();
        lastStructureCheckMs = System.currentTimeMillis();
        lastDynamicRefreshMs = lastStructureCheckMs;
    }
    int onlineCount() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.getConnection() != null ? mc.getConnection().getListedOnlinePlayers().size() : 0;
    }
    void collectRows() {
        Minecraft mc = Minecraft.getInstance();
        rows.clear();
        if (mc == null || mc.getConnection() == null) { rowsVersion++; invalidateFilter(); return; }
        Collection<PlayerInfo> online = mc.getConnection().getListedOnlinePlayers();
        List<PlayerInfo> sorted = new ArrayList<>(online);
        sorted.sort(Comparator.comparingInt((PlayerInfo i) -> -i.getTabListOrder()).thenComparing(i -> i.getGameMode() == GameType.SPECTATOR).thenComparing(i -> teamName(i).toLowerCase(Locale.ROOT)).thenComparing(i -> profileName(i).toLowerCase(Locale.ROOT), String.CASE_INSENSITIVE_ORDER));
        Set<UUID> live = new HashSet<>();
        for (PlayerInfo info : sorted) {
            if (info == null || info.getProfile() == null) continue;
            GameProfile p = info.getProfile();
            UUID id = p.id();
            String name = p.name();
            if (id == null || name == null || name.isBlank()) continue;
            Component d = displayName(info, name);
            CategoryType rel = CategoryService.get(name);
            int col = rel == CategoryType.DEFAULT ? PlayerRelations.get().colorDefault() : CategoryService.getColor(name);
            rows.add(new PlayerRow(id, p, name, d, searchText(name, d), rel, col, info.getLatency(), info.getGameMode() == GameType.SPECTATOR));
            live.add(id);
        }
        skinCache.keySet().retainAll(live);
        rowsVersion++;
        invalidateFilter();
    }
    void refreshDynamicRows() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getConnection() == null || rows.isEmpty()) return;
        boolean changed = false;
        for (int i = 0; i < rows.size(); i++) {
            PlayerRow r = rows.get(i);
            PlayerInfo info = mc.getConnection().getPlayerInfo(r.id());
            if (info == null || info.getProfile() == null) { collectRows(); return; }
            CategoryType rel = CategoryService.get(r.name());
            int col = rel == CategoryType.DEFAULT ? PlayerRelations.get().colorDefault() : CategoryService.getColor(r.name());
            int lat = info.getLatency();
            boolean spec = info.getGameMode() == GameType.SPECTATOR;
            if (lat == r.latency() && spec == r.spectator() && rel == r.relation() && col == r.relationColor()) continue;
            rows.set(i, new PlayerRow(r.id(), r.profile(), r.name(), r.displayName(), r.searchText(), rel, col, lat, spec));
            changed = true;
        }
        if (changed) { rowsVersion++; invalidateFilter(); }
    }
    long structureSignature() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getConnection() == null) return 0L;
        long h = 0x6A09E667F3BCC909L;
        for (PlayerInfo info : mc.getConnection().getListedOnlinePlayers()) {
            if (info == null || info.getProfile() == null) continue;
            GameProfile p = info.getProfile();
            h = mix(h, p.id()); h = mix(h, p.name()); h = mix(h, info.getTabListOrder());
            h = mix(h, info.getGameMode() == GameType.SPECTATOR ? 1 : 0); h = mix(h, teamName(info));
            Component raw = info.getTabListDisplayName(); h = mix(h, raw != null ? raw.getString() : "");
            String name = p.name() == null ? "" : p.name();
            CategoryType rel = CategoryService.get(name);
            h = mix(h, rel.ordinal()); h = mix(h, rel == CategoryType.DEFAULT ? PlayerRelations.get().colorDefault() : CategoryService.getColor(name));
        }
        return h;
    }
    Component displayName(PlayerInfo info, String fallback) {
        Minecraft mc = Minecraft.getInstance();
        Component d = null;
        if (mc != null && mc.gui != null && mc.gui.hud != null && mc.gui.hud.getTabList() != null) d = mc.gui.hud.getTabList().getNameForDisplay(info);
        if (d == null) d = info.getTabListDisplayName();
        if (d == null || d.getString().isBlank()) d = Component.literal(fallback);
        return LegacyTextUtil.convertLegacyCodes(d);
    }
    static String searchText(String name, Component d) {
        String raw = (name == null ? "" : name) + " " + (d == null ? "" : d.getString());
        return LegacyTextUtil.stripLegacy(raw).trim().toLowerCase(Locale.ROOT);
    }
    static long mix(long h, int v) { return (h ^ v) * 0x100000001B3L; }
    static long mix(long h, UUID v) {
        if (v == null) return mix(h, 0);
        h = (h ^ v.getMostSignificantBits()) * 0x100000001B3L;
        return (h ^ v.getLeastSignificantBits()) * 0x100000001B3L;
    }
    static long mix(long h, String v) { return (h ^ (v != null ? v.hashCode() : 0)) * 0x100000001B3L; }
    static String teamName(PlayerInfo i) { return i != null && i.getTeam() != null && i.getTeam().getName() != null ? i.getTeam().getName() : ""; }
    static String profileName(PlayerInfo i) { GameProfile p = i != null ? i.getProfile() : null; return p != null && p.name() != null ? p.name() : ""; }
    record PlayerRow(UUID id, GameProfile profile, String name, Component displayName, String searchText, CategoryType relation, int relationColor, int latency, boolean spectator) {}
}
