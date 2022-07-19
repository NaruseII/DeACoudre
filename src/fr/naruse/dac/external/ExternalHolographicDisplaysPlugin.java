package fr.naruse.dac.external;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fr.naruse.api.async.CollectionManager;
import fr.naruse.api.async.ThreadGlobal;
import fr.naruse.api.logging.GlobalLogger;
import fr.naruse.dac.database.PlayerStatistics;
import fr.naruse.dac.database.StatisticType;
import fr.naruse.dac.main.DACPlugin;
import fr.naruse.dac.arena.ArenaCollection;
import fr.naruse.dac.utils.Constant;
import fr.naruse.dac.utils.GameSettings;
import fr.naruse.dac.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExternalHolographicDisplaysPlugin extends HologramPlugin<Hologram> {

    public ExternalHolographicDisplaysPlugin(DACPlugin pl) {
        super(pl);

        this.reload();

    }

    @Override
    protected void insertLines(List<String> lines) {
        this.hologram.clearLines();
        this.hologram.appendTextLine(MessageManager.get("hologramTitle"));
        for (String line : lines) {
            this.hologram.appendTextLine(line);
        }
    }

    @Override
    protected void deleteHologram() {
        this.hologram.delete();
    }

    @Override
    protected void createHologram() {
        this.hologram = HologramsAPI.createHologram(this.pl, this.location);
    }


}
