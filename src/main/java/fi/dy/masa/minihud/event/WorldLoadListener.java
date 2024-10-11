package fi.dy.masa.minihud.event;

import javax.annotation.Nullable;
import java.io.File;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import fi.dy.masa.malilib.interfaces.IWorldLoadListener;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.data.DebugDataManager;
import fi.dy.masa.minihud.data.EntitiesDataManager;
import fi.dy.masa.minihud.data.HudDataManager;
import fi.dy.masa.minihud.renderer.OverlayRenderer;
import fi.dy.masa.minihud.renderer.RenderContainer;
import fi.dy.masa.minihud.renderer.shapes.ShapeManager;
import fi.dy.masa.minihud.util.DataStorage;

public class WorldLoadListener implements IWorldLoadListener
{
    @Override
    public void onWorldLoadPre(@Nullable ClientWorld worldBefore, @Nullable ClientWorld worldAfter, MinecraftClient mc)
    {
        // Save the settings before the integrated server gets shut down
        if (worldBefore != null)
        {
            this.writeDataPerDimension();

            // Quitting to main menu
            if (worldAfter == null)
            {
                this.writeDataGlobal();
            }
        }
        if (worldAfter != null)
        {
            DataStorage.getInstance().onWorldPre();
            HudDataManager.getInstance().onWorldPre();
            EntitiesDataManager.getInstance().onWorldPre();
            DebugDataManager.getInstance().onWorldPre();
        }
    }

    @Override
    public void onWorldLoadPost(@Nullable ClientWorld worldBefore, @Nullable ClientWorld worldAfter, MinecraftClient mc)
    {
        // Clear the cached data
        DataStorage.getInstance().reset(worldAfter == null);
        HudDataManager.getInstance().reset(worldAfter == null);
        EntitiesDataManager.getInstance().reset(worldAfter == null);
        DebugDataManager.getInstance().reset(worldAfter == null);

        // Logging in to a world or changing dimensions or respawning
        if (worldAfter != null)
        {
            // Logging in to a world, load the stored data
            if (worldBefore == null)
            {
                this.readStoredDataGlobal();
            }

            this.readStoredDataPerDimension();
            OverlayRenderer.resetRenderTimeout();
            DataStorage.getInstance().onWorldJoin();
            DataStorage.getInstance().setWorldRegistryManager(worldAfter.getRegistryManager());
            HudDataManager.getInstance().onWorldJoin();
            EntitiesDataManager.getInstance().onWorldJoin();
            DebugDataManager.getInstance().onWorldJoin();
        }
    }

    private void writeDataPerDimension()
    {
        File file = getCurrentStorageFile(false);
        JsonObject root = new JsonObject();

        root.add("data_storage", DataStorage.getInstance().toJson());
        root.add("hud_data", HudDataManager.getInstance().toJson());
        root.add("entities", EntitiesDataManager.getInstance().toJson());
        root.add("shapes", ShapeManager.INSTANCE.toJson());

        JsonUtils.writeJsonToFile(root, file);
    }

    private void writeDataGlobal()
    {
        File file = getCurrentStorageFile(true);
        JsonObject root = new JsonObject();

        root.add("renderers", RenderContainer.INSTANCE.toJson());

        JsonUtils.writeJsonToFile(root, file);
    }

    private void readStoredDataPerDimension()
    {
        // Per-dimension file
        File file = getCurrentStorageFile(false);
        JsonElement element = JsonUtils.parseJsonFile(file);

        if (element != null && element.isJsonObject())
        {
            JsonObject root = element.getAsJsonObject();

            if (JsonUtils.hasObject(root, "shapes"))
            {
                ShapeManager.INSTANCE.fromJson(JsonUtils.getNestedObject(root, "shapes", false));
            }

            if (JsonUtils.hasObject(root, "data_storage"))
            {
                DataStorage.getInstance().fromJson(JsonUtils.getNestedObject(root, "data_storage", false));
            }

            if (JsonUtils.hasObject(root, "hud_data"))
            {
                HudDataManager.getInstance().fromJson(JsonUtils.getNestedObject(root, "hud_data", false));
            }

            if (JsonUtils.hasObject(root, "entities"))
            {
                EntitiesDataManager.getInstance().fromJson(JsonUtils.getNestedObject(root, "entities", false));
            }
        }
    }

    private void readStoredDataGlobal()
    {
        // Global file
        File file = getCurrentStorageFile(true);
        JsonElement element = JsonUtils.parseJsonFile(file);

        if (element != null && element.isJsonObject())
        {
            JsonObject root = element.getAsJsonObject();

            if (JsonUtils.hasObject(root, "renderers"))
            {
                RenderContainer.INSTANCE.fromJson(JsonUtils.getNestedObject(root, "renderers", false));
            }
        }
    }

    public static File getCurrentConfigDirectory()
    {
        return new File(FileUtils.getConfigDirectory(), Reference.MOD_ID);
    }

    private static File getCurrentStorageFile(boolean globalData)
    {
        File dir = getCurrentConfigDirectory();

        if (dir.exists() == false && dir.mkdirs() == false)
        {
            MiniHUD.logger.warn("Failed to create the config directory '{}'", dir.getAbsolutePath());
        }

        return new File(dir, StringUtils.getStorageFileName(globalData, "", ".json", Reference.MOD_ID + "_default"));
    }
}
