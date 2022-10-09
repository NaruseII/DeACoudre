package fr.naruse.dac.external;

import eu.decentsoftware.holograms.api.holograms.Hologram;
import eu.decentsoftware.holograms.api.holograms.HologramLine;
import eu.decentsoftware.holograms.api.holograms.HologramPage;
import fr.naruse.dac.main.DACPlugin;
import fr.naruse.dac.utils.MessageManager;

import java.util.List;

public class ExternalDecentHologramPlugin extends HologramPlugin<Hologram>{

    public ExternalDecentHologramPlugin(DACPlugin pl) {
        super(pl);
    }

    @Override
    protected void insertLines(List<String> lines) {
        for (int i = 0; i < this.hologram.getPages().size(); i++) {
            this.hologram.removePage(i);
        }
        HologramPage hologramPage = this.hologram.addPage();

        hologramPage.addLine(new HologramLine(hologramPage, hologramPage.getNextLineLocation(), MessageManager.get("hologramTitle")));
        for (String line : lines) {
            hologramPage.addLine(new HologramLine(hologramPage, hologramPage.getNextLineLocation(), line));
        }
        this.hologram.showAll();
    }

    @Override
    protected void deleteHologram() {
        this.hologram.destroy();
        this.hologram.delete();
    }

    @Override
    protected void createHologram() {
        this.hologram = new Hologram("DACHologram", this.location, false);
        this.hologram.showAll();
    }
}
