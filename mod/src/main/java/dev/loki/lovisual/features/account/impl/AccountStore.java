package dev.loki.lovisual.features.account;

import dev.loki.lovisual.util.session.MicrosoftSessionResult;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Collection operations for {@link AccountConfig}: account lookup, add (offline / Microsoft),
 * remove, pin and index maintenance. Each method takes the owning {@link AccountConfig} so the
 * singleton keeps holding the state and public API while this collaborator stays small; active
 * -session and auth concerns live in {@link AccountSession}. Shares the {@code features.account}
 * package (lives in a subfolder) for package-private access to the owner's config values.
 */
final class AccountStore {

    private AccountStore() { }

    static List<AccountEntry> getAccounts(AccountConfig owner) {
        return owner.accounts.get();
    }

    static List<AccountEntry> getSortedAccounts(AccountConfig owner) {
        List<AccountEntry> sorted = new ArrayList<>(owner.accounts.get());
        sorted.sort(Comparator
                .comparing(AccountEntry::isPinned).reversed()
                .thenComparingInt(AccountEntry::getOriginalIndex));
        return sorted;
    }

    static AccountEntry getAccountBySortedIndex(AccountConfig owner, int sortedIndex) {
        List<AccountEntry> sorted = getSortedAccounts(owner);
        if (sortedIndex < 0 || sortedIndex >= sorted.size()) {
            return null;
        }
        return sorted.get(sortedIndex);
    }

    static AccountEntry findByName(AccountConfig owner, String name) {
        String normalized = AccountConfig.normalizeName(name);
        if (normalized.isEmpty()) {
            return null;
        }
        for (AccountEntry entry : owner.accounts.get()) {
            if (AccountConfig.normalizeName(entry.getName()).equals(normalized)) {
                return entry;
            }
        }
        return null;
    }

    static AccountEntry addAccount(AccountConfig owner, String name) {
        return addAccount(owner, name, AccountConfig.nowString(), null);
    }

    static AccountEntry addAccount(AccountConfig owner, String name, String date, Identifier skin) {
        String cleanName = AccountConfig.normalizeName(name);
        if (cleanName.isEmpty()) {
            return null;
        }

        AccountEntry existing = findByName(owner, cleanName);
        if (existing != null) {
            existing.setAuthType(AccountAuthType.OFFLINE);
            existing.setOnlineUuid(null);
            existing.setMicrosoftRefreshToken("");
            AccountSession.clearAuthorizationRequired(owner, existing.getName());
            if (!AccountConfig.isBlank(date)) {
                existing.setDate(date);
            }
            if (skin != null) {
                existing.setSkin(skin);
            }
            owner.save();
            return existing;
        }

        AccountEntry entry = new AccountEntry(cleanName, AccountConfig.isBlank(date) ? AccountConfig.nowString() : date,
                skin, false, owner.accounts.get().size());
        owner.accounts.get().add(entry);
        owner.save();
        return entry;
    }

    static AccountEntry addMicrosoftAccount(AccountConfig owner, MicrosoftSessionResult session) {
        if (session == null || AccountConfig.isBlank(session.name()) || session.uuid() == null
                || AccountConfig.isBlank(session.refreshToken())) {
            return null;
        }

        AccountEntry existing = findByName(owner, session.name());
        if (existing != null) {
            existing.applyMicrosoftSession(session);
            existing.setDate(AccountConfig.nowString());
            AccountSession.clearAuthorizationRequired(owner, existing.getName());
            owner.save();
            return existing;
        }

        AccountEntry entry = new AccountEntry(
                session.name(),
                AccountConfig.nowString(),
                null,
                false,
                owner.accounts.get().size(),
                AccountAuthType.MICROSOFT,
                session.uuid(),
                session.refreshToken()
        );
        owner.accounts.get().add(entry);
        AccountSession.clearAuthorizationRequired(owner, entry.getName());
        owner.save();
        return entry;
    }

    static void removeAccount(AccountConfig owner, AccountEntry entry) {
        if (entry == null) {
            return;
        }
        if (!owner.accounts.get().remove(entry)) {
            return;
        }

        if (isActive(owner, entry)) {
            AccountSession.clearActiveAccount(owner);
        } else {
            reindex(owner);
            owner.save();
        }
    }

    static void removeAccountBySortedIndex(AccountConfig owner, int sortedIndex) {
        removeAccount(owner, getAccountBySortedIndex(owner, sortedIndex));
    }

    static void clearAllAccounts(AccountConfig owner) {
        owner.accounts.get().clear();
        owner.activeAccountName.set("");
        owner.activeAccountDate.set("");
        owner.activeAccountSkin.set("");
        owner.microsoftAuthorizationRequiredName = "";
        owner.save();
    }

    static void togglePin(AccountConfig owner, int sortedIndex) {
        AccountEntry entry = getAccountBySortedIndex(owner, sortedIndex);
        if (entry == null) {
            return;
        }
        entry.togglePinned();
        owner.save();
    }

    static boolean isActive(AccountConfig owner, AccountEntry entry) {
        if (entry == null) {
            return false;
        }
        return AccountConfig.normalizeName(entry.getName()).equals(AccountConfig.normalizeName(owner.activeAccountName.get()));
    }

    static void reindex(AccountConfig owner) {
        List<AccountEntry> list = owner.accounts.get();
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setOriginalIndex(i);
        }
    }
}
