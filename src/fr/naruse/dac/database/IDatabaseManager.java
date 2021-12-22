package fr.naruse.dac.database;

import fr.naruse.api.logging.GlobalLogger;
import fr.naruse.dac.arena.ArenaCollection;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Map;

public interface IDatabaseManager {

    default void load(){
        GlobalLogger.Logger logger = new GlobalLogger.Logger("DatabaseManager");
        logger.info("Loading data...");
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            ArenaCollection.PLAYER_STATISTICS_BY_PLAYER.put(player, new PlayerStatistics(player.getUniqueId()));
        }
        logger.info("Done");
    }

    default void isRegistered(String uuid, DACSQLResponse sqlResponse) {
        this.isRegistered(uuid, sqlResponse, false);
    }

    void isRegistered(String uuid, DACSQLResponse sqlResponse, boolean inMainThread) ;

    void register(String uuid, Map<StatisticType, Integer> map) ;

    void getProperties(String uuid, DACSQLResponse sqlResponse) ;

    default void save(String uuid, Map<StatisticType, Integer> map) {
        this.save(uuid, map, false);
    }

    void save(String uuid, Map<StatisticType, Integer> map, boolean mainThread) ;

    void clearAll() ;

}
