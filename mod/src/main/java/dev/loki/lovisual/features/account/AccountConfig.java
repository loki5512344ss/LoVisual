package dev.loki.lovisual.features.account;

import net.minecraft.resources.Identifier;
import dev.loki.lovisual.config.definition.ConfigNameProvider;
import dev.loki.lovisual.config.io.ConfigSerializer;
import dev.loki.lovisual.config.io.JsonConfigObject;
import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.config.values.primitive.StringValue;
import dev.loki.lovisual.util.session.MicrosoftSessionResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Singleton config + API for the alt-account manager. Holds the persisted account list and the
 * active / authorization-required account fields, and stays the public entry point consumed by the
 * ClickGUI alt screens. Behavior is delegated within the {@code features.account} package:
 * {@link AccountsValue} (JSON), {@link AccountStore} (collection CRUD) and {@link AccountSession}
 * (active-account application + auth flag), keeping this facade under the file-size cap.
 */
public final class AccountConfig implements JsonConfigObject, ConfigNameProvider {

    public static final AccountConfig INSTANCE = new AccountConfig();

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    final AccountsValue accounts = new AccountsValue("accounts");
    final StringValue activeAccountName = new StringValue("active_name", "");
    final StringValue activeAccountDate = new StringValue("active_date", "");
    final StringValue activeAccountSkin = new StringValue("active_skin", "");
    String microsoftAuthorizationRequiredName = "";

    private AccountConfig() {
        ConfigSerializer.load(this);
        AccountSession.applyActiveSession(this);
    }

    public static AccountConfig get() {
        return INSTANCE;
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    static String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    static String nowString() {
        return LocalDateTime.now().format(DATE_FORMAT);
    }

    static Identifier parseIdentifier(String raw) {
        if (isBlank(raw)) {
            return null;
        }
        try {
            return Identifier.parse(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    static UUID parseUuid(String raw) {
        if (isBlank(raw)) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    public List<AccountEntry> getAccounts() {
        return AccountStore.getAccounts(this);
    }

    public List<AccountEntry> getSortedAccounts() {
        return AccountStore.getSortedAccounts(this);
    }

    public AccountEntry getAccountBySortedIndex(int sortedIndex) {
        return AccountStore.getAccountBySortedIndex(this, sortedIndex);
    }

    public AccountEntry findByName(String name) {
        return AccountStore.findByName(this, name);
    }

    public AccountEntry addAccount(String name) {
        return AccountStore.addAccount(this, name);
    }

    public AccountEntry addAccount(String name, String date, Identifier skin) {
        return AccountStore.addAccount(this, name, date, skin);
    }

    public AccountEntry addMicrosoftAccount(MicrosoftSessionResult session) {
        return AccountStore.addMicrosoftAccount(this, session);
    }

    public void removeAccount(AccountEntry entry) {
        AccountStore.removeAccount(this, entry);
    }

    public void removeAccountBySortedIndex(int sortedIndex) {
        AccountStore.removeAccountBySortedIndex(this, sortedIndex);
    }

    public void clearAllAccounts() {
        AccountStore.clearAllAccounts(this);
    }

    public void togglePin(int sortedIndex) {
        AccountStore.togglePin(this, sortedIndex);
    }

    public void setActiveAccount(AccountEntry entry) {
        AccountSession.setActiveAccount(this, entry);
    }

    public void setActiveAccount(String name, String date, Identifier skin) {
        AccountSession.setActiveAccount(this, name, date, skin);
    }

    public void clearActiveAccount() {
        AccountSession.clearActiveAccount(this);
    }

    public String getActiveAccountName() {
        return AccountSession.getActiveAccountName(this);
    }

    public String getActiveAccountDate() {
        return AccountSession.getActiveAccountDate(this);
    }

    public Identifier getActiveAccountSkin() {
        return AccountSession.getActiveAccountSkin(this);
    }

    public boolean isMicrosoftAuthorizationRequired() {
        return AccountSession.isMicrosoftAuthorizationRequired(this);
    }

    public AccountEntry getMicrosoftAuthorizationRequiredAccount() {
        return AccountSession.getMicrosoftAuthorizationRequiredAccount(this);
    }

    public void clearMicrosoftAuthorizationRequired() {
        AccountSession.clearMicrosoftAuthorizationRequired(this);
    }

    public void save() {
        AccountStore.reindex(this);
        ConfigSerializer.requestSave(this);
    }

    @Override
    public String getConfigName() {
        return "accounts";
    }

    @Override
    public List<ConfigValue<?>> getConfigValues() {
        List<ConfigValue<?>> values = new ArrayList<>();
        values.add(accounts);
        values.add(activeAccountName);
        values.add(activeAccountDate);
        values.add(activeAccountSkin);
        return values;
    }
}
