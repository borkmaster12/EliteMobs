package com.magmaguy.elitemobs.config.custombosses.premade;

import com.magmaguy.elitemobs.config.custombosses.CustomBossesConfigFields;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class WoodLeagueWave3Ranged extends CustomBossesConfigFields {
    public WoodLeagueWave3Ranged() {
        super("wood_league_wave_3_ranged",
                EntityType.PILLAGER,
                true,
                "$normalLevel Arena Crossbowman",
                "3");
        setMainHand(new ItemStack(Material.CROSSBOW));
        setFollowDistance(60);
        setPowers(Arrays.asList("attack_fire.yml"));
    }
}
