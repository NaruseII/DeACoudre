package fr.naruse.dac.utils;

import fr.naruse.dac.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.function.Consumer;
import java.util.function.Predicate;

public enum WinnerBroadcastReceiver {

    CURRENT_WORLD{
        @Override
        public void sendWinMessage(Arena arena, String message) {
            Bukkit.getOnlinePlayers().stream()
                    .filter((Predicate<Player>) player -> player.getWorld().equals(arena.getDivingLocation().getWorld()))
                    .forEach((Consumer<Player>) player -> player.sendMessage(message));
        }
    },
    SERVER_BROADCAST{
        @Override
        public void sendWinMessage(Arena arena, String message) {
            Bukkit.broadcastMessage(message);
        }
    },
    ONLY_WINNER{
        @Override
        public void sendWinMessage(Arena arena, String message) {
            if(arena.getPlayerInGameSize() != 0){
                arena.getPlayerInGame().getByIndex(0).sendMessage(message);
            }
        }
    },
    NO_ONE{
        @Override
        public void sendWinMessage(Arena arena, String message) { }
    },
    ;

    public abstract void sendWinMessage(Arena arena, String message) ;

    public static WinnerBroadcastReceiver fromOrdinal(int ordinal){
        for (WinnerBroadcastReceiver value : values()) {
            if(value.ordinal() == ordinal){
                return value;
            }
        }
        return null;
    }


}
