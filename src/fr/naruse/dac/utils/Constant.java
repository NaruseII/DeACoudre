package fr.naruse.dac.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.naruse.api.config.Configuration;
import fr.naruse.dac.database.IDatabaseManager;
import fr.naruse.dac.main.DACPlugin;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Constant {

    private final static Set<String> FORBIDDEN_BLOCKS_LABEL = Sets.newHashSet("SLAB", "FENCE", "PISTON", "SPAWNER", "BARRIER", "PLATE", "CHEST", "STAIRS", "LEAVES", "TRAP", "CRAFT", "TABLE", "PORTAL", "EMERALD", "ANVIL", "HOPPER", "COMMAND",
            "BEACON", "WALL", "DETECTOR", "JUMP", "STRUCTURE", "TNT", "BOMBE", "FIRE", "SHELF", "STOOL", "TABLE", "BENCH", "CHAIR", "SLOPE", "EFFECT", "CTF", "LAMP", "AIR", "EGG", "GLASS_PANE", "ROD", "REDSTONE", "CONCRETE_POWDER", "SHULKER", "BED",
    "SIGN", "DOOR", "CAKE", "CAULDRON", "BREWING_STAND", "BANNER");
    public static List<ItemStack> AUTHORIZED_BLOCKS = Lists.newArrayList();
    public static final Random RANDOM = new Random();
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

        AUTHORIZED_BLOCKS.clear();

        for (Material material : Arrays.stream(Material.values()).filter(material -> {
            for (String s : FORBIDDEN_BLOCKS_LABEL) {
                if (material.name().contains(s) && !material.name().contains("COLOR_LIGHT")) {
                    return false;
                }
            }
            return material.isBlock() && material.isSolid() && material.getId() != 2440 && material.getId() != 126 && material.getId() != 44 && material.getId() != 102 && material.getId() != 2430 && material.getId() != 2408
                    && material.getId() != 2499 && material.getId() != 2520 && material != Material.SLIME_BLOCK && material != Material.CACTUS && material != Material.SPONGE;
        }).collect(Collectors.toList())) {
            for (int i = 0; i <= getMetaCount(material); i++) {
                ItemStack itemStack = new ItemStack(material, 1, (byte) i);
                AUTHORIZED_BLOCKS.add(itemStack);
            }
        }
    }

    private static int getMetaCount(Material material){
        switch (material){
            case WOOL:
            case STAINED_GLASS:
            case STAINED_CLAY:
            case CONCRETE:
            case CONCRETE_POWDER:
                return 15;
            case QUARTZ_BLOCK:
            case SANDSTONE:
            case LOG_2:
                return 2;
            case WOOD:
            case LOG:
                return 3;
        }
        return 0;
    }
}

