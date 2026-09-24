package dev.loki.lovisual.features.gui.mainmenu.alt;

import dev.loki.lovisual.features.account.AccountConfig;
import dev.loki.lovisual.features.account.AccountEntry;
import dev.loki.lovisual.features.account.SkinManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Account mutation helpers for the alt manager screen.
 */
public final class AltAccountOps {
    private static final DateTimeFormatter ACCOUNT_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private AltAccountOps() {
    }

    public static void addAccount(AccountConfig config, String nickname) {
        String clean = normalize(nickname);
        if (clean.isEmpty()) return;
        AccountEntry entry = config.addAccount(clean, LocalDateTime.now().format(ACCOUNT_DATE), null);
        if (entry != null) {
            config.setActiveAccount(entry);
            SkinManager.getSkin(clean);
        }
    }

    public static String normalize(String s) {
        return s == null ? "" : s.trim();
    }

    public static String generateRandomNickname(AccountConfig config) {
        Random random = new Random();
        StringBuilder username = new StringBuilder();
        char[] vowels = {'a', 'e', 'i', 'o', 'u'};
        char[] consonants = {'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'n', 'p', 'r', 's', 't', 'v', 'w', 'x', 'y', 'z'};
        for (int attempts = 0; attempts < 10; attempts++) {
            username.setLength(0);
            int length = 6 + random.nextInt(5);
            boolean startVowel = random.nextBoolean();
            for (int i = 0; i < length; i++) {
                username.append((i & 1) == 0 ? (startVowel ? vowels[random.nextInt(vowels.length)] : consonants[random.nextInt(consonants.length)]) : (startVowel ? consonants[random.nextInt(consonants.length)] : vowels[random.nextInt(vowels.length)]));
            }
            if (random.nextInt(100) < 30) username.append(random.nextInt(100));
            String candidate = Character.toUpperCase(username.charAt(0)) + username.substring(1);
            if (config.findByName(candidate) == null) return candidate;
        }
        return "User" + (System.currentTimeMillis() % 1000L);
    }
}
