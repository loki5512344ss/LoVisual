package dev.loki.lovisual.features.gui.mainmenu.cloud;

import com.google.gson.JsonObject;
import dev.loki.lovisual.features.command.util.CommandOutput;
import dev.loki.lovisual.features.platform.PlatformSession;
import dev.loki.lovisual.features.platform.config.CloudSlots;
import dev.loki.lovisual.features.platform.config.ConfigCloudUploader;
import dev.loki.lovisual.features.platform.link.DeviceLinkFlow;
import dev.loki.lovisual.features.platform.showcase.ShowcaseClient;
import dev.loki.lovisual.features.platform.showcase.ShowcaseFormat;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Cloud configs on one screen: account link state, the four cloud slots
 * (save/load/publish) and the public showcase (try/copy). A thin view over
 * {@link CloudSlots}, {@link ConfigCloudUploader} and {@link ShowcaseClient};
 * results come back as chat-style status lines shown at the bottom.
 */
public final class CloudScreen extends Screen {

    private static final int PANEL_WIDTH = 340;
    private static final int ROW_HEIGHT = 22;
    private static final long REFRESH_DELAY_MS = 1200;
    private static final int TEXT = 0xFFE6E6E6;
    private static final int MUTED = 0xFF9A9A9A;
    private static final int ACCENT = 0xFFFFD866;

    private final Screen parent;
    private final DeviceLinkFlow linkFlow = new DeviceLinkFlow();
    private List<JsonObject> slots = List.of();
    private ShowcaseClient.Page showcase = new ShowcaseClient.Page(List.of(), false);
    private String sort = "new";
    private int page;
    private String linkCode = "";
    private long refreshAt;
    private boolean slotsRequested;
    private boolean showcaseRequested;

    public CloudScreen(Screen parent) {
        super(Component.literal("LoVisual Cloud"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = (width - PANEL_WIDTH) / 2;
        boolean linked = PlatformSession.get().isLinked();
        addButton(linked ? "Unlink" : "Link", x + PANEL_WIDTH - 60, 14, 60, this::toggleLink);
        for (int i = 0; i < 4 && linked; i++) {
            int slot = i + 1;
            int y = 46 + i * ROW_HEIGHT;
            addButton("Save", x + PANEL_WIDTH - 156, y, 48, () -> afterWrite(() -> ConfigCloudUploader.upload(slot, slotName(slot))));
            addButton("Load", x + PANEL_WIDTH - 104, y, 48, () -> CloudSlots.pull(slot));
            boolean published = isPublished(slot);
            addButton(published ? "Hide" : "Share", x + PANEL_WIDTH - 52, y, 52,
                    () -> afterWrite(() -> togglePublish(slot, published)));
        }
        int showY = 168;
        for (int i = 0; i < showcase.items().size(); i++) {
            String ref = String.valueOf(i + 1);
            int y = showY + 16 + i * ROW_HEIGHT;
            addButton("Try", x + PANEL_WIDTH - 104, y, 48, () -> ShowcaseClient.apply(ref));
            addButton("Copy", x + PANEL_WIDTH - 52, y, 52, () -> afterWrite(() -> ShowcaseClient.copy(ref, firstEmptySlot())));
        }
        addButton(sort.equals("new") ? "Sort: new" : "Sort: popular", x, showY + 16 + 5 * ROW_HEIGHT, 90, this::toggleSort);
        addButton("<", x + 96, showY + 16 + 5 * ROW_HEIGHT, 24, () -> turnPage(-1)).active = page > 0;
        addButton(">", x + 124, showY + 16 + 5 * ROW_HEIGHT, 24, () -> turnPage(1)).active = showcase.hasMore();
        addButton("Done", x + PANEL_WIDTH - 80, height - 28, 80, this::onClose);
        if (linked && !slotsRequested) {
            reloadSlots();
        }
        if (!showcaseRequested) {
            reloadShowcase();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (refreshAt != 0 && System.currentTimeMillis() >= refreshAt) {
            refreshAt = 0;
            reloadSlots();
        }
        g.fill(0, 0, width, height, 0xE0101014);
        int x = (width - PANEL_WIDTH) / 2;
        boolean linked = PlatformSession.get().isLinked();
        g.text(font, "LoVisual Cloud", x, 14, ACCENT);
        g.text(font, linked ? "Linked" : (linkCode.isEmpty() ? "Not linked" : "Enter code on the site: " + linkCode),
                x, 28, linked ? 0xFF7CE38B : MUTED);
        if (linked) {
            for (int i = 0; i < 4; i++) {
                g.text(font, slotText(i + 1), x, 52 + i * ROW_HEIGHT, TEXT);
            }
        } else {
            g.text(font, "Link your account to use cloud slots.", x, 52, MUTED);
        }
        g.text(font, "Showcase (" + sort + ", page " + (page + 1) + ")", x, 168, ACCENT);
        List<JsonObject> items = showcase.items();
        for (int i = 0; i < items.size(); i++) {
            g.text(font, font.plainSubstrByWidth(ShowcaseFormat.listingLine(i + 1, items.get(i)), PANEL_WIDTH - 112),
                    x, 184 + 4 + i * ROW_HEIGHT, TEXT);
        }
        if (items.isEmpty()) {
            g.text(font, "Nothing published yet.", x, 188, MUTED);
        }
        g.text(font, lastStatus(), x, height - 22, MUTED);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public void removed() {
        linkFlow.cancel();
    }

    @Override
    public void onClose() {
        ClientScreen.show(minecraft, parent);
    }

    private Button addButton(String label, int x, int y, int w, Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
                .bounds(x, y, w, 20).build());
    }

    private void toggleLink() {
        if (PlatformSession.get().isLinked()) {
            DeviceLinkFlow.unlink(result -> {
                slots = List.of();
                slotsRequested = false;
                rebuildWidgets();
            });
            return;
        }
        linkFlow.start(result -> {
            switch (result.status()) {
                case AWAITING_CONFIRMATION -> linkCode = result.userCode();
                case LINKED -> {
                    linkCode = "";
                    reloadSlots();
                    rebuildWidgets();
                }
                case FAILED -> {
                    linkCode = "";
                    CommandOutput.error("Link failed: " + result.message());
                }
            }
        });
    }

    private void toggleSort() {
        sort = sort.equals("new") ? "popular" : "new";
        page = 0;
        reloadShowcase();
    }

    private void turnPage(int delta) {
        page = Math.max(0, page + delta);
        reloadShowcase();
    }

    private void reloadShowcase() {
        showcaseRequested = true;
        ShowcaseClient.fetchPage(sort, page, result -> {
            showcase = result == null ? new ShowcaseClient.Page(List.of(), false) : result;
            rebuildWidgets();
        });
    }

    private void reloadSlots() {
        slotsRequested = true;
        CloudSlots.fetch(result -> {
            slots = result == null ? List.of() : result;
            rebuildWidgets();
        });
    }

    private void afterWrite(Runnable write) {
        write.run();
        refreshAt = System.currentTimeMillis() + REFRESH_DELAY_MS;
    }

    private void togglePublish(int slot, boolean published) {
        if (published) {
            ShowcaseClient.unpublish(slot);
        } else {
            ShowcaseClient.publish(slot, slotName(slot));
        }
    }

    private JsonObject slotData(int slot) {
        return slots.stream().filter(s -> s.get("slot").getAsInt() == slot).findFirst().orElse(null);
    }

    private String slotName(int slot) {
        JsonObject data = slotData(slot);
        return data == null ? "Slot " + slot : data.get("name").getAsString();
    }

    private boolean isPublished(int slot) {
        JsonObject data = slotData(slot);
        return data != null && data.has("published") && data.get("published").getAsBoolean();
    }

    private int firstEmptySlot() {
        for (int slot = 1; slot <= 4; slot++) {
            if (slotData(slot) == null) {
                return slot;
            }
        }
        return -1;
    }

    private String slotText(int slot) {
        JsonObject data = slotData(slot);
        return data == null ? slot + ". (empty)" : CloudSlots.slotLine(data);
    }

    private static String lastStatus() {
        List<CommandOutput.MessageLine> messages = CommandOutput.recentMessages();
        return messages.isEmpty() ? "" : messages.get(messages.size() - 1).text().getString();
    }
}
