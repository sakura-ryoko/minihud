package fi.dy.masa.minihud.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.debug.DebugHudEntryVisibility;
import net.minecraft.client.gui.hud.debug.DebugHudProfile;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.Identifier;

import fi.dy.masa.malilib.network.ClientPlayHandler;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.mixin.debug.IMixinClientPlayNetworkHandler;
import fi.dy.masa.minihud.network.ServuxDebugHandler;
import fi.dy.masa.minihud.network.ServuxDebugPacket;
import fi.dy.masa.minihud.util.DataStorage;
import fi.dy.masa.minihud.util.DebugRenderType;

public class DebugDataManager
{
    private static final DebugDataManager INSTANCE = new DebugDataManager();
//    private final static ServuxDebugHandler<ServuxDebugPacket.Payload> HANDLER = ServuxDebugHandler.getInstance();

	private final MinecraftClient mc;
    private boolean servuxServer;
    private boolean hasInValidServux;
    private String servuxVersion;
    private boolean shouldRegisterDebugService;

	private List<DebugRenderType> enabledRenderers;

    public DebugDataManager()
    {
        this.servuxServer = false;
        this.hasInValidServux = false;
        this.servuxVersion = "";
		this.mc = MinecraftClient.getInstance();
		this.enabledRenderers = new ArrayList<>();
    }

    public static DebugDataManager getInstance() { return INSTANCE; }

    public void onGameInit()
    {
//        ClientPlayHandler.getInstance().registerClientPlayHandler(HANDLER);
//        HANDLER.registerPlayPayload(ServuxDebugPacket.Payload.ID, ServuxDebugPacket.Payload.CODEC, IPluginClientPlayHandler.BOTH_CLIENT);
    }

//    public Identifier getNetworkChannel() {return ServuxDebugHandler.CHANNEL_ID;}
//
//    public IPluginClientPlayHandler<ServuxDebugPacket.Payload> getNetworkHandler() {return HANDLER;}

    public void reset(boolean isLogout)
    {
        if (isLogout)
        {
            MiniHUD.debugLog("DebugDataManager#reset() - log-out");
//            HANDLER.reset(this.getNetworkChannel());
//            HANDLER.resetFailures(this.getNetworkChannel());

			// reset config
            this.servuxServer = false;
            this.hasInValidServux = false;
            this.servuxVersion = "";
        }

	    this.onConfigSync();
    }

    public void onWorldPre()
    {
        if (!DataStorage.getInstance().hasIntegratedServer())
        {
//            HANDLER.registerPlayReceiver(ServuxDebugPacket.Payload.ID, HANDLER::receivePlayPayload);
        }

	    this.onConfigSync();
    }

    public void onWorldJoin()
    {
        MiniHUD.debugLog("DebugDataManager#onWorldJoin()");

        if (!DataStorage.getInstance().hasIntegratedServer())
        {
            if (RendererToggle.DEBUG_DATA_MAIN_TOGGLE.getBooleanValue())
            {
                this.registerDebugService();
            }
            else
            {
                this.unregisterDebugService();
            }
        }
    }

	public boolean isF3Enabled()
	{
		return this.mc.debugHudEntryList.isF3Enabled();
	}

	public boolean isDebugAlwaysEnabled(Identifier type)
	{
		return this.mc.debugHudEntryList.getVisibility(type) == DebugHudEntryVisibility.ALWAYS_ON;
	}

	public boolean toggleDebugAlwaysEnabled(Identifier type)
	{
		if (this.isDebugAlwaysEnabled(type))
		{
			this.mc.debugHudEntryList.setEntryVisibility(type, DebugHudEntryVisibility.NEVER);
			return false;
		}
		else
		{
			this.mc.debugHudEntryList.setEntryVisibility(type, DebugHudEntryVisibility.ALWAYS_ON);
			return true;
		}
	}

	public boolean setDebugAlwaysEnabled(Identifier type, boolean enabled)
	{
		if (!enabled)
		{
			this.mc.debugHudEntryList.setEntryVisibility(type, DebugHudEntryVisibility.NEVER);
			return false;
		}
		else if (enabled)
		{
			this.mc.debugHudEntryList.setEntryVisibility(type, DebugHudEntryVisibility.ALWAYS_ON);
			return true;
		}

		return this.isDebugAlwaysEnabled(type);
	}

	/**
	 * This patches the 'shouldShowDebugHud' so that the Chunk Borders, etc do not effect the MiniHUD Info Lines.
	 * @return (True|False)
	 */
	public boolean shouldShowDebugHudFix()
	{
		DebugHudProfile profile = this.mc.debugHudEntryList;
		Collection<Identifier> list = profile.getVisibleEntries();

		return (profile.isF3Enabled() || !this.checkVisibleEntries(list))
				&& (!this.mc.options.hudHidden || this.mc.currentScreen != null);
	}

	private boolean checkVisibleEntries(Collection<Identifier> list)
	{
		if (list.isEmpty()) return true;
		for (Identifier entry : list)
		{
			// Whitelist the Debug Renderer ones (see DebugHudEntries)
			switch (entry.getPath())
			{
				case "entity_hitboxes" -> { continue; }
				case "chunk_borders" -> { continue; }
				case "3d_crosshair" -> { continue; }
				case "chunk_section_paths" -> { continue; }
				case "chunk_section_octree" -> { continue; }
				case "chunk_section_visibility" -> { continue; }
				default -> { return false; }
			}
		}

		// Means it's safe, as if it were empty
		return true;
	}

	public List<DebugRenderType> getEnabledRenderers()
	{
		return this.enabledRenderers;
	}

	public boolean isDebugRendererEnabled(DebugRenderType type)
	{
		return this.enabledRenderers.contains(type);
	}

	public void setDebugRenderer(DebugRenderType type, boolean toggle)
	{
		MiniHUD.debugLog("DebugDataManager#setDebugRenderer: type [{}] -> [{}]", type.getName(), toggle);
		type.toggleSharedConstant(toggle);

		if (toggle)
		{
			this.enabledRenderers.add(type);
		}
		else
		{
			this.enabledRenderers.remove(type);
		}
	}

	public void toggleDebugRenderer(DebugRenderType type, boolean toggle)
	{
		this.setDebugRenderer(type, toggle);
		type.getCallback().setBooleanValue(toggle);
	}

	public boolean isDebugRenderingEnabled()
	{
		boolean result = this.enabledRenderers.contains(DebugRenderType.DEBUG_ENABLED);
		boolean config = RendererToggle.DEBUG_DATA_MAIN_TOGGLE.getBooleanValue();

		if (config != result)
		{
			this.toggleDebugRendering(config);
			result = config;
		}

		return result;
	}

	public void toggleDebugRendering(boolean toggle)
	{
		this.setDebugRenderer(DebugRenderType.DEBUG_ENABLED, toggle);
		this.reloadDebugRenderer();
	}

	public void onConfigSync()
	{
		boolean changed = false;

		for (DebugRenderType entry : DebugRenderType.VALUES)
		{
			boolean config = entry.getCallback().getBooleanValue();
			boolean enabled = this.isDebugRendererEnabled(entry);

			if (config != enabled)
			{
				this.setDebugRenderer(entry, config);
				changed = true;
			}
		}

		if (changed)
		{
			this.reloadDebugRenderer();
		}
	}

	public void reloadDebugRenderer()
	{
		if (this.mc.getNetworkHandler() != null)
		{
			((IMixinClientPlayNetworkHandler) this.mc.getNetworkHandler())
					.minihud_getDebugManager().clearAllSubscriptions();
		}

		this.mc.worldRenderer.debugRenderer.initRenderers();
	}

	public void setIsServuxServer()
    {
        this.servuxServer = true;
        if (this.hasInValidServux)
        {
            this.hasInValidServux = false;
        }
    }

    public void setServuxVersion(String ver)
    {
        if (ver != null && !ver.isEmpty())
        {
            this.servuxVersion = ver;
        }
        else
        {
            this.servuxVersion = "unknown";
        }
    }

    public String getServuxVersion()
    {
        if (this.hasServuxServer())
        {
            return this.servuxVersion;
        }

        return "not_connected";
    }

    public boolean hasServuxServer() { return this.servuxServer; }

    public void registerDebugService()
    {
        this.shouldRegisterDebugService = true;

        if (!this.hasServuxServer() && !DataStorage.getInstance().hasIntegratedServer() && !this.hasInValidServux)
        {
//            if (HANDLER.isPlayRegistered(this.getNetworkChannel()))
//            {
//                MiniHUD.debugLog("DebugDataManager#registerDebugService(): sending DEBUG_SERVICE_REGISTER to Servux");
//
//                NbtCompound nbt = new NbtCompound();
//                nbt.putInt("version", ServuxDebugPacket.PROTOCOL_VERSION);
//	            nbt.putString("minihud", Reference.MOD_STRING);
//
//                HANDLER.encodeClientData(ServuxDebugPacket.DebugServiceRegister(nbt));
//            }
        }
        else
        {
            this.shouldRegisterDebugService = false;
        }
    }

    public void requestMetadata()
    {
        if (this.shouldRegisterDebugService)
        {
            if (!this.hasServuxServer() && !DataStorage.getInstance().hasIntegratedServer() && !this.hasInValidServux)
            {
//                if (HANDLER.isPlayRegistered(this.getNetworkChannel()))
//                {
//                    MiniHUD.debugLog("DebugDataManager#requestMetadata(): sending REQUEST_METADATA to Servux");
//
//                    NbtCompound nbt = new NbtCompound();
//	                nbt.putInt("version", ServuxDebugPacket.PROTOCOL_VERSION);
//	                nbt.putString("minihud", Reference.MOD_STRING);
//					nbt.put("enabledRenderers", this.toNbtList());
//
//                    HANDLER.encodeClientData(ServuxDebugPacket.MetadataRequest(nbt));
//                }
            }
        }
    }

	public void updateMetadata()
	{
		if (this.shouldRegisterDebugService)
		{
			if (this.hasServuxServer() && !DataStorage.getInstance().hasIntegratedServer() && !this.hasInValidServux)
			{
//				if (HANDLER.isPlayRegistered(this.getNetworkChannel()))
//				{
//					MiniHUD.debugLog("DebugDataManager#requestMetadata(): sending UPDATE_METADATA to Servux");
//
//					NbtCompound nbt = new NbtCompound();
//					nbt.putInt("version", ServuxDebugPacket.PROTOCOL_VERSION);
//					nbt.putString("minihud", Reference.MOD_STRING);
//					nbt.put("enabledRenderers", this.toNbtList());
//
//					HANDLER.encodeClientData(ServuxDebugPacket.MetadataUpdate(nbt));
//				}
			}
		}
	}

	private NbtList toNbtList()
	{
		NbtList list = new NbtList();

		this.enabledRenderers.forEach(
				(type) ->
				{
					list.add(DebugRenderType.CODEC.encodeStart(NbtOps.INSTANCE, type).getOrThrow());
				}
		);

		return list;
	}

	private void fromNbtList(@Nonnull NbtList list)
	{
		if (list.isEmpty())
		{
			return;
		}

		List<DebugRenderType> received = new ArrayList<>();

		for (NbtElement entry : list)
		{
			try
			{
				DebugRenderType type = DebugRenderType.CODEC.parse(NbtOps.INSTANCE, entry).getOrThrow();

				if (type != null)
				{
					received.add(type);

					if (!this.isDebugRenderingEnabled())
					{
						this.toggleDebugRendering(true);
					}

					this.toggleDebugRenderer(type, true);
				}
			}
			catch (Exception err)
			{
				MiniHUD.LOGGER.warn("debug_data#fromNbtList: Exception decoding Nbt List; {}", err.getLocalizedMessage());
			}
		}
	}

	/**
	 * Resync stored Types
	 * @param list
	 */
	private void resyncFromList(List<DebugRenderType> list)
	{
		List<DebugRenderType> disable = new ArrayList<>();

		this.enabledRenderers.forEach(
				(type) ->
				{
					if (!list.contains(type))
					{
						disable.add(type);
					}
				}
		);

		// Remove entries not in received list
		for (DebugRenderType entry : disable)
		{
			this.toggleDebugRenderer(entry, false);
		}
	}

    public boolean receiveMetadata(NbtCompound data)
    {
        if (!this.hasServuxServer() && !DataStorage.getInstance().hasIntegratedServer() &&
            this.shouldRegisterDebugService)
        {
            MiniHUD.debugLog("DebugDataManager#receiveMetadata(): received METADATA from Servux");

//            if (data.getInt("version", -1) != ServuxDebugPacket.PROTOCOL_VERSION)
//            {
//                MiniHUD.LOGGER.warn("debugDataChannel: Mis-matched protocol version!");
//            }
//
//            this.setServuxVersion(data.getString("servux", "?"));
//            this.setIsServuxServer();
//
//            if (RendererToggle.DEBUG_DATA_MAIN_TOGGLE.getBooleanValue())
//            {
//                this.shouldRegisterDebugService = true;
//
//                NbtCompound nbt = new NbtCompound();
//	            nbt.putInt("version", ServuxDebugPacket.PROTOCOL_VERSION);
//	            nbt.putString("minihud", Reference.MOD_STRING);
//	            nbt.put("enabledRenderers", this.toNbtList());
//
//	            HANDLER.encodeClientData(ServuxDebugPacket.MetadataConfirm(nbt));
//                return true;
//            }
//            else
//            {
//                this.unregisterDebugService();
//            }
        }

        return false;
    }

    public void unregisterDebugService()
    {
        if (this.hasServuxServer() || !RendererToggle.DEBUG_DATA_MAIN_TOGGLE.getBooleanValue())
        {
            this.servuxServer = false;
            if (!this.hasInValidServux)
            {
                MiniHUD.debugLog("DebugDataManager#unregisterDebugService(): for {}", this.servuxVersion != null ? this.servuxVersion : "<unknown>");

//                HANDLER.encodeClientData(ServuxDebugPacket.DebugServiceUnregister(new NbtCompound()));
//                HANDLER.reset(HANDLER.getPayloadChannel());
            }
        }
        this.shouldRegisterDebugService = false;
    }

    public void onPacketFailure()
    {
        // Define how to handle multiple sendPayload failures
        this.shouldRegisterDebugService = false;
        this.servuxServer = false;
        this.hasInValidServux = true;
    }

    public boolean isEnabled()
    {
        return RendererToggle.DEBUG_DATA_MAIN_TOGGLE.getBooleanValue() &&
                DataStorage.getInstance().hasIntegratedServer();
    }
}
