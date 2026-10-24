package com.voranix.conditions.mob;

import com.voranix.conditions.script.Trigger;
import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.Map;

/** Definición de un mob personalizado (archivo de mobs/). */
public record CustomMob(
        String id,
        EntityType base,
        String model,
        String name,
        boolean nameVisible,
        Double health,
        Double damage,
        Double speed,
        Double scale,
        Double armor,
        Double knockbackResistance,
        Double followRange,
        boolean silent,
        boolean dropsVanilla,
        String script,
        Map<String, String> animations,
        List<Trigger> behaviors,
        EntityType replace,
        double replaceChance,
        List<String> worlds
) {
}
