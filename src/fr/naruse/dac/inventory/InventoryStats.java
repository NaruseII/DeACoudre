package fr.naruse.dac.inventory;

import fr.naruse.api.inventory.AbstractInventory;
import fr.naruse.dac.database.PlayerStatistics;
import fr.naruse.dac.database.StatisticType;
import fr.naruse.dac.arena.ArenaCollection;
import fr.naruse.dac.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class InventoryStats extends AbstractInventory {

    private final UUID uuid;
    private final String name;

    public InventoryStats(JavaPlugin pl, Player p, UUID uuid, String name) {
        super(pl, p, "§5§l"+ MessageManager.Builder.init("statsInvName").replace("player", name).build(), 9*3, false);

        this.uuid = uuid;
        this.name = name;

        this.initInventory(this.inventory);
        p.openInventory(this.inventory);
    }

    @Override
    protected void initInventory(Inventory inventory) {
        this.setDecoration(Material.STAINED_GLASS_PANE, true);
        PlayerStatistics playerStatistics = ArenaCollection.PLAYER_STATISTICS_BY_PLAYER.get(this.uuid);

        if(playerStatistics == null){
            inventory.setItem(4, this.buildItem(Material.BARRIER, 0, "§cNot found", false, null));
        }else{
            inventory.setItem(3, this.buildItem(Material.STAINED_CLAY, 5, MessageManager.Builder.init("inventory.wins").replace("wins", playerStatistics.getStatistic(StatisticType.WINS)).build(), false, null));
            inventory.setItem(5, this.buildItem(Material.STAINED_CLAY, 14, MessageManager.Builder.init("inventory.loses").replace("loses", playerStatistics.getStatistic(StatisticType.LOSES)).build(), false, null));
            inventory.setItem(12, this.buildItem(Material.STAINED_CLAY, 8, MessageManager.Builder.init("inventory.jumps").replace("jumps", playerStatistics.getStatistic(StatisticType.JUMPS)).build(), false, null));
            inventory.setItem(13, this.buildItem(Material.STAINED_CLAY, 12, MessageManager.Builder.init("inventory.games").replace("games", playerStatistics.getStatistic(StatisticType.GAMES)).build(), false, null));
            inventory.setItem(14, this.buildItem(Material.STAINED_CLAY, 4, MessageManager.Builder.init("inventory.fails").replace("fails", playerStatistics.getStatistic(StatisticType.FAILS)).build(), false, null));
            inventory.setItem(22, this.buildItem(Material.EMERALD_BLOCK, 0, MessageManager.Builder.init("inventory.perfects").replace("perfects", playerStatistics.getStatistic(StatisticType.PERFECTS)).build(), false, null));

            for (int i = 0; i < inventory.getSize(); i++) {
                if(inventory.getItem(i) == null){
                    inventory.setItem(i, inventory.getItem(0));
                }
            }
        }
    }

    @Override
    protected void actionPerformed(Player player, ItemStack itemStack, InventoryAction inventoryAction, int slot) {

    }
}
