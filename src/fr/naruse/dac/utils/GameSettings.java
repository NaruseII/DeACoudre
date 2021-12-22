package fr.naruse.dac.utils;

import fr.naruse.api.config.Configuration;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;

public class GameSettings<T> {

    public static final GameSettings<Boolean> TP_TO_LAST_LOCATION = new GameSettings("tpToLastLocation", false);
    public static final GameSettings<Boolean> FIREWORKS = new GameSettings<>("Fireworks", true);
    public static final GameSettings<Boolean> HOLOGRAM = new GameSettings<>("Hologram", false);

    public static final GameSettings<Integer> WAITING_TIMER = new GameSettings<>("timer.wait", 30);
    public static final GameSettings<Integer> JUMP_TIMER = new GameSettings<>("timer.jump", 15);
    public static final GameSettings<Integer> MINIMUM_LIVES = new GameSettings<>("lives.min", 1);
    public static final GameSettings<Integer> MAXIMUM_LIVES = new GameSettings<>("lives.max", -1);
    public static final GameSettings<Integer> WIN_VAULT_REWARDS = new GameSettings<>("rewards.vault.win", -1);
    public static final GameSettings<Integer> LOOSE_VAULT_REWARDS = new GameSettings<>("rewards.vault.loose", -1);
    public static final GameSettings<Integer> WINNER_BROADCAST_RECEIVER = new GameSettings<>("rewards.vault.loose", 0);

    public static final GameSettings<String> DATABASE_TABLE_NAME = new GameSettings<>("sql.tableName", "spleef_stats");

    public static GameSettings findSetting(String path){
        final GameSettings[] gameSettings = {null};
        forEach(settings -> {
            if(settings.getPath().equalsIgnoreCase(path)){
                gameSettings[0] = settings;
            }
        });

        return gameSettings[0];
    }

    private static void forEach(Consumer<GameSettings> consumer){
        try{
            for (Field field : GameSettings.class.getDeclaredFields()) {
                if(field.getType().isAssignableFrom(GameSettings.class) && Modifier.isStatic(field.getModifiers())){
                    GameSettings gameSettings = (GameSettings) field.get(null);

                    consumer.accept(gameSettings);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    protected final String path;
    protected T value;
    private final Configuration.ConfigurationSection section;
    private final String finalArg;

    private GameSettings(String prettyName, T defaultValue) {
        this.path = prettyName;
        this.value = defaultValue;

        Configuration.ConfigurationSection section = Constant.CONFIGURATION.getSection("gameSettings");
        String[] args = path.split("\\.");

        for (int i = 0; i < args.length-1; i++) {
            String sectionName = args[i];
            if(!section.contains(sectionName)){
                section = section.newSection(sectionName);
            }else{
                section = section.getSection(sectionName);
            }
        }

        this.section = section;
        this.finalArg = args[args.length-1];

        if(this.section.contains(this.finalArg)){
            if(defaultValue instanceof Integer){
                this.value = (T) (Integer) this.section.getInt(this.finalArg);
            }else {
                this.value = this.section.get(this.finalArg);
            }
        }
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;

        this.section.set(this.finalArg, this.value);

        Constant.CONFIGURATION.save();
    }

    public String getPath() {
        return path;
    }
}
