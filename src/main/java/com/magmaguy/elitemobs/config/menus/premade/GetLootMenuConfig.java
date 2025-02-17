package com.magmaguy.elitemobs.config.menus.premade;

import com.magmaguy.elitemobs.MetadataHandler;
import com.magmaguy.elitemobs.config.ConfigurationEngine;
import com.magmaguy.elitemobs.config.menus.MenusConfigFields;
import com.magmaguy.elitemobs.utils.ItemStackGenerator;
import com.magmaguy.elitemobs.utils.ItemStackSerializer;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class GetLootMenuConfig extends MenusConfigFields {
    public static String menuName;
    public static ItemStack infoItem;
    public static ItemStack leftArrowItem, rightArrowItem, previousLootItem, nextLootItem;
    public static String tierTranslation, itemFilterTranslation;

    public GetLootMenuConfig() {
        super("get_loot_menu", true);
    }

    @Override
    public void processAdditionalFields() {

        menuName = ConfigurationEngine.setString(fileConfiguration, "Menu name", "[EM] Getloot menu");

        ItemStackSerializer.serialize(
                "Info button",
                ItemStackGenerator.generateSkullItemStack("magmaguy",
                        "&4&lEliteMobs &r&cby &4&lMagmaGuy",
                        Arrays.asList("&8Support the plugins you enjoy!"),
                        MetadataHandler.signatureID),
                fileConfiguration);
        infoItem = ItemStackSerializer.deserialize("Info button", fileConfiguration);

        ItemStackSerializer.serialize(
                "Left button",
                ItemStackGenerator.generateSkullItemStack("MHF_ArrowLeft",
                        "Previous Item Ranks",
                        Arrays.asList(""),
                        MetadataHandler.signatureID),
                fileConfiguration);
        leftArrowItem = ItemStackSerializer.deserialize("Left button", fileConfiguration);

        ItemStackSerializer.serialize(
                "Right button",
                ItemStackGenerator.generateSkullItemStack("MHF_ArrowRight",
                        "Next Item Ranks",
                        Arrays.asList(""),
                        MetadataHandler.signatureID),
                fileConfiguration);
        rightArrowItem = ItemStackSerializer.deserialize("Right button", fileConfiguration);

        tierTranslation = ConfigurationEngine.setString(fileConfiguration, "tierTranslation", "Tier");
        itemFilterTranslation = ConfigurationEngine.setString(fileConfiguration, "itemFilterTranslation", "Filter by items of this rank.");

        ItemStackSerializer.serialize(
                "previousLoot",
                ItemStackGenerator.generateSkullItemStack("MHF_ArrowLeft",
                        "Previous Loot Page",
                        Arrays.asList(""),
                        MetadataHandler.signatureID),
                fileConfiguration);
        previousLootItem = ItemStackSerializer.deserialize("previousLoot", fileConfiguration);

        ItemStackSerializer.serialize(
                "nextLoot",
                ItemStackGenerator.generateSkullItemStack("MHF_ArrowRight",
                        "Next Loot Page",
                        Arrays.asList(""), MetadataHandler.signatureID),
                fileConfiguration);
        nextLootItem = ItemStackSerializer.deserialize("nextLoot", fileConfiguration);

    }

}
