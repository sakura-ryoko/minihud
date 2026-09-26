package fi.dy.masa.minihud.info;

import com.google.common.collect.ImmutableList;

import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.block.InfoLineLookingAtBlock;
import fi.dy.masa.minihud.info.block.InfoLineLookingAtChunk;
import fi.dy.masa.minihud.info.camera.*;
import fi.dy.masa.minihud.info.chunk.*;
import fi.dy.masa.minihud.info.entity.*;
import fi.dy.masa.minihud.info.generic.*;
import fi.dy.masa.minihud.info.player.InfoLinePing;
import fi.dy.masa.minihud.info.player.InfoLinePlayerExp;
import fi.dy.masa.minihud.info.player.InfoLineSculkWarningLevel;
import fi.dy.masa.minihud.info.state.InfoLineBlockProps;
import fi.dy.masa.minihud.info.state.InfoLineHoneyLevel;
import fi.dy.masa.minihud.info.te.InfoLineBeeCount;
import fi.dy.masa.minihud.info.te.InfoLineComparator;
import fi.dy.masa.minihud.info.te.InfoLineFurnaceExp;
import fi.dy.masa.minihud.info.world.*;

public class InfoLineTypes
{
    // Generic
    public static final InfoLineType<InfoLineFPS>                   FPS                     = InfoLineType.build(InfoLineFPS::new,                  InfoToggle.FPS, ImmutableList.of(InfoLineFlag.GENERIC), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineMemory>                MEMORY                  = InfoLineType.build(InfoLineMemory::new,               InfoToggle.MEMORY_USAGE, ImmutableList.of(InfoLineFlag.GENERIC), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineTimeIRL>               TIME_IRL                = InfoLineType.build(InfoLineTimeIRL::new,              InfoToggle.TIME_REAL, ImmutableList.of(InfoLineFlag.GENERIC), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineChunkSections>         CHUNK_SECTIONS          = InfoLineType.build(InfoLineChunkSections::new,        InfoToggle.CHUNK_SECTIONS, ImmutableList.of(InfoLineFlag.GENERIC), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineChunkSectionsFull>     CHUNK_SECTIONS_FULL     = InfoLineType.build(InfoLineChunkSectionsFull::new,    InfoToggle.CHUNK_SECTIONS_FULL, ImmutableList.of(InfoLineFlag.GENERIC), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineChunkUpdates>          CHUNK_UPDATES           = InfoLineType.build(InfoLineChunkUpdates::new,         InfoToggle.CHUNK_UPDATES, ImmutableList.of(InfoLineFlag.GENERIC), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineParticleCount>         PARTICLE_COUNT          = InfoLineType.build(InfoLineParticleCount::new,        InfoToggle.PARTICLE_COUNT, ImmutableList.of(InfoLineFlag.GENERIC), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineServerTPS>             SERVER_TPS              = InfoLineType.build(InfoLineServerTPS::new,            InfoToggle.SERVER_TPS, ImmutableList.of(InfoLineFlag.GENERIC), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineServux>                SERVUX                  = InfoLineType.build(InfoLineServux::new,               InfoToggle.SERVUX, ImmutableList.of(InfoLineFlag.GENERIC), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineWeather>               WEATHER                 = InfoLineType.build(InfoLineWeather::new,              InfoToggle.WEATHER, ImmutableList.of(InfoLineFlag.GENERIC), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineMobCaps>               MOB_CAPS                = InfoLineType.build(InfoLineMobCaps::new,              InfoToggle.MOB_CAPS, ImmutableList.of(InfoLineFlag.GENERIC), InfoLineGroups.EMPTY);
	public static final InfoLineType<InfoLineBlockBreakSpeed>       BLOCK_BREAK_SPEED       = InfoLineType.build(InfoLineBlockBreakSpeed::new,      InfoToggle.BLOCK_BREAK_SPEED, ImmutableList.of(InfoLineFlag.GENERIC), InfoLineGroups.EMPTY);
	public static final InfoLineType<InfoLineSneaking>              SNEAKING                = InfoLineType.build(InfoLineSneaking::new,             InfoToggle.SNEAKING, ImmutableList.of(InfoLineFlag.GENERIC), InfoLineGroups.SPRINT_SNEAK);
	public static final InfoLineType<InfoLineSprinting>             SPRINTING               = InfoLineType.build(InfoLineSprinting::new,            InfoToggle.SPRINTING, ImmutableList.of(InfoLineFlag.GENERIC), InfoLineGroups.SPRINT_SNEAK);

    // World / Best World
    public static final InfoLineType<InfoLineTimeWorld>             TIME_WORLD              = InfoLineType.build(InfoLineTimeWorld::new,            InfoToggle.TIME_WORLD, ImmutableList.of(InfoLineFlag.WORLD), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineTimeWorldFormatted>    TIME_WORLD_FORMATTED    = InfoLineType.build(InfoLineTimeWorldFormatted::new,   InfoToggle.TIME_WORLD_FORMATTED, ImmutableList.of(InfoLineFlag.WORLD), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineTimeTotalModulo>       TIME_TOTAL_MODULO       = InfoLineType.build(InfoLineTimeTotalModulo::new,      InfoToggle.TIME_TOTAL_MODULO, ImmutableList.of(InfoLineFlag.WORLD), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineTimeDayModulo>         TIME_DAY_MODULO         = InfoLineType.build(InfoLineTimeDayModulo::new,        InfoToggle.TIME_DAY_MODULO, ImmutableList.of(InfoLineFlag.WORLD), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineDifficulty>            DIFFICULTY              = InfoLineType.build(InfoLineDifficulty::new,           InfoToggle.DIFFICULTY, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.CHUNK_POS), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineLoadedChunks>          LOADED_CHUNKS           = InfoLineType.build(InfoLineLoadedChunks::new,         InfoToggle.LOADED_CHUNKS_COUNT, ImmutableList.of(InfoLineFlag.BEST_WORLD), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineSlimeChunk>            SLIME_CHUNK             = InfoLineType.build(InfoLineSlimeChunk::new,           InfoToggle.SLIME_CHUNK, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.BLOCK_POS), InfoLineGroups.EMPTY);
	public static final InfoLineType<InfoLineEntities>              ENTITIES                = InfoLineType.build(InfoLineEntities::new,             InfoToggle.ENTITIES, ImmutableList.of(InfoLineFlag.WORLD), InfoLineGroups.WORLD_ENTITIES);
	public static final InfoLineType<InfoLineEntitiesClientWorld>   ENTITIES_CLIENT_WORLD   = InfoLineType.build(InfoLineEntitiesClientWorld::new,  InfoToggle.ENTITIES_CLIENT_WORLD, ImmutableList.of(InfoLineFlag.BEST_WORLD), InfoLineGroups.EMPTY);
	public static final InfoLineType<InfoLineTileEntities>          TILE_ENTITIES           = InfoLineType.build(InfoLineTileEntities::new,         InfoToggle.TILE_ENTITIES, ImmutableList.of(InfoLineFlag.WORLD), InfoLineGroups.WORLD_ENTITIES);

    // Block
    public static final InfoLineType<InfoLineLookingAtBlock>        LOOKING_AT_BLOCK        = InfoLineType.build(InfoLineLookingAtBlock::new,       InfoToggle.LOOKING_AT_BLOCK, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.BLOCK_POS), InfoLineGroups.BLOCK_LOOKING);
	public static final InfoLineType<InfoLineBlockInChunk>          BLOCK_IN_CHUNK          = InfoLineType.build(InfoLineBlockInChunk::new,         InfoToggle.BLOCK_IN_CHUNK, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.BLOCK_POS), InfoLineGroups.EMPTY);

    // Block State
    public static final InfoLineType<InfoLineHoneyLevel>            HONEY_LEVEL             = InfoLineType.build(InfoLineHoneyLevel::new,           InfoToggle.HONEY_LEVEL, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.BLOCK_STATE), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineBlockProps>            BLOCK_PROPS             = InfoLineType.build(InfoLineBlockProps::new,           InfoToggle.BLOCK_PROPS, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.BLOCK_STATE), InfoLineGroups.EMPTY);

	// Chunk
	public static final InfoLineType<InfoLineLookingAtChunk>        LOOKING_AT_CHUNK        = InfoLineType.build(InfoLineLookingAtChunk::new,       InfoToggle.LOOKING_AT_BLOCK_CHUNK, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.CHUNK_POS, InfoLineFlag.BLOCK_POS), InfoLineGroups.BLOCK_LOOKING);
	public static final InfoLineType<InfoLineLightLevel>            LIGHT_LEVEL             = InfoLineType.build(InfoLineLightLevel::new,           InfoToggle.LIGHT_LEVEL, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.CHUNK_POS, InfoLineFlag.BLOCK_POS), InfoLineGroups.EMPTY);
	public static final InfoLineType<InfoLineBiome>                 BIOME                   = InfoLineType.build(InfoLineBiome::new,                InfoToggle.BIOME, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.CHUNK_POS, InfoLineFlag.BLOCK_POS), InfoLineGroups.EMPTY);
	public static final InfoLineType<InfoLineBiomeRegName>          BIOME_REG_NAME          = InfoLineType.build(InfoLineBiomeRegName::new,         InfoToggle.BIOME_REG_NAME, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.CHUNK_POS, InfoLineFlag.BLOCK_POS), InfoLineGroups.EMPTY);
	public static final InfoLineType<InfoLineBlockPos>              BLOCK_POS               = InfoLineType.build(InfoLineBlockPos::new,             InfoToggle.BLOCK_POS, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.CHUNK_POS, InfoLineFlag.BLOCK_POS), InfoLineGroups.CHUNK_POS);
	public static final InfoLineType<InfoLineChunkPos>              CHUNK_POS               = InfoLineType.build(InfoLineChunkPos::new,             InfoToggle.CHUNK_POS, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.CHUNK_POS, InfoLineFlag.BLOCK_POS), InfoLineGroups.CHUNK_POS);
	public static final InfoLineType<InfoLineRegionFile>            REGION_FILE             = InfoLineType.build(InfoLineRegionFile::new,           InfoToggle.REGION_FILE, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.CHUNK_POS, InfoLineFlag.BLOCK_POS), InfoLineGroups.CHUNK_POS);

	// Camera
	public static final InfoLineType<InfoLineDistance>              DISTANCE                = InfoLineType.build(InfoLineDistance::new,             InfoToggle.DISTANCE, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.CAMERA), InfoLineGroups.EMPTY);
	public static final InfoLineType<InfoLineFacing>                FACING                  = InfoLineType.build(InfoLineFacing::new,               InfoToggle.FACING, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.CAMERA), InfoLineGroups.EMPTY);
	public static final InfoLineType<InfoLineRotationYaw>           ROTATION_YAW            = InfoLineType.build(InfoLineRotationYaw::new,          InfoToggle.ROTATION_YAW, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.CAMERA), InfoLineGroups.CAMERA_ROTATION_SPEED);
	public static final InfoLineType<InfoLineRotationPitch>         ROTATION_PITCH          = InfoLineType.build(InfoLineRotationPitch::new,        InfoToggle.ROTATION_PITCH, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.CAMERA), InfoLineGroups.CAMERA_ROTATION_SPEED);
	public static final InfoLineType<InfoLineSpeed>                 SPEED                   = InfoLineType.build(InfoLineSpeed::new,                InfoToggle.SPEED, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.CAMERA), InfoLineGroups.CAMERA_ROTATION_SPEED);
	public static final InfoLineType<InfoLineSpeedHV>               SPEED_HV                = InfoLineType.build(InfoLineSpeedHV::new,              InfoToggle.SPEED_HV, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.CAMERA), InfoLineGroups.EMPTY);
	public static final InfoLineType<InfoLineSpeedAxis>             SPEED_AXIS              = InfoLineType.build(InfoLineSpeedAxis::new,            InfoToggle.SPEED_AXIS, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.CAMERA), InfoLineGroups.EMPTY);
	public static final InfoLineType<InfoLineCoordinates>           COORDINATES             = InfoLineType.build(InfoLineCoordinates::new,          InfoToggle.COORDINATES, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.CAMERA), InfoLineGroups.CAMERA_COORDS);
	public static final InfoLineType<InfoLineCoordinatesScaled>     COORDINATES_SCALED      = InfoLineType.build(InfoLineCoordinatesScaled::new,    InfoToggle.COORDINATES_SCALED, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.CAMERA), InfoLineGroups.CAMERA_COORDS);
	public static final InfoLineType<InfoLineDimension>             DIMENSION               = InfoLineType.build(InfoLineDimension::new,            InfoToggle.DIMENSION, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.CAMERA), InfoLineGroups.CAMERA_COORDS);

	// Player
	public static final InfoLineType<InfoLinePlayerExp>             PLAYER_EXP              = InfoLineType.build(InfoLinePlayerExp::new,            InfoToggle.PLAYER_EXPERIENCE, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.PLAYER), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLinePing>                  PING                    = InfoLineType.build(InfoLinePing::new,                 InfoToggle.PING, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.PLAYER), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineSculkWarningLevel>     SCULK_WARNING_LEVEL     = InfoLineType.build(InfoLineSculkWarningLevel::new,    InfoToggle.SCULK_WARNING_LEVEL, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.PLAYER), InfoLineGroups.EMPTY);

    // Block Entity
    public static final InfoLineType<InfoLineFurnaceExp>            FURNACE_EXP             = InfoLineType.build(InfoLineFurnaceExp::new,           InfoToggle.FURNACE_XP, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.TILE_ENTITY), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineBeeCount>              BEE_COUNT               = InfoLineType.build(InfoLineBeeCount::new,             InfoToggle.BEE_COUNT, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.TILE_ENTITY), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineComparator>            COMPARATOR              = InfoLineType.build(InfoLineComparator::new,           InfoToggle.COMPARATOR_OUTPUT, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.TILE_ENTITY), InfoLineGroups.EMPTY);

    // Entity
    public static final InfoLineType<InfoLineEntityRegName>         ENTITY_REG              = InfoLineType.build(InfoLineEntityRegName::new,        InfoToggle.ENTITY_REG_NAME, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.ENTITY), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineLookingAtEffects>      LOOKING_AT_EFFECTS      = InfoLineType.build(InfoLineLookingAtEffects::new,     InfoToggle.LOOKING_AT_EFFECTS, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.ENTITY), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineLookingAtEntity>       LOOKING_AT_ENTITY       = InfoLineType.build(InfoLineLookingAtEntity::new,      InfoToggle.LOOKING_AT_ENTITY, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.ENTITY), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineLookingAtPlayerExp>    LOOKING_AT_PLAYER_EXP   = InfoLineType.build(InfoLineLookingAtPlayerExp::new,   InfoToggle.LOOKING_AT_PLAYER_EXP, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.ENTITY), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineZombieConversion>      ZOMBIE_CONVERSION       = InfoLineType.build(InfoLineZombieConversion::new,     InfoToggle.ZOMBIE_CONVERSION, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.ENTITY), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineEntityVariant>         ENTITY_VARIANT          = InfoLineType.build(InfoLineEntityVariant::new,        InfoToggle.ENTITY_VARIANT, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.ENTITY), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineDolphinTreasure>       DOLPHIN_TREASURE        = InfoLineType.build(InfoLineDolphinTreasure::new,      InfoToggle.DOLPHIN_TREASURE, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.ENTITY), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLinePandaGene>             PANDA_GENE              = InfoLineType.build(InfoLinePandaGene::new,            InfoToggle.PANDA_GENE, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.ENTITY), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineHomePos>               HOME_POS                = InfoLineType.build(InfoLineHomePos::new,              InfoToggle.ENTITY_HOME_POS, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.ENTITY), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineHorseJump>             HORSE_JUMP              = InfoLineType.build(InfoLineHorseJump::new,            InfoToggle.HORSE_JUMP, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.VEHICLE, InfoLineFlag.ENTITY), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineHorseSpeed>            HORSE_SPEED             = InfoLineType.build(InfoLineHorseSpeed::new,           InfoToggle.HORSE_SPEED, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.VEHICLE, InfoLineFlag.ENTITY), InfoLineGroups.EMPTY);
    public static final InfoLineType<InfoLineHorseMaxHealth>        HORSE_MAX_HEALTH        = InfoLineType.build(InfoLineHorseMaxHealth::new,       InfoToggle.HORSE_MAX_HEALTH, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.VEHICLE, InfoLineFlag.ENTITY), InfoLineGroups.EMPTY);
	public static final InfoLineType<InfoLineCopperAging>           COPPER_AGING            = InfoLineType.build(InfoLineCopperAging::new, 			InfoToggle.ENTITY_COPPER_AGING, ImmutableList.of(InfoLineFlag.WORLD, InfoLineFlag.ENTITY), InfoLineGroups.EMPTY);
}
