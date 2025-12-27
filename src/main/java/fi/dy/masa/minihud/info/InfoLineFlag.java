package fi.dy.masa.minihud.info;

public enum InfoLineFlag
{
    GENERIC,
    WORLD,
    BEST_WORLD,     // Returns the World as the "BestWorld" value
	BLOCK_POS,
	BLOCK_STATE,
    CHUNK_POS,      // Returns BlockPos as the FlooredPos
    PLAYER,
	CAMERA,
    VEHICLE,
    ENTITY,
    TILE_ENTITY
}
