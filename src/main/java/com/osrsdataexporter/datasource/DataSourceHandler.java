package com.osrsdataexporter.datasource;

import com.osrsdataexporter.OsrsDataExporterConfig;
import com.osrsdataexporter.model.AccountContext;
import com.osrsdataexporter.model.DataType;
import com.osrsdataexporter.model.ExportPayload;
import com.osrsdataexporter.model.ExportRecord;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

/**
 * Abstract base for data source handlers. Each handler encapsulates
 * the config check, debounce state, and snapshot logic for a single data type.
 *
 * @param <T> the specific {@link ExportRecord} subtype this handler produces
 */
@Slf4j
public abstract class DataSourceHandler<T extends ExportRecord>
{
	/** Debounce delay for item container changes (bank, inventory, group storage). */
	protected static final long ITEM_DEBOUNCE_DELAY_MS = 2000;

	/** Debounce delay for skill changes — longer because XP ticks fire rapidly. */
	protected static final long SKILLS_DEBOUNCE_DELAY_MS = 5000;

	protected final Client client;
	protected final OsrsDataExporterConfig config;
	private final DataType dataType;
	private final long debounceDelayMs;
	private ScheduledFuture<?> pendingExport;

	protected DataSourceHandler(Client client, OsrsDataExporterConfig config, DataType dataType, long debounceDelayMs)
	{
		this.client = client;
		this.config = config;
		this.dataType = dataType;
		this.debounceDelayMs = debounceDelayMs;
	}

	/**
	 * Returns the data type this handler produces.
	 */
	public DataType getDataType()
	{
		return dataType;
	}

	/**
	 * Whether this data source is currently enabled via plugin config.
	 */
	public abstract boolean isEnabled();

	/**
	 * Returns {@code true} if this handler can process the given event.
	 * Each subclass checks the event type and any routing criteria
	 * (e.g. container ID for item events).
	 */
	public abstract boolean canHandle(Object event);

	/**
	 * Processes an event this handler has claimed via {@link #canHandle(Object)}.
	 * Checks config, snapshots data, and schedules a debounced export.
	 *
	 * @param event   the RuneLite event that triggered this handler
	 * @param account the current player's account context
	 * @param executor   the background executor for scheduling exports
	 * @param dispatcher callback to dispatch the payload to exporters
	 */
	public abstract void handleEvent(
		Object event,
		AccountContext account,
		ScheduledExecutorService executor,
		Consumer<ExportPayload<? extends ExportRecord>> dispatcher);

	/**
	 * Schedules a debounced export, cancelling any previously pending one.
	 *
	 * @param payload    the export payload to dispatch
	 * @param executor   the background executor
	 * @param dispatcher callback to dispatch the payload to exporters
	 */
	protected final void scheduleExport(
		ExportPayload<T> payload,
		ScheduledExecutorService executor,
		Consumer<ExportPayload<? extends ExportRecord>> dispatcher)
	{
		if (pendingExport != null)
		{
			pendingExport.cancel(false);
		}

		pendingExport = executor.schedule(
			() -> dispatcher.accept(payload),
			debounceDelayMs,
			TimeUnit.MILLISECONDS
		);

		log.debug("{} change detected, export scheduled (debounce {}ms)", dataType.getIdentifier(), debounceDelayMs);
	}

	/**
	 * Cancels any pending debounced export.
	 */
	public void shutdown()
	{
		if (pendingExport != null)
		{
			pendingExport.cancel(false);
			pendingExport = null;
		}
	}
}
