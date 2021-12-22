package fr.naruse.dac.task;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.naruse.api.async.Runner;
import fr.naruse.api.config.Configuration;
import fr.naruse.dac.main.DACPlugin;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BlockFinderTask extends Runner {

    private final Set<Block> blockSet = Sets.newHashSet();

    private final DACPlugin pl;
    private final Player p;
    private final Location startLocation;
    private final Configuration configuration;

    public BlockFinderTask(DACPlugin pl, Player p, Configuration configuration) {
        this.pl = pl;
        this.p = p;
        this.startLocation = p.getLocation();
        this.configuration = configuration;
    }

    private final List<Block> nextToCheck = Lists.newArrayList();
    private boolean lastModulo = false;

    @Override
    public void run() {
        if(this.nextToCheck.isEmpty()){
            if(!this.blockSet.isEmpty()){
                this.p.sendMessage("§aBlocks finder task stopped. Found "+this.blockSet.size()+" blocks.");
                this.p.sendMessage("§aSaving them...");
                this.saveBlocks();
                this.p.sendMessage("§aSave done. §c('/dac reload' required)");
                this.setCancelled(true);
                return;
            }else{
                this.checkBlock(this.startLocation.getBlock());
            }
        }else{
            Block block = this.nextToCheck.get(0);
            this.nextToCheck.remove(block);
            this.checkBlock(block);
        }

        if(this.blockSet.size() % 10 == 0){
            if(!this.lastModulo){
                this.lastModulo = true;
                this.p.sendMessage("§7"+blockSet.size()+" blocks found...");
            }
        }else{
            this.lastModulo = false;
        }
    }

    private void saveBlocks() {
        List<Map> list;
        if(this.configuration.contains("blocks")){
            list = this.configuration.get("blocks");
        }else{
            list = Lists.newArrayList();
        }
        list.addAll(this.blockSet.stream().map((Function<Block, Map>) block -> block.getLocation().serialize()).collect(Collectors.toList()));
        this.configuration.set("blocks", list);
        this.configuration.save();
    }

    private void checkBlock(Block block){
        for (Block surrounding : this.getSurroundings(block)) {
            if((surrounding.getType().name().contains("WATER") || surrounding.getType().name().contains("LAVA")) && !this.blockSet.contains(surrounding)){
                this.nextToCheck.add(surrounding);
                this.blockSet.add(surrounding);
            }
        }
    }

    private Block[] getSurroundings(Block block){
        return new Block[]{
                block.getRelative(BlockFace.UP),
                block.getRelative(BlockFace.DOWN),
                block.getRelative(BlockFace.NORTH)
                , block.getRelative(BlockFace.SOUTH)
                , block.getRelative(BlockFace.EAST)
                , block.getRelative(BlockFace.WEST)};
    }
}
