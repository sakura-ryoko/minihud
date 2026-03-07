package fi.dy.masa.minihud.info;

import java.util.List;

import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.block.*;
import fi.dy.masa.minihud.info.camera.*;
import fi.dy.masa.minihud.info.chunk.*;
import fi.dy.masa.minihud.info.generic.*;
import fi.dy.masa.minihud.info.player.*;
import fi.dy.masa.minihud.info.state.*;
import fi.dy.masa.minihud.info.entity.*;
import fi.dy.masa.minihud.info.te.*;
import fi.dy.masa.minihud.info.world.*;

public class InfoLineTypes
{
    // Generic
    public static final InfoLineType<InfoLineFPS>                   FPS                     = InfoLineType.build(InfoLineFPS::new,                  InfoToggle.FPS, List.of(InfoLineFlag.GENERIC));
//	public static final InfoLineType<InfoLineGPU>                   GPU                     = InfoLineType.build(InfoLineGPU::new,                  InfoToggle.GPU, List.of(InfoLineFlag.GENERIC));
    public static final InfoLineType<InfoLineMemory>                MEMORY                  = InfoLineType.build(InfoLineMemory::new,               InfoToggle.MEMORY_USAGE, List.of(InfoLineFlag.GENERIC));
    public static final InfoLineType<InfoLineTimeIRL>               TIME_IRL                = InfoLineType.build(InfoLineTimeIRL::new,              InfoToggle.TIME_REAL, List.of(InfoLineFlag.GENERIC));
    public static final InfoLineType<InfoLineChunkSections>         CHUNK_SECTIONS          = InfoLineType.build(InfoLineChunkSections::new,        InfoToggle.CHUNK_SECTIONS, List.of(InfoLineFlag.GENERIC));
    public static final InfoLineType<InfoLineChunkSectionsFull>     CHUNK_SECTIONS_FULL     = InfoLineType.build(InfoLineChunkSectionsFull::new,    InfoToggle.CHUNK_SECTIONS_FULL, List.of(InfoLineFlag.GENERIC));
    public static final InfoLineType<InfoLineChunkUpdates>          CHUNK_UPDATES           = InfoLineType.build(InfoLineChunkUpdates::new,         InfoToggle.CHUNK_UPDATES, List.of(InfoLineFlag.GENERIC));
    public static final InfoLineType<InfoLineParticleCount>         PARTICLE_COUNT          = InfoLineType.build(InfoLineParticleCount::new,        InfoToggle.PARTICLE_COUNT, List.of(InfoLineFlag.GENERIC));
    public static final InfoLineType<InfoLineServerTPS>             SERVER_TPS              = InfoLineType.build(InfoLineServerTPS::new,            InfoToggle.SERVER_TPS, List.of(InfoLineFlag.GENERIC));
    public static final InfoLineType<InfoLineServux>                SERVUX                  = InfoLineType.build(InfoLineServux::new,               InfoToggle.SERVUX, List.of(InfoLineFlag.GENERIC));
    public static final InfoLineType<InfoLineWeather>               WEATHER                 = InfoLineType.build(InfoLineWeather::new,              InfoToggle.WEATHER, List.of(InfoLineFlag.GENERIC));
    public static final InfoLineType<InfoLineMobCaps>               MOB_CAPS                = InfoLineType.build(InfoLineMobCaps::new,              InfoToggle.MOB_CAPS, List.of(InfoLineFlag.GENERIC));
	public static final InfoLineType<InfoLineBlockBreakSpeed>       BLOCK_BREAK_SPEED       = InfoLineType.build(InfoLineBlockBreakSpeed::new,      InfoToggle.BLOCK_BREAK_SPEED, List.of(InfoLineFlag.GENERIC));
	public static final InfoLineType<InfoLineSprinting>             SPRINTING               = InfoLineType.build(InfoLineSprinting::new,            InfoToggle.SPRINTING, List.of(InfoLineFlag.GENERIC));

    // World / Best World
    public static final InfoLineType<InfoLineTimeWorld>             TIME_WORLD              = InfoLineType.build(InfoLineTimeWorld::new,            InfoToggle.TIME_WORLD, List.of(InfoLineFlag.WORLD));
    public static final InfoLineType<InfoLineTimeWorldFormatted>    TIME_WORLD_FORMATTED    = InfoLineType.build(InfoLineTimeWorldFormatted::new,   InfoToggle.TIME_WORLD_FORMATTED, List.of(InfoLineFlag.WORLD));
    public static final InfoLineType<InfoLineTimeTotalModulo>       TIME_TOTAL_MODULO       = InfoLineType.build(InfoLineTimeTotalModulo::new,      InfoToggle.TIME_TOTAL_MODULO, List.of(InfoLineFlag.WORLD));
    public static final InfoLineType<InfoLineTimeDayModulo>         TIME_DAY_MODULO         = InfoLineType.build(InfoLineTimeDayModulo::new,        InfoToggle.TIME_DAY_MODULO, List.of(InfoLineFlag.WORLD));
    public static final InfoLineType<InfoLineDifficulty>            DIFFICULTY              = InfoLineType.build(InfoLineDifficulty::new,           InfoToggle.DIFFICULTY, List.of(InfoLineFlag.WORLD, InfoLineFlag.CHUNK_POS));
    public static final InfoLineType<InfoLineLoadedChunks>          LOADED_CHUNKS           = InfoLineType.build(InfoLineLoadedChunks::new,         InfoToggle.LOADED_CHUNKS_COUNT, List.of(InfoLineFlag.BEST_WORLD));
    public static final InfoLineType<InfoLineSlimeChunk>            SLIME_CHUNK             = InfoLineType.build(InfoLineSlimeChunk::new,           InfoToggle.SLIME_CHUNK, List.of(InfoLineFlag.WORLD, InfoLineFlag.BLOCK_POS));
	public static final InfoLineType<InfoLineEntities>              ENTITIES                = InfoLineType.build(InfoLineEntities::new,             InfoToggle.ENTITIES, List.of(InfoLineFlag.WORLD));
	public static final InfoLineType<InfoLineEntitiesClientWorld>   ENTITIES_CLIENT_WORLD   = InfoLineType.build(InfoLineEntitiesClientWorld::new,  InfoToggle.ENTITIES_CLIENT_WORLD, List.of(InfoLineFlag.BEST_WORLD));
	public static final InfoLineType<InfoLineTileEntities>          TILE_ENTITIES           = InfoLineType.build(InfoLineTileEntities::new,         InfoToggle.TILE_ENTITIES, List.of(InfoLineFlag.WORLD));

    // Block
    public static final InfoLineType<InfoLineLookingAtBlock>        LOOKING_AT_BLOCK        = InfoLineType.build(InfoLineLookingAtBlock::new,       InfoToggle.LOOKING_AT_BLOCK, List.of(InfoLineFlag.WORLD, InfoLineFlag.BLOCK_POS));
	public static final InfoLineType<InfoLineBlockInChunk>          BLOCK_IN_CHUNK          = InfoLineType.build(InfoLineBlockInChunk::new,         InfoToggle.BLOCK_IN_CHUNK, List.of(InfoLineFlag.WORLD, InfoLineFlag.BLOCK_POS));

    // Block State
    public static final InfoLineType<InfoLineHoneyLevel>            HONEY_LEVEL             = InfoLineType.build(InfoLineHoneyLevel::new,           InfoToggle.HONEY_LEVEL, List.of(InfoLineFlag.WORLD, InfoLineFlag.BLOCK_STATE));
    public static final InfoLineType<InfoLineBlockProps>            BLOCK_PROPS             = InfoLineType.build(InfoLineBlockProps::new,           InfoToggle.BLOCK_PROPS, List.of(InfoLineFlag.WORLD, InfoLineFlag.BLOCK_STATE));

	// Chunk
	public static final InfoLineType<InfoLineLookingAtChunk>        LOOKING_AT_CHUNK        = InfoLineType.build(InfoLineLookingAtChunk::new,       InfoToggle.LOOKING_AT_BLOCK_CHUNK, List.of(InfoLineFlag.WORLD, InfoLineFlag.CHUNK_POS, InfoLineFlag.BLOCK_POS));
	public static final InfoLineType<InfoLineLightLevel>            LIGHT_LEVEL             = InfoLineType.build(InfoLineLightLevel::new,           InfoToggle.LIGHT_LEVEL, List.of(InfoLineFlag.WORLD, InfoLineFlag.CHUNK_POS, InfoLineFlag.BLOCK_POS));
	public static final InfoLineType<InfoLineBiome>                 BIOME                   = InfoLineType.build(InfoLineBiome::new,                InfoToggle.BIOME, List.of(InfoLineFlag.WORLD, InfoLineFlag.CHUNK_POS, InfoLineFlag.BLOCK_POS));
	public static final InfoLineType<InfoLineBiomeRegName>          BIOME_REG_NAME          = InfoLineType.build(InfoLineBiomeRegName::new,         InfoToggle.BIOME_REG_NAME, List.of(InfoLineFlag.WORLD, InfoLineFlag.CHUNK_POS, InfoLineFlag.BLOCK_POS));
	public static final InfoLineType<InfoLineBlockPos>              BLOCK_POS               = InfoLineType.build(InfoLineBlockPos::new,             InfoToggle.BLOCK_POS, List.of(InfoLineFlag.WORLD, InfoLineFlag.CHUNK_POS, InfoLineFlag.BLOCK_POS));
	public static final InfoLineType<InfoLineChunkPos>              CHUNK_POS               = InfoLineType.build(InfoLineChunkPos::new,             InfoToggle.CHUNK_POS, List.of(InfoLineFlag.WORLD, InfoLineFlag.CHUNK_POS, InfoLineFlag.BLOCK_POS));
	public static final InfoLineType<InfoLineRegionFile>            REGION_FILE             = InfoLineType.build(InfoLineRegionFile::new,           InfoToggle.REGION_FILE, List.of(InfoLineFlag.WORLD, InfoLineFlag.CHUNK_POS, InfoLineFlag.BLOCK_POS));

	// Camera
	public static final InfoLineType<InfoLineDistance>              DISTANCE                = InfoLineType.build(InfoLineDistance::new,             InfoToggle.DISTANCE, List.of(InfoLineFlag.WORLD, InfoLineFlag.CAMERA));
	public static final InfoLineType<InfoLineFacing>                FACING                  = InfoLineType.build(InfoLineFacing::new,               InfoToggle.FACING, List.of(InfoLineFlag.WORLD, InfoLineFlag.CAMERA));
	public static final InfoLineType<InfoLineRotationYaw>           ROTATION_YAW            = InfoLineType.build(InfoLineRotationYaw::new,          InfoToggle.ROTATION_YAW, List.of(InfoLineFlag.WORLD, InfoLineFlag.CAMERA));
	public static final InfoLineType<InfoLineRotationPitch>         ROTATION_PITCH          = InfoLineType.build(InfoLineRotationPitch::new,        InfoToggle.ROTATION_PITCH, List.of(InfoLineFlag.WORLD, InfoLineFlag.CAMERA));
	public static final InfoLineType<InfoLineSpeed>                 SPEED                   = InfoLineType.build(InfoLineSpeed::new,                InfoToggle.SPEED, List.of(InfoLineFlag.WORLD, InfoLineFlag.CAMERA));
	public static final InfoLineType<InfoLineSpeedHV>               SPEED_HV                = InfoLineType.build(InfoLineSpeedHV::new,              InfoToggle.SPEED_HV, List.of(InfoLineFlag.WORLD, InfoLineFlag.CAMERA));
	public static final InfoLineType<InfoLineSpeedAxis>             SPEED_AXIS              = InfoLineType.build(InfoLineSpeedAxis::new,            InfoToggle.SPEED_AXIS, List.of(InfoLineFlag.WORLD, InfoLineFlag.CAMERA));
	public static final InfoLineType<InfoLineCoordinates>           COORDINATES             = InfoLineType.build(InfoLineCoordinates::new,          InfoToggle.COORDINATES, List.of(InfoLineFlag.WORLD, InfoLineFlag.CAMERA));
	public static final InfoLineType<InfoLineCoordinatesScaled>     COORDINATES_SCALED      = InfoLineType.build(InfoLineCoordinatesScaled::new,    InfoToggle.COORDINATES_SCALED, List.of(InfoLineFlag.WORLD, InfoLineFlag.CAMERA));
	public static final InfoLineType<InfoLineDimension>             DIMENSION               = InfoLineType.build(InfoLineDimension::new,            InfoToggle.DIMENSION, List.of(InfoLineFlag.WORLD, InfoLineFlag.CAMERA));

	// Player
	public static final InfoLineType<InfoLinePlayerExp>             PLAYER_EXP              = InfoLineType.build(InfoLinePlayerExp::new,            InfoToggle.PLAYER_EXPERIENCE, List.of(InfoLineFlag.WORLD, InfoLineFlag.PLAYER));
    public static final InfoLineType<InfoLinePing>                  PING                    = InfoLineType.build(InfoLinePing::new,                 InfoToggle.PING, List.of(InfoLineFlag.WORLD, InfoLineFlag.PLAYER));
    public static final InfoLineType<InfoLineSculkWarningLevel>     SCULK_WARNING_LEVEL     = InfoLineType.build(InfoLineSculkWarningLevel::new,    InfoToggle.SCULK_WARNING_LEVEL, List.of(InfoLineFlag.WORLD, InfoLineFlag.PLAYER));

    // Block Entity
    public static final InfoLineType<InfoLineFurnaceExp>            FURNACE_EXP             = InfoLineType.build(InfoLineFurnaceExp::new,           InfoToggle.FURNACE_XP, List.of(InfoLineFlag.WORLD, InfoLineFlag.TILE_ENTITY));
    public static final InfoLineType<InfoLineBeeCount>              BEE_COUNT               = InfoLineType.build(InfoLineBeeCount::new,             InfoToggle.BEE_COUNT, List.of(InfoLineFlag.WORLD, InfoLineFlag.TILE_ENTITY));
    public static final InfoLineType<InfoLineComparator>            COMPARATOR              = InfoLineType.build(InfoLineComparator::new,           InfoToggle.COMPARATOR_OUTPUT, List.of(InfoLineFlag.WORLD, InfoLineFlag.TILE_ENTITY));

    // Entity
    public static final InfoLineType<InfoLineEntityRegName>         ENTITY_REG              = InfoLineType.build(InfoLineEntityRegName::new,        InfoToggle.ENTITY_REG_NAME, List.of(InfoLineFlag.WORLD, InfoLineFlag.ENTITY));
    public static final InfoLineType<InfoLineLookingAtEffects>      LOOKING_AT_EFFECTS      = InfoLineType.build(InfoLineLookingAtEffects::new,     InfoToggle.LOOKING_AT_EFFECTS, List.of(InfoLineFlag.WORLD, InfoLineFlag.ENTITY));
    public static final InfoLineType<InfoLineLookingAtEntity>       LOOKING_AT_ENTITY       = InfoLineType.build(InfoLineLookingAtEntity::new,      InfoToggle.LOOKING_AT_ENTITY, List.of(InfoLineFlag.WORLD, InfoLineFlag.ENTITY));
    public static final InfoLineType<InfoLineLookingAtPlayerExp>    LOOKING_AT_PLAYER_EXP   = InfoLineType.build(InfoLineLookingAtPlayerExp::new,   InfoToggle.LOOKING_AT_PLAYER_EXP, List.of(InfoLineFlag.WORLD, InfoLineFlag.ENTITY));
    public static final InfoLineType<InfoLineZombieConversion>      ZOMBIE_CONVERSION       = InfoLineType.build(InfoLineZombieConversion::new,     InfoToggle.ZOMBIE_CONVERSION, List.of(InfoLineFlag.WORLD, InfoLineFlag.ENTITY));
    public static final InfoLineType<InfoLineEntityVariant>         ENTITY_VARIANT          = InfoLineType.build(InfoLineEntityVariant::new,        InfoToggle.ENTITY_VARIANT, List.of(InfoLineFlag.WORLD, InfoLineFlag.ENTITY));
    public static final InfoLineType<InfoLineDolphinTreasure>       DOLPHIN_TREASURE        = InfoLineType.build(InfoLineDolphinTreasure::new,      InfoToggle.DOLPHIN_TREASURE, List.of(InfoLineFlag.WORLD, InfoLineFlag.ENTITY));
    public static final InfoLineType<InfoLinePandaGene>             PANDA_GENE              = InfoLineType.build(InfoLinePandaGene::new,            InfoToggle.PANDA_GENE, List.of(InfoLineFlag.WORLD, InfoLineFlag.ENTITY));
    public static final InfoLineType<InfoLineHomePos>               HOME_POS                = InfoLineType.build(InfoLineHomePos::new,              InfoToggle.ENTITY_HOME_POS, List.of(InfoLineFlag.WORLD, InfoLineFlag.ENTITY));
    public static final InfoLineType<InfoLineHorseJump>             HORSE_JUMP              = InfoLineType.build(InfoLineHorseJump::new,            InfoToggle.HORSE_JUMP, List.of(InfoLineFlag.WORLD, InfoLineFlag.VEHICLE, InfoLineFlag.ENTITY));
    public static final InfoLineType<InfoLineHorseSpeed>            HORSE_SPEED             = InfoLineType.build(InfoLineHorseSpeed::new,           InfoToggle.HORSE_SPEED, List.of(InfoLineFlag.WORLD, InfoLineFlag.VEHICLE, InfoLineFlag.ENTITY));
    public static final InfoLineType<InfoLineHorseMaxHealth>        HORSE_MAX_HEALTH        = InfoLineType.build(InfoLineHorseMaxHealth::new,       InfoToggle.HORSE_MAX_HEALTH, List.of(InfoLineFlag.WORLD, InfoLineFlag.VEHICLE, InfoLineFlag.ENTITY));
	public static final InfoLineType<InfoLineCopperAging>           COPPER_AGING            = InfoLineType.build(InfoLineCopperAging::new, 			InfoToggle.ENTITY_COPPER_AGING, List.of(InfoLineFlag.WORLD, InfoLineFlag.ENTITY));
}
