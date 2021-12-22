package fr.naruse.dac.arena;

public class ArenaStatus {

    private Status currentGameStatus = Status.WAITING;

    public boolean isActive(Status status){
        return currentGameStatus == status;
    }

    public void setActive(Status status){
        this.currentGameStatus = status;
    }

    public enum Status {

        WAITING,
        IN_GAME,
        ;

    }

}
