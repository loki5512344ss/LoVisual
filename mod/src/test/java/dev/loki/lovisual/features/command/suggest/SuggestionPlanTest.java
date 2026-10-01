package dev.loki.lovisual.features.command.suggest;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SuggestionPlanTest {
    private static final StringRange RANGE = StringRange.between(1, 3);

    @Test
    void realCompletionsAreKeptInOrder() {
        SuggestionPlan plan = SuggestionPlan.of(List.of(new Suggestion(RANGE, "toggle"), new Suggestion(RANGE, "t")));
        assertEquals(2, plan.suggestions().getList().size());
        assertEquals(RANGE, plan.suggestions().getRange());
        assertNull(plan.ghostHint());
    }

    @Test
    void usageHintBecomesGhostTextNotACompletion() {
        SuggestionPlan plan = SuggestionPlan.of(List.of(new Suggestion(RANGE, "%toggle <module>")));
        assertNull(plan.suggestions());
        assertEquals("<module>", plan.ghostHint());
    }

    @Test
    void mixedListSeparatesHintFromChoices() {
        SuggestionPlan plan = SuggestionPlan.of(List.of(new Suggestion(RANGE, "%bind <key>"), new Suggestion(RANGE, "esp")));
        assertEquals(1, plan.suggestions().getList().size());
        assertEquals("<key>", plan.ghostHint());
    }

    @Test
    void emptyOrNullInputGivesNothing() {
        assertNull(SuggestionPlan.of(null).suggestions());
        assertNull(SuggestionPlan.of(List.of()).ghostHint());
    }
}
