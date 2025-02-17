package com.magmaguy.elitemobs.config.mobproperties.premade;

import com.magmaguy.elitemobs.config.mobproperties.MobPropertiesConfigFields;
import org.bukkit.entity.EntityType;

import java.util.Arrays;

public class EliteWardenConfig extends MobPropertiesConfigFields {
    public EliteWardenConfig() {
        super("elite_warden",
                EntityType.WARDEN,
                true,
                "&fLvl &2$level &fElite &3Warden",
                Arrays.asList("$entity &csensed $player &cwas nearby!"),
                45);
    }
}
