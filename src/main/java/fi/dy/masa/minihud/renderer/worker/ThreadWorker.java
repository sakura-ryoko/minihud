package fi.dy.masa.minihud.renderer.worker;

import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.util.DataStorage;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;

public class ThreadWorker implements Runnable
{
    private boolean shouldRun = true;

    @Override
    public void run()
    {
        while (this.shouldRun)
        {
            try
            {
                ChunkTask task = DataStorage.getInstance().getNextTask();

                if (task != null)
                {
                    this.processTask(task.task);
                }
            }
            catch (InterruptedException e)
            {
                MiniHUD.LOGGER.debug("Stopping worker thread due to an interrupt");
                return;
            }
            catch (Throwable throwable)
            {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "MiniHUD worker thread");
                Minecraft.getInstance().delayCrashRaw(Minecraft.getInstance().fillReport(crashreport));
                return;
            }
        }
    }

    public void stopThread()
    {
        this.shouldRun = false;
    }

    protected void processTask(final Runnable task) throws InterruptedException
    {
        task.run();
    }
}
