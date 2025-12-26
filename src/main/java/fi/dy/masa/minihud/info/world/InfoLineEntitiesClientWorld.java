package fi.dy.masa.minihud.info.world;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.info.InfoLine;
import fi.dy.masa.minihud.info.InfoLineContext;
import fi.dy.masa.minihud.mixin.world.IMixinServerWorld;
import fi.dy.masa.minihud.util.IServerEntityManager;

public class InfoLineEntitiesClientWorld extends InfoLine
{
    private static final String ENT_KEY = Reference.MOD_ID+".info_line.entities_client_world";

    public InfoLineEntitiesClientWorld(InfoToggle type)
    {
        super(type);
    }

    public InfoLineEntitiesClientWorld()
    {
        this(InfoToggle.ENTITIES_CLIENT_WORLD);
    }

    @Override
    public boolean succeededType() { return false; }

    @Override
    public List<Entry> parse(@Nonnull InfoLineContext ctx)
    {
        if (this.getClientWorld() == null)
        {
            return null;
        }

        return this.parseWorld(ctx.world() == null ? this.getClientWorld() : ctx.world());
    }

    @Override
    public List<Entry> parseWorld(@Nonnull World world)
    {
        List<Entry> list = new ArrayList<>();
	    int countClient = ((ClientWorld) this.getClientWorld()).getRegularEntityCount();

	    if (this.mc().isIntegratedServerRunning() &&
		    world instanceof ServerWorld serverWorld)
	    {
		    IServerEntityManager manager = (IServerEntityManager) ((IMixinServerWorld) serverWorld).minihud_getEntityManager();
		    int indexSize = manager.minihud$getIndexSize();

		    list.add(this.translate(ENT_KEY+".server", countClient, indexSize));

		    return list;
	    }

	    list.add(this.translate(ENT_KEY, countClient));
        return list;
    }
}
