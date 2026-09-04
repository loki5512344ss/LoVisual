/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.config.values;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SetValue extends ConfigValue<Set<String>> {

    public SetValue(String name) {
        super(name, new LinkedHashSet<>());
    }

    public SetValue(String name, Set<String> def) {
        super(name, new LinkedHashSet<>(def));
    }

    @Override
    public void set(Set<String> v) {
        value = v == null ? new LinkedHashSet<>() : new LinkedHashSet<>(v);
    }

    @Override
    public Object toJson() {
        return new ArrayList<>(value); // JSON array
    }

    @Override
    public void fromJson(Object json) {
        if (json instanceof List<?> list) {
            LinkedHashSet<String> copy = new LinkedHashSet<>();
            for (Object o : list) {
                if (o instanceof String s)
                    copy.add(s);
            }
            value = copy;
        }
    }
}
