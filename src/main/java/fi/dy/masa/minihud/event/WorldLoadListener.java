package fi.dy.masa.minihud.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import fi.dy.masa.minihud.renderer.OverlayRendererVillagerInfo;
import fi.dy.masa.minihud.renderer.RenderContainer;
import fi.dy.masa.minihud.renderer.shapes.ShapeManager;
import fi.dy.masa.minihud.renderer.worker.WorkerDaemonHandler;
import fi.dy.masa.minihud.util.DataStorage;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WorldLoadListener implements IWorldLoadListener
{
    @Override
    public void onWorldLoadImmutable(RegistryAccess.Frozen immutable)
    {
        DataStorage.getInstance().setWorldRegistryManager(immutable);
    }

    @Override
    public void onWorldLoadPre(@Nullable ClientLevel worldBefore, @Nullable ClientLevel worldAfter, Minecraft mc)
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
    public void onWorldLoadPost(@Nullable ClientLevel worldBefore, @Nullable ClientLevel worldAfter, Minecraft mc)
    {
        // Clear the cached data
        DataStorage.getInstance().reset(worldAfter == null);
        HudDataManager.getInstance().reset(worldAfter == null);
        EntitiesDataManager.getInstance().reset(worldAfter == null);
        OverlayRendererVillagerInfo.INSTANCE.reset(worldAfter == null);
        DebugDataManager.getInstance().reset(worldAfter == null);
        OverlayRenderer.reset();

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
//            DataStorage.getInstance().setWorldRegistryManager(worldAfter.registryAccess());
            HudDataManager.getInstance().onWorldJoin();
            EntitiesDataManager.getInstance().onWorldJoin();
            DebugDataManager.getInstance().onWorldJoin();
            WorkerDaemonHandler.INSTANCE.start();
        }
        else
        {
            WorkerDaemonHandler.INSTANCE.endAll();
        }
    }

    private void writeDataPerDimension()
    {
        Path file = getCurrentStorageFile(false);
        JsonObject root = new JsonObject();

        JsonObject entry = DataStorage.getInstance().toJson();

        if (!entry.isEmpty())
        {
            root.add("data_storage", entry);
        }

        entry = ShapeManager.INSTANCE.toJson();

        if (!entry.isEmpty())
        {
            root.add("shapes", entry);
        }

        // Delete file if the data is "empty"
        if (root.isEmpty())
        {
            if (Files.exists(file))
            {
                try
                {
                    Files.delete(file);
                }
                catch (IOException e)
                {
                    MiniHUD.LOGGER.warn("writeDataPerDimension: Failed to delete file '{}'; {}", file, e.getLocalizedMessage());
                }
            }

            return;
        }

        JsonUtils.writeJsonToFileAsPath(root, file);
    }

    private void writeDataGlobal()
    {
        Path file = getCurrentStorageFile(true);
        JsonObject root = new JsonObject();

        JsonObject entry = RenderContainer.INSTANCE.toJson();

        if (!entry.isEmpty())
        {
            root.add("renderers", entry);
        }

        entry = HudDataManager.getInstance().toJson();

        if (!entry.isEmpty())
        {
            root.add("hud_data", entry);
        }

        // Delete file if the data is "empty"
        if (root.isEmpty())
        {
            if (Files.exists(file))
            {
                try
                {
                    Files.delete(file);
                }
                catch (IOException e)
                {
                    MiniHUD.LOGGER.warn("writeDataGlobal: Failed to delete file '{}'; {}", file, e.getLocalizedMessage());
                }
            }

            return;
        }

        JsonUtils.writeJsonToFileAsPath(root, file);
    }

    private void readStoredDataPerDimension()
    {
        // Per-dimension file
        Path file = getCurrentStorageFile(false);
        JsonElement element = JsonUtils.parseJsonFileAsPath(file);

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

            // Backwards compat
            if (JsonUtils.hasObject(root, "hud_data"))
            {
                HudDataManager.getInstance().fromJson(JsonUtils.getNestedObject(root, "hud_data", false));
            }
        }
    }

    private void readStoredDataGlobal()
    {
        // Global file
        Path file = getCurrentStorageFile(true);
        JsonElement element = JsonUtils.parseJsonFileAsPath(file);

        if (element != null && element.isJsonObject())
        {
            JsonObject root = element.getAsJsonObject();

            if (JsonUtils.hasObject(root, "renderers"))
            {
                RenderContainer.INSTANCE.fromJson(JsonUtils.getNestedObject(root, "renderers", false));
            }

            if (JsonUtils.hasObject(root, "hud_data"))
            {
                HudDataManager.getInstance().fromJson(JsonUtils.getNestedObject(root, "hud_data", false));
            }
        }
    }

    public static Path getCurrentConfigDirectory()
    {
        return FileUtils.getConfigDirectoryAsPath().resolve(Reference.MOD_ID);
    }

    private static Path getCurrentStorageFile(boolean globalData)
    {
        Path saveDir = getCurrentConfigDirectory();

        if (!Files.exists(saveDir))
        {
            FileUtils.createDirectoriesIfMissing(saveDir);
            //MiniHUD.debugLog("getCurrentStorageFile(): Creating directory '{}'.", saveDir.toAbsolutePath());
        }

        if (!Files.isDirectory(saveDir))
        {
            MiniHUD.LOGGER.warn("getCurrentStorageFile(): Failed to create the config directory '{}'", saveDir.toAbsolutePath());
        }

        return saveDir.resolve(StringUtils.getStorageFileName(globalData, "", ".json", Reference.MOD_ID + "_default"));
    }
}
