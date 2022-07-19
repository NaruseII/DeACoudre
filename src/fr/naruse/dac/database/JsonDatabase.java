package fr.naruse.dac.database;

import com.google.common.collect.Maps;
import fr.naruse.api.config.Configuration;
import fr.naruse.dac.main.DACPlugin;
import fr.naruse.dac.arena.ArenaCollection;

import java.io.File;
import java.util.Map;

public class JsonDatabase implements IDatabaseManager{

    private final Configuration configuration;

    public JsonDatabase(DACPlugin pl) {
        this.configuration = new Configuration(new File(pl.getDataFolder(), "statistics.json"), false);
    }

    @Override
    public void isRegistered(String uuid, DACSQLResponse sqlResponse, boolean inMainThread) {
        sqlResponse.handleResponse(this.configuration.contains(uuid));
    }

    @Override
    public void register(String uuid, Map<StatisticType, Integer> map) {
        Configuration.ConfigurationSection section = this.configuration.contains(uuid) ? this.configuration.getSection(uuid) : this.configuration.newSection(uuid);
        map.forEach((statisticType, integer) -> section.set(statisticType.name(), integer));
        this.configuration.save();
    }

    @Override
    public void getProperties(String uuid, DACSQLResponse sqlResponse) {
        Configuration.ConfigurationSection section = this.configuration.getSection(uuid);
        Map<StatisticType, Integer> map = Maps.newHashMap();

        for (StatisticType statisticType : StatisticType.values()) {
            map.put(statisticType, section.contains(statisticType.name()) ? section.getInt(statisticType.name()) : 0);
        }

        sqlResponse.handleResponse(map);
    }

    @Override
    public void save(String uuid, Map<StatisticType, Integer> map, boolean mainThread) {
        Configuration.ConfigurationSection section = this.configuration.contains(uuid) ? this.configuration.getSection(uuid) : this.configuration.newSection(uuid);
        map.forEach((statisticType, integer) -> section.set(statisticType.name(), integer));
        this.configuration.save();
    }

    @Override
    public void clearAll() {
        ArenaCollection.PLAYER_STATISTICS_BY_PLAYER.forEachValue(playerStatistics -> playerStatistics.clear());
        this.configuration.clear();
        this.configuration.save();
    }
}
