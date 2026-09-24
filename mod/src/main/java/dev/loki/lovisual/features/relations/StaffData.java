package dev.loki.lovisual.features.relations;

import net.minecraft.network.chat.Component;

import java.util.*;

/**
 * Provides sorted staff entries based on PlayerRelations and StaffTracker.
 */
public final class StaffData {

    public static final StaffData INSTANCE = new StaffData();

    private StaffData() {
    }

    public List<StaffEntry> getEntries() {
        Set<String> staffList = PlayerRelations.get().getStaff();
        Map<UUID, StaffTracker.StaffInfo> map = StaffTracker.INSTANCE.all();
        List<StaffEntry> out = new ArrayList<>();

        for (String rawName : staffList) {
            StaffTracker.StaffInfo info = findByName(map, rawName);

            if (info == null) {
                out.add(new StaffEntry(
                        rawName,
                        Component.literal(rawName),
                        StaffTracker.Status.OFFLINE,
                        -1L,
                        0L
                ));
                continue;
            }

            out.add(new StaffEntry(
                    rawName,
                    info.displayName != null
                            ? info.displayName
                            : Component.literal(rawName),
                    info.status,
                    info.offlineSince,
                    info.lastEntityGone
            ));
        }

        out.sort(Comparator.comparing((StaffEntry e) -> sortPriority(e.status))
                .thenComparing(e -> e.name.toLowerCase(Locale.ROOT)));
        return out;
    }

    private static int sortPriority(StaffTracker.Status st) {
        return switch (st) {
            case VANISH -> 0;
            case GM3 -> 1;
            case GM2 -> 2;
            case GM1 -> 3;
            case GM0 -> 4;
            case OFFLINE -> 5;
            case WAITING -> 6;
            default -> 7;
        };
    }

    private static StaffTracker.StaffInfo findByName(Map<UUID, StaffTracker.StaffInfo> map, String name) {
        for (StaffTracker.StaffInfo info : map.values()) {
            if (info.name.equalsIgnoreCase(name)) {
                return info;
            }
        }
        return null;
    }

    public record StaffEntry(
            String name,
            Component displayName,
            StaffTracker.Status status,
            long offlineSince,
            long lastGone
    ) {
    }

}
