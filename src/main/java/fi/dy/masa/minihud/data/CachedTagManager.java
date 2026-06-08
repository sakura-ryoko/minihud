package fi.dy.masa.minihud.data;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;

import fi.dy.masa.malilib.data.CachedBlockTags;
import fi.dy.masa.malilib.data.CachedTagKey;
import fi.dy.masa.malilib.data.CachedTagUtils;
import fi.dy.masa.minihud.Reference;

public class CachedTagManager
{
	public static final CachedTagKey LIGHTNING_RODS_KEY     = new CachedTagKey(Reference.MOD_ID, "lightning_rods");

	public static List<CachedTagKey> getKeys()
	{
		List<CachedTagKey> list = new ArrayList<>();

		list.add(LIGHTNING_RODS_KEY);

		return list;
	}

	public static void startCache()
	{
		clearCache();

		CachedBlockTags.getInstance().build(LIGHTNING_RODS_KEY, buildLightningRodBlockCache());
	}

	private static void clearCache()
	{
		CachedBlockTags.getInstance().clearEntry(LIGHTNING_RODS_KEY);
	}

	private static List<String> buildLightningRodBlockCache()
	{
		List<String> list = new ArrayList<>();

		list.add("#"+ BlockTags.LIGHTNING_RODS.location().toString());

		return list;
	}

	public static boolean isLightningRod(Block block)
	{
		return CachedTagUtils.matchBlockTag(LIGHTNING_RODS_KEY, block);
	}
}
