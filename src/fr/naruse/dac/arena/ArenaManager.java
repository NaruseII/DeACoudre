package fr.naruse.dac.arena;

import com.google.common.collect.Maps;
import fr.naruse.api.config.Configuration;
import fr.naruse.api.logging.GlobalLogger;
import fr.naruse.dac.main.DACPlugin;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ArenaManager {

    private static final GlobalLogger.Logger LOGGER = new GlobalLogger.Logger("ArenaManager");
    public static Map<String, String> MISCONFIGURED_REASON = Maps.newHashMap();

    private static DACPlugin pl;

    public static void init(DACPlugin plugin) {
        pl = plugin;
        reload();
    }

    public static void reload(){
        LOGGER.info("Shutting down current arenas...");
        ArenaCollection.ARENAS.forEach(arena -> arena.shutdown());
        ArenaCollection.ARENAS.clear();
        ArenaCollection.CONFIGURATION_BY_NAME.clear();

        LOGGER.info("Loading arenas...");

        if(pl.getArenaFolder().listFiles() != null){
            for (File file : pl.getArenaFolder().listFiles()) {
                if(!file.getName().endsWith(".json")){
                    LOGGER.warn("File '"+file.getName()+"' in 'plugins/DeACoudre/arenas/ is not a json file");
                    continue;
                }

                Configuration configuration = new Configuration(file, false);
                String name = configuration.get("name");

                LOGGER.info("Loading '"+name+"'...");

                Location[] locations = getLocations(configuration); // Diving, Arena, End, Lobby
                if(locations == null){
                    continue;
                }

                int[] capacities = getCapacities(configuration); // Min, Max
                if(capacities == null){
                    continue;
                }

                boolean isOpen = configuration.getBoolean("isOpen");

                List<Block> blockList = null;
                if(configuration.contains("blocks")){
                    blockList = ((List<Map>) configuration.get("blocks")).stream().map(map -> Location.deserialize(map).getBlock()).collect(Collectors.toList());
                    LOGGER.info(blockList.size()+" blocks found for '"+name+"'");
                }

                Arena arena = new Arena(pl, configuration, name, locations, capacities, isOpen, blockList);

                ArenaCollection.CONFIGURATION_BY_NAME.put(name, configuration);
                ArenaCollection.ARENAS.add(arena);
            }
        }

        LOGGER.info(ArenaCollection.ARENAS.size()+" arenas found");
    }

    private static Location[] getLocations(Configuration configuration) {
        String name = configuration.get("name");
        if(!configuration.contains("locations")){
            LOGGER.error("No location found for arena '"+name+"'");
            MISCONFIGURED_REASON.put(name, "No Location Found");
            return null;
        }
        Configuration.ConfigurationSection section = configuration.getSection("locations");
        if(!section.contains("diving")){
            LOGGER.error("Location 'Diving' not found for arena '"+name+"'");
            MISCONFIGURED_REASON.put(name, "No Diving Location Found");
            return null;
        }
        if(!section.contains("arena")){
            LOGGER.error("Location 'Arena' not found for arena '"+name+"'");
            MISCONFIGURED_REASON.put(name, "No Arena Location Found");
            return null;
        }
        if(!section.contains("end")){
            LOGGER.error("Location 'End' not found for arena '"+name+"'");
            MISCONFIGURED_REASON.put(name, "No End Location Found");
            return null;
        }
        Location diving = null;
        Location arena = null;
        Location end = null;
        Location lobby = null;
        try{
            diving = Location.deserialize(section.getSection("diving").getAll());
            arena = Location.deserialize(section.getSection("arena").getAll());
            end = Location.deserialize(section.getSection("end").getAll());
            if(section.contains("lobby")){
                lobby = Location.deserialize(section.getSection("lobby").getAll());
            }
        }catch (Exception e){
            LOGGER.error("World not found for location '"+(arena == null ? "Arena" : diving == null ? "Diving" : end == null ? "End" : "Lobby")+"' for arena '"+configuration.get("name")+"'");
            MISCONFIGURED_REASON.put(name, "World Not Found");
            return null;
        }
        return new Location[]{diving, arena, end, lobby};
    }

    private static int[] getCapacities(Configuration configuration) {
        String name = configuration.get("name");
        if(!configuration.contains("capacities")){
            LOGGER.error("No capacity found for arena '"+name+"'");
            MISCONFIGURED_REASON.put(name, "No Capacities Found");
            return null;
        }
        Configuration.ConfigurationSection section = configuration.getSection("capacities");
        if(!section.contains("min")){
            LOGGER.error("Capacity 'Min' not found for arena '"+name+"'");
            MISCONFIGURED_REASON.put(name, "No Min Found");
            return null;
        }
        if(!section.contains("max")){
            LOGGER.error("Capacity 'Max' not found for arena '"+name+"'");
            MISCONFIGURED_REASON.put(name, "No Max Found");
            return null;
        }

        return new int[]{section.getInt("min"), section.getInt("max")};
    }
}
