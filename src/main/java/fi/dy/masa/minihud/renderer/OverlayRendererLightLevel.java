package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
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
    private static final Identifier TEXTURE_NUMBERS = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/misc/light_level_numbers.png");

    private final List<LightLevelInfo> lightInfos;
    private BlockPos.MutableBlockPos mutablePos;
    private Direction lastDirection;

    private boolean tagsBroken;
    private boolean needsUpdate;
    private boolean hasData;

    protected OverlayRendererLightLevel()
    {
        this.lightInfos = new ArrayList<>();
        this.mutablePos = new BlockPos.MutableBlockPos();
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
    public boolean shouldRender(Minecraft mc)
    {
        return RendererToggle.OVERLAY_LIGHT_LEVEL.getBooleanValue();
    }

    @Override
    public boolean needsUpdate(Entity entity, Minecraft mc)
    {
        return this.needsUpdate || this.lastUpdatePos == null ||
                Math.abs(entity.getX() - this.lastUpdatePos.getX()) > 4 ||
                Math.abs(entity.getY() - this.lastUpdatePos.getY()) > 4 ||
                Math.abs(entity.getZ() - this.lastUpdatePos.getZ()) > 4 ||
                (Configs.Generic.LIGHT_LEVEL_NUMBER_ROTATION.getBooleanValue() && this.lastDirection != entity.getDirection());
    }

    @Override
    public void update(Vec3 cameraPos, Entity entity, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null)
        {
            this.needsUpdate = false;
            return;
        }

//        long pre = System.nanoTime();
        BlockPos pos = PositionUtils.getEntityBlockPos(entity);
        this.hasData = this.updateLightLevels(mc.level, pos);
        this.renderThrough = Configs.Generic.LIGHT_LEVEL_RENDER_THROUGH.getBooleanValue();

        if (this.hasData())
        {
            this.render(cameraPos, mc, profiler);
        }

//        System.out.printf("LL markers: %d, time: %.3f s\n", this.lightInfos.size(), (double) (System.nanoTime() - pre) / 1000000000D);

        this.lastUpdatePos = pos;
        this.lastDirection = entity.getDirection();
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
            this.renderObjects.add(new RenderObjectVbo(() -> this.getName() + " Quads", MaLiLibPipelines.POSITION_TEX_COLOR_MASA_LEQUAL_DEPTH_NO_CULL));
            this.renderObjects.add(new RenderObjectVbo(() -> this.getName() + " Lines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH));
        }
    }

    @Override
    public void render(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        this.allocateBuffers();
        this.renderTexQuads(cameraPos, mc, profiler);
        this.renderOutlines(cameraPos, mc, profiler);
    }

    private void renderTexQuads(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        profiler.push("light_level_quads");
        int safeThreshold = Configs.Generic.LIGHT_LEVEL_THRESHOLD_SAFE.getIntegerValue();
        int dimThreshold = Configs.Generic.LIGHT_LEVEL_THRESHOLD_DIM.getIntegerValue();
        Direction numberFacing = Configs.Generic.LIGHT_LEVEL_NUMBER_ROTATION.getBooleanValue() ? mc.player.getDirection() : Direction.NORTH;
        boolean useColoredNumbers = Configs.Generic.LIGHT_LEVEL_COLORED_NUMBERS.getBooleanValue();
        LightLevelNumberMode numberMode = (LightLevelNumberMode) Configs.Generic.LIGHT_LEVEL_NUMBER_MODE.getOptionListValue();

        // this.renderThrough ? MaLiLibPipelines.POSITION_TEX_COLOR_SIMPLE : MaLiLibPipelines.POSITION_TEX_COLOR_LESSER_DEPTH
        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "minihud:light_level/tex_quads", this.renderThrough ? MaLiLibPipelines.POSITION_TEX_COLOR_MASA_NO_DEPTH_NO_CULL : MaLiLibPipelines.POSITION_TEX_COLOR_MASA_LEQUAL_DEPTH_NO_CULL);
        PoseStack matrices = new PoseStack();

        try
        {
            ctx.bindTexture(TEXTURE_NUMBERS, 0, 256, 256);
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("bindTexture Exception: {}", err.getMessage());
            return;
        }

        matrices.pushPose();
        PoseStack.Pose e = matrices.last();

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
            MeshData meshData = builder.build();

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

        matrices.popPose();
        profiler.pop();
    }

    private void renderOutlines(Vec3 cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        profiler.push("light_level_outlines");
        int safeThreshold = Configs.Generic.LIGHT_LEVEL_THRESHOLD_SAFE.getIntegerValue();
        int dimThreshold = Configs.Generic.LIGHT_LEVEL_THRESHOLD_DIM.getIntegerValue();
        LightLevelMarkerMode markerMode = (LightLevelMarkerMode) Configs.Generic.LIGHT_LEVEL_MARKER_MODE.getOptionListValue();

        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(() -> "minihud:light_level/outlines", this.renderThrough ? MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL : MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH);

        if (markerMode == LightLevelMarkerMode.SQUARE)
        {
            this.renderMarkers(this::renderLightLevelSquare, cameraPos, safeThreshold, dimThreshold, this.glLineWidth, builder);
        }
        else if (markerMode == LightLevelMarkerMode.CROSS)
        {
            this.renderMarkers(this::renderLightLevelCross, cameraPos, safeThreshold, dimThreshold, this.glLineWidth, builder);
        }

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
            MiniHUD.LOGGER.error("OverlayRendererLightLevel#renderOutlines(): Exception; {}", err.getMessage());
        }

        profiler.pop();
    }

    @Override
    public void reset()
    {
        super.reset();
        this.tagsBroken = false;
        this.lightInfos.clear();
        this.mutablePos = new BlockPos.MutableBlockPos();
        this.lastDirection = Direction.NORTH;
        this.hasData = false;
    }

    private void renderNumbers(Vec3 cameraPos,
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
                               PoseStack.Pose e)
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
                               Vec3 cameraPos,
                               int safeThreshold,
                               int dimThreshold,
							   float lineWidth,
                               BufferBuilder buffer)
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
                double x = BlockPos.getX(pos) - offsetX;
                double y = (autoHeight ? info.y : BlockPos.getY(pos)) - offsetY;
                double z = BlockPos.getZ(pos) - offsetZ;

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

                renderer.render((float) x, (float) y, (float) z, color, (float) offset1, (float) offset2, lineWidth, buffer);
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
                                         PoseStack.Pose e)
    {
        LightLevelRenderCondition condition = (LightLevelRenderCondition) Configs.Generic.LIGHT_LEVEL_NUMBER_CONDITION.getOptionListValue();
        boolean autoHeight = Configs.Generic.LIGHT_LEVEL_AUTO_HEIGHT.getBooleanValue();
        Color4f color;

        for (LightLevelInfo info : this.lightInfos)
        {
            if (condition.shouldRender(info.block, dimThreshold, safeThreshold))
            {
                long pos = info.pos;
                double x = BlockPos.getX(pos) - dx;
                double y = (autoHeight ? info.y : BlockPos.getY(pos)) - dy;
                double z = BlockPos.getZ(pos) - dz;
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

    private void renderLightLevelTextureColor(float x, float y, float z, Direction facing, int lightLevel, Color4f color, BufferBuilder buffer, PoseStack.Pose e)
    {
        float w = 0.25f;
        float u = (lightLevel & 0x3) * w;
        float v = (lightLevel >> 2) * w;
        y += 0.005F;

        Matrix4f m = e.pose();

        switch (facing)
        {
            case NORTH:
                buffer.addVertex(m, x, y, z).setUv(u    , v    ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(m, x, y, z + 1).setUv(u    , v + w).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(m, x + 1, y, z + 1).setUv(u + w, v + w).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(m, x + 1, y, z).setUv(u + w, v    ).setColor(color.r, color.g, color.b, color.a);
                break;

            case SOUTH:
                buffer.addVertex(m, x + 1, y, z + 1).setUv(u    , v    ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(m, x + 1, y, z    ).setUv(u    , v + w).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(m, x    , y, z    ).setUv(u + w, v + w).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(m, x    , y, z + 1).setUv(u + w, v    ).setColor(color.r, color.g, color.b, color.a);
                break;

            case EAST:
                buffer.addVertex(m, x + 1, y, z    ).setUv(u    , v    ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(m, x    , y, z    ).setUv(u    , v + w).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(m, x    , y, z + 1).setUv(u + w, v + w).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(m, x + 1, y, z + 1).setUv(u + w, v    ).setColor(color.r, color.g, color.b, color.a);
                break;

            case WEST:
                buffer.addVertex(m, x    , y, z + 1).setUv(u    , v    ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(m, x + 1, y, z + 1).setUv(u    , v + w).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(m, x + 1, y, z    ).setUv(u + w, v + w).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(m, x    , y, z    ).setUv(u + w, v    ).setColor(color.r, color.g, color.b, color.a);
                break;

            default:
        }
    }

    private void renderLightLevelCross(float x, float y, float z, Color4f color, float offset1, float offset2, float lineWidth, BufferBuilder buffer)
    {
        y += 0.005F;

        buffer.addVertex(x + offset1, y, z + offset1).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(x + offset2, y, z + offset2).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        buffer.addVertex(x + offset1, y, z + offset2).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(x + offset2, y, z + offset1).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
    }

    private void renderLightLevelSquare(float x, float y, float z, Color4f color, float offset1, float offset2, float lineWidth, BufferBuilder buffer)
    {
        y += 0.005F;

        buffer.addVertex(x + offset1, y, z + offset1).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(x + offset1, y, z + offset2).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        buffer.addVertex(x + offset1, y, z + offset2).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(x + offset2, y, z + offset2).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        buffer.addVertex(x + offset2, y, z + offset2).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(x + offset2, y, z + offset1).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        buffer.addVertex(x + offset2, y, z + offset1).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(x + offset1, y, z + offset1).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
    }

    private boolean updateLightLevels(Level world, BlockPos center)
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
        LevelLightEngine lightingProvider = world.getChunkSource().getLightEngine();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        final int worldTopHeight = world.getMaxY() + 1;
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
                LevelChunk chunk = world.getChunk(cx, cz);
                final int startY = Math.max(minY, world.getMinY());
                final int endY = Math.min(maxY, WorldUtils.getHighestSectionYOffset(chunk) + 15 + 1);

                for (int y = startY; y <= endY; ++y)
                {
                    if (y > startY)
                    {
                        // If there are no blocks in the section below this layer, then we can skip it
                        LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y - 1));

                        if (section.hasOnlyAir())
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
                                int block = y < worldTopHeight ? lightingProvider.getLayerListener(LightLayer.BLOCK).getLightValue(mutablePos) : 0;
                                int sky   = y < worldTopHeight ? lightingProvider.getLayerListener(LightLayer.SKY).getLightValue(mutablePos) : 15;
                                double topY = state.getShape(chunk, mutablePos).max(Direction.Axis.Y);

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

    private boolean canSpawnAtWrapper(int x, int y, int z, ChunkAccess chunk, Level world, boolean skipBlockCheck)
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
    private boolean canSpawnAt(int x, int y, int z, ChunkAccess chunk, Level world, boolean skipBlockCheck)
    {
        this.mutablePos.set(x, y - 1, z);
        BlockState stateDown = chunk.getBlockState(this.mutablePos);

        if ((skipBlockCheck && stateDown.isAir() == false && (stateDown.getBlock() instanceof LiquidBlock) == false) ||
            stateDown.isValidSpawn(world, this.mutablePos, EntityType.CREEPER))
        {
            this.mutablePos.set(x, y, z);
            BlockState state = chunk.getBlockState(this.mutablePos);

            if (this.isClearForSpawnWrapper(world, this.mutablePos, state, state.getFluidState(), EntityType.WITHER_SKELETON))
            {
                this.mutablePos.set(x, y + 1, z);
                BlockState stateUp1 = chunk.getBlockState(this.mutablePos);

                return this.isClearForSpawnWrapper(world, this.mutablePos, stateUp1, state.getFluidState(), EntityType.WITHER_SKELETON);
            }

            if (state.getFluidState().is(FluidTags.WATER))
            {
                this.mutablePos.set(x, y + 1, z);
                BlockState stateUp1 = chunk.getBlockState(this.mutablePos);

                return stateUp1.getFluidState().is(FluidTags.WATER) &&
                       chunk.getBlockState(this.mutablePos.set(x, y + 2, z)).isRedstoneConductor(world, this.mutablePos) == false;
            }
        }

        return false;
    }

    public boolean isClearForSpawnWrapper(BlockGetter blockView, BlockPos pos, BlockState state, FluidState fluidState, EntityType<?> entityType)
    {
        return this.tagsBroken ? isClearForSpawnStripped(blockView, pos, state, fluidState, entityType) : NaturalSpawner.isValidEmptySpawnBlock(blockView, pos, state, fluidState, entityType);
    }

    /**
     * This method is basically a copy of SpawnHelper.isClearForSpawn(), except that
     * it removes any calls to BlockState.isIn(), which causes an exception on certain
     * ViaVersion servers that have old 1.12.2 worlds.
     * (or possibly newer versions as well, but older than 1.16 or 1.15 or whenever the tag syncing was added)
     */
    public static boolean isClearForSpawnStripped(BlockGetter blockView, BlockPos pos, BlockState state, FluidState fluidState, EntityType<?> entityType)
    {
        if (state.isCollisionShapeFullBlock(blockView, pos) || state.isSignalSource() || fluidState.isEmpty() == false)
        {
            return false;
        }
        else if (state.is(BlockTags.INVALID_SPAWN_INSIDE))
        {
            return false;
        }

        // this also calls BlockState isIn()
        return entityType.isBlockDangerous(state) == false;
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
        void render(float x, float y, float z, Color4f color, float offset1, float offset2, float lineWidth, BufferBuilder buffer);
    }
}
