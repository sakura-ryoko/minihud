package fi.dy.masa.minihud.renderer;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import com.mojang.blaze3d.buffers.BufferUsage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.data.HudDataManager;
import fi.dy.masa.minihud.util.MiscUtils;

public class OverlayRendererSlimeChunks extends OverlayRendererBase
{
    public static final OverlayRendererSlimeChunks INSTANCE = new OverlayRendererSlimeChunks();
    public double overlayTopY;
    protected boolean needsUpdate = true;

    protected boolean wasSeedKnown;
    protected long seed;
    protected double topY;

    private final List<Box> slimeChunks;
    private boolean hasData;

    protected OverlayRendererSlimeChunks()
    {
        this.slimeChunks = new ArrayList<>();
        this.wasSeedKnown = false;
        this.seed = -1L;
        this.topY = 40;
        this.useCulling = true;
        this.hasData = false;
    }

    @Override
    public String getName()
    {
        return "SlimeChunks";
    }

    public void setNeedsUpdate()
    {
        this.needsUpdate = true;
    }

    public void setOverlayTopY(double y)
    {
        this.overlayTopY = y;
    }

    public void incrementOverlayTopY(double y)
    {
        this.overlayTopY += y;
    }

    public void onEnabled()
    {
        if (Configs.Generic.SLIME_CHUNK_TOP_TO_PLAYER.getBooleanValue())
        {
            Entity entity = EntityUtils.getCameraEntity();

            if (entity != null)
            {
                this.overlayTopY = entity.getY();
            }
        }
        else
        {
            this.overlayTopY = 40;
        }

        this.setNeedsUpdate();
    }

    @Override
    public boolean shouldRender(MinecraftClient mc)
    {
        return RendererToggle.OVERLAY_SLIME_CHUNKS_OVERLAY.getBooleanValue() && mc.world != null &&
                HudDataManager.getInstance().isWorldSeedKnown(mc.world) &&
                MiscUtils.isOverworld(mc.world);
    }

    @Override
    public boolean needsUpdate(Entity entity, MinecraftClient mc)
    {
        if (this.needsUpdate)
        {
            return true;
        }

        World world = entity.getEntityWorld();
        boolean isSeedKnown = HudDataManager.getInstance().isWorldSeedKnown(world);
        long seed = HudDataManager.getInstance().getWorldSeed(world);

        if (this.topY != this.overlayTopY || this.wasSeedKnown != isSeedKnown || this.seed != seed)
        {
            return true;
        }

        int ex = (int) Math.floor(entity.getX());
        int ez = (int) Math.floor(entity.getZ());
        int lx = this.lastUpdatePos.getX();
        int lz = this.lastUpdatePos.getZ();

        return Math.abs(lx - ex) > 16 || Math.abs(lz - ez) > 16;
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc, Profiler profiler)
    {
        this.calculateChunks(entity, mc);

        this.renderThrough = Configs.Generic.SLIME_CHUNK_RENDER_THROUGH.getBooleanValue();

        if (this.hasData())
        {
            this.render(cameraPos, mc, profiler);
        }

//        System.out.printf("SlimeChunk count - %d\n", this.slimeChunks.size());

        this.needsUpdate = false;
    }

    @Override
    public boolean hasData()
    {
        return this.hasData && !this.slimeChunks.isEmpty();
    }

    @Override
    public void render(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        this.allocateBuffers();
        this.renderQuads(cameraPos, mc, profiler);
        this.renderOutlines(cameraPos, mc, profiler);
    }

    private void renderQuads(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        final Color4f colorSides = Configs.Colors.SLIME_CHUNKS_OVERLAY_COLOR.getColor();
        profiler.push("slime_chunk_quads");
        RenderObjectVbo ctx = this.renderObjects.getFirst();
        BufferBuilder builder = ctx.start(() -> "SlimeChunks Quads", this.renderThrough ? MaLiLibPipelines.POSITION_COLOR_MASA_NO_DEPTH_NO_CULL : MaLiLibPipelines.POSITION_COLOR_MASA_LEQUAL_DEPTH_OFFSET_1, BufferUsage.STATIC_WRITE);
        // MaLiLibPipelines.POSITION_COLOR_LESSER_DEPTH
        MatrixStack matrices = new MatrixStack();

        matrices.push();

        for (Box bb : this.slimeChunks)
        {
            float x1 = (float)(bb.minX - cameraPos.x);
            float y1 = (float)(bb.minY - cameraPos.y);
            float z1 = (float)(bb.minZ - cameraPos.z);
            float x2 = (float)(bb.maxX - cameraPos.x);
            float y2 = (float)(bb.maxY - cameraPos.y);
            float z2 = (float)(bb.maxZ - cameraPos.z);

            fi.dy.masa.malilib.render.RenderUtils.drawBoxAllSidesBatchedQuads(x1, y1, z1, x2, y2, z2, colorSides, builder);
        }

        try
        {
            BuiltBuffer meshData = builder.endNullable();

            if (meshData != null)
            {
                ctx.upload(meshData, this.shouldResort);

                if (this.shouldResort)
                {
                    ctx.startResorting(meshData, ctx.createVertexSorter(cameraPos));
                }

                meshData.close();
            }
        }
        catch (Exception err)
        {
            MiniHUD.LOGGER.error("OverlayRendererSlimeChunks#renderQuads(): Exception; {}", err.getMessage());
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

        final Color4f colorLines = Color4f.fromColor(Configs.Colors.SLIME_CHUNKS_OVERLAY_COLOR.getColor().getIntValue(), 1.0F);
        profiler.push("slime_chunk_outlines");
        RenderObjectVbo ctx = this.renderObjects.get(1);
        BufferBuilder builder = ctx.start(() -> "SlimeChunks Outlines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH, BufferUsage.STATIC_WRITE);
//        MatrixStack matrices = new MatrixStack();

//        matrices.push();
//        MatrixStack.Entry e = matrices.peek();

        for (Box bb : this.slimeChunks)
        {
            float x1 = (float)(bb.minX - cameraPos.x);
            float y1 = (float)(bb.minY - cameraPos.y);
            float z1 = (float)(bb.minZ - cameraPos.z);
            float x2 = (float)(bb.maxX - cameraPos.x);
            float y2 = (float)(bb.maxY - cameraPos.y);
            float z2 = (float)(bb.maxZ - cameraPos.z);

            fi.dy.masa.malilib.render.RenderUtils.drawBoxAllEdgesBatchedLines(x1, y1, z1, x2, y2, z2, colorLines, builder);
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
            MiniHUD.LOGGER.error("OverlayRendererSlimeChunks#renderOutlines(): Exception; {}", err.getMessage());
        }

//        matrices.pop();
        profiler.pop();
    }

    @Override
    public void reset()
    {
        super.reset();
        this.slimeChunks.clear();
        this.wasSeedKnown = false;
        this.seed = -1L;
        this.topY = 40;
        this.hasData = false;
    }

    private void calculateChunks(Entity entity, MinecraftClient mc)
    {
        HudDataManager data = HudDataManager.getInstance();
        World world = entity.getEntityWorld();
        final int centerX = MathHelper.floor(entity.getX()) >> 4;
        final int centerZ = MathHelper.floor(entity.getZ()) >> 4;
        BlockPos.Mutable pos1 = new BlockPos.Mutable();
        BlockPos.Mutable pos2 = new BlockPos.Mutable();

        this.topY = this.overlayTopY;
        this.wasSeedKnown = data.isWorldSeedKnown(world);
        this.seed = data.getWorldSeed(world);
        this.hasData = false;
//        this.slimeChunks.clear();

        if (this.wasSeedKnown)
        {
            int r = MathHelper.clamp(Configs.Generic.SLIME_CHUNK_OVERLAY_RADIUS.getIntegerValue(), -1, 40);

            if (r == -1)
            {
                r = mc.options.getViewDistance().getValue();
            }

            int minY = world != null ? world.getBottomY() : -64;
            int topY = (int) Math.floor(this.topY);
            int count = 0;

            for (int xOff = -r; xOff <= r; xOff++)
            {
                for (int zOff = -r; zOff <= r; zOff++)
                {
                    int cx = centerX + xOff;
                    int cz = centerZ + zOff;

                    if (MiscUtils.canSlimeSpawnInChunk(cx, cz, this.seed))
                    {
                        pos1.set( cx << 4,       minY,  cz << 4      );
                        pos2.set((cx << 4) + 15, topY, (cz << 4) + 15);

                        Box bb = Box.enclosing(pos1, pos2);

                        if (!this.slimeChunks.contains(bb))
                        {
                            this.slimeChunks.add(bb);
                        }

                        count++;
                    }
                }
            }

            if (count > 0)
            {
                this.hasData = true;
            }
        }
    }

    @Override
    public String getSaveId()
    {
        return "slime_chunks";
    }

    @Override
    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();
        obj.add("y_top", new JsonPrimitive(this.overlayTopY));
        return obj;
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        this.overlayTopY = JsonUtils.getFloat(obj, "y_top");
    }
}
