package fr.naruse.dac.external;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fr.naruse.api.async.CollectionManager;
import fr.naruse.api.async.ThreadGlobal;
import fr.naruse.api.logging.GlobalLogger;
import fr.naruse.dac.database.PlayerStatistics;
import fr.naruse.dac.database.StatisticType;
import fr.naruse.dac.main.DACPlugin;
import fr.naruse.dac.arena.ArenaCollection;
import fr.naruse.dac.utils.Constant;
import fr.naruse.dac.utils.GameSettings;
import fr.naruse.dac.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExternalHolographicDisplaysPlugin {

    private static final GlobalLogger.Logger LOGGER = new GlobalLogger.Logger("Hologram");

    private final DACPlugin pl;
    private long millis = 0;
    private Location location;
    private Hologram hologram;

    public ExternalHolographicDisplaysPlugin(DACPlugin pl) {
        this.pl = pl;

        this.reload();
        CollectionManager.INFINITE_SECOND_THREAD_RUNNABLE_SET.add(() -> {
            if(System.currentTimeMillis()-this.millis > 60000 && this.hologram != null){
                this.millis = System.currentTimeMillis();

                Map<Integer, List<OfflinePlayer>> map = Maps.newHashMap();

                for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {

                    PlayerStatistics playerStatistics = ArenaCollection.PLAYER_STATISTICS_BY_PLAYER.get(offlinePlayer);
                    if(playerStatistics == null){
                        continue;
                    }

                    int points = playerStatistics.getStatistic(StatisticType.PERFECTS) + playerStatistics.getStatistic(StatisticType.WINS) -
                            playerStatistics.getStatistic(StatisticType.FAILS) - playerStatistics.getStatistic(StatisticType.LOSES);
                    if(points < 0){
                        points = 0;
                    }

                    if (!map.containsKey(points)) {
                        map.put(points, Lists.newArrayList());
                    }
                    map.get(points).add(offlinePlayer);
                }

                List<Integer> list = Lists.newArrayList(map.keySet());
                Collections.sort(list);
                Collections.reverse(list);

                List<String> lines = Lists.newArrayList();

                int count = 1;
                for (Integer aDouble : list) {
                    for (OfflinePlayer offlinePlayer : map.get(aDouble)) {
                        if(count > 10){
                            break;
                        }
                        lines.add(MessageManager.Builder.init("hologramFormat")
                                .replace("rank", count)
                                .replace("name", offlinePlayer.getName())
                                .replace("points", aDouble)
                                .build());
                        count++;
                    }
                    if(count > 10){
                        break;
                    }
                }

                ThreadGlobal.runSync(() -> {

                    if(this.hologram != null){
                        this.hologram.clearLines();
                        this.hologram.appendTextLine(MessageManager.get("hologramTitle"));
                        for (String line : lines) {
                            this.hologram.appendTextLine(line);
                        }
                    }

                });

            }
        });

    }

    public void reload(){
        if(this.hologram != null){
            this.hologram.delete();
            this.hologram = null;
        }
        if(Constant.CONFIGURATION.contains("hologramLocation")){
            try{
                this.location = Location.deserialize(Constant.CONFIGURATION.getSection("hologramLocation").getAll());
            }catch (Exception e){
                LOGGER.error("World not found for location");
                this.location = null;
            }
        }else{
            this.location = null;
        }

        if(this.location != null && GameSettings.HOLOGRAM.getValue()){
            this.hologram = HologramsAPI.createHologram(pl, this.location);
        }
    }

    public void onDisable(){
        if(this.hologram != null){
            this.hologram.delete();
        }
    }
}
