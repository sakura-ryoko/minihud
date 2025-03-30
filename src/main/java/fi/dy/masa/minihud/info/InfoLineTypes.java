package fi.dy.masa.minihud.info;

import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.state.*;
import fi.dy.masa.minihud.info.entity.*;
import fi.dy.masa.minihud.info.te.*;

public class InfoLineTypes
{
    // Block
    public static final InfoLineType<InfoLineHoneyLevel>            HONEY_LEVEL             = InfoLineType.build(InfoLineHoneyLevel::new,           InfoToggle.HONEY_LEVEL);

    // Block Entity
    public static final InfoLineType<InfoLineFurnaceExp>            FURNACE_EXP             = InfoLineType.build(InfoLineFurnaceExp::new,           InfoToggle.FURNACE_XP);
    public static final InfoLineType<InfoLineBeeCount>              BEE_COUNT               = InfoLineType.build(InfoLineBeeCount::new,             InfoToggle.BEE_COUNT);
    public static final InfoLineType<InfoLineComparator>            COMPARATOR              = InfoLineType.build(InfoLineComparator::new,           InfoToggle.COMPARATOR_OUTPUT);

    // Entity
    public static final InfoLineType<InfoLineLookingAtEffects>      LOOKING_AT_EFFECTS      = InfoLineType.build(InfoLineLookingAtEffects::new,     InfoToggle.LOOKING_AT_EFFECTS);
    public static final InfoLineType<InfoLineLookingAtEntity>       LOOKING_AT_ENTITY       = InfoLineType.build(InfoLineLookingAtEntity::new,      InfoToggle.LOOKING_AT_ENTITY);
    public static final InfoLineType<InfoLineLookingAtPlayerExp>    LOOKING_AT_PLAYER_EXP   = InfoLineType.build(InfoLineLookingAtPlayerExp::new,   InfoToggle.LOOKING_AT_PLAYER_EXP);
    public static final InfoLineType<InfoLineZombieConversion>      ZOMBIE_CONVERSION       = InfoLineType.build(InfoLineZombieConversion::new,     InfoToggle.ZOMBIE_CONVERSION);
    public static final InfoLineType<InfoLineEntityVariant>         ENTITY_VARIANT          = InfoLineType.build(InfoLineEntityVariant::new,        InfoToggle.ENTITY_VARIANT);
    public static final InfoLineType<InfoLineDolphinTreasure>       DOLPHIN_TREASURE        = InfoLineType.build(InfoLineDolphinTreasure::new,      InfoToggle.DOLPHIN_TREASURE);
    public static final InfoLineType<InfoLinePandaGene>             PANDA_GENE              = InfoLineType.build(InfoLinePandaGene::new,            InfoToggle.PANDA_GENE);
}
