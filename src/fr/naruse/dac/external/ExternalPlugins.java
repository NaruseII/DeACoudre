package fr.naruse.dac.external;

import fr.naruse.api.logging.GlobalLogger;
import fr.naruse.dac.database.JsonDatabase;
import fr.naruse.dac.main.DACPlugin;
import fr.naruse.dac.utils.Constant;

public class ExternalPlugins {

    private static final GlobalLogger.Logger LOGGER = new GlobalLogger.Logger("ExternalPlugins");

    public static ExternalVaultPlugin EXTERNAL_VAULT_PLUGIN;
    public static ExternalDBAPIPlugin EXTERNAL_DBAPI_PLUGIN;
    public static ExternalHolographicDisplaysPlugin EXTERNAL_HOLOGRAPHIC_DISPLAYS_PLUGIN;

    public static void init(DACPlugin pl) {
        LOGGER.info("Loading...");

        if(pl.getServer().getPluginManager().getPlugin("Vault") != null){
            LOGGER.info("'Vault' found");
            EXTERNAL_VAULT_PLUGIN = new ExternalVaultPlugin(pl).load();
        }

        if(pl.getServer().getPluginManager().getPlugin("DBAPI") != null && EXTERNAL_DBAPI_PLUGIN == null){
            LOGGER.info("'DBAPI' found");
            EXTERNAL_DBAPI_PLUGIN = new ExternalDBAPIPlugin(pl);
            Constant.DATABASE_MANAGER = EXTERNAL_DBAPI_PLUGIN;
        }else{
            Constant.DATABASE_MANAGER = new JsonDatabase(pl);
        }

        if(pl.getServer().getPluginManager().getPlugin("HolographicDisplays") != null && EXTERNAL_HOLOGRAPHIC_DISPLAYS_PLUGIN == null){
            LOGGER.info("'HolographicDisplays' found");
            EXTERNAL_HOLOGRAPHIC_DISPLAYS_PLUGIN = new ExternalHolographicDisplaysPlugin(pl);
        }

        LOGGER.info("Done");
    }



}
