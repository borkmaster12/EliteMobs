package com.magmaguy.elitemobs.config.customevents.premade;

import com.magmaguy.elitemobs.config.customevents.CustomEventsConfigFields;
import com.magmaguy.elitemobs.events.CustomEvent;

import java.util.Arrays;

public class WeaponsGoblinEvent extends CustomEventsConfigFields {
    public WeaponsGoblinEvent() {
        super("weapons_goblin", true);
        setEventType(CustomEvent.EventType.TIMED);
        setBossFilenames(Arrays.asList("weapons_goblin.yml"));
        setLocalCooldown(120D);
        setGlobalCooldown(25D);
        setWeight(100D);
        setCustomSpawn("normal_surface_spawn.yml");
        setEventDuration(30);
    }
}
