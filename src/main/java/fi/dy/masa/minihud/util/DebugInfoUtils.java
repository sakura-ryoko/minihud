package fi.dy.masa.minihud.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.debug.DebugHudEntries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.data.DebugDataManager;

public class DebugInfoUtils
{
    public static void toggleDebugRenderer(IConfigBoolean config)
    {
        if (config == RendererToggle.DEBUG_CHUNK_BORDER)
        {
			boolean enabled = DebugDataManager.getInstance().isDebugAlwaysEnabled(DebugHudEntries.CHUNK_BORDERS);

            if (enabled != RendererToggle.DEBUG_CHUNK_BORDER.getBooleanValue())
            {
                enabled = DebugDataManager.getInstance().toggleDebugAlwaysEnabled(DebugHudEntries.CHUNK_BORDERS);
                debugWarn(enabled ? "debug.chunk_boundaries.on" : "debug.chunk_boundaries.off");
            }
        }
        else if (config == RendererToggle.DEBUG_OCTREEE)
        {
			toggleDebugHud(DebugHudEntries.CHUNK_SECTION_OCTREE, config, true);
        }
        else if (config == RendererToggle.DEBUG_WATER)
        {
	        toggleDebugHud(DebugHudEntries.VISUALIZE_WATER_LEVELS, config, true);
        }
        else if (config == RendererToggle.DEBUG_HEIGHTMAP)
        {
	        toggleDebugHud(DebugHudEntries.VISUALIZE_HEIGHTMAP, config, true);
        }
        else if (config == RendererToggle.DEBUG_COLLISION_BOXES)
        {
	        toggleDebugHud(DebugHudEntries.VISUALIZE_COLLISION_BOXES, config, true);
        }
        else if (config == RendererToggle.DEBUG_SUPPORTING_BLOCK)
        {
	        toggleDebugHud(DebugHudEntries.VISUALIZE_ENTITY_SUPPORTING_BLOCKS, config, true);
        }
        else if (config == RendererToggle.DEBUG_BLOCK_LIGHT)
        {
	        toggleDebugHud(DebugHudEntries.VISUALIZE_BLOCK_LIGHT_LEVELS, config, true);
        }
        else if (config == RendererToggle.DEBUG_SKY_LIGHT)
        {
	        toggleDebugHud(DebugHudEntries.VISUALIZE_SKY_LIGHT_LEVELS, config, true);
        }
        else if (config == RendererToggle.DEBUG_BLOCK_OUTLINE)
        {
	        toggleDebugHud(DebugHudEntries.VISUALIZE_SOLID_FACES, config, true);
        }
        else if (config == RendererToggle.DEBUG_CHUNK_LOADING)
        {
	        toggleDebugHud(DebugHudEntries.VISUALIZE_CHUNKS_ON_SERVER, config, true);
        }
        else if (config == RendererToggle.DEBUG_SKYLIGHT_SECTIONS)
        {
	        toggleDebugHud(DebugHudEntries.VISUALIZE_SKY_LIGHT_SECTIONS, config, true);
        }
        else if (config == RendererToggle.DEBUG_ENTITY_HITBOXES)
        {
	        toggleDebugHud(DebugHudEntries.ENTITY_HITBOXES, config, true);
        }
        else if (config == RendererToggle.DEBUG_CHUNK_INFO)
        {
	        toggleDebugHud(DebugHudEntries.CHUNK_SECTION_PATHS, config, true);
        }
        else if (config == RendererToggle.DEBUG_CHUNK_OCCLUSION)
        {
	        toggleDebugHud(DebugHudEntries.CHUNK_SECTION_VISIBILITY, config, true);
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
			if (DebugDataManager.getInstance().isDebugRendererEnabled(type) != config.getBooleanValue())
			{
				DebugDataManager.getInstance().setDebugRenderer(type, config.getBooleanValue());
				DebugDataManager.getInstance().updateMetadata();
			}
		}
	}

    private static void debugWarn(String key, Object... args)
    {
        MinecraftClient.getInstance().inGameHud
		        .getChatHud()
		        .addMessage(Text.empty()
		                        .append(Text.translatable("debug.prefix").formatted(Formatting.YELLOW, Formatting.BOLD))
		                        .append(" ")
		                        .append(Text.translatable(key, args)
		                        )
		        );
    }
}
