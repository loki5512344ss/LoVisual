package dev.loki.lovisual.features.command.suggest;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits raw command suggestions into pickable completions and a usage-hint placeholder
 * (an entry like "%toggle &lt;module&gt;"), so the vanilla chat popup lists only real choices
 * and shows the hint as ghost text instead of inserting it.
 *
 * @param suggestions pickable completions, all sharing the first one's range; null when none
 * @param ghostHint   placeholder such as "&lt;module&gt;" to show after the cursor, or null
 */
public record SuggestionPlan(Suggestions suggestions, String ghostHint) {

    public static SuggestionPlan of(List<Suggestion> raw) {
        List<Suggestion> real = new ArrayList<>();
        String hint = null;
        if (raw != null) {
            for (Suggestion s : raw) {
                String text = s.getText();
                int open = text.lastIndexOf('<');
                if (open >= 0 && text.endsWith(">")) {
                    if (hint == null) hint = text.substring(open);
                } else {
                    real.add(s);
                }
            }
        }
        if (real.isEmpty()) return new SuggestionPlan(null, hint);
        StringRange range = real.get(0).getRange();
        return new SuggestionPlan(new Suggestions(range, real), hint);
    }
}
