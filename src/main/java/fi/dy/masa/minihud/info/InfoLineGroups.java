package fi.dy.masa.minihud.info;

import com.google.common.collect.ImmutableList;

public class InfoLineGroups
{
	public static final ImmutableList<String> EMPTY = ImmutableList.of();
	public static final ImmutableList<String> BLOCK_LOOKING = ImmutableList.of(InfoLineKeys.LOOKING_AT_BLOCK, InfoLineKeys.LOOKING_AT_BLOCK_CHUNK);
	public static final ImmutableList<String> CAMERA_COORDS = ImmutableList.of(InfoLineKeys.COORDINATES, InfoLineKeys.COORDINATES_SCALED, InfoLineKeys.DIMENSION);
	public static final ImmutableList<String> CAMERA_ROTATION_SPEED = ImmutableList.of(InfoLineKeys.ROTATION_YAW, InfoLineKeys.ROTATION_PITCH, InfoLineKeys.SPEED);
	public static final ImmutableList<String> CHUNK_POS = ImmutableList.of(InfoLineKeys.BLOCK_POS, InfoLineKeys.CHUNK_POS, InfoLineKeys.REGION_FILE);
	public static final ImmutableList<String> SPRINT_SNEAK = ImmutableList.of(InfoLineKeys.SNEAKING, InfoLineKeys.SPRINTING);
	public static final ImmutableList<String> WORLD_ENTITIES = ImmutableList.of(InfoLineKeys.ENTITIES, InfoLineKeys.TILE_ENTITIES);
}
