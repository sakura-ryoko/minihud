package fi.dy.masa.minihud.util;

import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.data.DebugDataManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class DebugInfoUtils
{
    public static void toggleDebugRenderer(IConfigBoolean config)
    {
        if (config == RendererToggle.DEBUG_CHUNK_BORDER)
        {
			boolean enabled = DebugDataManager.getInstance().isDebugAlwaysEnabled(DebugScreenEntries.CHUNK_BORDERS);

            if (enabled != RendererToggle.DEBUG_CHUNK_BORDER.getBooleanValue())
            {
                enabled = DebugDataManager.getInstance().toggleDebugAlwaysEnabled(DebugScreenEntries.CHUNK_BORDERS);
                debugWarn(enabled ? "debug.chunk_boundaries.on" : "debug.chunk_boundaries.off");
            }
        }
        else if (config == RendererToggle.DEBUG_CHUNK_SECTION_OCTREEE)
        {
			toggleDebugHud(DebugScreenEntries.CHUNK_SECTION_OCTREE, config, true);
        }
        else if (config == RendererToggle.DEBUG_ENTITY_HITBOXES)
        {
	        toggleDebugHud(DebugScreenEntries.ENTITY_HITBOXES, config, true);
        }
        else if (config == RendererToggle.DEBUG_BLOCK_OUTLINE)
        {
	        toggleDebugHud(DebugScreenEntries.VISUALIZE_SOLID_FACES, config, true);
        }
        else if (config == RendererToggle.DEBUG_WATER)
        {
	        toggleDebugHud(DebugScreenEntries.VISUALIZE_WATER_LEVELS, config, true);
        }
        else if (config == RendererToggle.DEBUG_HEIGHTMAP)
        {
	        toggleDebugHud(DebugScreenEntries.VISUALIZE_HEIGHTMAP, config, true);
        }
        else if (config == RendererToggle.DEBUG_COLLISION_BOXES)
        {
	        toggleDebugHud(DebugScreenEntries.VISUALIZE_COLLISION_BOXES, config, true);
        }
        else if (config == RendererToggle.DEBUG_SUPPORTING_BLOCK)
        {
	        toggleDebugHud(DebugScreenEntries.VISUALIZE_ENTITY_SUPPORTING_BLOCKS, config, true);
        }
        else if (config == RendererToggle.DEBUG_BLOCK_LIGHT)
        {
	        toggleDebugHud(DebugScreenEntries.VISUALIZE_BLOCK_LIGHT_LEVELS, config, true);
        }
        else if (config == RendererToggle.DEBUG_SKY_LIGHT)
        {
	        toggleDebugHud(DebugScreenEntries.VISUALIZE_SKY_LIGHT_LEVELS, config, true);
        }
        else if (config == RendererToggle.DEBUG_CHUNK_LOADING)
        {
	        toggleDebugHud(DebugScreenEntries.VISUALIZE_CHUNKS_ON_SERVER, config, true);
        }
        else if (config == RendererToggle.DEBUG_SKYLIGHT_SECTIONS)
        {
	        toggleDebugHud(DebugScreenEntries.VISUALIZE_SKY_LIGHT_SECTIONS, config, true);
        }
        else if (config == RendererToggle.DEBUG_CHUNK_SECTION_PATHS)
        {
	        toggleDebugHud(DebugScreenEntries.CHUNK_SECTION_PATHS, config, true);
        }
        else if (config == RendererToggle.DEBUG_CHUNK_SECTION_VISIBILITY)
        {
	        toggleDebugHud(DebugScreenEntries.CHUNK_SECTION_VISIBILITY, config, true);
        }
    }

	private static void toggleDebugHud(Identifier type, IConfigBoolean config, boolean feedback)
	{
		boolean enabled = DebugDataManager.getInstance().isDebugAlwaysEnabled(type);

		if (enabled != config.getBooleanValue())
		{
			enabled = DebugDataManager.getInstance().toggleDebugAlwaysEnabled(type);
		}

		if (feedback)
		{
			MiniHUD.LOGGER.warn("Toggled Vanilla '{}' Debug Renderer [{}].", type.toString(), enabled);
		}
	}

	public static void toggleDebugDataConfig(IConfigBoolean config)
	{
		DebugRenderType type = DebugRenderType.fromCallbackStatic(config);

		if (type != null)
		{
			DebugDataManager.getInstance().onConfigSync();
			DebugDataManager.getInstance().updateMetadata();
		}
	}

    private static void debugWarn(String key, Object... args)
    {
        Minecraft.getInstance().gui
		        .getChat()
		        .addMessage(Component.empty()
		                        .append(Component.translatable("debug.prefix").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
		                        .append(" ")
		                        .append(Component.translatable(key, args)
		                        )
		        );
    }

	/**
	 * Keep the debug Hud status configs in sync with Vanilla.
	 */
	public static void onUpdateVisibleEntries(DebugScreenEntryList inst)
	{
		if (inst != null)
		{
			RendererToggle.DEBUG_CHUNK_BORDER.setBooleanValueNoCallback(inst.isCurrentlyEnabled(DebugScreenEntries.CHUNK_BORDERS));
			RendererToggle.DEBUG_ENTITY_HITBOXES.setBooleanValueNoCallback(inst.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES));
			RendererToggle.DEBUG_BLOCK_OUTLINE.setBooleanValueNoCallback(inst.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_SOLID_FACES));
			RendererToggle.DEBUG_WATER.setBooleanValueNoCallback(inst.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_WATER_LEVELS));
			RendererToggle.DEBUG_HEIGHTMAP.setBooleanValueNoCallback(inst.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_HEIGHTMAP));
			RendererToggle.DEBUG_COLLISION_BOXES.setBooleanValueNoCallback(inst.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_COLLISION_BOXES));
			RendererToggle.DEBUG_SUPPORTING_BLOCK.setBooleanValueNoCallback(inst.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_ENTITY_SUPPORTING_BLOCKS));
			RendererToggle.DEBUG_BLOCK_LIGHT.setBooleanValueNoCallback(inst.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_BLOCK_LIGHT_LEVELS));
			RendererToggle.DEBUG_SKY_LIGHT.setBooleanValueNoCallback(inst.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_SKY_LIGHT_LEVELS));
			RendererToggle.DEBUG_CHUNK_LOADING.setBooleanValueNoCallback(inst.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_CHUNKS_ON_SERVER));
			RendererToggle.DEBUG_SKYLIGHT_SECTIONS.setBooleanValueNoCallback(inst.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_SKY_LIGHT_SECTIONS));
			RendererToggle.DEBUG_CHUNK_SECTION_OCTREEE.setBooleanValueNoCallback(inst.isCurrentlyEnabled(DebugScreenEntries.CHUNK_SECTION_OCTREE));
			RendererToggle.DEBUG_CHUNK_SECTION_PATHS.setBooleanValueNoCallback(inst.isCurrentlyEnabled(DebugScreenEntries.CHUNK_SECTION_PATHS));
			RendererToggle.DEBUG_CHUNK_SECTION_VISIBILITY.setBooleanValueNoCallback(inst.isCurrentlyEnabled(DebugScreenEntries.CHUNK_SECTION_VISIBILITY));
		}
	}
}
