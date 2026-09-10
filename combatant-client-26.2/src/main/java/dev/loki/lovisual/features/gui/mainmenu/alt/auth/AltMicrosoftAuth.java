/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.mainmenu.alt.auth;

import dev.loki.lovisual.features.account.AccountConfig;
import dev.loki.lovisual.features.gui.mainmenu.alt.AltAccountOps;
import dev.loki.lovisual.features.account.AccountEntry;
import dev.loki.lovisual.features.account.SkinManager;
import dev.loki.lovisual.util.session.MicrosoftSessionResult;
import dev.loki.lovisual.util.session.microsoft.auth.MicrosoftAuthService;
import dev.loki.lovisual.util.session.microsoft.auth.MicrosoftDeviceCode;
import dev.loki.lovisual.util.text.edit.ClipboardUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static dev.loki.lovisual.features.gui.mainmenu.alt.render.AltUiMetrics.tr;

/**
 * Microsoft device-code authentication flow state for the alt manager screen.
 */
public final class AltMicrosoftAuth {
    private final AccountConfig accountConfig;
    private Runnable onSettled = () -> {
    };

    private boolean authPending;
    private String userCode = "";
    private String verificationUrl = "";
    private String status = "";

    public AltMicrosoftAuth(AccountConfig accountConfig) {
        this.accountConfig = accountConfig;
    }

    public void setOnSettled(Runnable onSettled) {
        this.onSettled = onSettled == null ? () -> {
        } : onSettled;
    }

    public boolean isAuthPending() {
        return authPending;
    }

    public String getUserCode() {
        return userCode;
    }

    public String getVerificationUrl() {
        return verificationUrl;
    }

    public String getStatus() {
        return status;
    }

    public void start() {
        if (authPending) {
            copyAuthData();
            return;
        }

        authPending = true;
        userCode = "";
        verificationUrl = "";
        status = tr("screen.lovisual.alt_manager.status.requesting_code");

        MicrosoftAuthService.requestDeviceCode()
                .thenCompose(code -> {
                    dispatchToClient(() -> showDeviceCode(code));
                    return MicrosoftAuthService.loginWithDeviceCode(code);
                })
                .whenComplete((session, throwable) -> dispatchToClient(() -> {
                    if (throwable != null) {
                        finish(null, throwable);
                    } else {
                        finish(session, null);
                    }
                }));
    }

    private void showDeviceCode(MicrosoftDeviceCode code) {
        if (code == null) {
            return;
        }
        userCode = code.userCode();
        verificationUrl = code.verificationUri().toString();
        status = tr("screen.lovisual.alt_manager.status.use_link");
        copyAuthData();
    }

    private void finish(MicrosoftSessionResult session, Throwable throwable) {
        authPending = false;
        if (throwable != null) {
            status = tr("screen.lovisual.alt_manager.status.auth_failed");
            userCode = "";
            verificationUrl = "";
            Throwable root = unwrap(throwable);
            if (root != null && root.getMessage() != null && !root.getMessage().isBlank()) {
                status = ellipsizeStatus(root.getMessage());
            }
            return;
        }

        AccountEntry entry = accountConfig.addMicrosoftAccount(session);
        if (entry == null) {
            status = tr("screen.lovisual.alt_manager.status.auth_failed");
            userCode = "";
            verificationUrl = "";
            return;
        }

        accountConfig.setActiveAccount(entry);
        SkinManager.getSkin(entry.getName());
        status = tr("screen.lovisual.alt_manager.status.authorized", entry.getName());
        userCode = "";
        verificationUrl = "";
        onSettled.run();
    }

    public void copyAuthData() {
        if (!userCode.isBlank()) {
            ClipboardUtil.copy(userCode);
            status = tr("screen.lovisual.alt_manager.status.code_copied");
        } else if (!verificationUrl.isBlank()) {
            ClipboardUtil.copy(verificationUrl);
            status = tr("screen.lovisual.alt_manager.status.link_copied");
        }
    }

    public void openLoginSite() {
        String target = verificationUrl.isBlank() ? "https://www.microsoft.com/link" : verificationUrl;
        Util.getPlatform().openUri(target);
    }

    private static void dispatchToClient(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            runnable.run();
            return;
        }
        if (mc.isSameThread()) {
            runnable.run();
        } else {
            mc.execute(runnable);
        }
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof ExecutionException) && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static String ellipsizeStatus(String text) {
        String clean = AltAccountOps.normalize(text);
        return clean.length() <= 34 ? clean : clean.substring(0, 31) + "...";
    }
}
