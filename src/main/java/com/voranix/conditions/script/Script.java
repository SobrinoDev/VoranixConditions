package com.voranix.conditions.script;

import java.util.List;

public record Script(String name, String description, boolean enabledByDefault, List<Trigger> triggers) {
}
