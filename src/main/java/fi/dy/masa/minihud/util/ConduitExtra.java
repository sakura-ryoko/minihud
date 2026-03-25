package fi.dy.masa.minihud.util;

public interface ConduitExtra
{
    boolean minihud$getStoredActiveStatus();

    int minihud$getCurrentActivatingBlockCount();

    int minihud$getStoredActivatingBlockCount();

    void minihud$setActivatingBlockCount(int count);

    void minihud$setWasActive(boolean wasActive);
}
