package fr.naruse.dac.utils;

import fr.naruse.api.async.CollectionManager;
import fr.naruse.api.logging.GlobalLogger;
import fr.naruse.dac.main.DACPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.URL;
import java.util.Scanner;
import java.util.logging.Level;

public class Updater {

    private static final GlobalLogger.Logger LOGGER = new GlobalLogger.Logger("Updater");

    private static boolean UPDATE_AVAILABLE = false;
    private static String CURRENT_VERSION, ONLINE_VERSION;

    public static void checkNewVersion(DACPlugin pl) {
        CollectionManager.SECOND_THREAD_RUNNABLE_SET.add(() -> {
            try {
                Thread.sleep(1000);
                LOGGER.log(Level.INFO, "");
                LOGGER.log(Level.INFO, "Checking for new versions...");
                LOGGER.log(Level.INFO, "");
                if(needToUpdate(pl)){
                    LOGGER.log(Level.INFO, "");
                    LOGGER.log(Level.WARNING, "The plugin needs to be updated! https://www.spigotmc.org/resources/dé-à-coudre.59231/");
                    LOGGER.log(Level.INFO, "");

                    UPDATE_AVAILABLE = true;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if(p.hasPermission("dac")){
                            sendMessage(p);
                        }
                    }
                }else{
                    LOGGER.log(Level.INFO, "");
                    LOGGER.log(Level.INFO, "The plugin is up to date!");
                    LOGGER.log(Level.INFO, "");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Could not update the plugin. This does not change the functioning of the plugin");
                e.printStackTrace();
            }
        });
    }

    private static boolean needToUpdate(DACPlugin pl) {
        try{
            CURRENT_VERSION = pl.getDescription().getVersion();

            URL url = new URL("https://raw.githubusercontent.com/NaruseII/DeACoudre/master/src/plugin.yml");
            Scanner scanner = new Scanner(url.openStream());
            ONLINE_VERSION = null;
            while (scanner.hasNext()){
                String line = scanner.nextLine();
                if(line.startsWith("version")){
                    ONLINE_VERSION = line.split(": ")[1];
                    break;
                }
            }
            if(ONLINE_VERSION == null){
                LOGGER.log(Level.SEVERE, "Could not check the online version. This does not change the functioning of the plugin");
                return false;
            }

            LOGGER.log(Level.INFO, "Local version: "+ CURRENT_VERSION);
            LOGGER.log(Level.INFO, "Online version: "+ ONLINE_VERSION);

            if(CURRENT_VERSION.equals(ONLINE_VERSION)){
                return false;
            }else{
                return true;
            }
        }catch (Exception e){
            LOGGER.log(Level.SEVERE, "Could not check the online version. This does not change the functioning of the plugin");
        }
        return false;
    }

    public static void sendMessage(Player p){
        p.sendMessage(MessageManager.Builder.init("needAnUpdate")
                .replace("currentVersion", CURRENT_VERSION)
                .replace("newVersion", ONLINE_VERSION)
                .replace("url", "https://www.spigotmc.org/resources/dé-à-coudre.59231/")
                .build());
    }

    public static boolean isUpdateAvailable() {
        return UPDATE_AVAILABLE;
    }
}
