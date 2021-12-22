package fr.naruse.dac.utils;

import fr.naruse.api.config.Configuration;
import fr.naruse.dac.database.IDatabaseManager;
import fr.naruse.dac.main.DACPlugin;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;

public class Constant {

    public static final ItemStack LEAVE_ITEM = new ItemStack(Material.BARRIER);
    public static Configuration CONFIGURATION;
    public static IDatabaseManager DATABASE_MANAGER;

    public static void init(DACPlugin pl) {
        CONFIGURATION = new Configuration(new File(pl.getDataFolder(), "config.json"), pl.getClass(), "resources/config.json");
    }

    public static void postInit(DACPlugin pl){
        ItemMeta meta = LEAVE_ITEM.getItemMeta();
        meta.setDisplayName(MessageManager.get("leaveItemName"));
        LEAVE_ITEM.setItemMeta(meta);
    }
}
