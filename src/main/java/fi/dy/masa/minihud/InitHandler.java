package fi.dy.masa.minihud;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.*;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.data.DebugDataManager;
import fi.dy.masa.minihud.data.EntitiesDataManager;
import fi.dy.masa.minihud.data.HudDataManager;
import fi.dy.masa.minihud.event.*;
import fi.dy.masa.minihud.gui.GuiConfigs;
import fi.dy.masa.minihud.hotkeys.KeyCallbacks;
import fi.dy.masa.minihud.renderer.OverlayRendererVillagerInfo;
import fi.dy.masa.minihud.renderer.worker.WorkerDaemonHandler;
import fi.dy.masa.minihud.util.DataStorage;

public class InitHandler implements IInitializationHandler
{
    @Override
    public void registerModHandlers()
    {
        ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, new Configs());
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(
                new ModInfo(Reference.MOD_ID, Reference.MOD_NAME, GuiConfigs::new)
        );

        DataStorage.getInstance().onGameInit();
        HudDataManager.getInstance().onGameInit();
        EntitiesDataManager.getInstance().onGameInit();
        DebugDataManager.getInstance().onGameInit();

        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerMouseInputHandler(InputHandler.getInstance());

        RenderHandler renderer = RenderHandler.getInstance();
        RenderEventHandler.getInstance().registerGameOverlayRenderer(renderer);
        RenderEventHandler.getInstance().registerTooltipLastRenderer(renderer);
        RenderEventHandler.getInstance().registerWorldLastRenderer(renderer);

        WorldLoadListener listener = new WorldLoadListener();
        WorldLoadHandler.getInstance().registerWorldLoadPreHandler(listener);
        WorldLoadHandler.getInstance().registerWorldLoadPostHandler(listener);

        ServerListener serverListener = new ServerListener();
        ServerHandler.getInstance().registerServerHandler(serverListener);

        TickHandler.getInstance().registerClientTickHandler(new ClientTickHandler());
        TickHandler.getInstance().registerClientTickHandler(EntitiesDataManager.getInstance());
        TickHandler.getInstance().registerClientTickHandler(OverlayRendererVillagerInfo.INSTANCE);
        TickHandler.getInstance().registerClientTickHandler(WorkerDaemonHandler.INSTANCE);

        KeyCallbacks.init();
    }
}
