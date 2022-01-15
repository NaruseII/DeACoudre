package fr.naruse.dac.inventory;

import fr.naruse.api.inventory.AbstractInventory;
import fr.naruse.dac.arena.ArenaCollection;
import fr.naruse.dac.player.PlayerData;
import fr.naruse.dac.utils.Constant;
import fr.naruse.dac.utils.MessageManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class InventoryBlockChoice extends AbstractInventory {

    private final int page;

    public InventoryBlockChoice(JavaPlugin pl, Player p, int page) {
        super(pl, p, MessageManager.Builder.init("blockChoiceInvTitle").replace("page", page).build(), 9*6, false);
        this.page = page-1;

        this.initInventory(this.inventory);
        p.openInventory(this.inventory);
    }

    @Override
    protected void initInventory(Inventory inventory) {
        this.setDecoration(Material.STAINED_GLASS_PANE, true);

        if(Constant.AUTHORIZED_BLOCKS.size() >= 28*this.page){
            int count = 28*this.page;
            for (int i = 28*this.page; i < Constant.AUTHORIZED_BLOCKS.size(); i++) {
                if(count >= 28*(this.page+1)){
                    break;
                }

                ItemStack itemStack = Constant.AUTHORIZED_BLOCKS.get(i);
                itemStack.setAmount(1);
                inventory.addItem(itemStack);

                count++;
            }
        }

        inventory.setItem(5*9+2, this.buildItem(Material.WOOL, 3, MessageManager.get("previous"), false, null));
        inventory.setItem(5*9+4, this.buildItem(Material.BARRIER, 0, MessageManager.get("back"), false, null));
        inventory.setItem(5*9+6, this.buildItem(Material.WOOL, 5, MessageManager.get("next"), false, null));
    }

    @Override
    protected void actionPerformed(Player player, ItemStack itemStack, InventoryAction inventoryAction, int slot) {
        if(slot == 5*9+2){
            if(this.page+1 > 1){
                new InventoryBlockChoice(this.pl, this.p, this.page);
                return;
            }
        }else if(slot == 5*9+6){
            new InventoryBlockChoice(this.pl, this.p, this.page+2);
            return;
        }else if(slot == 5*9+4){
            this.p.closeInventory();
            return;
        }else if(itemStack != null && itemStack.getType() != Material.STAINED_GLASS_PANE){

            if(Constant.BLOCK_PERMISSION_MAP.containsKey(itemStack)){
                String permission = Constant.BLOCK_PERMISSION_MAP.get(itemStack);

                if(!"null".equalsIgnoreCase(permission) && !player.hasPermission(permission)){
                    player.sendMessage(MessageManager.get("blockPermissionDenied"));
                    return;
                }
            }

            PlayerData playerData = ArenaCollection.PLAYER_DATA_BY_PLAYER.get(p);
            if(playerData != null){
                player.closeInventory();

                ItemStack itemStack1 = itemStack.clone();
                ItemMeta meta = itemStack1.getItemMeta();
                meta.setDisplayName(MessageManager.get("blockChoiceItemName"));
                itemStack1.setItemMeta(meta);

                playerData.setCurrentBlock(itemStack1);
                player.getInventory().setItem(4, itemStack1);
            }
        }
    }
}
