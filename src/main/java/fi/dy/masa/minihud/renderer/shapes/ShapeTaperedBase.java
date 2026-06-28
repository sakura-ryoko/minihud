package fi.dy.masa.minihud.renderer.shapes;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.data.json.JsonUtils;
import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.malilib.util.position.Vec3d;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.renderer.RenderObjectVbo;
import fi.dy.masa.minihud.renderer.RenderUtils;

public abstract class ShapeTaperedBase extends ShapeBase
{
	private static final int DEFAULT_MAX_RADIUS = 1024;
	private static final int DEFAULT_MAX_HEIGHT = 384;      // World Height max

	private int maxRadius = DEFAULT_MAX_RADIUS;
	private int maxHeight = DEFAULT_MAX_HEIGHT;
	protected BlockPos origin;
	protected int bottomRadius;
	protected int topRadius;
	protected int height;
	protected Direction direction;
	private boolean hasData = false;

	public ShapeTaperedBase(ShapeType type, Color4f color)
	{
		super(type, color);
		this.origin = BlockPos.ZERO;
		this.bottomRadius = 8;
		this.topRadius = 0;
		this.height = 16;
		this.direction = Direction.UP;
		this.useCulling = false;
	}

	public BlockPos getOrigin()
	{
		return this.origin;
	}

	public int getBottomRadius()
	{
		return this.bottomRadius;
	}

	public int getTopRadius()
	{
		return this.topRadius;
	}

	public int getHeight()
	{
		return this.height;
	}

	public Direction getDirection()
	{
		return this.direction;
	}

	public void setBottomRadius(int bottomRadius)
	{
		if (bottomRadius >= 0 && bottomRadius <= this.maxRadius)
		{
			this.bottomRadius = bottomRadius;
			this.setNeedsUpdate();
		}
	}

	public void setTopRadius(int topRadius)
	{
		if (topRadius >= 0 && topRadius <= this.maxRadius)
		{
			this.topRadius = topRadius;
			this.setNeedsUpdate();
		}
	}

	public void setHeight(int height)
	{
		if (height >= 0 && height <= this.maxHeight)
		{
			this.height = height;
			this.setNeedsUpdate();
		}
	}

	public void setDirection(Direction direction)
	{
		if (direction != null && this.direction != direction)
		{
			this.direction = direction;
			this.setNeedsUpdate();
		}
	}

	public void setOrigin(BlockPos origin)
	{
		this.origin = origin;
		this.setNeedsUpdate();
	}

	@Override
	public void onShapeInit()
	{
		super.onShapeInit();
		Entity cameraEntity = EntityUtils.getCameraEntity();

		if (cameraEntity != null && this.origin == BlockPos.ZERO)
		{
			this.moveToPosition(Vec3d.of(cameraEntity.position()));
		}
	}

	@Override
	public void moveToPosition(Vec3d pos)
	{
		this.setOrigin(BlockPos.containing(pos.x, pos.y, pos.z));

		InfoUtils.printActionbarMessage(
				String.format("Moved shape to %d %d %d", this.origin.getX(), this.origin.getY(), this.origin.getZ())
		);
	}

	protected abstract boolean isInsideLayer(int u, int v, double currentBoundary);

	/**
	 * Translates the generic slicing loop (u, v, h) into actual world coordinates
	 * so the shape can point in any direction.
	 */
	protected BlockPos remapToBlockPos(BlockPos start, int u, int v, int h, Direction dir)
	{
		return switch (dir)
		{
			case UP    -> start.offset(u, h, v);
			case DOWN  -> start.offset(u, -h, v);
			case NORTH -> start.offset(u, v, -h);
			case SOUTH -> start.offset(u, v, h);
			case WEST  -> start.offset(-h, u, v);
			case EAST  -> start.offset(h, u, v);
		};
	}

	protected List<BlockPos> generateShapeBlocks()
	{
		List<BlockPos> blocks = new ArrayList<>();
		int minY = this.mc.level != null ? this.mc.level.getMinY() : -64;
		int maxY = this.mc.level != null ? this.mc.level.getMaxY() : 320;
		int safeHeight = Math.max(1, this.height);

		for (int h = 0; h <= safeHeight; h++)
		{
			double progress = (double) h / safeHeight;
			double currentBoundary = this.bottomRadius - ((this.bottomRadius - this.topRadius) * progress);
			int bound = (int) Math.ceil(currentBoundary);

			for (int u = -bound; u <= bound; u++)
			{
				for (int v = -bound; v <= bound; v++)
				{
					if (this.isInsideLayer(u, v, currentBoundary))
					{
						BlockPos pos = this.remapToBlockPos(this.origin, u, v, h, this.direction);

						if (pos.getY() < minY || pos.getY() >= maxY)
						{
							continue;
						}

						blocks.add(pos);
					}
				}
			}
		}

		return blocks;
	}

	@Override
	public void update(Vec3d cameraPos, Entity entity, Minecraft mc, ProfilerFiller profiler)
	{
		this.hasData = true;
		this.render(cameraPos, mc, profiler);
		this.needsUpdate = false;
	}

	@Override
	public boolean hasData()
	{
		return this.hasData;
	}

	@Override
	public void render(Vec3d cameraPos, Minecraft mc, ProfilerFiller profiler)
	{
		this.allocateBuffers(this.renderLines);
		this.renderQuads(cameraPos, mc, profiler);

		if (this.renderLines)
		{
			this.renderOutlines(cameraPos, mc, profiler);
		}
	}

	private void renderQuads(Vec3d cameraPos, Minecraft mc, ProfilerFiller profiler)
	{
		if (mc.level == null || mc.player == null) return;

		String shapeName = this.type.name().toLowerCase();
		profiler.push(shapeName + "_quads");
		RenderObjectVbo ctx = this.renderObjects.getFirst();
		BufferBuilder builder = ctx.start(() -> Reference.MOD_ID+":"+shapeName+"/quads", this.renderThroughShape ? MaLiLibPipelines.MINIHUD_SHAPE_NO_DEPTH_OFFSET : MaLiLibPipelines.MINIHUD_SHAPE_OFFSET_NO_CULL, 0);

		if (this.shouldRenderCenterBlock())
		{
			fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxSidesBatchedQuads(this.origin, cameraPos.toVanilla(), this.color, 0.001, builder);
		}

		this.renderShapeGeometry(cameraPos, builder, false);

		try
		{
			MeshData meshData = builder.build();

			if (meshData != null)
			{
				ctx.upload(meshData, this.shouldResort);
				if (this.shouldResort)
				{
					ctx.startResorting(meshData, ctx.createVertexSorter(cameraPos.toVanilla()));
				}
				meshData.close();
			}
		}
		catch (Exception err)
		{
			MiniHUD.LOGGER.error("ShapeTaperedBase#renderQuads(): Exception; {}", err.getMessage());
		}

		profiler.pop();
	}

	private void renderOutlines(Vec3d cameraPos, Minecraft mc, ProfilerFiller profiler)
	{
		if (mc.level == null || mc.player == null || !this.renderLines) return;

		String shapeName = this.type.name().toLowerCase();
		profiler.push(shapeName + "_outlines");
		RenderObjectVbo ctx = this.renderObjects.get(1);
		BufferBuilder builder = ctx.start(() -> Reference.MOD_ID+":"+shapeName+"/outlines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH, 0);

		if (this.shouldRenderCenterBlock())
		{
			fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(this.origin, cameraPos.toVanilla(), this.colorLines, 0.001, this.glLineWidth, builder);
		}

		this.renderShapeGeometry(cameraPos, builder, true);

		try
		{
			MeshData meshData = builder.build();
			if (meshData != null)
			{
				ctx.upload(meshData, false);
				meshData.close();
			}
		}
		catch (Exception err)
		{
			MiniHUD.LOGGER.error("ShapeTaperedBase#renderOutlines(): Exception; {}", err.getMessage());
		}

		profiler.pop();
	}

	private void renderShapeGeometry(Vec3d cameraPos, BufferBuilder builder, boolean isOutline)
	{
		List<BlockPos> blocks = this.generateShapeBlocks();
		LongOpenHashSet positions = new LongOpenHashSet(blocks.size());

		for (BlockPos pos : blocks)
		{
			positions.add(pos.asLong());
		}

		fi.dy.masa.minihud.util.shape.SphereUtils.RingPositionTest dummyTest = (adjX, adjY, adjZ, side) -> false;
		double expand = 0;

		if (isOutline)
		{
			RenderUtils.renderCircleBlockOutlines(positions, PositionUtils.ALL_DIRECTIONS, dummyTest, this.renderType, this.layerRange, this.colorLines, expand, cameraPos, this.glLineWidth, builder);
		}
		else
		{
			RenderUtils.renderCircleBlockPositions(positions, PositionUtils.ALL_DIRECTIONS, dummyTest, this.renderType, this.layerRange, this.color, expand, cameraPos, builder);
		}
	}

	@Override
	public List<String> getWidgetHoverLines()
	{
		List<String> lines = super.getWidgetHoverLines();

		lines.add(StringUtils.translate("minihud.gui.hover.shape.origin_value", this.origin.getX(), this.origin.getY(), this.origin.getZ()));
		lines.add(StringUtils.translate("minihud.gui.hover.shape.bottom_radius_value", this.bottomRadius));
		lines.add(StringUtils.translate("minihud.gui.hover.shape.top_radius_value", this.topRadius));
		lines.add(StringUtils.translate("minihud.gui.hover.shape.height_value", this.height));
		lines.add(StringUtils.translate("minihud.gui.hover.shape.direction_value", this.direction.name().toUpperCase()));

		return lines;
	}

	@Override
	public JsonObject toJson()
	{
		JsonObject obj = super.toJson();

		if (this.maxRadius != DEFAULT_MAX_RADIUS)
		{
			obj.add("max_radius", new JsonPrimitive(this.maxRadius));
		}

		if (this.maxHeight != DEFAULT_MAX_HEIGHT)
		{
			obj.add("max_height", new JsonPrimitive(this.maxHeight));
		}

		obj.add("origin_x", new JsonPrimitive(this.origin.getX()));
		obj.add("origin_y", new JsonPrimitive(this.origin.getY()));
		obj.add("origin_z", new JsonPrimitive(this.origin.getZ()));
		obj.add("bottom_radius", new JsonPrimitive(this.bottomRadius));
		obj.add("top_radius", new JsonPrimitive(this.topRadius));
		obj.add("height", new JsonPrimitive(this.height));
		obj.add("direction", new JsonPrimitive(this.direction.getName()));

		return obj;
	}

	@Override
	public void fromJson(JsonObject obj)
	{
		super.fromJson(obj);

		if (JsonUtils.hasInteger(obj, "origin_x") && JsonUtils.hasInteger(obj, "origin_y") && JsonUtils.hasInteger(obj, "origin_z"))
		{
			this.origin = new BlockPos(
					JsonUtils.getInteger(obj, "origin_x"),
					JsonUtils.getInteger(obj, "origin_y"),
					JsonUtils.getInteger(obj, "origin_z")
			);
		}

		if (JsonUtils.hasInteger(obj, "max_radius"))
		{
			int parsedMax = JsonUtils.getIntegerOrDefault(obj, "max_radius", DEFAULT_MAX_RADIUS);

			if (parsedMax > 0)
			{
				this.maxRadius = parsedMax;
			}
		}

		if (JsonUtils.hasInteger(obj, "max_height"))
		{
			int parsedMax = JsonUtils.getIntegerOrDefault(obj, "max_height", DEFAULT_MAX_HEIGHT);

			if (parsedMax > 0)
			{
				this.maxHeight = parsedMax;
			}
		}

		if (JsonUtils.hasInteger(obj, "bottom_radius"))
		{
			this.bottomRadius = JsonUtils.getInteger(obj, "bottom_radius");
		}

		if (JsonUtils.hasInteger(obj, "top_radius"))
		{
			this.topRadius = JsonUtils.getInteger(obj, "top_radius");
		}

		if (JsonUtils.hasInteger(obj, "height"))
		{
			this.height = JsonUtils.getInteger(obj, "height");
		}

		if (JsonUtils.hasString(obj, "direction"))
		{
			String result = JsonUtils.getString(obj, "direction");

			if (result == null || result.isEmpty())
			{
				this.direction = Direction.UP;
			}
			else
			{
				this.direction = Direction.byName(result);
			}
		}
	}

	@Override
	public void reset()
	{
		super.reset();
		this.hasData = false;
	}
}
