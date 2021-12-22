package fr.naruse.dac.main;

import fr.naruse.api.logging.GlobalLogger;
import fr.naruse.api.main.APIInit;
import fr.naruse.dac.arena.ArenaCollection;
import fr.naruse.dac.arena.ArenaManager;
import fr.naruse.dac.cmd.DACCommand;
import fr.naruse.dac.event.Listeners;
import fr.naruse.dac.external.ExternalPlugins;
import fr.naruse.dac.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.function.Consumer;

public class DACPlugin extends JavaPlugin {

    private File arenaFolder;

    @Override
    public void onEnable() {
        super.onEnable();

        APIInit.init(this);
        APIInit.disableEntityAsyncListAdd();
        GlobalLogger.setPluginLogger(this.getLogger());

        Constant.init(this);
        MessageManager.init(this);
        Constant.postInit(this);
        ExternalPlugins.init(this);
        new Metrics(this, 13683);

        this.arenaFolder = new File(this.getDataFolder(), "arenas");
        ArenaManager.init(this);

        this.getCommand("dac").setExecutor(new DACCommand(this));
        Listeners listeners;
        this.getServer().getPluginManager().registerEvents(listeners = new Listeners(this), this);

        Updater.checkNewVersion(this);

        Bukkit.getOnlinePlayers().forEach((Consumer<Player>) player -> listeners.join(new PlayerJoinEvent(player, "")));
    }

    @Override
    public void onDisable() {
        super.onDisable();

        ArenaCollection.PLAYER_STATISTICS_BY_PLAYER.forEachValue(playerStatistics -> {
            playerStatistics.save();
        });
        if(ExternalPlugins.EXTERNAL_HOLOGRAPHIC_DISPLAYS_PLUGIN != null){
            ExternalPlugins.EXTERNAL_HOLOGRAPHIC_DISPLAYS_PLUGIN.onDisable();
        }
        APIInit.shutdown();
        ArenaCollection.ARENAS.forEach(arena -> arena.shutdown());
    }

    public File getArenaFolder() {
        return arenaFolder;
    }

}
