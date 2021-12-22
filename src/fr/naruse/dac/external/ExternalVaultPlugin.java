package fr.naruse.dac.external;

import fr.naruse.dac.main.DACPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class ExternalVaultPlugin {

    private final DACPlugin pl;
    private Economy economy;

    ExternalVaultPlugin(DACPlugin pl) {
        this.pl = pl;
    }

    public ExternalVaultPlugin load(){
        RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            this.economy = economyProvider.getProvider();
            return this;
        }
        return null;
    }

    public void deposit(Player p, int value){
        this.economy.depositPlayer(p, value);
    }
}
