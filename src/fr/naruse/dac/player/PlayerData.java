package fr.naruse.dac.player;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scoreboard.Scoreboard;

public class PlayerData {

    private final Player p;

    private boolean flying;
    private boolean allowFly;
    private Inventory inventory;
    private Location lastLocation;
    private GameMode gameMode;
    private Scoreboard lastScoreboard;

    public PlayerData(Player p) {
        this.p = p;
    }

    public void setPlayerData() {
        this.inventory = Bukkit.createInventory(null, 9*6, this.p.getUniqueId().toString());
        for(int i = 0; i < this.inventory.getSize(); i++){
            try{
                if(this.p.getInventory().getItem(i) != null){
                    this.inventory.setItem(i, this.p.getInventory().getItem(i));
                }
            }catch (Exception e){
                break;
            }
        }
        this.gameMode = this.p.getGameMode();
        this.lastScoreboard = this.p.getScoreboard();
        this.flying = this.p.isFlying();
        this.allowFly = this.p.getAllowFlight();
        this.lastLocation = this.p.getLocation();
    }

    public void giveBackPlayerData(){
        if(this.inventory == null){
            return;
        }
        for(int i = 0; i < 9*6; i++){
            if(this.inventory.getItem(i) != null){
                this.p.getInventory().setItem(i, this.inventory.getItem(i));
            }
        }
        this.p.setGameMode(this.gameMode);
        if(this.lastScoreboard != null){
            this.p.setScoreboard(this.lastScoreboard);
            this.lastScoreboard = null;
        }else{
            this.p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
        this.p.setAllowFlight(this.allowFly);
        this.p.setFlying(this.flying);
    }

    public Location getLastLocation() {
        return lastLocation;
    }
}
