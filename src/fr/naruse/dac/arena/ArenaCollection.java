package fr.naruse.dac.arena;

import fr.naruse.api.async.CollectionManager;
import fr.naruse.api.config.Configuration;
import fr.naruse.dac.database.PlayerStatistics;
import fr.naruse.dac.player.PlayerData;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicReference;

public class ArenaCollection {

    public static final CollectionManager.Map<String, Configuration> CONFIGURATION_BY_NAME = new CollectionManager.Map<>();
    public static final CollectionManager.List<Arena> ARENAS = new CollectionManager.List<>();
    public static final CollectionManager.Map<Player, Arena> ARENA_BY_PLAYER = new CollectionManager.Map<>();
    public static final CollectionManager.Map<Player, PlayerData> PLAYER_DATA_BY_PLAYER = new CollectionManager.Map<>();
    public static final CollectionManager.Map<OfflinePlayer, PlayerStatistics> PLAYER_STATISTICS_BY_PLAYER = new CollectionManager.Map<>();

    public static Arena getArenaByName(String name){
        AtomicReference<Arena> arena = new AtomicReference<>();
        ARENAS.forEach(o -> {
            if(o.getName().equalsIgnoreCase(name)){
                arena.set(o);
            }
        });
        return arena.get();
    }

}
