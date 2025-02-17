package com.magmaguy.elitemobs.config.customitems.premade;

import com.magmaguy.elitemobs.config.customitems.CustomItemsConfigFields;
import org.bukkit.Material;

import java.util.Arrays;

public class SummonMerchantScrollConfig extends CustomItemsConfigFields {
    public SummonMerchantScrollConfig() {
        super("summon_merchant_scroll",
                true,
                Material.PAPER,
                "&6Summon Merchant Scroll",
                Arrays.asList("&aNeed to sell an item?", "&aRight-click to activate", "&aor yell &9Jeeves!"));
        setEnchantments(Arrays.asList("SUMMON_MERCHANT,1"));
        setDropWeight("5");
    }
}
