package fi.dy.masa.minihud.renderer;

import java.util.*;
import java.util.function.Predicate;
import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

import fi.dy.masa.malilib.config.IConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.texture.MaLiLibComplexBinding;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.malilib.util.position.Vec3d;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.mixin.render.IMixinByteBufferBuilder;
import fi.dy.masa.minihud.mixin.render.IMixinRenderObjectVbo;
import fi.dy.masa.minihud.renderer.shapes.ShapeManager;
import fi.dy.masa.minihud.renderer.shapes.ShapeSpawnSphere;
import fi.dy.masa.minihud.util.*;

public class OverlayRendererLightLevel extends OverlayRendererBase
{
    public static final OverlayRendererLightLevel INSTANCE = new OverlayRendererLightLevel();
    private static final Identifier TEXTURE_NUMBERS = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/misc/light_level_numbers.png");

    private final Timer timer;
    private final LightLevelChunkInfo.Map lightInfos;
    private BlockPos.MutableBlockPos mutablePos;
    private Direction lastDirection;
    private LightLevelPosProvider lastProvider;

    private boolean ignoreRange;
    private boolean tagsBroken;
    private boolean needsUpdate;
    private boolean needsDataUpdate;

    // if implemented as method param, it'll break tweakermore.
    private boolean underWater;

    protected OverlayRendererLightLevel()
    {
        this.timer = new Timer();
        this.lightInfos = new LightLevelChunkInfo.Map();
        this.mutablePos = new BlockPos.MutableBlockPos();
        this.lastDirection = Direction.NORTH;
        this.needsDataUpdate = true;
    }

    @Override
    public String getName()
    {
        return "LightLevel";
    }

    public void markNeedsUpdate(boolean clearData)
    {
        this.markNeedsUpdate(clearData, clearData == false);
    }

    public void markNeedsUpdate(boolean clearData, boolean renderOnly)
    {
        if (clearData)
        {
            this.needsDataUpdate = true;
            this.lightInfos.clear();
        }
        else if (renderOnly == false)
        {
            this.needsDataUpdate = true;
        }

        this.needsUpdate = true;
        // Clean buffers when receiving the RenderCallback.
        this.clearBuffers();
    }

    public void markChunkChanged(int cx, int cz)
    {
        this.needsUpdate = true;
        this.needsDataUpdate = true;
        this.lightInfos.removeNeighbors(cx, cz);
    }

    public void onSpawnSphereUpdate()
    {
        boolean hasShapeInteraction = Configs.Generic.LIGHT_LEVEL_SPAWN_SPHERE_FULL.getBooleanValue() ||
                Configs.Generic.LIGHT_LEVEL_SPAWN_SPHERE_LIMIT.getBooleanValue();

        if (hasShapeInteraction)
        {
            this.markNeedsUpdate(true);
        }
    }

    @Override
    public boolean shouldRender(Minecraft mc)
    {
        return RendererToggle.OVERLAY_LIGHT_LEVEL.getBooleanValue();
    }

    @Override
    public boolean needsUpdate(Entity entity, Minecraft mc)
    {
        if (this.needsDataUpdate || this.lastUpdatePos == null)
        {
            this.needsDataUpdate = true;
            return true;
        }

        int dist = PositionUtils.getEntityBlockPos(entity).distManhattan(this.lastUpdatePos);
        if (dist >= this.timer.getMinDist())
        {
            this.needsDataUpdate = true;
            return true;
        }

        return this.needsUpdate ||
                Configs.Generic.LIGHT_LEVEL_ANIMATION.getOptionListValue() != LightLevelAnimation.NONE ||
                (Configs.Generic.LIGHT_LEVEL_NUMBER_ROTATION.getBooleanValue() &&
                        Configs.Generic.LIGHT_LEVEL_NUMBER_CONDITION.getOptionListValue() != LightLevelRenderCondition.NEVER &&
                        this.lastDirection != entity.getDirection());
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity, Minecraft mc, ProfilerFiller profiler)
    {
        this.needsUpdate = false;
        if (mc.level == null)
        {
            this.needsDataUpdate = false;
            return;
        }

        this.timer.timeUpdate();
        BlockPos pos = PositionUtils.getEntityBlockPos(entity);
        if (this.needsDataUpdate)
        {
            this.needsDataUpdate = false;
            this.updateLightLevels(mc.level, pos);
        }
        this.renderThrough = Configs.Generic.LIGHT_LEVEL_RENDER_THROUGH.getBooleanValue();

        this.timer.timeRender();
        if (this.hasData())
        {
            this.render(cameraPos, mc, profiler);
        }

        this.timer.timeEnd();
//        this.timer.debugLog();

        this.lastUpdatePos = pos;
        this.lastDirection = entity.getDirection();
    }

    @Override
    public boolean hasData()
    {
        return this.lightInfos.isEmpty() == false;
    }

    @Override
    public void allocateBuffers()
    {
        // Don't reallocate it unless empty; using start() calls reset() anyways.
        if (this.renderObjects.isEmpty())
        {
            this.renderObjects.add(new RenderObjectVbo(() -> this.getName() + " Quads", MaLiLibPipelines.POSITION_TEX_COLOR_MASA_LEQUAL_DEPTH_NO_CULL, 0));
            this.renderObjects.add(new RenderObjectVbo(() -> this.getName() + " Lines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH, 0));
        }
    }

    @Override
    public void render(Vec3d cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        this.allocateBuffers();
        this.renderTexQuads(cameraPos, mc, profiler);
        this.renderOutlines(cameraPos, mc, profiler);
    }

    private void renderTexQuads(Vec3d cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        LightLevelNumberMode numberMode = (LightLevelNumberMode) Configs.Generic.LIGHT_LEVEL_NUMBER_MODE.getOptionListValue();
        LightLevelRenderCondition condition = (LightLevelRenderCondition) Configs.Generic.LIGHT_LEVEL_NUMBER_CONDITION.getOptionListValue();
        if (numberMode == LightLevelNumberMode.NONE || condition == LightLevelRenderCondition.NEVER)
        {
            return;
        }

        profiler.push("light_level_quads");
        int safeThreshold = Configs.Generic.LIGHT_LEVEL_THRESHOLD_SAFE.getIntegerValue();
        int dimThreshold = Configs.Generic.LIGHT_LEVEL_THRESHOLD_DIM.getIntegerValue();
        Direction numberFacing = Configs.Generic.LIGHT_LEVEL_NUMBER_ROTATION.getBooleanValue()
                ? Objects.requireNonNullElse(mc.getCameraEntity(), mc.player).getDirection()
                : Direction.NORTH;
        boolean useColoredNumbers = Configs.Generic.LIGHT_LEVEL_COLORED_NUMBERS.getBooleanValue();

        // this.renderThrough ? MaLiLibPipelines.POSITION_TEX_COLOR_SIMPLE : MaLiLibPipelines.POSITION_TEX_COLOR_LESSER_DEPTH
        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "minihud:light_level/tex_quads", this.renderThrough ? MaLiLibPipelines.POSITION_TEX_COLOR_MASA_NO_DEPTH_NO_CULL : MaLiLibPipelines.POSITION_TEX_COLOR_MASA_LEQUAL_DEPTH_NO_CULL, 0);
        PoseStack matrices = new PoseStack();

        try
        {
//            ctx.bindTexture(TEXTURE_NUMBERS, 0, 256, 256);
            ctx.prepareComplexTextures(List.of(new MaLiLibComplexBinding("Sampler0", TEXTURE_NUMBERS, () -> null)));
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

    private void renderOutlines(Vec3d cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        if (mc.level == null || mc.player == null)
        {
            return;
        }

        LightLevelMarkerMode markerMode = (LightLevelMarkerMode) Configs.Generic.LIGHT_LEVEL_MARKER_MODE.getOptionListValue();
        LightLevelRenderCondition condition = (LightLevelRenderCondition) Configs.Generic.LIGHT_LEVEL_MARKER_CONDITION.getOptionListValue();
        if (markerMode == LightLevelMarkerMode.NONE || condition == LightLevelRenderCondition.NEVER)
        {
            return;
        }

        profiler.push("light_level_outlines");
        int safeThreshold = Configs.Generic.LIGHT_LEVEL_THRESHOLD_SAFE.getIntegerValue();
        int dimThreshold = Configs.Generic.LIGHT_LEVEL_THRESHOLD_DIM.getIntegerValue();

        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(() -> "minihud:light_level/outlines", this.renderThrough ? MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL : MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH, 0);
        // According to spark, this saves ~10% cpu time by setting initial ByteBufferBuilder capacity.
        // If anything breaks, this can be safely commented out from here to ensureCapacity.
        Predicate<Integer> blockLightCondition = b -> condition.shouldRender(b, dimThreshold, safeThreshold);
        int marks = this.ignoreRange
                ? this.lightInfos.countAllMarks(blockLightCondition)
                : this.lightInfos.countMarks(cameraPos, Configs.Generic.LIGHT_LEVEL_RANGE.getIntegerValue(), blockLightCondition);
        int vertexPerMark = markerMode == LightLevelMarkerMode.SQUARE ? 8 : 4;
        // vertices limits at 16777215, see BufferBuilder#beginVertex
        // These shouldn't be critical if any of these value changed in the future. Except some wasted memory.
        int vertexCount = Math.min(marks * vertexPerMark, 16777215);
        int bufferSize = vertexCount * ctx.getSelectedVertexFormat().getVertexSize();
        //noinspection resource
        ((IMixinByteBufferBuilder) ((IMixinRenderObjectVbo) ctx).minihud_getAlloc()).minihud_ensureCapacity(bufferSize);


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
        this.needsDataUpdate = true;
        this.lastProvider = null;
    }

    @SuppressWarnings({"EnhancedSwitchMigration", "DuplicateBranchesInSwitch"})
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
                               Vec3d cameraPos,
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
        LightLevelAnimation animation = (LightLevelAnimation) Configs.Generic.LIGHT_LEVEL_ANIMATION.getOptionListValue();
        double markerSize = Configs.Generic.LIGHT_LEVEL_MARKER_SIZE.getDoubleValue();
        double offsetX = cameraPos.x;
        double offsetY = cameraPos.y - Configs.Generic.LIGHT_LEVEL_RENDER_OFFSET.getDoubleValue();
        double offsetZ = cameraPos.z;
        double offset1 = (1.0 - markerSize) / 2.0;
        double offset2 = (1.0 - offset1);
        Color4f color;
        int range = this.ignoreRange ? Integer.MAX_VALUE : Configs.Generic.LIGHT_LEVEL_RANGE.getIntegerValue();
        int minY = this.ignoreRange ? Integer.MIN_VALUE : (int) Math.floor(offsetY - range);
        int minX = (int) Math.floor(offsetX - range);
        int minZ = (int) Math.floor(offsetZ - range);
        int maxX = (int) Math.ceil(offsetX + range);
        int maxZ = (int) Math.ceil(offsetZ + range);

        Predicate<Integer> blockLightPredicate = b -> condition.shouldRender(b, dimThreshold, safeThreshold);
        int totalVertices = 0;

        double timeMs = (System.nanoTime() >>> 1L) / 500000.0;

        for (LightLevelChunkInfo chunk : this.lightInfos.values())
        {
            if (chunk.isEmpty())
            {
                continue;
            }

            int xzMask = this.ignoreRange ? 0 :
                    LightLevelPosLimiter.createXZMask(chunk.x >> 4, chunk.z >> 4, minX, minZ, maxX, maxZ);

            for (LightLevelInfo info : chunk.iterable(minY, xzMask, blockLightPredicate))
            {
                double x = info.x - offsetX;
                double y = info.y - offsetY;
                double z = info.z - offsetZ;
                if (y > range)
                {
                    break;
                }

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

                if (animation != LightLevelAnimation.NONE)
                {
                    double dy = info.y - cameraPos.y;
                    color = animation.apply(color, Math.sqrt(x * x + dy * dy + z * z), timeMs);
                }

                totalVertices += renderer.render((float) x, (float) y, (float) z, color, (float) offset1, (float) offset2, lineWidth, buffer);
                // vertices limits at 16777215, see BufferBuilder#beginVertex
                if (totalVertices > 16777200)
                {
                    return;
                }
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
        Color4f color;
        int range = Configs.Generic.LIGHT_LEVEL_NUMBER_RANGE.getIntegerValue();
        int minY = (int) Math.floor(dy - range);
        int minX = (int) Math.floor(dx - range);
        int minZ = (int) Math.floor(dz - range);
        int maxX = (int) Math.ceil(dx + range);
        int maxZ = (int) Math.ceil(dz + range);

        Predicate<Integer> blockLightPredicate = b -> condition.shouldRender(b, dimThreshold, safeThreshold);

        for (LightLevelChunkInfo chunk : this.lightInfos.values())
        {
            if (chunk.isEmpty())
            {
                continue;
            }

            int xzMask = LightLevelPosLimiter.createXZMask(chunk.x >> 4, chunk.z >> 4, minX, minZ, maxX, maxZ);

            for (LightLevelInfo info : chunk.iterable(minY, xzMask, blockLightPredicate))
            {
                double x = info.x - dx;
                double y = info.y - dy;
                double z = info.z - dz;
                if (y > range)
                {
                    break;
                }

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
                buffer.addVertex(m, x    , y, z    ).setUv(u    , v    ).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(m, x    , y, z + 1).setUv(u    , v + w).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(m, x + 1, y, z + 1).setUv(u + w, v + w).setColor(color.r, color.g, color.b, color.a);
                buffer.addVertex(m, x + 1, y, z    ).setUv(u + w, v    ).setColor(color.r, color.g, color.b, color.a);
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

    private int renderLightLevelCross(float x, float y, float z, Color4f color, float offset1, float offset2, float lineWidth, BufferBuilder buffer)
    {
        y += 0.005F;

        buffer.addVertex(x + offset1, y, z + offset1).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(x + offset2, y, z + offset2).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        buffer.addVertex(x + offset1, y, z + offset2).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);
        buffer.addVertex(x + offset2, y, z + offset1).setColor(color.r, color.g, color.b, color.a).setLineWidth(lineWidth);

        return 4;
    }

    private int renderLightLevelSquare(float x, float y, float z, Color4f color, float offset1, float offset2, float lineWidth, BufferBuilder buffer)
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

        return 8;
    }

    private void updateLightLevels(Level world, BlockPos center)
    {
        //System.out.printf("LL center %s\n", center.toShortString());

        final boolean collisionCheck = Configs.Generic.LIGHT_LEVEL_COLLISION_CHECK.getBooleanValue();
        final boolean underWater = Configs.Generic.LIGHT_LEVEL_UNDER_WATER.getBooleanValue();
        final boolean autoHeight = Configs.Generic.LIGHT_LEVEL_AUTO_HEIGHT.getBooleanValue();
        final boolean skipBlockCheck = Configs.Generic.LIGHT_LEVEL_SKIP_BLOCK_CHECK.getBooleanValue();
        final boolean ssFull = Configs.Generic.LIGHT_LEVEL_SPAWN_SPHERE_FULL.getBooleanValue();
        final boolean ssLimit = Configs.Generic.LIGHT_LEVEL_SPAWN_SPHERE_LIMIT.getBooleanValue();
        final boolean slimeMode = Configs.Generic.LIGHT_LEVEL_SLIME_MODE.getBooleanValue();
        final boolean aggressiveOpt = Configs.Generic.LIGHT_LEVEL_AGGRESSIVE_OPTIMIZATION.getBooleanValue();
        final int cx = center.getX();
        final int cy = center.getY();
        final int cz = center.getZ();
        final int worldTopHeight = world.getMaxY() + 1;
        LevelLightEngine lightingProvider = world.getChunkSource().getLightEngine();

        this.underWater = underWater;

        List<ShapeSpawnSphere> spawnSpheres = ssFull || ssLimit ? ShapeManager.INSTANCE.getAllShapes().stream()
                .filter(s -> s instanceof ShapeSpawnSphere && s.isEnabled())
                .map(s -> (ShapeSpawnSphere) s)
                .filter(s -> s.contains(cx, cy, cz))
                .sorted(Comparator.<ShapeSpawnSphere>comparingDouble(s -> s.getMargin() + s.getRadius()).reversed())
                .toList() : null;

        // put heavier limiter last
        LightLevelPosLimiter.List limiters = new LightLevelPosLimiter.List(this.lightInfos::contains);
        if (slimeMode)
        {
            limiters.add(new LightLevelPosLimiter.SlimeChunk(world, cx, cy, cz, aggressiveOpt));
        }
        if (spawnSpheres != null && spawnSpheres.isEmpty() == false)
        {
            limiters.add(new LightLevelPosLimiter.SpawnSphere(cx, cy, cz, aggressiveOpt, spawnSpheres));
        }

        LightLevelPosIterator iterator = aggressiveOpt
                ? LightLevelPosIterator.PALETTE_LOOKUP
                : LightLevelPosIterator.DEFAULT;
        LightLevelPosProvider provider = ssFull && spawnSpheres.isEmpty() == false
                ? new LightLevelPosProvider.SpawnSphere(cx, cy, cz, aggressiveOpt, spawnSpheres, limiters, iterator)
                : new LightLevelPosProvider.Default(cx, cy, cz, aggressiveOpt, limiters, iterator);

        if (provider.isSame(this.lastProvider) == false)
        {
            this.lightInfos.clear();
        }
        this.lastProvider = provider;
        this.ignoreRange = provider.ignoreRange();

        provider.generate(world, underWater, (chunkX, chunkZ, chunkIterator) ->
        {
            LevelChunk chunk = world.getChunk(chunkX, chunkZ);
            if (chunk.isEmpty())
            {
                return;
            }

            if (this.timer.shouldProcessNextChunk() == false)
            {
                this.needsDataUpdate = true;
                provider.stop();
                return;
            }

            LightLevelChunkInfo info = new LightLevelChunkInfo(chunkX, chunkZ, world.getMinY());
            this.lightInfos.put(chunkX, chunkZ, info);

            chunkIterator.accept(chunk, (x, y, z, pos, state) ->
            {
                if (this.canSpawnAtWrapper(x, y, z, chunk, world, skipBlockCheck) == false)
                {
                    return;
                }

                if ((collisionCheck == false || state.getCollisionShape(chunk, pos).isEmpty()) &&
                        (underWater || state.getFluidState().isEmpty()))
                {
                    int block = 0;
                    int sky = 15;
                    double topY = autoHeight ? state.getShape(chunk, pos).max(Direction.Axis.Y) : 0;

                    if (slimeMode)
                    {
                        sky = 0;
                    }
                    else if (y < worldTopHeight)
                    {
                        block = lightingProvider.getLayerListener(LightLayer.BLOCK).getLightValue(pos);
                        sky = lightingProvider.getLayerListener(LightLayer.SKY).getLightValue(pos);
                    }

                    // Don't render the light level marker if it would be raised all the way to the next block space
                    if (topY < 1)
                    {
                        info.push(x, y, z, (float) Math.max(0, topY), block, sky);
                    }
                }
            });

            info.seal();
            this.timer.processedChunk();
        });
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
            stateDown.isValidSpawn(world, this.mutablePos, EntityTypes.CREEPER))
        {
            this.mutablePos.set(x, y, z);
            BlockState state = chunk.getBlockState(this.mutablePos);

            if (this.isClearForSpawnWrapper(world, this.mutablePos, state, state.getFluidState(), EntityTypes.WITHER_SKELETON))
            {
                this.mutablePos.set(x, y + 1, z);
                BlockState stateUp1 = chunk.getBlockState(this.mutablePos);

                return this.isClearForSpawnWrapper(world, this.mutablePos, stateUp1, state.getFluidState(), EntityTypes.WITHER_SKELETON);
            }

            if (this.underWater && state.getFluidState().is(FluidTags.WATER))
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

    private class Timer
    {
        private static final int MAX_SCAN_MICROS = 15000;
        private int update, render, end;
        private int chunks;
        private final short[] history = new short[16];
        private int index = 0;
        private int dist;

        private boolean wasUpdateWorkSplit = false;
        private int accumulatedChunks, accumulatedUpdateStart, accumulatedUpdateTime;

        public void timeUpdate()
        {
            this.update = (int) (System.nanoTime() / 1000L);
            this.chunks = 0;

            if (this.accumulatedChunks > 0 && this.wasUpdateWorkSplit == false)
            {
                this.accumulatedChunks = 0;
                this.accumulatedUpdateTime = 0;
            }
        }

        public void timeRender()
        {
            this.render = (int) (System.nanoTime() / 1000L);

            if (this.chunks > 0)
            {
                if (this.wasUpdateWorkSplit || OverlayRendererLightLevel.this.needsDataUpdate)
                {
                    if (this.wasUpdateWorkSplit == false)
                    {
                        this.accumulatedUpdateStart = this.update;
                    }
                    this.accumulatedChunks += this.chunks;
                    this.accumulatedUpdateTime += this.render - this.update;
                    this.wasUpdateWorkSplit = OverlayRendererLightLevel.this.needsDataUpdate;
                }
            }
            else
            {
                this.wasUpdateWorkSplit = false;
            }
        }

        public void timeEnd()
        {
            this.end = (int) (System.nanoTime() / 1000L);
            short score = (short) Math.clamp(this.end - this.render - 5000, 0, 20000);
            this.history[this.index] = score;
            if (++this.index >= 16)
            {
                this.index = 0;
            }
            this.dist = 0;
        }

        public void processedChunk()
        {
            this.chunks++;
        }

        public boolean shouldProcessNextChunk()
        {
            return this.chunks == 0 ||
                    ((int) (System.nanoTime() / 1000L) - this.update) / this.chunks * (this.chunks + 1) < MAX_SCAN_MICROS;
        }

        public int getMinDist()
        {
            if (this.dist > 0)
            {
                return this.dist;
            }

            int sum = 0;
            for (int i = 0; i < 16; ++i)
            {
                sum += this.history[i];
            }

            this.dist = 1 + sum / 16 / 2500;
            return this.dist;
        }

        @SuppressWarnings("unused")
        public void debugLog()
        {
            System.out.printf("LL markers: %d, updated %d chunks in %.3f ms, rendered in %.3f ms, min dist: %d\n",
                    OverlayRendererLightLevel.this.lightInfos.values().stream()
                            .mapToInt(LightLevelChunkInfo::size).sum(),
                    this.chunks,
                    (double) (this.render - this.update) / 1000,
                    (double) (this.end - this.render) / 1000,
                    this.getMinDist()
            );

            if (this.accumulatedChunks > 0 && this.wasUpdateWorkSplit == false)
            {
                System.out.printf("LL finished %d chunk scanning in %.3f ms, %.3f ms spent on update\n",
                        this.accumulatedChunks,
                        (double) (this.render - this.accumulatedUpdateStart) / 1000,
                        (double) this.accumulatedUpdateTime / 1000
                );

                this.accumulatedChunks = 0;
                this.accumulatedUpdateTime = 0;
            }
        }
    }

    private interface IMarkerRenderer
    {
        /**
         * @return vertex count
         */
        int render(float x, float y, float z, Color4f color, float offset1, float offset2, float lineWidth, BufferBuilder buffer);
    }
}
