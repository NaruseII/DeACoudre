package fr.naruse.dac.event;

import fr.naruse.dac.arena.Arena;
import fr.naruse.dac.database.PlayerStatistics;
import fr.naruse.dac.main.DACPlugin;
import fr.naruse.dac.arena.ArenaCollection;
import fr.naruse.dac.utils.Constant;
import fr.naruse.dac.utils.Updater;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class Listeners implements Listener {

    private final DACPlugin pl;

    public Listeners(DACPlugin pl) {
        this.pl = pl;
    }

    @EventHandler
    public void interact(PlayerInteractEvent e){
        Player p = e.getPlayer();

        if(e.getItem() != null){
            if(e.getItem().equals(Constant.LEAVE_ITEM) && ArenaCollection.ARENA_BY_PLAYER.contains(p)){
                Arena arena = ArenaCollection.ARENA_BY_PLAYER.get(p);
                arena.removePlayer(p);
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
        PlayerStatistics playerStatistics = ArenaCollection.PLAYER_STATISTICS_BY_PLAYER.get(p);
        if(playerStatistics == null){
            ArenaCollection.PLAYER_STATISTICS_BY_PLAYER.put(p, new PlayerStatistics(p.getUniqueId()));
        }else{
            playerStatistics.reload();
        }
    }

}
