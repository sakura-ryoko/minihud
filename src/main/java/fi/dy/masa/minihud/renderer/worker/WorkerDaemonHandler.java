package fi.dy.masa.minihud.renderer.worker;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import com.google.common.collect.Queues;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.client.Minecraft;

import fi.dy.masa.malilib.interfaces.IThreadDaemonHandler;
import fi.dy.masa.malilib.util.MathUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.data.DebugDataManager;
import fi.dy.masa.minihud.data.EntitiesDataManager;
import fi.dy.masa.minihud.data.HudDataManager;
import fi.dy.masa.minihud.util.DataStorage;

// New Thread Worker system utilizing the MaLiLib Interface.
public class WorkerDaemonHandler implements IThreadDaemonHandler<AbstractWorkerTask<?>>
{
	public static final WorkerDaemonHandler INSTANCE = new WorkerDaemonHandler();
	private static final float TASK_INTERVAL = 3.0F;
	private final int threadCount = this.calculateMaxThreads();
	private final ConcurrentHashMap<String, Pair<Thread, WorkerDaemonExecutor>> threadMap = this.builder();
	private final PriorityBlockingQueue<AbstractWorkerTask<?>> queue = Queues.newPriorityBlockingQueue();
	private long lastTick;

	private int calculateMaxThreads()
	{
		// Don't use more than 1 / 4 of possible Platform threads for this; or MAX_PLATFORM_THREADS.
		return MathUtils.clamp((Runtime.getRuntime().availableProcessors() / 4), 1, Reference.MAX_PLATFORM_THREADS);
	}

	private ConcurrentHashMap<String, Pair<Thread, WorkerDaemonExecutor>> builder()
	{
		ConcurrentHashMap<String, Pair<Thread, WorkerDaemonExecutor>> threads = new ConcurrentHashMap<>(this.threadCount, 0.9f, 1);
		String prefix = Reference.MOD_NAME+" Worker Thread ";

		for (int i = 0; i < this.threadCount; i++)
		{
			String name = prefix + (i+1);
			ThreadFactory FACTORY = Thread.ofPlatform().name(name).daemon(true).factory();
			WorkerDaemonExecutor executor = new WorkerDaemonExecutor();

			threads.put(name, Pair.of(FACTORY.newThread(executor), executor));
		}

		return threads;
	}

	private WorkerDaemonHandler()
	{
		this.lastTick = System.currentTimeMillis();
		this.start();
	}

	@Override
	public void start()
	{
		MiniHUD.LOGGER.info("Starting [{}] Worker Daemon threads", this.threadMap.size());

		synchronized (this.threadMap)
		{
			this.threadMap.forEach(
					(name, pair) ->
					{
						pair.getLeft().start();
						pair.getRight().start();
					}
			);
		}
	}

	@Override
	public void stop()
	{
		MiniHUD.debugLog("Stopping [{}] Worker Daemon threads", this.threadMap.size());

		synchronized (this.threadMap)
		{
			this.threadMap.forEach(
					(name, pair) ->
					{
						pair.getRight().stop();
						pair.getLeft().interrupt();
					}
			);
		}
	}

	@Override
	public synchronized void reset()
	{
		this.queue.clear();
	}

	@Override
	public synchronized void addTask(AbstractWorkerTask task)
	{
		if (this.queue.size() < 64000)
		{
			this.queue.add(task);
		}
	}

	@Override
	public synchronized AbstractWorkerTask<?> getNextTask()
	{
		return this.queue.poll();
	}

	@Override
	public long getTaskInterval()
	{
		return MathUtils.floor(TASK_INTERVAL * 1000L);
	}

	@Override
	public void onClientTick(Minecraft mc)
	{
		final long now = System.currentTimeMillis();

		if ((now - this.lastTick) > this.getTaskInterval())
		{
			this.ensureThreadSafety();
			this.lastTick = now;
		}
	}

	// TODO -- is this even necessary?
	private void ensureThreadSafety()
			throws RuntimeException
	{
		this.threadMap.forEach(
				(name, pair) ->
				{
					if (!pair.getLeft().isAlive() || pair.getLeft().isInterrupted())
					{
						String err = String.format("'%s' was killed [%s]", name, this.getThreadStatus(pair.getLeft()));
						this.reset();
						this.stop();

						DebugDataManager.getInstance().reset(true);
						EntitiesDataManager.getInstance().reset(true);
						HudDataManager.getInstance().reset(true);
						DataStorage.getInstance().reset(true);
						Configs.saveToFile();
						MiniHUD.LOGGER.fatal(err);

						throw new RuntimeException(err);
					}
				}
		);
	}

	private String getThreadStatus(Thread thread)
	{
		if (thread == null)
		{
			return "<>";
		}

		return "(" + thread.threadId() + ')'
				+ "/"
				+ thread.getState().name();
	}

	public void endAll()
	{
		this.reset();
		this.stop();
	}

	@Override
	public void close() throws Exception
	{
		this.endAll();
	}
}
