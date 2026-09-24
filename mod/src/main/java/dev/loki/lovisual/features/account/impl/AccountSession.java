package dev.loki.lovisual.features.account;

import dev.loki.lovisual.util.logging.DebugLog;
import dev.loki.lovisual.util.session.SessionChanger;
import dev.loki.lovisual.util.session.microsoft.auth.MicrosoftAuthService;
import net.minecraft.resources.Identifier;

/**
 * Owns the "current session" concerns of {@link AccountConfig}: applying the active account
 * (offline rename, launcher-session restore, or Microsoft refresh-token login) and the active /
 * authorization-required account fields. Async failures are logged and, for Microsoft, recorded
 * via the authorization-required flag. Takes the owning {@link AccountConfig} for package-private
 * field access; lives in a subfolder to keep both this class and the manager under the size caps.
 */
final class AccountSession {

    private AccountSession() { }

    /** Re-apply the persisted active account on startup. */
    static void applyActiveSession(AccountConfig owner) {
        if (AccountConfig.isBlank(owner.activeAccountName.get())) {
            return;
        }
        AccountEntry entry = AccountStore.findByName(owner, owner.activeAccountName.get());
        if (entry != null) {
            applySession(owner, entry);
        } else {
            SessionChanger.changeUsername(owner.activeAccountName.get());
        }
    }

    /** Apply the given account's session, dispatching by auth type and token availability. */
    static void applySession(AccountConfig owner, AccountEntry entry) {
        if (entry == null) {
            return;
        }
        if (!entry.isMicrosoft()) {
            applyOfflineSession(entry);
            return;
        }
        String refreshToken = entry.getMicrosoftRefreshToken();
        if (AccountConfig.isBlank(refreshToken)) {
            applyMicrosoftWithoutToken(owner, entry);
            return;
        }
        applyMicrosoftSession(owner, entry, refreshToken);
    }

    private static void applyOfflineSession(AccountEntry entry) {
        SessionChanger.changeUsername(entry.getName())
                .exceptionally(t -> {
                    DebugLog.error("[AccountConfig] Failed to apply offline account session: " + entry.getName(), t);
                    return null;
                });
    }

    private static void applyMicrosoftWithoutToken(AccountConfig owner, AccountEntry entry) {
        if (SessionChanger.canUseOriginalLauncherSession(entry.getName(), entry.getOnlineUuid())) {
            clearAuthorizationRequired(owner, entry.getName());
            SessionChanger.restoreOriginalLauncherSession(false)
                    .exceptionally(t -> {
                        DebugLog.error("[AccountConfig] Failed to restore launcher Microsoft session: " + entry.getName(), t);
                        markAuthorizationRequired(owner, entry);
                        return null;
                    });
            return;
        }
        markAuthorizationRequired(owner, entry);
        DebugLog.warnOnce("alt-microsoft-token-empty-" + entry.getName(), "[AccountConfig] Microsoft account requires authorization: %s", entry.getName());
    }

    private static void applyMicrosoftSession(AccountConfig owner, AccountEntry entry, String refreshToken) {
        clearAuthorizationRequired(owner, entry.getName());
        MicrosoftAuthService.loginWithRefreshToken(refreshToken)
                .thenCompose(session -> {
                    entry.applyMicrosoftSession(session);
                    owner.activeAccountName.set(session.name());
                    owner.activeAccountDate.set(entry.getDate() == null ? "" : entry.getDate());
                    owner.save();
                    return SessionChanger.changeSession(session.toLoginData());
                })
                .exceptionally(t -> {
                    DebugLog.error("[AccountConfig] Failed to apply Microsoft account session: " + entry.getName(), t);
                    markAuthorizationRequired(owner, entry);
                    return null;
                });
    }

    static void setActiveAccount(AccountConfig owner, AccountEntry entry) {
        if (entry == null) {
            clearActiveAccount(owner);
            return;
        }

        owner.activeAccountName.set(AccountConfig.normalizeName(entry.getName()));
        owner.activeAccountDate.set(entry.getDate() == null ? "" : entry.getDate());
        Identifier skin = entry.getSkin();
        owner.activeAccountSkin.set(skin == null ? "" : skin.toString());
        applySession(owner, entry);
        owner.save();
    }

    static void setActiveAccount(AccountConfig owner, String name, String date, Identifier skin) {
        String cleanName = AccountConfig.normalizeName(name);
        if (cleanName.isEmpty()) {
            clearActiveAccount(owner);
            return;
        }

        AccountEntry entry = AccountStore.findByName(owner, cleanName);
        if (entry == null) {
            entry = AccountStore.addAccount(owner, cleanName, date, skin);
        } else {
            if (!AccountConfig.isBlank(date)) {
                entry.setDate(date);
            }
            if (skin != null) {
                entry.setSkin(skin);
            }
        }
        setActiveAccount(owner, entry);
    }

    static void clearActiveAccount(AccountConfig owner) {
        owner.activeAccountName.set("");
        owner.activeAccountDate.set("");
        owner.activeAccountSkin.set("");
        AccountStore.reindex(owner);
        owner.save();
    }

    static String getActiveAccountName(AccountConfig owner) {
        return owner.activeAccountName.get();
    }

    static String getActiveAccountDate(AccountConfig owner) {
        return owner.activeAccountDate.get();
    }

    static Identifier getActiveAccountSkin(AccountConfig owner) {
        return AccountConfig.parseIdentifier(owner.activeAccountSkin.get());
    }

    static boolean isMicrosoftAuthorizationRequired(AccountConfig owner) {
        return getMicrosoftAuthorizationRequiredAccount(owner) != null;
    }

    static AccountEntry getMicrosoftAuthorizationRequiredAccount(AccountConfig owner) {
        if (AccountConfig.isBlank(owner.microsoftAuthorizationRequiredName)) {
            return null;
        }
        AccountEntry entry = AccountStore.findByName(owner, owner.microsoftAuthorizationRequiredName);
        return entry != null && entry.isMicrosoft() ? entry : null;
    }

    static void clearMicrosoftAuthorizationRequired(AccountConfig owner) {
        owner.microsoftAuthorizationRequiredName = "";
    }

    static void markAuthorizationRequired(AccountConfig owner, AccountEntry entry) {
        owner.microsoftAuthorizationRequiredName = entry == null ? "" : AccountConfig.normalizeName(entry.getName());
    }

    static void clearAuthorizationRequired(AccountConfig owner, String name) {
        if (AccountConfig.normalizeName(name).equalsIgnoreCase(AccountConfig.normalizeName(owner.microsoftAuthorizationRequiredName))) {
            owner.microsoftAuthorizationRequiredName = "";
        }
    }
}
