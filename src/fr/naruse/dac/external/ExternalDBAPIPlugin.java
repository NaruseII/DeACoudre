package fr.naruse.dac.external;

import com.google.common.collect.Maps;
import fr.naruse.dac.database.DACSQLResponse;
import fr.naruse.dac.database.IDatabaseManager;
import fr.naruse.dac.database.StatisticType;
import fr.naruse.dac.main.DACPlugin;
import fr.naruse.dac.arena.ArenaCollection;
import fr.naruse.dac.utils.GameSettings;
import fr.naruse.dbapi.api.DatabaseAPI;
import fr.naruse.dbapi.database.Database;
import fr.naruse.dbapi.sql.SQLHelper;
import fr.naruse.dbapi.sql.SQLRequest;
import fr.naruse.dbapi.sql.SQLResponse;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class ExternalDBAPIPlugin implements IDatabaseManager {

    private final DACPlugin pl;

    private final Database database;

    ExternalDBAPIPlugin(DACPlugin pl) {
        this.pl = pl;

        DatabaseAPI.createNewDatabase(database = new Database("DeACoudre", GameSettings.DATABASE_TABLE_NAME.getValue()) {
            @Override
            public String getQuery() {
                return "CREATE TABLE `" +  GameSettings.DATABASE_TABLE_NAME.getValue() + "` ("
                        + "`uuid` varchar(64) COLLATE utf8_unicode_ci NOT NULL,"
                        + "`perfects` BIGINT NOT NULL,"
                        + "`fails` BIGINT NOT NULL,"
                        + "`wins` BIGINT NOT NULL,"
                        + "`loses` BIGINT NOT NULL,"
                        + "`games` BIGINT NOT NULL,"
                        + "`jumps` BIGINT NOT NULL)";
            }
        });
    }

    @Override
    public void isRegistered(String uuid, DACSQLResponse sqlResponse, boolean inMainThread) {
        SQLRequest sqlRequest = new SQLRequest(SQLHelper.getSelectRequest(GameSettings.DATABASE_TABLE_NAME.getValue(), "perfects", "uuid"), uuid);
        if(inMainThread){
            boolean exists = this.database.hasDirectAccount(sqlRequest);
            sqlResponse.handleResponse(exists);
        }else{
            this.database.hasAccount(sqlRequest, new SQLResponse() {
                @Override
                public void handleResponse(Object response) {
                    super.handleResponse(response);
                    if (response == null) {
                        return;
                    }
                    boolean exists = (boolean) response;
                    sqlResponse.handleResponse(exists);
                }
            });
        }
    }

    @Override
    public void register(String uuid, Map<StatisticType, Integer> map) {
        for (StatisticType value : StatisticType.values()) {
            if(!map.containsKey(value) || map.get(value) == null){
                map.put(value, 0);
            }
        }
        SQLRequest sqlRequest = new SQLRequest(SQLHelper.getInsertRequest(GameSettings.DATABASE_TABLE_NAME.getValue(), new String[]{"uuid", "perfects", "fails", "wins", "loses", "games", "jumps"}), uuid,
                map.get(StatisticType.PERFECTS), map.get(StatisticType.FAILS), map.get(StatisticType.WINS), map.get(StatisticType.LOSES), map.get(StatisticType.GAMES), map.get(StatisticType.JUMPS));
        this.database.prepareStatement(sqlRequest);
    }

    @Override
    public void getProperties(String uuid, DACSQLResponse sqlResponse) {
        SQLRequest sqlRequest = new SQLRequest(SQLHelper.getSelectRequest(GameSettings.DATABASE_TABLE_NAME.getValue(), "*", "uuid"), uuid);
        this.database.getResultSet(sqlRequest, new SQLResponse() {
            @Override
            public void handleResponse(Object response) {
                super.handleResponse(response);
                if (response == null) {
                    return;
                }
                ResultSet resultSet = (ResultSet) response;

                Map<StatisticType, Integer> map = Maps.newHashMap();

                try {
                    resultSet.next();
                    map.put(StatisticType.PERFECTS, resultSet.getInt("perfects"));
                    map.put(StatisticType.FAILS, resultSet.getInt("fails"));
                    map.put(StatisticType.WINS, resultSet.getInt("wins"));
                    map.put(StatisticType.LOSES, resultSet.getInt("loses"));
                    map.put(StatisticType.GAMES, resultSet.getInt("games"));
                    map.put(StatisticType.JUMPS, resultSet.getInt("jumps"));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                sqlResponse.handleResponse(map);
            }
        });
    }

    @Override
    public void save(String uuid, Map<StatisticType, Integer> map, boolean mainThread) {
        SQLRequest sqlRequest = new SQLRequest(SQLHelper.getUpdateRequest(GameSettings.DATABASE_TABLE_NAME.getValue(), new String[]{"perfects", "fails", "wins", "loses", "games", "jumps"}, "uuid"),
                map.get(StatisticType.PERFECTS), map.get(StatisticType.FAILS), map.get(StatisticType.WINS), map.get(StatisticType.LOSES), map.get(StatisticType.GAMES), map.get(StatisticType.JUMPS), uuid);
        if(mainThread){
            this.database.prepareDirectStatement(sqlRequest);
        }else{
            this.database.prepareStatement(sqlRequest);
        }
    }

    @Override
    public void clearAll() {
        SQLRequest sqlRequest = new SQLRequest(SQLHelper.getTruncateRequest(GameSettings.DATABASE_TABLE_NAME.getValue()));
        this.database.prepareStatement(sqlRequest);
        ArenaCollection.PLAYER_STATISTICS_BY_PLAYER.forEachValue(playerStatistics -> playerStatistics.clear());
    }
}
