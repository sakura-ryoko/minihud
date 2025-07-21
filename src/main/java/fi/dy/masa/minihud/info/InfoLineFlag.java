package fi.dy.masa.minihud.info;

public enum InfoLineFlag
{
    GENERIC,
    WORLD,
    BEST_WORLD,     // Returns the World as the "BestWorld" value
    POS,
    CHUNK_POS,      // Returns BlockPos as the FlooredPos
    BLOCK,
    STATE,
    PLAYER,
    VEHICLE,
    ENTITY,
    TILE_ENTITY
}
