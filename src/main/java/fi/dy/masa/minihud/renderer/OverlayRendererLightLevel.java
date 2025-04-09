package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;

import com.mojang.blaze3d.buffers.BufferUsage;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;

import fi.dy.masa.malilib.config.IConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.util.LightLevelMarkerMode;
import fi.dy.masa.minihud.util.LightLevelNumberMode;
import fi.dy.masa.minihud.util.LightLevelRenderCondition;

public class OverlayRendererLightLevel extends OverlayRendererBase
{
    public static final OverlayRendererLightLevel INSTANCE = new OverlayRendererLightLevel();
    private static final Identifier TEXTURE_NUMBERS = Identifier.of(Reference.MOD_ID, "textures/misc/light_level_numbers.png");

    private final List<LightLevelInfo> lightInfos;
    private BlockPos.Mutable mutablePos;
    private Direction lastDirection;

    private boolean tagsBroken;
    private boolean needsUpdate;
    private boolean hasData;

    protected OverlayRendererLightLevel()
    {
        this.lightInfos = new ArrayList<>();
        this.mutablePos = new BlockPos.Mutable();
        this.lastDirection = Direction.NORTH;
        this.hasData = false;
    }

    @Override
    public String getName()
    {
        return "LightLevel";
    }

    public void setNeedsUpdate()
    {
        this.needsUpdate = true;
        // Clean buffers when receiving the RenderCallback.
        this.clearBuffers();
    }

    @Override
    public boolean shouldRender(MinecraftClient mc)
    {
        return RendererToggle.OVERLAY_LIGHT_LEVEL.getBooleanValue();
    }

    @Override
    public boolean needsUpdate(Entity entity, MinecraftClient mc)
    {
        return this.needsUpdate || this.lastUpdatePos == null ||
                Math.abs(entity.getX() - this.lastUpdatePos.getX()) > 4 ||
                Math.abs(entity.getY() - this.lastUpdatePos.getY()) > 4 ||
                Math.abs(entity.getZ() - this.lastUpdatePos.getZ()) > 4 ||
                (Configs.Generic.LIGHT_LEVEL_NUMBER_ROTATION.getBooleanValue() && this.lastDirection != entity.getHorizontalFacing());
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null)
        {
            this.needsUpdate = false;
            return;
        }

//        long pre = System.nanoTime();
        BlockPos pos = PositionUtils.getEntityBlockPos(entity);
        this.hasData = this.updateLightLevels(mc.world, pos);
        this.renderThrough = Configs.Generic.LIGHT_LEVEL_RENDER_THROUGH.getBooleanValue();

        if (this.hasData())
        {
            this.render(cameraPos, mc, profiler);
        }

//        System.out.printf("LL markers: %d, time: %.3f s\n", this.lightInfos.size(), (double) (System.nanoTime() - pre) / 1000000000D);

        this.lastUpdatePos = pos;
        this.lastDirection = entity.getHorizontalFacing();
        this.needsUpdate = false;
    }

    @Override
    public boolean hasData()
    {
        return this.hasData && !this.lightInfos.isEmpty();
    }

    @Override
    public void allocateBuffers()
    {
        // Don't reallocate it unless empty; using start() calls reset() anyways.
        if (this.renderObjects.isEmpty())
        {
            this.renderObjects.add(new RenderObjectVbo(() -> this.getName() + " Quads", MaLiLibPipelines.POSITION_TEX_COLOR_MASA_NO_DEPTH_NO_CULL, BufferUsage.STATIC_WRITE));
            this.renderObjects.add(new RenderObjectVbo(() -> this.getName() + " Lines", MaLiLibPipelines.LINES_MASA_SIMPLE, BufferUsage.STATIC_WRITE));
        }
    }

    @Override
    public void render(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        this.allocateBuffers();
        this.renderTexQuads(cameraPos, mc, profiler);
        this.renderOutlines(cameraPos, mc, profiler);
    }

    private void renderTexQuads(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        profiler.push("light_level_quads");
        int safeThreshold = Configs.Generic.LIGHT_LEVEL_THRESHOLD_SAFE.getIntegerValue();
        int dimThreshold = Configs.Generic.LIGHT_LEVEL_THRESHOLD_DIM.getIntegerValue();
        Direction numberFacing = Configs.Generic.LIGHT_LEVEL_NUMBER_ROTATION.getBooleanValue() ? mc.player.getHorizontalFacing() : Direction.NORTH;
        boolean useColoredNumbers = Configs.Generic.LIGHT_LEVEL_COLORED_NUMBERS.getBooleanValue();
        LightLevelNumberMode numberMode = (LightLevelNumberMode) Configs.Generic.LIGHT_LEVEL_NUMBER_MODE.getOptionListValue();

        // this.renderThrough ? MaLiLibPipelines.POSITION_TEX_COLOR_SIMPLE : MaLiLibPipelines.POSITION_TEX_COLOR_LESSER_DEPTH
        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "Light Level Quads", this.renderThrough ? MaLiLibPipelines.POSITION_TEX_COLOR_MASA_NO_DEPTH_NO_CULL : MaLiLibPipelines.POSITION_TEX_COLOR_MASA_LESSER_DEPTH, BufferUsage.STATIC_WRITE);
        MatrixStack matrices = new MatrixStack();

        try
        {
            ctx.bindTexture(TEXTURE_NUMBERS, 0, 256, 256);
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("bindTexture Exception: {}", err.getMessage());
            return;
        }

        matrices.push();
//        fi.dy.masa.malilib.render.RenderUtils.bindTexture(TEXTURE_NUMBERS);

        MatrixStack.Entry e = matrices.peek();

        if (numberMode == LightLevelNumberMode.BLOCK || numberMode == LightLevelNumberMode.BOTH)
        {
            this.renderNumbers(cameraPos, LightLevelNumberMode.BLOCK,
                               Configs.Generic.LIGHT_LEVEL_NUMBER_OFFSET_BLOCK_X,
                               Configs.Generic.LIGHT_LEVEL_NUMBER_OFFSET_BLOCK_Y,
                               Configs.Colors.LIGHT_LEVEL_NUMBER_BLOCK_LIT,
                               Configs.Colors.LIGHT_LEVEL_NUMBER_BLOCK_DIM,
                               Configs.Colors.LIGHT_LEVEL_NUMBER_BLOCK_DARK,
                               useColoredNumbers, safeThreshold, dimThreshold, numberFacing, builder, e);
        }

        if (numberMode == LightLevelNumberMode.SKY || numberMode == LightLevelNumberMode.BOTH)
        {
            this.renderNumbers(cameraPos, LightLevelNumberMode.SKY,
                               Configs.Generic.LIGHT_LEVEL_NUMBER_OFFSET_SKY_X,
                               Configs.Generic.LIGHT_LEVEL_NUMBER_OFFSET_SKY_Y,
                               Configs.Colors.LIGHT_LEVEL_NUMBER_SKY_LIT,
                               Configs.Colors.LIGHT_LEVEL_NUMBER_SKY_DIM,
                               Configs.Colors.LIGHT_LEVEL_NUMBER_SKY_DARK,
                               useColoredNumbers, safeThreshold, dimThreshold, numberFacing, builder, e);
        }

        try
        {
            BuiltBuffer meshData = builder.endNullable();

            if (meshData != null)
            {
                ctx.upload(meshData, false);
                meshData.close();
            }
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererLightLevel#renderQuads(): Exception; {}", err.getMessage());
        }

        matrices.pop();
        profiler.pop();
    }

    private void renderOutlines(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        profiler.push("light_level_outlines");
        int safeThreshold = Configs.Generic.LIGHT_LEVEL_THRESHOLD_SAFE.getIntegerValue();
        int dimThreshold = Configs.Generic.LIGHT_LEVEL_THRESHOLD_DIM.getIntegerValue();
        LightLevelMarkerMode markerMode = (LightLevelMarkerMode) Configs.Generic.LIGHT_LEVEL_MARKER_MODE.getOptionListValue();

        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(() -> "Light Level Lines", RenderPipelines.LINES, BufferUsage.STATIC_WRITE);
        MatrixStack matrices = new MatrixStack();

        matrices.push();
        MatrixStack.Entry e = matrices.peek();

        if (markerMode == LightLevelMarkerMode.SQUARE)
        {
            this.renderMarkers(this::renderLightLevelSquare, cameraPos, safeThreshold, dimThreshold, builder, e);
        }
        else if (markerMode == LightLevelMarkerMode.CROSS)
        {
            this.renderMarkers(this::renderLightLevelCross, cameraPos, safeThreshold, dimThreshold, builder, e);
        }

        try
        {
            BuiltBuffer meshData = builder.endNullable();

            if (meshData != null)
            {
                ctx.upload(meshData, false);
                meshData.close();
            }
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererLightLevel#renderOutlines(): Exception; {}", err.getMessage());
        }

        matrices.pop();
        profiler.pop();
    }

    @Override
    public void reset()
    {
        super.reset();
        this.tagsBroken = false;
        this.lightInfos.clear();
        this.mutablePos = new BlockPos.Mutable();
        this.lastDirection = Direction.NORTH;
        this.hasData = false;
    }

    private void renderNumbers(Vec3d cameraPos,
                               LightLevelNumberMode mode,
                               IConfigDouble cfgOffX,
                               IConfigDouble cfgOffZ,
                               ConfigColor cfgColorLit,
                               ConfigColor cfgColorDim,
                               ConfigColor cfgColorDark,
                               boolean useColoredNumbers,
                               int safeThreshold,
                               int dimThreshold,
                               Direction numberFacing,
                               BufferBuilder buffer,
                               MatrixStack.Entry e)
    {
        double ox = cfgOffX.getDoubleValue();
        double oz = cfgOffZ.getDoubleValue();
        double tmpX, tmpZ;
        double offsetY = Configs.Generic.LIGHT_LEVEL_RENDER_OFFSET.getDoubleValue();
        Color4f colorLit, colorDim, colorDark;

        switch (numberFacing)
        {
            case NORTH: tmpX = -ox; tmpZ = -oz; break;
            case SOUTH: tmpX =  ox; tmpZ =  oz; break;
            case WEST:  tmpX = -oz; tmpZ =  ox; break;
            case EAST:  tmpX =  oz; tmpZ = -ox; break;
            default:    tmpX = -ox; tmpZ = -oz; break;
        }

        if (useColoredNumbers)
        {
            colorLit = cfgColorLit.getColor();
            colorDim = cfgColorDim.getColor();
            colorDark = cfgColorDark.getColor();
        }
        else
        {
            colorLit = Color4f.fromColor(0xFFFFFFFF);
            colorDim = colorLit;
            colorDark = colorLit;
        }

        this.renderLightLevelNumbers(tmpX + cameraPos.x, cameraPos.y - offsetY, tmpZ + cameraPos.z, numberFacing,
                                     safeThreshold, dimThreshold, mode, colorLit, colorDim, colorDark, buffer, e);
    }

    private void renderMarkers(IMarkerRenderer renderer,
                               Vec3d cameraPos,
                               int safeThreshold,
                               int dimThreshold,
                               BufferBuilder buffer,
                               MatrixStack.Entry e)
    {
        Color4f colorBlockLit = Configs.Colors.LIGHT_LEVEL_MARKER_BLOCK_LIT.getColor();
        Color4f colorDim = Configs.Colors.LIGHT_LEVEL_MARKER_DIM.getColor();
        Color4f colorSkyLit = Configs.Colors.LIGHT_LEVEL_MARKER_SKY_LIT.getColor();
        Color4f colorDark = Configs.Colors.LIGHT_LEVEL_MARKER_DARK.getColor();
        LightLevelRenderCondition condition = (LightLevelRenderCondition) Configs.Generic.LIGHT_LEVEL_MARKER_CONDITION.getOptionListValue();
        double markerSize = Configs.Generic.LIGHT_LEVEL_MARKER_SIZE.getDoubleValue();
        double offsetX = cameraPos.x;
        double offsetY = cameraPos.y - Configs.Generic.LIGHT_LEVEL_RENDER_OFFSET.getDoubleValue();
        double offsetZ = cameraPos.z;
        double offset1 = (1.0 - markerSize) / 2.0;
        double offset2 = (1.0 - offset1);
        boolean autoHeight = Configs.Generic.LIGHT_LEVEL_AUTO_HEIGHT.getBooleanValue();
        Color4f color;

        for (LightLevelInfo info : this.lightInfos)
        {
            if (condition.shouldRender(info.block, dimThreshold, safeThreshold))
            {
                long pos = info.pos;
                double x = BlockPos.unpackLongX(pos) - offsetX;
                double y = (autoHeight ? info.y : BlockPos.unpackLongY(pos)) - offsetY;
                double z = BlockPos.unpackLongZ(pos) - offsetZ;

                if (info.block < safeThreshold)
                {
                    color = info.sky >= safeThreshold ? colorSkyLit : colorDark;
                }
                else if (info.block > dimThreshold)
                {
                    color = colorBlockLit;
                }
                else
                {
                    color = colorDim;
                }

                renderer.render((float) x, (float) y, (float) z, color, (float) offset1, (float) offset2, buffer, e);
            }
        }
    }

    private void renderLightLevelNumbers(double dx, double dy, double dz,
                                         Direction facing,
                                         int safeThreshold,
                                         int dimThreshold,
                                         LightLevelNumberMode numberMode,
                                         Color4f colorLit,
                                         Color4f colorDim,
                                         Color4f colorDark,
                                         BufferBuilder buffer,
                                         MatrixStack.Entry e)
    {
        LightLevelRenderCondition condition = (LightLevelRenderCondition) Configs.Generic.LIGHT_LEVEL_NUMBER_CONDITION.getOptionListValue();
        boolean autoHeight = Configs.Generic.LIGHT_LEVEL_AUTO_HEIGHT.getBooleanValue();
        Color4f color;

        for (LightLevelInfo info : this.lightInfos)
        {
            if (condition.shouldRender(info.block, dimThreshold, safeThreshold))
            {
                long pos = info.pos;
                double x = BlockPos.unpackLongX(pos) - dx;
                double y = (autoHeight ? info.y : BlockPos.unpackLongY(pos)) - dy;
                double z = BlockPos.unpackLongZ(pos) - dz;
                int lightLevel = numberMode == LightLevelNumberMode.BLOCK ? info.block : info.sky;

                if (lightLevel < safeThreshold)
                {
                    color = colorDark;
                }
                else if (lightLevel > dimThreshold)
                {
                    color = colorLit;
                }
                else
                {
                    color = colorDim;
                }

                this.renderLightLevelTextureColor((float) x, (float) y, (float) z, facing, lightLevel, color, buffer, e);
            }
        }
    }

    private void renderLightLevelTextureColor(float x, float y, float z, Direction facing, int lightLevel, Color4f color, BufferBuilder buffer, MatrixStack.Entry e)
    {
        float w = 0.25f;
        float u = (lightLevel & 0x3) * w;
        float v = (lightLevel >> 2) * w;
        y += 0.005F;

        Matrix4f m = e.getPositionMatrix();

        switch (facing)
        {
            case NORTH:
                buffer.vertex(m, x, y, z).texture(u    , v    ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(m, x, y, z + 1).texture(u    , v + w).color(color.r, color.g, color.b, color.a);
                buffer.vertex(m, x + 1, y, z + 1).texture(u + w, v + w).color(color.r, color.g, color.b, color.a);
                buffer.vertex(m, x + 1, y, z).texture(u + w, v    ).color(color.r, color.g, color.b, color.a);
                break;

            case SOUTH:
                buffer.vertex(m, x + 1, y, z + 1).texture(u    , v    ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(m, x + 1, y, z    ).texture(u    , v + w).color(color.r, color.g, color.b, color.a);
                buffer.vertex(m, x    , y, z    ).texture(u + w, v + w).color(color.r, color.g, color.b, color.a);
                buffer.vertex(m, x    , y, z + 1).texture(u + w, v    ).color(color.r, color.g, color.b, color.a);
                break;

            case EAST:
                buffer.vertex(m, x + 1, y, z    ).texture(u    , v    ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(m, x    , y, z    ).texture(u    , v + w).color(color.r, color.g, color.b, color.a);
                buffer.vertex(m, x    , y, z + 1).texture(u + w, v + w).color(color.r, color.g, color.b, color.a);
                buffer.vertex(m, x + 1, y, z + 1).texture(u + w, v    ).color(color.r, color.g, color.b, color.a);
                break;

            case WEST:
                buffer.vertex(m, x    , y, z + 1).texture(u    , v    ).color(color.r, color.g, color.b, color.a);
                buffer.vertex(m, x + 1, y, z + 1).texture(u    , v + w).color(color.r, color.g, color.b, color.a);
                buffer.vertex(m, x + 1, y, z    ).texture(u + w, v + w).color(color.r, color.g, color.b, color.a);
                buffer.vertex(m, x    , y, z    ).texture(u + w, v    ).color(color.r, color.g, color.b, color.a);
                break;

            default:
        }
    }

    private void renderLightLevelCross(float x, float y, float z, Color4f color, float offset1, float offset2, BufferBuilder buffer, MatrixStack.Entry e)
    {
        y += 0.005F;

        buffer.vertex(e, x + offset1, y, z + offset1).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        buffer.vertex(e, x + offset2, y, z + offset2).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);

        buffer.vertex(e, x + offset1, y, z + offset2).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        buffer.vertex(e, x + offset2, y, z + offset1).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
    }

    private void renderLightLevelSquare(float x, float y, float z, Color4f color, float offset1, float offset2, BufferBuilder buffer, MatrixStack.Entry e)
    {
        y += 0.005F;

        buffer.vertex(e, x + offset1, y, z + offset1).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        buffer.vertex(e, x + offset1, y, z + offset2).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);

        buffer.vertex(e, x + offset1, y, z + offset2).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        buffer.vertex(e, x + offset2, y, z + offset2).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);

        buffer.vertex(e, x + offset2, y, z + offset2).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        buffer.vertex(e, x + offset2, y, z + offset1).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);

        buffer.vertex(e, x + offset2, y, z + offset1).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
        buffer.vertex(e, x + offset1, y, z + offset1).color(color.r, color.g, color.b, color.a).normal(e, 0.0f, 0.0f, 0.0f);
    }

    private boolean updateLightLevels(World world, BlockPos center)
    {
        this.lightInfos.clear();

        //System.out.printf("LL center %s\n", center.toShortString());

        int radius = Configs.Generic.LIGHT_LEVEL_RANGE.getIntegerValue();
        final int minX = center.getX() - radius;
        final int minY = center.getY() - radius;
        final int minZ = center.getZ() - radius;
        final int maxX = center.getX() + radius;
        final int maxY = center.getY() + radius;
        final int maxZ = center.getZ() + radius;
        final int minCX = (minX >> 4);
        final int minCZ = (minZ >> 4);
        final int maxCX = (maxX >> 4);
        final int maxCZ = (maxZ >> 4);
        LightingProvider lightingProvider = world.getChunkManager().getLightingProvider();
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        final int worldTopHeight = world.getTopYInclusive() + 1;
        final boolean collisionCheck = Configs.Generic.LIGHT_LEVEL_COLLISION_CHECK.getBooleanValue();
        final boolean underWater = Configs.Generic.LIGHT_LEVEL_UNDER_WATER.getBooleanValue();
        final boolean autoHeight = Configs.Generic.LIGHT_LEVEL_AUTO_HEIGHT.getBooleanValue();
        final boolean skipBlockCheck = Configs.Generic.LIGHT_LEVEL_SKIP_BLOCK_CHECK.getBooleanValue();

        for (int cx = minCX; cx <= maxCX; ++cx)
        {
            final int startX = Math.max( cx << 4      , minX);
            final int endX   = Math.min((cx << 4) + 15, maxX);

            for (int cz = minCZ; cz <= maxCZ; ++cz)
            {
                final int startZ = Math.max( cz << 4      , minZ);
                final int endZ   = Math.min((cz << 4) + 15, maxZ);
                WorldChunk chunk = world.getChunk(cx, cz);
                final int startY = Math.max(minY, world.getBottomY());
                final int endY = Math.min(maxY, WorldUtils.getHighestSectionYOffset(chunk) + 15 + 1);

                for (int y = startY; y <= endY; ++y)
                {
                    if (y > startY)
                    {
                        // If there are no blocks in the section below this layer, then we can skip it
                        ChunkSection section = chunk.getSection(chunk.getSectionIndex(y - 1));

                        if (section.isEmpty())
                        {
                            //y += 16 - (y & 0xF);
                            continue;
                        }
                    }

                    for (int x = startX; x <= endX; ++x)
                    {
                        for (int z = startZ; z <= endZ; ++z)
                        {
                            if (this.canSpawnAtWrapper(x, y, z, chunk, world, skipBlockCheck) == false)
                            {
                                continue;
                            }

                            mutablePos.set(x, y, z);
                            BlockState state = chunk.getBlockState(mutablePos);

                            if ((collisionCheck == false || state.getCollisionShape(chunk, mutablePos).isEmpty()) &&
                                (underWater || state.getFluidState().isEmpty()))
                            {
                                int block = y < worldTopHeight ? lightingProvider.get(LightType.BLOCK).getLightLevel(mutablePos) : 0;
                                int sky   = y < worldTopHeight ? lightingProvider.get(LightType.SKY).getLightLevel(mutablePos) : 15;
                                double topY = state.getOutlineShape(chunk, mutablePos).getMax(Direction.Axis.Y);

                                // Don't render the light level marker if it would be raised all the way to the next block space
                                if (autoHeight == false || topY < 1)
                                {
                                    float posY = topY >= 0 ? y + (float) topY : y;
                                    this.lightInfos.add(new LightLevelInfo(mutablePos.asLong(), posY, block, sky));
                                    //y += 2; // if the spot is spawnable, that means the next spawnable spot can be the third block up
                                }
                            }
                        }
                    }
                }
            }
        }

        return this.lightInfos.isEmpty() == false;
    }

    private boolean canSpawnAtWrapper(int x, int y, int z, Chunk chunk, World world, boolean skipBlockCheck)
    {
        try
        {
            return this.canSpawnAt(x, y, z, chunk, world, skipBlockCheck);
        }
        catch (Exception e)
        {
            InfoUtils.showGuiOrInGameMessage(Message.MessageType.WARNING, 8000, "This dimension seems to have missing block tag data, the light level will not use the normal block spawnability checks in this dimension. This is known to happen on some Waterfall/BungeeCord/ViaVersion/whatever setups that have an older MC version at the back end.");
            this.tagsBroken = true;

            return false;
        }
    }

    /**
     * This method mimics the one from WorldEntitySpawner, but takes in the Chunk to avoid that lookup
     */
    private boolean canSpawnAt(int x, int y, int z, Chunk chunk, World world, boolean skipBlockCheck)
    {
        this.mutablePos.set(x, y - 1, z);
        BlockState stateDown = chunk.getBlockState(this.mutablePos);

        if ((skipBlockCheck && stateDown.isAir() == false && (stateDown.getBlock() instanceof FluidBlock) == false) ||
            stateDown.allowsSpawning(world, this.mutablePos, EntityType.CREEPER))
        {
            this.mutablePos.set(x, y, z);
            BlockState state = chunk.getBlockState(this.mutablePos);

            if (this.isClearForSpawnWrapper(world, this.mutablePos, state, state.getFluidState(), EntityType.WITHER_SKELETON))
            {
                this.mutablePos.set(x, y + 1, z);
                BlockState stateUp1 = chunk.getBlockState(this.mutablePos);

                return this.isClearForSpawnWrapper(world, this.mutablePos, stateUp1, state.getFluidState(), EntityType.WITHER_SKELETON);
            }

            if (state.getFluidState().isIn(FluidTags.WATER))
            {
                this.mutablePos.set(x, y + 1, z);
                BlockState stateUp1 = chunk.getBlockState(this.mutablePos);

                return stateUp1.getFluidState().isIn(FluidTags.WATER) &&
                       chunk.getBlockState(this.mutablePos.set(x, y + 2, z)).isSolidBlock(world, this.mutablePos) == false;
            }
        }

        return false;
    }

    public boolean isClearForSpawnWrapper(BlockView blockView, BlockPos pos, BlockState state, FluidState fluidState, EntityType<?> entityType)
    {
        return this.tagsBroken ? isClearForSpawnStripped(blockView, pos, state, fluidState, entityType) : SpawnHelper.isClearForSpawn(blockView, pos, state, fluidState, entityType);
    }

    /**
     * This method is basically a copy of SpawnHelper.isClearForSpawn(), except that
     * it removes any calls to BlockState.isIn(), which causes an exception on certain
     * ViaVersion servers that have old 1.12.2 worlds.
     * (or possibly newer versions as well, but older than 1.16 or 1.15 or whenever the tag syncing was added)
     */
    public static boolean isClearForSpawnStripped(BlockView blockView, BlockPos pos, BlockState state, FluidState fluidState, EntityType<?> entityType)
    {
        if (state.isFullCube(blockView, pos) || state.emitsRedstonePower() || fluidState.isEmpty() == false)
        {
            return false;
        }
        /*
        else if (state.isIn(BlockTags.PREVENT_MOB_SPAWNING_INSIDE))
        {
            return false;
        }

        // this also calls BlockState isIn()
        return entityType.method_29496(state) == false;
        */

        return true;
    }

    public static class LightLevelInfo
    {
        public long pos;
        public byte block;
        public byte sky;
        public float y;

        public LightLevelInfo(long pos, float y, int block, int sky)
        {
            this.pos = pos;
            this.y = y;
            this.block = (byte) block;
            this.sky = (byte) sky;
        }
    }

    private interface IMarkerRenderer
    {
        void render(float x, float y, float z, Color4f color, float offset1, float offset2, BufferBuilder buffer, MatrixStack.Entry e);
    }
}
