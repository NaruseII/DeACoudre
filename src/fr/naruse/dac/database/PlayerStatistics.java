package fr.naruse.dac.database;

import com.google.common.collect.Maps;
import fr.naruse.dac.utils.Constant;

import java.util.Map;
import java.util.UUID;

public class PlayerStatistics {

    private final String uuid;
    private final Map<StatisticType, Integer> map = Maps.newHashMap();
    private final Map<StatisticType, Long> lastIncrementMap = Maps.newHashMap();

    public PlayerStatistics(UUID uuid) {
        this.uuid = uuid.toString();

        this.reload();
    }

    public void reload() {
        for (StatisticType value : StatisticType.values()) {
            this.map.put(value, 0);
        }
        Constant.DATABASE_MANAGER.isRegistered(this.uuid, new DACSQLResponse(){
            @Override
            public void handleResponse(Object response) {
                boolean exists = (boolean) response;
                if(exists){
                    Constant.DATABASE_MANAGER.getProperties(uuid, new DACSQLResponse(){
                        @Override
                        public void handleResponse(Object response) {
                            super.handleResponse(response);
                            map.putAll((Map<StatisticType, Integer>) response);
                        }
                    });
                }else{
                    Constant.DATABASE_MANAGER.register(uuid, map);
                }
            }
        });
    }

    public Integer getStatistic(StatisticType statisticType){
        return this.map.get(statisticType);
    }

    public void increment(StatisticType statisticType){
        Long l = this.lastIncrementMap.get(statisticType);
        if(l != null && System.currentTimeMillis()-l <= 1000){
            return;
        }

        this.map.put(statisticType, this.map.get(statisticType)+1);
        Constant.DATABASE_MANAGER.save(this.uuid, this.map);

        this.lastIncrementMap.put(statisticType, System.currentTimeMillis());
    }

    public void set(StatisticType statisticType, int value){
        this.map.put(statisticType, value);
        Constant.DATABASE_MANAGER.save(this.uuid, this.map);
    }

    public void save() {
        Constant.DATABASE_MANAGER.save(this.uuid, this.map);
    }

    public void clear() {
        for (StatisticType value : StatisticType.values()) {
            this.set(value, 0);
        }
    }

    @Override
    public String toString() {
        return "PlayerStatistics{" +
                "map=" + map +
                '}';
    }
}
