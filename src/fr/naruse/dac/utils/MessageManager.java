package fr.naruse.dac.utils;

import fr.naruse.api.config.Configuration;
import fr.naruse.api.logging.GlobalLogger;
import fr.naruse.dac.main.DACPlugin;

import java.io.File;

public class MessageManager {

    private static GlobalLogger.Logger LOGGER = new GlobalLogger.Logger("MessagesManager");

    private static Configuration CONFIGURATION_ENGLISH;
    private static Configuration CONFIGURATION_FRENCH;
    private static Configuration CONFIGURATION_CUSTOM;

    public static Lang CURRENT_LANG;

    public static void init(DACPlugin pl){
        LOGGER.info("Loading messages...");
        CONFIGURATION_ENGLISH = new Configuration(new File(pl.getDataFolder(), "messages/english.json"), pl.getClass(), "resources/messages/english.json");
        CONFIGURATION_FRENCH = new Configuration(new File(pl.getDataFolder(), "messages/french.json"), pl.getClass(), "resources/messages/french.json");
        CONFIGURATION_CUSTOM = new Configuration(new File(pl.getDataFolder(), "messages/custom.json"), pl.getClass(), "resources/messages/english.json");
        try{
            CURRENT_LANG = Lang.valueOf(Constant.CONFIGURATION.get("lang"));
        }catch (Exception e){
            LOGGER.error("Lang '"+Constant.CONFIGURATION.get("lang")+"' not recognized");
            CURRENT_LANG = Lang.ENGLISH;
        }
        LOGGER.info("Done");
    }

    public static String get(String path){
        Configuration configuration = CURRENT_LANG == Lang.ENGLISH ? CONFIGURATION_ENGLISH : CURRENT_LANG == Lang.FRENCH ? CONFIGURATION_FRENCH : CONFIGURATION_CUSTOM;
        if(path.contains(".")){

            Configuration.ConfigurationSection section = configuration.getMainSection();
            String[] args = path.split("\\.");

            for (int i = 0; i < args.length-1; i++) {
                String sectionName = args[i];
                if(!section.contains(sectionName)){
                    LOGGER.error("Message '"+path+"' not found");
                    return "{Message Not Found}";
                }
                section = section.getSection(sectionName);
            }

            return section.get(args[args.length-1]);
        }

        if(!configuration.contains(path)){
            LOGGER.error("Message '"+path+"' not found");
            return "{Message Not Found}";
        }

        return configuration.get(path);
    }

    public static class Builder {

        private String message;

        private Builder(String message) {
            this.message = message;
        }

        public static Builder init(String path){
            return new Builder(get(path));
        }

        public Builder replace(String from, Object by){
            this.message = this.message.replace("{"+from+"}", by.toString());
            return this;
        }

        public String build(){
            return this.message;
        }
    }

    public enum Lang {

        ENGLISH,
        FRENCH,
        CUSTOM

    }

}
