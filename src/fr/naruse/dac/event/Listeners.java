package fr.naruse.dac.event;

import fr.naruse.dac.arena.Arena;
import fr.naruse.dac.arena.ArenaStatus;
import fr.naruse.dac.database.PlayerStatistics;
import fr.naruse.dac.inventory.InventoryBlockChoice;
import fr.naruse.dac.main.DACPlugin;
import fr.naruse.dac.arena.ArenaCollection;
import fr.naruse.dac.utils.Constant;
import fr.naruse.dac.utils.MessageManager;
import fr.naruse.dac.utils.Updater;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;

public class Listeners implements Listener {

    private final DACPlugin pl;

    public Listeners(DACPlugin pl) {
        this.pl = pl;
    }

    @EventHandler
    public void interact(PlayerInteractEvent e){
        Player p = e.getPlayer();

        if(e.getItem() != null){
            if(ArenaCollection.ARENA_BY_PLAYER.contains(p)){
                Arena arena = ArenaCollection.ARENA_BY_PLAYER.get(p);

                if(e.getItem().equals(Constant.LEAVE_ITEM)){
                    arena.removePlayer(p);
                }else if(arena.getStatus().isActive(ArenaStatus.Status.WAITING) && e.getItem().hasItemMeta() && e.getItem().getItemMeta().getDisplayName() != null
                        && e.getItem().getItemMeta().getDisplayName().equals(MessageManager.get("blockChoiceItemName"))){
                    new InventoryBlockChoice(this.pl, p, 1);
                }

                e.setCancelled(true);
            }
            return;
        }
        if(e.getClickedBlock() == null){
            return;
        }
        Block block = e.getClickedBlock();
        if(!(block.getState() instanceof Sign)){
            return;
        }
        if(e.getAction() != Action.RIGHT_CLICK_BLOCK){
            if(p.hasPermission("dac.sign.break")){
                return;
            }
        }
        Sign sign = (Sign) block.getState();
        for (int i = 0; i < ArenaCollection.ARENAS.size(); i++) {
            Arena arena = ArenaCollection.ARENAS.getByIndex(i);
            arena.registerNewSign(sign);
            if(ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(ChatColor.stripColor((arena.getName())))){
                arena.addPlayer(p, false);
                e.setCancelled(true);
                break;
            }
        }
        if(p.hasPermission("dac.sign.create")) {
            if (sign.getLine(0).equalsIgnoreCase("-!!-") && sign.getLine(3).equalsIgnoreCase("-!!-")) {
                if (sign.getLine(1).equalsIgnoreCase(sign.getLine(2))) {
                    for (int i = 0; i < ArenaCollection.ARENAS.size(); i++) {
                        Arena arena = ArenaCollection.ARENAS.getByIndex(i);
                        if (arena.getName().equals(sign.getLine(1))) {
                            sign.setLine(0, "§6§l"+arena.getName());
                            sign.update();
                            arena.registerNewSign(sign);
                            arena.updateSigns();
                            return;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void damageEvent(EntityDamageEvent e){
        if(e.getEntity() instanceof Player){
            Player p = (Player) e.getEntity();
            if(ArenaCollection.ARENA_BY_PLAYER.contains(p)){
                e.setCancelled(true);
                if(e.getCause() == EntityDamageEvent.DamageCause.FALL) {
                    ArenaCollection.ARENA_BY_PLAYER.get(p).onDamage(p);
                }
            }
        }
    }

    @EventHandler
    public void join(PlayerJoinEvent e){
        Player p = e.getPlayer();
        if(p.hasPermission("dac") && Updater.isUpdateAvailable()){
            Updater.sendMessage(p);
        }
        PlayerStatistics playerStatistics = ArenaCollection.PLAYER_STATISTICS_BY_PLAYER.get(p.getUniqueId());
        if(playerStatistics == null){
            ArenaCollection.PLAYER_STATISTICS_BY_PLAYER.put(p.getUniqueId(), new PlayerStatistics(p.getUniqueId()));
        }else{
            playerStatistics.reload();
        }
    }

    @EventHandler
    public void quit(PlayerQuitEvent e){
        Player p = e.getPlayer();
        Arena arena = ArenaCollection.ARENA_BY_PLAYER.get(p);
        if(arena != null){
            arena.removePlayer(p, true);
        }
    }

    @EventHandler
    public void breakBlock(BlockBreakEvent e){
        this.cancelIfNeeded(e, e.getPlayer());
    }

    @EventHandler
    public void placeBlock(BlockPlaceEvent e){
        this.cancelIfNeeded(e, e.getPlayer());
    }

    @EventHandler
    public void pickUp(PlayerPickupItemEvent e){
        this.cancelIfNeeded(e, e.getPlayer());
    }

    @EventHandler
    public void foodLevelChange(FoodLevelChangeEvent e){
        if(e.getEntity() instanceof Player){
            this.cancelIfNeeded(e, (Player) e.getEntity());
        }
    }

    @EventHandler
    public void throwItemEvent(PlayerDropItemEvent e){
        this.cancelIfNeeded(e, e.getPlayer());
    }

    private void cancelIfNeeded(Cancellable e, Player p){
        if(ArenaCollection.ARENA_BY_PLAYER.contains(p)){
            e.setCancelled(true);
        }
    }

}
