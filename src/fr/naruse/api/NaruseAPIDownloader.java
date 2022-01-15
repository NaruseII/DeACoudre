package fr.naruse.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

public class NaruseAPIDownloader {

    public static void checkConfigAPI(JavaPlugin javaPlugin){
        Plugin plugin = Bukkit.getPluginManager().getPlugin("NaruseAPI");
        if(plugin != null){
            String version = getVersion("https://raw.githubusercontent.com/NaruseII/ConfigAPI/master/src/plugin.yml");
            if(!version.equalsIgnoreCase(plugin.getDescription().getVersion())){
                Bukkit.getPluginManager().disablePlugin(plugin);
            }else{
                return;
            }
        }

        File file = new File(javaPlugin.getDataFolder().getParentFile(), "NaruseConfigAPI.jar");
        if(file.exists()){
            file.delete();
        }
        if(!downloadFile("https://github.com/NaruseII/ConfigAPI/blob/master/out/artifacts/NaruseAPI/NaruseAPI.jar?raw=true", file)){
            javaPlugin.getLogger().severe("Unable to download ConfigAPI");
            Bukkit.getPluginManager().disablePlugin(javaPlugin);
            return;
        }
        try {
            Plugin pl = Bukkit.getPluginManager().loadPlugin(file);
            Bukkit.getPluginManager().enablePlugin(pl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void checkSecondThreadAPI(JavaPlugin javaPlugin){
        Plugin plugin = Bukkit.getPluginManager().getPlugin("SecondThreadAPI");
        if(plugin != null){
            String version = getVersion("https://raw.githubusercontent.com/NaruseII/SecondThreadAPI/master/src/plugin.yml");
            if(!version.equalsIgnoreCase(plugin.getDescription().getVersion())){
                Bukkit.getPluginManager().disablePlugin(plugin);
            }else{
                return;
            }
        }

        File file = new File(javaPlugin.getDataFolder().getParentFile(), "NaruseSecondThreadAPI.jar");
        if(file.exists()){
            file.delete();
        }
        if(!downloadFile("https://github.com/NaruseII/SecondThreadAPI/blob/master/out/artifacts/NaruseSpigotAPI/NaruseSpigotAPI.jar?raw=true", file)){
            javaPlugin.getLogger().severe("Unable to download ConfigAPI");
            Bukkit.getPluginManager().disablePlugin(javaPlugin);
            return;
        }
        try {
            Plugin pl = Bukkit.getPluginManager().loadPlugin(file);
            Bukkit.getPluginManager().enablePlugin(pl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getVersion(String urlString){
        try{
            URL url = new URL(urlString);
            Scanner scanner = new Scanner(url.openStream());
            while (scanner.hasNext()){
                String line = scanner.nextLine();
                if(line.startsWith("version")){
                    return line.split(": ")[1];
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private static boolean downloadFile(String host, File dest) {
        try {
            BufferedInputStream in = new BufferedInputStream(new URL(host).openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(dest);
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}

