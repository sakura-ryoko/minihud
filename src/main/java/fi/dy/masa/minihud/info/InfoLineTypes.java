package fi.dy.masa.minihud.info;

import java.util.List;

import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.block.*;
import fi.dy.masa.minihud.info.generic.*;
import fi.dy.masa.minihud.info.state.*;
import fi.dy.masa.minihud.info.entity.*;
import fi.dy.masa.minihud.info.te.*;

public class InfoLineTypes
{
    // Generic
    public static final InfoLineType<InfoLineLoadedChunks>          LOADED_CHUNKS           = InfoLineType.build(InfoLineLoadedChunks::new,         InfoToggle.LOADED_CHUNKS_COUNT, List.of(InfoLineFlag.GENERIC));
    public static final InfoLineType<InfoLineServerTPS>             SERVER_TPS              = InfoLineType.build(InfoLineServerTPS::new,            InfoToggle.SERVER_TPS, List.of(InfoLineFlag.GENERIC));

    // Block
    public static final InfoLineType<InfoLineLookingAtBlock>        LOOKING_AT_BLOCK        = InfoLineType.build(InfoLineLookingAtBlock::new,       InfoToggle.LOOKING_AT_BLOCK, List.of(InfoLineFlag.BLOCK));
    public static final InfoLineType<InfoLineLookingAtChunk>        LOOKING_AT_CHUNK        = InfoLineType.build(InfoLineLookingAtChunk::new,       InfoToggle.LOOKING_AT_BLOCK_CHUNK, List.of(InfoLineFlag.BLOCK));

    // Block State
    public static final InfoLineType<InfoLineHoneyLevel>            HONEY_LEVEL             = InfoLineType.build(InfoLineHoneyLevel::new,           InfoToggle.HONEY_LEVEL, List.of(InfoLineFlag.STATE));
    public static final InfoLineType<InfoLineBlockProps>            BLOCK_PROPS             = InfoLineType.build(InfoLineBlockProps::new,           InfoToggle.BLOCK_PROPS, List.of(InfoLineFlag.STATE));

    // Block Entity
    public static final InfoLineType<InfoLineFurnaceExp>            FURNACE_EXP             = InfoLineType.build(InfoLineFurnaceExp::new,           InfoToggle.FURNACE_XP, List.of(InfoLineFlag.TILE_ENTITY));
    public static final InfoLineType<InfoLineBeeCount>              BEE_COUNT               = InfoLineType.build(InfoLineBeeCount::new,             InfoToggle.BEE_COUNT, List.of(InfoLineFlag.TILE_ENTITY));
    public static final InfoLineType<InfoLineComparator>            COMPARATOR              = InfoLineType.build(InfoLineComparator::new,           InfoToggle.COMPARATOR_OUTPUT, List.of(InfoLineFlag.TILE_ENTITY));

    // Entity
    public static final InfoLineType<InfoLineEntityRegName>         ENTITY_REG              = InfoLineType.build(InfoLineEntityRegName::new,        InfoToggle.ENTITY_REG_NAME, List.of(InfoLineFlag.ENTITY));
    public static final InfoLineType<InfoLineLookingAtEffects>      LOOKING_AT_EFFECTS      = InfoLineType.build(InfoLineLookingAtEffects::new,     InfoToggle.LOOKING_AT_EFFECTS, List.of(InfoLineFlag.ENTITY));
    public static final InfoLineType<InfoLineLookingAtEntity>       LOOKING_AT_ENTITY       = InfoLineType.build(InfoLineLookingAtEntity::new,      InfoToggle.LOOKING_AT_ENTITY, List.of(InfoLineFlag.ENTITY));
    public static final InfoLineType<InfoLineLookingAtPlayerExp>    LOOKING_AT_PLAYER_EXP   = InfoLineType.build(InfoLineLookingAtPlayerExp::new,   InfoToggle.LOOKING_AT_PLAYER_EXP, List.of(InfoLineFlag.ENTITY));
    public static final InfoLineType<InfoLineZombieConversion>      ZOMBIE_CONVERSION       = InfoLineType.build(InfoLineZombieConversion::new,     InfoToggle.ZOMBIE_CONVERSION, List.of(InfoLineFlag.ENTITY));
    public static final InfoLineType<InfoLineEntityVariant>         ENTITY_VARIANT          = InfoLineType.build(InfoLineEntityVariant::new,        InfoToggle.ENTITY_VARIANT, List.of(InfoLineFlag.ENTITY));
    public static final InfoLineType<InfoLineDolphinTreasure>       DOLPHIN_TREASURE        = InfoLineType.build(InfoLineDolphinTreasure::new,      InfoToggle.DOLPHIN_TREASURE, List.of(InfoLineFlag.ENTITY));
    public static final InfoLineType<InfoLinePandaGene>             PANDA_GENE              = InfoLineType.build(InfoLinePandaGene::new,            InfoToggle.PANDA_GENE, List.of(InfoLineFlag.ENTITY));
    public static final InfoLineType<InfoLineHomePos>               HOME_POS                = InfoLineType.build(InfoLineHomePos::new,              InfoToggle.ENTITY_HOME_POS, List.of(InfoLineFlag.ENTITY));
    public static final InfoLineType<InfoLineHorseJump>             HORSE_JUMP              = InfoLineType.build(InfoLineHorseJump::new,            InfoToggle.HORSE_JUMP, List.of(InfoLineFlag.VEHICLE, InfoLineFlag.ENTITY));
    public static final InfoLineType<InfoLineHorseSpeed>            HORSE_SPEED             = InfoLineType.build(InfoLineHorseSpeed::new,           InfoToggle.HORSE_SPEED, List.of(InfoLineFlag.VEHICLE, InfoLineFlag.ENTITY));
}
