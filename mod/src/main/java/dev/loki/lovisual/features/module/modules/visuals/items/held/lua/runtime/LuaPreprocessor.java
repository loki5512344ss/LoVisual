package dev.loki.lovisual.features.module.modules.visuals.items.held.lua.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Препроцессор скриптов: строка {@code global.x = v;} становится локальной переменной,
 * которая берётся из {@code registry} (живёт между кадрами), а в конец скрипта дописывается
 * сохранение значений обратно. Так скрипту не нужны собственные глобальные таблицы.
 */
public final class LuaPreprocessor {

    private static final Pattern GLOBAL_DECL =
            Pattern.compile("(?m)^[ \\t]*global\\.([A-Za-z_][A-Za-z0-9_]*)[ \\t]*=[ \\t]*(.+?)[ \\t]*;?[ \\t]*$");

    private LuaPreprocessor() {
    }

    public static String process(String source) {
        List<String> names = new ArrayList<>();
        Matcher m = GLOBAL_DECL.matcher(source);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            names.add(name);
            String line = "local " + name + " = registry:getOrDefault('" + name + "', " + m.group(2) + ")";
            m.appendReplacement(out, Matcher.quoteReplacement(line));
        }
        m.appendTail(out);
        for (String name : names) {
            out.append("\nregistry:put('").append(name).append("', ").append(name).append(')');
        }
        return out.toString();
    }
}
