package fi.dy.masa.minihud.info;

import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.mixin.render.IGlTimer;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlTimer;
import net.minecraft.client.gui.hud.debug.DebugHudEntries;
import net.minecraft.util.Util;

public class InfoLineProfiler
{
	public static final InfoLineProfiler INSTANCE = new InfoLineProfiler();
	@Nullable
	private GlTimer.Query glQuery;
	private long metricsDuration;
	private long lastMetricsTime;
	private double gpuUtilization;
	private boolean measurementEnable = false;

	private InfoLineProfiler() { }

	@ApiStatus.Internal
	private boolean shouldGPUProfilerStop()
	{
		return MinecraftClient.getInstance().debugHudEntryList.isEntryVisible(DebugHudEntries.GPU_UTILIZATION) || !InfoToggle.GPU.getBooleanValue();
	}

	private int getGPUQueryId()
	{
        return ((IGlTimer) GlTimer.getInstance()).minihud_getQueryId();
	}

	@ApiStatus.Internal
	public void GPUStage1()
	{
		if (this.shouldGPUProfilerStop())
		{
			this.stopGPUProfiler();
			return;
		}

		if ((this.glQuery == null || this.glQuery.isResultAvailable()) && this.getGPUQueryId() == 0)
		{
			this.measurementEnable = true;
			GlTimer.getInstance().beginProfile();
		}
		else
		{
			this.stopGPUProfiler();
		}
	}

	@ApiStatus.Internal
	public void GPUStage2()
	{
		if (this.shouldGPUProfilerStop())
		{
			this.stopGPUProfiler();
			return;
		}

		if (this.measurementEnable && this.getGPUQueryId() != 0)
		{
			GlTimer.getInstance().endProfile();
		}
		else
		{
			this.stopGPUProfiler();
		}
	}

	@ApiStatus.Internal
	public void GPUStage3()
	{
		if (this.shouldGPUProfilerStop())
		{
			this.stopGPUProfiler();
			return;
		}

		final long nanoTime = Util.getMeasuringTimeNano();

		if (this.measurementEnable)
		{
			this.metricsDuration = nanoTime - this.lastMetricsTime;
		}

		this.lastMetricsTime = nanoTime;
	}

	@ApiStatus.Internal
	public void GPUStage4()
	{
		if (this.shouldGPUProfilerStop())
		{
			this.stopGPUProfiler();
			return;
		}

		if (this.measurementEnable)
		{
			if (this.glQuery != null && this.glQuery.isResultAvailable())
			{
				this.gpuUtilization = this.glQuery.queryResult() * 100.0 / this.metricsDuration;
			}
		}
	}

	@ApiStatus.Internal
	private void stopGPUProfiler()
	{
		if (this.glQuery != null)
		{
			this.glQuery.close();
			this.glQuery = null;
		}

		this.gpuUtilization = 0.0;
		this.metricsDuration = 0L;
		this.lastMetricsTime = 0L;
		this.measurementEnable = false;
	}

	public double getGpuUtilization()
	{
		return this.gpuUtilization;
	}
}
