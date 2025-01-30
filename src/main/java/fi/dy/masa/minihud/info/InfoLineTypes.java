package fi.dy.masa.minihud.info;

import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.entity.InfoLineEntityVariant;
import fi.dy.masa.minihud.info.entity.InfoLineLookingAtEffects;
import fi.dy.masa.minihud.info.entity.InfoLineLookingAtEntity;
import fi.dy.masa.minihud.info.te.InfoLineFurnaceExp;

public class InfoLineTypes
{
    // Block Entity
    public static final InfoLineType<InfoLineFurnaceExp>        FURNACE_EXP             = InfoLineType.build(InfoLineFurnaceExp::new,       InfoToggle.FURNACE_XP);

    // Entity
    public static final InfoLineType<InfoLineLookingAtEffects>  LOOKING_AT_EFFECTS      = InfoLineType.build(InfoLineLookingAtEffects::new,  InfoToggle.LOOKING_AT_EFFECTS);
    public static final InfoLineType<InfoLineLookingAtEntity>   LOOKING_AT_ENTITY       = InfoLineType.build(InfoLineLookingAtEntity::new,   InfoToggle.LOOKING_AT_ENTITY);
    public static final InfoLineType<InfoLineEntityVariant>     ENTITY_VARIANT          = InfoLineType.build(InfoLineEntityVariant::new,     InfoToggle.ENTITY_VARIANT);
}
