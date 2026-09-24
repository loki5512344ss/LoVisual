package dev.loki.lovisual.features.gui.mainmenu.alt;

import dev.loki.lovisual.features.account.AccountConfig;
import dev.loki.lovisual.features.account.SkinManager;
import dev.loki.lovisual.features.gui.mainmenu.alt.auth.AltMicrosoftAuth;
import dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.Layout;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.math.HudScale;
import net.minecraft.client.Minecraft;

import static dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.*;

/**
 * Input/state controller for the alt manager screen: nickname entry, mode toggling, scrolling and left-panel click routing.
 */
public final class AltManagerController {
    private final AccountConfig accountConfig;
    private final AltMicrosoftAuth auth;
    private final AltPanelAnims anims = new AltPanelAnims();
    private boolean microsoftMode;
    private String nicknameText = "";
    private boolean nicknameFieldFocused;
    private float fixedWidth;
    private float fixedHeight;
    private float scrollOffset;
    private float targetScrollOffset;

    public AltManagerController(AccountConfig accountConfig, AltMicrosoftAuth auth) {
        this.accountConfig = accountConfig;
        this.auth = auth;
    }

    public final AltPanelAnims getAnims() { return anims; }

    public final boolean isMicrosoftMode() { return microsoftMode; }

    public final void setMicrosoftMode(boolean microsoftMode) { this.microsoftMode = microsoftMode; }

    public final String getNickname() { return nicknameText; }

    public final boolean isNicknameFocused() { return nicknameFieldFocused; }

    public final float getScrollOffset() { return scrollOffset; }

    public final float getFixedWidth() { return fixedWidth; }

    public final float getFixedHeight() { return fixedHeight; }

    public void updateUiMetrics(Minecraft mc, int width, int height) {
        if (mc == null) {
            fixedWidth = width;
            fixedHeight = height;
            return;
        }
        int fbw = mc.getWindow().getWidth();
        int fbh = mc.getWindow().getHeight();
        fixedWidth = Math.max(1f, HudScale.virtualWidth(fbw, fbh));
        fixedHeight = Math.max(1f, HudScale.virtualHeight(fbw, fbh));
    }

    public void clampScroll() {
        float max = Math.max(0f, (float) Math.ceil(accountConfig.getSortedAccounts().size() / 2.0) * (CARD_H + CARD_GAP) - (RIGHT_H - 31f * SCALE));
        scrollOffset = Math.min(Math.max(scrollOffset, 0f), max);
        targetScrollOffset = Math.min(Math.max(targetScrollOffset, 0f), max);
    }

    public boolean scrollBy(float mouseX, float mouseY, float amount) {
        Layout l = layout();
        if (!inside(mouseX, mouseY, l.rightX(), l.topY(), RIGHT_W, RIGHT_H)) return false;
        targetScrollOffset -= amount * (25f * SCALE);
        clampScroll();
        return true;
    }

    public void updateScroll() {
        float dt = AnimationUtility.deltaTime();
        scrollOffset = animate(scrollOffset, targetScrollOffset, dt, 14f);
        scrollOffset = AnimationUtility.snap(scrollOffset, targetScrollOffset, 0.25f);
    }

    public Layout layout() {
        float totalW = LEFT_W + GAP + RIGHT_W;
        float totalH = LEFT_TOP_H + GAP + LEFT_BOTTOM_H;
        return new Layout(
                fixedWidth * 0.5f - totalW * 0.5f,
                fixedHeight * 0.5f - totalH * 0.5f,
                fixedWidth * 0.5f - totalW * 0.5f + LEFT_W + GAP,
                fixedHeight * 0.5f - totalH * 0.5f + LEFT_TOP_H + GAP
        );
    }

    public boolean handleClick(float mouseX, float mouseY) {
        Layout l = layout();
        float modeY = l.topY() + 27f * SCALE;
        float modeX = l.leftX() + 5f * SCALE;
        float modeW = LEFT_W - 10f * SCALE;
        float modeGap = 2f * SCALE;
        float modeItemW = (modeW - modeGap) * 0.5f;
        if (inside(mouseX, mouseY, modeX, modeY, modeItemW, 13f * SCALE)) {
            anims.pressOfflineMode();
            microsoftMode = false;
            nicknameFieldFocused = false;
            return true;
        }
        if (inside(mouseX, mouseY, modeX + modeItemW + modeGap, modeY, modeItemW, 13f * SCALE)) {
            anims.pressMicrosoftMode();
            microsoftMode = true;
            nicknameFieldFocused = false;
            return true;
        }
        float fieldX = l.leftX() + 5f * SCALE;
        float fieldY = l.topY() + 57f * SCALE;
        float fieldW = LEFT_W - 10f * SCALE - ADD_SIZE - 3f * SCALE;
        if (!microsoftMode && inside(mouseX, mouseY, fieldX, fieldY, fieldW, FIELD_H)) {
            nicknameFieldFocused = true;
            return true;
        }
        nicknameFieldFocused = false;
        if (microsoftMode && inside(mouseX, mouseY, fieldX, fieldY, fieldW, FIELD_H)) {
            auth.copyAuthData();
            return true;
        }
        float addX = fieldX + fieldW + 3f * SCALE;
        if (inside(mouseX, mouseY, addX, fieldY, ADD_SIZE, ADD_SIZE)) {
            anims.pressAdd();
            if (microsoftMode) auth.start();
            else commitNickname();
            return true;
        }
        float buttonX = l.leftX() + 5f * SCALE;
        float buttonW = LEFT_W - 10f * SCALE;
        float randomY = fieldY + FIELD_H + 5f * SCALE;
        float openSiteY = randomY + TOP_BUTTON_H + 5f * SCALE;
        float clearY = (microsoftMode ? openSiteY : randomY) + TOP_BUTTON_H + 5f * SCALE;
        if (inside(mouseX, mouseY, buttonX, randomY, buttonW, TOP_BUTTON_H)) {
            anims.pressRandom();
            if (microsoftMode) {
                if (auth.getUserCode().isBlank()) auth.start();
                else auth.copyAuthData();
            } else {
                AltAccountOps.addAccount(accountConfig, AltAccountOps.generateRandomNickname(accountConfig));
                nicknameText = "";
            }
            return true;
        }
        if (microsoftMode && inside(mouseX, mouseY, buttonX, openSiteY, buttonW, TOP_BUTTON_H)) {
            anims.pressOpenSite();
            auth.openLoginSite();
            return true;
        }
        if (inside(mouseX, mouseY, buttonX, clearY, buttonW, TOP_BUTTON_H)) {
            anims.pressClear();
            accountConfig.clearAllAccounts();
            SkinManager.clearCache();
            scrollOffset = 0f;
            targetScrollOffset = 0f;
            return true;
        }
        return false;
    }

    public void commitNickname() {
        if (!nicknameText.isBlank()) {
            AltAccountOps.addAccount(accountConfig, nicknameText);
            nicknameText = "";
        }
    }

    public boolean handleKey(int key) {
        if (key == 259) {
            if (!nicknameText.isEmpty()) nicknameText = nicknameText.substring(0, nicknameText.length() - 1);
            return true;
        }
        if (key == 257 || key == 335) {
            commitNickname();
            nicknameFieldFocused = false;
            return true;
        }
        if (key == 256) {
            nicknameFieldFocused = false;
            return true;
        }
        return false;
    }

    public boolean handleChar(int codepoint) {
        if ((Character.isLetterOrDigit(codepoint) || codepoint == '_') && nicknameText.length() < 16) {
            nicknameText += Character.toString(codepoint);
            return true;
        }
        return false;
    }
}
