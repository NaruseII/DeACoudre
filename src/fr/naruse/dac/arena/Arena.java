package fr.naruse.dac.arena;

import com.google.common.collect.Sets;
import fr.naruse.api.FireworkUtils;
import fr.naruse.api.ScoreboardSign;
import fr.naruse.api.async.CollectionManager;
import fr.naruse.api.async.RunnerPerSecond;
import fr.naruse.api.async.ThreadGlobal;
import fr.naruse.api.config.Configuration;
import fr.naruse.dac.database.StatisticType;
import fr.naruse.dac.external.ExternalPlugins;
import fr.naruse.dac.main.DACPlugin;
import fr.naruse.dac.player.PlayerData;
import fr.naruse.dac.utils.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;

public class Arena extends RunnerPerSecond {

    private final DACPlugin pl;
    private final Configuration configuration;
    private final String name;
    private final Location divingLocation;
    private final Location arenaLocation;
    private final Location endLocation;
    private final Location lobbyLocation;
    private final int min;
    private final int max;
    private boolean isOpen;
    private final List<Block> blockRegionList;

    private final CollectionManager.List<Player> playerInGame = new CollectionManager.List<>();
    private final CollectionManager.List<Sign> signs = new CollectionManager.List<>();
    private final CollectionManager.Map<Player, Integer> playerLives = new CollectionManager.Map<>();
    private final CollectionManager.List<Block> blockToReplaceByWater = new CollectionManager.List<>();
    private final CollectionManager.List<Block> blockToReplaceByLava = new CollectionManager.List<>();
    private final ArenaStatus status = new ArenaStatus();
    private final ScoreboardSign<DACPlugin> scoreboardSign;

    private Player currentJumper;

    public Arena(DACPlugin pl, Configuration configuration, String name, Location[] locations, int[] capacities, boolean isOpen, List<Block> blockRegionList) {
        this.pl = pl;
        this.configuration = configuration;
        this.name = name;
        this.divingLocation = locations[0];
        this.arenaLocation = locations[1];
        this.endLocation = locations[2];
        this.lobbyLocation = locations[3];
        this.min = capacities[0];
        this.max = capacities[1];
        this.isOpen = isOpen;
        this.blockRegionList = blockRegionList;

        this.scoreboardSign = new ScoreboardSign<>(pl, DisplaySlot.SIDEBAR, "§6§l"+name);
        this.scoreboardSign.disableOnlyOneScore();

        this.start();

        ThreadGlobal.runSyncLater(() -> this.registerNewSigns(), 20);
    }

    private int currentTimer = GameSettings.WAITING_TIMER.getValue();

    @Override
    public void runPerSecond() {

        if (this.status.isActive(ArenaStatus.Status.WAITING)) {

            if(this.currentTimer <= 0){
                this.startGame();
            }else{
                if(this.playerInGame.size() >= this.min){
                    this.currentTimer--;
                }else{
                    this.currentTimer = GameSettings.WAITING_TIMER.getValue();
                }

                this.playerInGame.forEach(player -> {
                    PlayerData playerData = ArenaCollection.PLAYER_DATA_BY_PLAYER.get(player);
                    if(playerData != null && playerData.getCurrentBlock() == null && !Constant.AUTHORIZED_BLOCKS.isEmpty()){
                        ItemStack itemStack = Constant.AUTHORIZED_BLOCKS.get(Constant.RANDOM.nextInt(Constant.AUTHORIZED_BLOCKS.size())).clone();
                        ItemMeta meta = itemStack.getItemMeta();
                        meta.setDisplayName(MessageManager.get("blockChoiceItemName"));
                        itemStack.setItemMeta(meta);

                        ThreadGlobal.runSync(() -> player.getInventory().setItem(4, itemStack));
                    }
                });

            }
        }else{
            if(this.currentTimer <= 0){
                if(this.currentJumper != null){
                    this.sendGameMessage(MessageManager.Builder.init("jumperLoseCauseTime").replace("player", this.currentJumper.getName()).build());
                }
                this.makeJumperLoose();
            }else{
                this.currentTimer--;
            }

            if(this.playerInGame.size() == 1 && this.min != 1){
                Player winner = this.playerInGame.getByIndex(0);

                WinnerBroadcastReceiver winnerBroadcastReceiver = WinnerBroadcastReceiver.fromOrdinal(GameSettings.WINNER_BROADCAST_RECEIVER.getValue());
                if(winnerBroadcastReceiver != null){
                    int lives = this.playerLives.get(winner);
                    winnerBroadcastReceiver.sendWinMessage(this, this.getFormattedName() + MessageManager.Builder.init("winsThisGame")
                            .replace("player", winner.getName())
                            .replace("lives", lives)
                            .replace("lifePleuralOrSingular", lives > 1 ? MessageManager.get("lifePleural") : MessageManager.get("lifeSingular"))
                            .build());
                }

                if(GameSettings.WIN_VAULT_REWARDS.getValue() > 0 && ExternalPlugins.EXTERNAL_VAULT_PLUGIN != null){
                    ExternalPlugins.EXTERNAL_VAULT_PLUGIN.deposit(winner, GameSettings.WIN_VAULT_REWARDS.getValue());
                }

                ArenaCollection.PLAYER_STATISTICS_BY_PLAYER.get(winner).increment(StatisticType.WINS);

                this.restart();
            }else if(this.playerInGame.size() == 0){
                this.restart();
            }

        }

        // Little square timer on action bar
        if(this.playerInGame.size() != 0){
            String barMessage = this.getSquaredTimer();
            ThreadGlobal.runSync(() -> this.playerInGame.forEach(player -> player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(barMessage))));
        }
    }

    @Override
    public void run() {
        super.run();

        if(this.currentJumper != null){
            if(this.isCorrectBlock(this.currentJumper.getLocation())){

                Set<Block> set = Sets.newHashSet();
                Location location = this.currentJumper.getLocation().clone();

                if(this.blockRegionList != null && !this.blockRegionList.contains(location.getBlock())){
                    this.makeJumperLooseLive();
                    return;
                }

                boolean isPerfect = !this.isCorrectBlock(location.getBlock().getRelative(BlockFace.NORTH)) && !this.isCorrectBlock(location.getBlock().getRelative(BlockFace.SOUTH))
                        && !this.isCorrectBlock(location.getBlock().getRelative(BlockFace.EAST)) && !this.isCorrectBlock(location.getBlock().getRelative(BlockFace.WEST));

                if(isPerfect){

                    int newAmount = this.playerLives.get(this.currentJumper)+1;
                    if(GameSettings.MAXIMUM_LIVES.getValue() >= 1 && newAmount > GameSettings.MAXIMUM_LIVES.getValue()){
                        newAmount = GameSettings.MAXIMUM_LIVES.getValue();
                    }

                    this.playerLives.put(this.currentJumper, newAmount);

                    this.sendGameMessage(MessageManager.Builder.init("madeAPerfect").replace("player", this.currentJumper.getName()).build());

                    if(GameSettings.FIREWORKS.getValue()){
                        FireworkUtils.build(this.pl, this.currentJumper, 2);
                    }

                    ArenaCollection.PLAYER_STATISTICS_BY_PLAYER.get(this.currentJumper).increment(StatisticType.PERFECTS);
                }

                for (int i = 0; i < 2; i++) { // Getting all above and below blocks
                    while (true){
                        boolean isWater = location.getBlock().getType().name().contains("WATER");
                        boolean isLava = location.getBlock().getType().name().contains("LAVA");

                        if(isWater || isLava){
                            set.add(location.getBlock());
                            if(isWater){
                                this.blockToReplaceByWater.add(location.getBlock());
                            }else {
                                this.blockToReplaceByLava.add(location.getBlock());
                            }
                            location = location.getBlock().getRelative(0, i == 0 ? -1 : 1, 0).getLocation();
                        }else{
                            location = this.currentJumper.getLocation().clone().add(0, 1, 0);
                            break;
                        }
                    }
                }

                if(!set.isEmpty()){

                    Material[] material = new Material[]{Material.STONE};
                    byte[] data = {0};

                    PlayerData playerData = ArenaCollection.PLAYER_DATA_BY_PLAYER.get(this.currentJumper);
                    if(playerData != null && playerData.getCurrentBlock() != null){
                        material[0] = playerData.getCurrentBlock().getType();
                        data[0] = playerData.getCurrentBlock().getData().getData();
                    }

                    ThreadGlobal.runSync(() -> set.forEach(block -> {
                        block.setType(isPerfect ? Material.EMERALD_BLOCK : material[0]);
                        block.setData(isPerfect ? (byte) 0 : data[0]);
                    })); // Setting blocks
                }
                this.nextJumper();

                this.checkArenaIsFilled();
            }
        }
    }

    public void checkArenaIsFilled() {
        if(this.blockRegionList == null){
            return;
        }
        if(this.blockRegionList.size() == this.blockToReplaceByWater.size()+this.blockToReplaceByLava.size()){

            StringBuilder names = new StringBuilder();
            StringBuilder lives = new StringBuilder();

            if(this.playerInGame.size() > 1){
                for (int i = 0; i < this.playerInGame.size()-1; i++) {
                    Player p = this.playerInGame.getByIndex(i);
                    if(i != 0){
                        names.append(", ");
                        lives.append(", ");
                    }
                    names.append(p.getName());
                    lives.append(this.playerLives.get(p));
                }
                Player p = this.playerInGame.getByIndex(this.playerLives.size()-1);
                names.append(MessageManager.get("and"));
                names.append(p.getName());
                lives.append(MessageManager.get("and"));
                lives.append(this.playerLives.get(p));
            }else{
                Player p = this.playerInGame.getByIndex(0);
                names.append(p.getName());
                lives.append(this.playerLives.get(p));
            }

            if(GameSettings.WIN_VAULT_REWARDS.getValue() > 0 && ExternalPlugins.EXTERNAL_VAULT_PLUGIN != null){
                this.playerInGame.forEach(player -> {
                    ExternalPlugins.EXTERNAL_VAULT_PLUGIN.deposit(player, GameSettings.WIN_VAULT_REWARDS.getValue());
                    ArenaCollection.PLAYER_STATISTICS_BY_PLAYER.get(player).increment(StatisticType.WINS);
                });
            }

            this.restart();

            WinnerBroadcastReceiver winnerBroadcastReceiver = WinnerBroadcastReceiver.fromOrdinal(GameSettings.WINNER_BROADCAST_RECEIVER.getValue());
            if(winnerBroadcastReceiver != null){
                winnerBroadcastReceiver.sendWinMessage(this, this.getFormattedName() + MessageManager.Builder.init("theyWinThisGame")
                        .replace("players", names)
                        .replace("lives", lives)
                        .replace("lifePleuralOrSingular", MessageManager.get("lifePleural"))
                        .build());
            }
        }
    }

    public void startGame() {
        this.status.setActive(ArenaStatus.Status.IN_GAME);
        this.sendGameMessage(MessageManager.get("gameStarts"));
        this.nextJumper();

        this.playerInGame.forEach(player -> {
            ArenaCollection.PLAYER_STATISTICS_BY_PLAYER.get(player).increment(StatisticType.GAMES);
            PlayerData playerData = ArenaCollection.PLAYER_DATA_BY_PLAYER.get(player);
            if(playerData != null && playerData.getCurrentBlock() == null){
                if(!Constant.AUTHORIZED_BLOCKS.isEmpty()){
                    ItemStack itemStack = Constant.AUTHORIZED_BLOCKS.get(Constant.RANDOM.nextInt(Constant.AUTHORIZED_BLOCKS.size())).clone();
                    ItemMeta meta = itemStack.getItemMeta();
                    meta.setDisplayName(MessageManager.get("blockChoiceItemName"));
                    itemStack.setItemMeta(meta);

                    player.getInventory().setItem(4, itemStack);
                    playerData.setCurrentBlock(itemStack);
                }

            }
        });
    }

    public void nextJumper() {
        if (this.status.isActive(ArenaStatus.Status.WAITING) || this.playerInGame.isEmpty()) {
            return;
        }
        if(this.currentJumper != null){
            Player currentJumper = this.currentJumper;
            ThreadGlobal.runSync(() -> currentJumper.teleport(this.arenaLocation));
            this.currentJumper.sendMessage(this.getFormattedName() + MessageManager.get("successfulJump"));
            this.playerInGame.remove(this.currentJumper);
            this.playerInGame.add(this.currentJumper);
            this.currentJumper.setFireTicks(0);
        }
        this.currentJumper = this.playerInGame.getByIndex(0);

        ThreadGlobal.runSync(() -> {
            this.currentJumper.teleport(this.divingLocation);
            this.updateScoreboard();
        });

        this.currentJumper.sendMessage(this.getFormattedName() + MessageManager.get("yourTurn"));
        if(this.playerInGame.size() > 1){
            this.playerInGame.getByIndex(1).sendTitle("", MessageManager.get("youAreNextToJump"));
        }
        this.currentTimer = GameSettings.JUMP_TIMER.getValue();

        ArenaCollection.PLAYER_STATISTICS_BY_PLAYER.get(this.currentJumper).increment(StatisticType.JUMPS);
    }

    public void restart(){
        if(!Bukkit.isPrimaryThread()){
            ThreadGlobal.runSync(() -> this.restart());
            return;
        }
        this.currentTimer = GameSettings.WAITING_TIMER.getValue();
        this.status.setActive(ArenaStatus.Status.WAITING);
        this.playerInGame.clone().forEach(player -> this.removePlayer(player, false));
        if(!this.blockToReplaceByLava.isEmpty()){
            ThreadGlobal.runSync(() -> {
                this.blockToReplaceByLava.forEach(block -> block.setType(Material.STATIONARY_LAVA));
                this.blockToReplaceByLava.clear();
            });
        }
        if(!this.blockToReplaceByWater.isEmpty()){
            ThreadGlobal.runSync(() -> {
                this.blockToReplaceByWater.forEach(block -> block.setType(Material.STATIONARY_WATER));
                this.blockToReplaceByWater.clear();
            });
        }
        this.updateSigns();
        this.updateScoreboard();
    }

    public void shutdown() {
        this.restart();
        this.setCancelled(true);
    }

    public void makeJumperLoose() {
        if(this.currentJumper == null){
            this.nextJumper();
            return;
        }
        ThreadGlobal.runSync(() -> {

            if(GameSettings.LOOSE_VAULT_REWARDS.getValue() > 0 && ExternalPlugins.EXTERNAL_VAULT_PLUGIN != null){
                ExternalPlugins.EXTERNAL_VAULT_PLUGIN.deposit(this.currentJumper, GameSettings.LOOSE_VAULT_REWARDS.getValue());
            }

            ArenaCollection.PLAYER_STATISTICS_BY_PLAYER.get(this.currentJumper).increment(StatisticType.LOSES);

            this.removePlayer(this.currentJumper, false);
            this.nextJumper();
        });
    }

    public void makeJumperLooseLive(){
        if(GameSettings.FIREWORKS.getValue()){
            FireworkUtils.build(this.pl, this.currentJumper, 0);
        }
        ArenaCollection.PLAYER_STATISTICS_BY_PLAYER.get(this.currentJumper).increment(StatisticType.FAILS);
        if(this.playerLives.get(this.currentJumper) <= 1){
            this.sendGameMessage(MessageManager.Builder.init("fail").replace("player", this.currentJumper.getName()).build());
            this.makeJumperLoose();
        }else{
            this.playerLives.put(this.currentJumper, this.playerLives.get(this.currentJumper)-1);
            this.sendGameMessage(MessageManager.Builder.init("failAndLoseALife").replace("player", this.currentJumper.getName()).build());
            this.nextJumper();
        }
    }

    public void onDamage(Player p) {
        if(this.currentJumper != p){
            return;
        }
        this.makeJumperLooseLive();
    }

    public boolean addPlayer(Player p, boolean force){
        if(ArenaCollection.ARENA_BY_PLAYER.contains(p)){
            p.sendMessage(this.getFormattedName() + MessageManager.get("youAlreadyAreInAnArena"));
            return false;
        }

        if(this.status.isActive(ArenaStatus.Status.IN_GAME) && !force){
            p.sendMessage(this.getFormattedName() + MessageManager.get("gameAlreadyStarted"));
            return false;
        }

        if(this.playerInGame.size() >= this.max && !force){
            p.sendMessage(this.getFormattedName() + MessageManager.get("gameFull"));
            return false;
        }

        if(!this.isOpen && !force){
            p.sendMessage(this.getFormattedName() + MessageManager.get("gameClosed"));
            return false;
        }

        PlayerData playerData = ArenaCollection.PLAYER_DATA_BY_PLAYER.get(p);
        if(playerData == null){
            ArenaCollection.PLAYER_DATA_BY_PLAYER.put(p, playerData = new PlayerData(p));
        }
        playerData.setPlayerData();
        playerData.setCurrentBlock(null);

        p.setGameMode(GameMode.SURVIVAL);
        this.scoreboardSign.apply(p);
        ArenaCollection.ARENA_BY_PLAYER.put(p, this);
        this.playerInGame.add(p);
        this.playerLives.put(p, Math.abs(GameSettings.MINIMUM_LIVES.getValue()));
        this.sendGameMessage(MessageManager.Builder.init("playerJoined").replace("player", p.getName()).build());
        p.getInventory().clear();
        p.getInventory().setHeldItemSlot(0);
        p.getInventory().setItem(8, Constant.LEAVE_ITEM);
        p.setInvulnerable(false);
        p.setFoodLevel(20);
        p.setHealth(p.getMaxHealth());
        p.setFlying(false);
        p.setAllowFlight(false);

        if(this.status.isActive(ArenaStatus.Status.IN_GAME)){
            p.teleport(this.arenaLocation);
        }else if(this.lobbyLocation != null){
            p.teleport(this.lobbyLocation);
        }

        this.updateSigns();
        this.updateScoreboard();

        return true;
    }

    public void removePlayer(Player p){
        this.removePlayer(p, true);
    }

    public void removePlayer(Player p, boolean withMessages){
        if(!this.playerInGame.contains(p)){
            return;
        }

        if(withMessages){
            this.sendGameMessage(MessageManager.Builder.init("leaveArena").replace("player", p.getName()).build());
        }
        this.playerInGame.remove(p);
        if(this.currentJumper == p){
            this.currentJumper = null;
        }

        p.getInventory().clear();
        p.updateInventory();
        p.setVelocity(new Vector());
        p.setFallDistance(0f);
        p.setInvulnerable(false);
        p.setFoodLevel(20);
        p.setHealth(p.getMaxHealth());
        ArenaCollection.ARENA_BY_PLAYER.remove(p);
        this.playerLives.remove(p);

        PlayerData data = ArenaCollection.PLAYER_DATA_BY_PLAYER.get(p);
        data.giveBackPlayerData();

        if(GameSettings.TP_TO_LAST_LOCATION.getValue()){
            p.teleport(data.getLastLocation());
        }else{
            p.teleport(this.endLocation);
        }

        this.updateSigns();
        this.updateScoreboard();

        if (this.status.isActive(ArenaStatus.Status.IN_GAME)) {
            ArenaCollection.PLAYER_STATISTICS_BY_PLAYER.get(p).increment(StatisticType.LOSES);
        }
    }

    private void updateScoreboard() {
        this.scoreboardSign.clearLines();
        for (int i = 0; i < (this.playerInGame.size() > 15 ? 15 : this.playerInGame.size()); i++) {
            Player player = this.playerInGame.getByIndex(i);
            this.scoreboardSign.setScore((this.currentJumper == player ? "§a§l» " : "§7")+player.getName(), this.playerLives.get(player));
        }
    }

    public void updateSigns() {
        this.signs.forEach(sign -> {
            if(!sign.getChunk().isLoaded()){
                return;
            }
            sign.setLine(0, "§6§l"+this.name);
            if(this.isOpen){

                String line2Tag = "§a";
                if(this.playerInGame.size() >= (int) (this.max*0.8)){
                    line2Tag = "§c";
                }else if(this.playerInGame.size() >= (int) (this.max*0.6)){
                    line2Tag = "§e";
                }
                sign.setLine(1, line2Tag+this.playerInGame.size()+"/"+this.max);

                if (this.status.isActive(ArenaStatus.Status.WAITING)) {

                    sign.setLine(2, this.playerInGame.size() >= this.min ? MessageManager.get("sign.ready") : MessageManager.Builder.init("sign.missing").replace("missing", this.min-this.playerInGame.size()).build());
                    sign.setLine(3, MessageManager.get("sign.join"));

                }else{

                    sign.setLine(2, "");
                    sign.setLine(3, MessageManager.get("sign.inGame"));

                }
            }else{
                sign.setLine(1, "");
                sign.setLine(2, MessageManager.get("sign.closed"));
                sign.setLine(3, "");
            }
            sign.update();
        });
    }

    public void registerNewSigns() {
        for (World world : Bukkit.getWorlds()) {
            for(Chunk c : world.getLoadedChunks()){
                for(BlockState state : c.getTileEntities()){
                    if(state instanceof Sign){
                        Sign sign = (Sign) state;
                        this.registerNewSign(sign);
                    }
                }
            }
        }
        this.updateSigns();
    }

    public void registerNewSign(Sign sign){
        if(sign.getLine(0).equals("§6§l"+this.name)){
            if(!this.signs.contains(sign)){
                this.signs.add(sign);
            }
        }
    }

    public String getSquaredTimer() {
        StringBuilder square = new StringBuilder();
        int initialTimer = this.status.isActive(ArenaStatus.Status.WAITING) ? GameSettings.WAITING_TIMER.getValue() : GameSettings.JUMP_TIMER.getValue();

        for (int i = 0; i < initialTimer - 1; i++) {
            if (i == this.currentTimer) {
                square.append("there⬛");
            } else {
                square.append("⬛");
            }
        }

        return "§a"+square.toString().replace("there", "§c");
    }

    public String getFormattedName(){
        return "§6§l"+this.name+ " §f§l» ";
    }

    public String getName() {
        return this.name;
    }

    public void sendGameMessage(String message){
        this.playerInGame.forEach(player -> player.sendMessage(this.getFormattedName() + message));
    }

    private boolean isCorrectBlock(Location location){
        return this.isCorrectBlock(location.getBlock());
    }

    private boolean isCorrectBlock(Block block){
        return block.getType().name().contains("WATER") || block.getType().name().contains("LAVA");
    }

    public int getMin() {
        return this.min;
    }

    public int getMax() {
        return this.max;
    }

    public ArenaStatus getStatus() {
        return this.status;
    }

    public int getPlayerInGameSize(){
        return this.playerInGame.size();
    }

    public void setOpen(boolean open) {
        this.isOpen = open;
    }

    public Location getDivingLocation() {
        return this.divingLocation;
    }

    public Location getLobbyLocation() {
        return this.lobbyLocation;
    }

    public Location getArenaLocation() {
        return this.arenaLocation;
    }

    public Location getEndLocation() {
        return this.endLocation;
    }

    public CollectionManager.List<Player> getPlayerInGame() {
        return this.playerInGame.clone();
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    public CollectionManager.List<Sign> getSigns() {
        return this.signs;
    }

    public int getCurrentTimer() {
        return this.currentTimer;
    }

    public List<Block> getBlockRegionList() {
        return this.blockRegionList;
    }

    public CollectionManager.List<Block> getBlockToReplaceByLava() {
        return this.blockToReplaceByLava;
    }

    public CollectionManager.List<Block> getBlockToReplaceByWater() {
        return this.blockToReplaceByWater;
    }

    public CollectionManager.Map<Player, Integer> getPlayerLives() {
        return this.playerLives;
    }

    public Player getCurrentJumper() {
        return this.currentJumper;
    }

    public ScoreboardSign<DACPlugin> getScoreboardSign() {
        return this.scoreboardSign;
    }
}
