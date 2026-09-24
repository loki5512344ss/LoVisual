package dev.loki.lovisual.features.gui.mainmenu.alt.render.cards;

import dev.loki.lovisual.features.account.AccountConfig;
import dev.loki.lovisual.features.account.AccountEntry;
import dev.loki.lovisual.features.gui.mainmenu.alt.AltManagerController;
import dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics;
import dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.Layout;
import dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.PanelColors;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.*;

/**
 * Accounts list of the alt manager: scrollable card grid, per-card animations and click routing.
 */
public final class AltCardsRenderer {
    final Map<String, Float> cardHoverAnims = new HashMap<>();
    final Map<String, Float> cardActiveAnims = new HashMap<>();
    final Map<String, Float> cardPressAnims = new HashMap<>();
    final Map<String, Float> pinHoverAnims = new HashMap<>();
    final Map<String, Float> pinPressAnims = new HashMap<>();
    final Map<String, Float> deleteHoverAnims = new HashMap<>();
    final Map<String, Float> deletePressAnims = new HashMap<>();

    private final TextRenderer titleRenderer;
    private final TextRenderer bodyRenderer;
    private final AltCardRenderer cardRenderer;
    private final AccountConfig accountConfig = AccountConfig.get();

    public AltCardsRenderer(TextRenderer titleRenderer, TextRenderer bodyRenderer, TextRenderer menuIconRenderer, TextRenderer guiIconRenderer) {
        this.titleRenderer = titleRenderer;
        this.bodyRenderer = bodyRenderer;
        this.cardRenderer = new AltCardRenderer(accountConfig, titleRenderer, bodyRenderer, menuIconRenderer, guiIconRenderer);
    }

    public void draw(GuiGraphicsExtractor context, AltManagerController controller, float mouseX, float mouseY,
                     Layout l, PanelColors c) {
        float scrollOffset = controller.getScrollOffset();
        updateAnims(controller, mouseX, mouseY, l, scrollOffset);
        cardRenderer.drawActivePanel(context, l.leftX(), l.bottomY(), c);
        drawAccountsPanel(context, mouseX, mouseY, l.rightX(), l.topY(), c, scrollOffset);
    }

    public boolean handleClick(AltManagerController controller, float mouseX, float mouseY) {
        Layout l = controller.layout();
        float scrollOffset = controller.getScrollOffset();

        float listX = l.rightX() + 5f * SCALE;
        float listY = l.topY() + 28f * SCALE;
        float listW = RIGHT_W - 10f * SCALE;
        float listH = RIGHT_H - 31f * SCALE;
        if (!inside(mouseX, mouseY, listX, listY, listW, listH)) return false;

        List<AccountEntry> accounts = accountConfig.getSortedAccounts();
        float cardW = (listW - CARD_GAP) * 0.5f;
        for (int i = 0; i < accounts.size(); i++) {
            int col = i % 2;
            int row = i / 2;
            float cardX = listX + col * (cardW + CARD_GAP);
            float cardY = listY + row * (CARD_H + CARD_GAP) - scrollOffset;
            if (cardY + CARD_H < listY || cardY > listY + listH) continue;

            float btnY = cardY + CARD_H - CARD_BTN - 5f * SCALE;
            float pinX = cardX + cardW - CARD_BTN * 2f - 8f * SCALE;
            float delX = cardX + cardW - CARD_BTN - 5f * SCALE;
            AccountEntry entry = accounts.get(i);

            if (inside(mouseX, mouseY, pinX, btnY, CARD_BTN, CARD_BTN)) {
                pinPressAnims.put(entry.getName(), 1f);
                entry.togglePinned();
                if (entry.isPinned()) accountConfig.setActiveAccount(entry);
                else accountConfig.save();
                return true;
            }
            if (inside(mouseX, mouseY, delX, btnY, CARD_BTN, CARD_BTN)) {
                deletePressAnims.put(entry.getName(), 1f);
                accountConfig.removeAccountBySortedIndex(i);
                controller.clampScroll();
                return true;
            }
            if (inside(mouseX, mouseY, cardX, cardY, cardW, CARD_H)) {
                cardPressAnims.put(entry.getName(), 1f);
                accountConfig.setActiveAccount(entry);
                return true;
            }
        }
        return false;
    }

    private void drawAccountsPanel(GuiGraphicsExtractor context, float mouseX, float mouseY, float x, float y, PanelColors c, float scrollOffset) {
        AltUiMetrics.draw(titleRenderer, tr("screen.lovisual.alt_manager.accounts_list"), x + 8f * SCALE, y + 7f * SCALE, TITLE_FONT, c.title());

        List<AccountEntry> accounts = accountConfig.getSortedAccounts();
        if (accounts.isEmpty()) {
            drawCentered(bodyRenderer, tr("screen.lovisual.alt_manager.no_accounts_added"), x + RIGHT_W * 0.5f, y + RIGHT_H * 0.5f + 2f * SCALE, EMPTY_FONT, c.mutedLabel());
            return;
        }

        float listX = x + 5f * SCALE;
        float listY = y + 28f * SCALE;
        float listW = RIGHT_W - 10f * SCALE;
        float listH = RIGHT_H - 31f * SCALE;
        float cardW = (listW - CARD_GAP) * 0.5f;

        boolean clipped = ScissorFunction.pushRaw(listX, listY - 3f * SCALE, listW, listH + 6f * SCALE);
        try {
            for (int i = 0; i < accounts.size(); i++) {
                int col = i % 2;
                int row = i / 2;
                float cardX = listX + col * (cardW + CARD_GAP);
                float cardY = listY + row * (CARD_H + CARD_GAP) - scrollOffset;
                if (cardY + CARD_H < listY - 10f || cardY > listY + listH + 10f) continue;
                cardRenderer.renderCard(this, context, accounts.get(i), mouseX, mouseY, cardX, cardY, cardW, listY, listH, c);
            }
        } finally {
            if (clipped) ScissorFunction.pop();
        }
    }

    private void updateAnims(AltManagerController controller, float mouseX, float mouseY, Layout l, float scrollOffset) {
        float dt = AnimationUtility.deltaTime();

        float listX = l.rightX() + 5f * SCALE;
        float listY = l.topY() + 28f * SCALE;
        float listW = RIGHT_W - 10f * SCALE;
        float listH = RIGHT_H - 31f * SCALE;
        float cardW = (listW - CARD_GAP) * 0.5f;
        String active = accountConfig.getActiveAccountName();
        List<AccountEntry> accounts = accountConfig.getSortedAccounts();
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < accounts.size(); i++) {
            AccountEntry entry = accounts.get(i);
            String key = entry.getName();
            keys.add(key);
            int col = i % 2;
            int row = i / 2;
            float cardX = listX + col * (cardW + CARD_GAP);
            float cardY = listY + row * (CARD_H + CARD_GAP) - scrollOffset;
            float btnY = cardY + CARD_H - CARD_BTN - 5f * SCALE;
            float pinX = cardX + cardW - CARD_BTN * 2f - 8f * SCALE;
            float delX = cardX + cardW - CARD_BTN - 5f * SCALE;
            boolean inList = mouseY >= listY && mouseY <= listY + listH;
            boolean cardHover = inList && inside(mouseX, mouseY, cardX, cardY, cardW, CARD_H);
            boolean pinHover = inList && inside(mouseX, mouseY, pinX, btnY, CARD_BTN, CARD_BTN);
            boolean delHover = inList && inside(mouseX, mouseY, delX, btnY, CARD_BTN, CARD_BTN);
            cardHoverAnims.put(key, animate(cardHoverAnims.getOrDefault(key, 0f), cardHover ? 1f : 0f, dt, 10f));
            cardActiveAnims.put(key, animate(cardActiveAnims.getOrDefault(key, 0f), key.equals(active) ? 1f : 0f, dt, 8f));
            cardPressAnims.put(key, animate(cardPressAnims.getOrDefault(key, 0f), 0f, dt, 7f));
            pinHoverAnims.put(key, animate(pinHoverAnims.getOrDefault(key, 0f), pinHover ? 1f : 0f, dt, 11f));
            pinPressAnims.put(key, animate(pinPressAnims.getOrDefault(key, 0f), 0f, dt, 8f));
            deleteHoverAnims.put(key, animate(deleteHoverAnims.getOrDefault(key, 0f), delHover ? 1f : 0f, dt, 11f));
            deletePressAnims.put(key, animate(deletePressAnims.getOrDefault(key, 0f), 0f, dt, 8f));
        }
        prune(cardHoverAnims, keys);
        prune(cardActiveAnims, keys);
        prune(cardPressAnims, keys);
        prune(pinHoverAnims, keys);
        prune(pinPressAnims, keys);
        prune(deleteHoverAnims, keys);
        prune(deletePressAnims, keys);
    }
}
