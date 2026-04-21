package com.osrsdataexporter;

import com.google.gson.Gson;
import com.google.inject.Provides;
import com.osrsdataexporter.export.DataExporter;
import com.osrsdataexporter.export.DataExporterFactory;
import com.osrsdataexporter.model.BankRecord;
import com.osrsdataexporter.model.DataType;
import com.osrsdataexporter.model.ExportPayload;
import com.osrsdataexporter.model.ExportRecord;
import com.osrsdataexporter.model.InventoryRecord;
import com.osrsdataexporter.model.ItemEntry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.WorldType;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

/**
 * OSRS Data Exporter plugin.
 *
 * <p>Captures account data (bank and inventory contents) and exports it
 * to configurable targets via the adapter/factory pattern. All export I/O
 * is performed off the client thread to avoid any impact on game performance.</p>
 *
 * <p>Exports are smart-debounced: rapid item container changes (e.g.
 * depositing multiple items quickly) are coalesced into a single export
 * after a quiet period. Bank and inventory each have independent debounce timers.</p>
 */
@Slf4j
@PluginDescriptor(
	name = "OSRS Data Exporter",
	description = "Exports account data (bank, inventory, etc.) to external storage targets.",
	tags = {"bank", "inventory", "export", "data"}
)
public class OsrsDataExporterPlugin extends Plugin
{
	/**
	 * Debounce delay in milliseconds. After the last container change event,
	 * the export will wait this long before executing. If another change
	 * arrives within this window, the timer resets.
	 */
	private static final long DEBOUNCE_DELAY_MS = 2000;

	private static final String EXECUTOR_THREAD_NAME = "osrs-data-exporter";

	@Inject
	private Client client;

	@Inject
	private OsrsDataExporterConfig config;

	@Inject
	private Gson gson;

	private ScheduledExecutorService executor;
	private DataExporterFactory exporterFactory;

	/**
	 * Handle for the currently pending debounced bank export.
	 * Cancelled and replaced each time a new bank change arrives.
	 */
	private ScheduledFuture<?> pendingBankExport;

	/**
	 * Handle for the currently pending debounced inventory export.
	 * Cancelled and replaced each time a new inventory change arrives.
	 */
	private ScheduledFuture<?> pendingInventoryExport;

	@Override
	protected void startUp()
	{
		executor = Executors.newSingleThreadScheduledExecutor(r ->
		{
			Thread t = new Thread(r, EXECUTOR_THREAD_NAME);
			t.setDaemon(true);
			return t;
		});
		exporterFactory = new DataExporterFactory(config, gson);
		exporterFactory.init();
		log.info("OSRS Data Exporter started");
	}

	@Override
	protected void shutDown()
	{
		if (pendingBankExport != null)
		{
			pendingBankExport.cancel(false);
			pendingBankExport = null;
		}

		if (pendingInventoryExport != null)
		{
			pendingInventoryExport.cancel(false);
			pendingInventoryExport = null;
		}

		if (exporterFactory != null)
		{
			exporterFactory.shutdown();
			exporterFactory = null;
		}

		if (executor != null)
		{
			executor.shutdownNow();
			executor = null;
		}

		log.info("OSRS Data Exporter stopped");
	}

	@Provides
	OsrsDataExporterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OsrsDataExporterConfig.class);
	}

	/**
	 * Routes item container changes to the appropriate handler.
	 */
	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.BANK)
		{
			handleBankChange(event.getItemContainer());
		}
		else if (event.getContainerId() == InventoryID.INV)
		{
			handleInventoryChange(event.getItemContainer());
		}
	}

	/**
	 * Handles a bank container change by snapshotting data and scheduling
	 * a debounced export. Guards against disabled config, seasonal worlds,
	 * and unauthenticated sessions.
	 */
	private void handleBankChange(ItemContainer container)
	{
		if (!shouldExportBankData())
		{
			return;
		}

		long accountHash = client.getAccountHash();
		if (accountHash == -1)
		{
			return;
		}

		ExportPayload<BankRecord> payload = snapshotBankData(accountHash, container);
		pendingBankExport = scheduleDebouncedExport(pendingBankExport, payload, "Bank");
	}

	/**
	 * Handles an inventory container change by snapshotting data and scheduling
	 * a debounced export. Guards against disabled config, seasonal worlds,
	 * and unauthenticated sessions.
	 */
	private void handleInventoryChange(ItemContainer container)
	{
		if (!shouldExportInventoryData())
		{
			return;
		}

		long accountHash = client.getAccountHash();
		if (accountHash == -1)
		{
			return;
		}

		ExportPayload<InventoryRecord> payload = snapshotInventoryData(accountHash, container);
		pendingInventoryExport = scheduleDebouncedExport(pendingInventoryExport, payload, "Inventory");
	}

	/**
	 * Checks whether bank data export is currently enabled and allowed.
	 *
	 * @return true if bank export is enabled and the world is not seasonal
	 */
	private boolean shouldExportBankData()
	{
		return config.exportBankData() && !isSeasonalWorld();
	}

	/**
	 * Checks whether inventory data export is currently enabled and allowed.
	 *
	 * @return true if inventory export is enabled and the world is not seasonal
	 */
	private boolean shouldExportInventoryData()
	{
		return config.exportInventoryData() && !isSeasonalWorld();
	}

	/**
	 * Snapshots the current bank contents into an export payload.
	 * Must be called on the client thread where item composition lookups
	 * are safe and essentially free (in-memory cache hits).
	 *
	 * @param accountHash the player's account hash
	 * @param container   the bank item container
	 * @return a payload ready for dispatch to exporters
	 */
	private ExportPayload<BankRecord> snapshotBankData(long accountHash, ItemContainer container)
	{
		Item[] items = container.getItems();
		Instant timestamp = Instant.now();
		List<ItemEntry> entries = buildItemEntries(items);

		BankRecord record = new BankRecord(accountHash, timestamp, entries);
		return new ExportPayload<>(DataType.BANK, record);
	}

	/**
	 * Snapshots the current inventory contents into an export payload.
	 * Must be called on the client thread where item composition lookups
	 * are safe and essentially free (in-memory cache hits).
	 *
	 * @param accountHash the player's account hash
	 * @param container   the inventory item container
	 * @return a payload ready for dispatch to exporters
	 */
	private ExportPayload<InventoryRecord> snapshotInventoryData(long accountHash, ItemContainer container)
	{
		Item[] items = container.getItems();
		Instant timestamp = Instant.now();
		List<ItemEntry> entries = buildItemEntries(items);

		InventoryRecord record = new InventoryRecord(accountHash, timestamp, entries);
		return new ExportPayload<>(DataType.INVENTORY, record);
	}

	/**
	 * Converts raw items into a list of {@link ItemEntry} records,
	 * resolving composition data (name, members, tradeable, price) from the cache.
	 *
	 * @param items the raw items from an item container
	 * @return list of item entries, excluding empty slots
	 */
	private List<ItemEntry> buildItemEntries(Item[] items)
	{
		List<ItemEntry> entries = new ArrayList<>(items.length);

		for (Item item : items)
		{
			int id = item.getId();
			int quantity = item.getQuantity();

			if (id <= 0 || quantity == 0)
			{
				continue;
			}

			ItemComposition composition = client.getItemDefinition(id);
			entries.add(new ItemEntry(id, composition.getName(), quantity,
				composition.isMembers(), composition.isTradeable(), composition.getPrice()));
		}

		return entries;
	}

	/**
	 * Schedules a debounced export, cancelling any previously pending one.
	 * The actual I/O runs on the background executor after the debounce window.
	 *
	 * @param pendingFuture the current pending future to cancel, or null
	 * @param payload       the export payload to dispatch
	 * @param label         a human-readable label for logging (e.g. "Bank", "Inventory")
	 * @return the newly scheduled future
	 */
	private ScheduledFuture<?> scheduleDebouncedExport(
		ScheduledFuture<?> pendingFuture,
		ExportPayload<? extends ExportRecord> payload,
		String label)
	{
		if (pendingFuture != null)
		{
			pendingFuture.cancel(false);
		}

		ScheduledFuture<?> future = executor.schedule(
			() -> dispatchExport(payload),
			DEBOUNCE_DELAY_MS,
			TimeUnit.MILLISECONDS
		);

		log.debug("{} change detected, export scheduled (debounce {}ms)", label, DEBOUNCE_DELAY_MS);
		return future;
	}

	/**
	 * Checks whether the player is on a seasonal or temporary game mode world
	 * (Leagues, Deadman, Tournament, Fresh Start, or nosave beta).
	 * Data from these worlds is non-permanent and should not be exported.
	 *
	 * @return true if the current world is seasonal/temporary
	 */
	private boolean isSeasonalWorld()
	{
		EnumSet<WorldType> worldTypes = client.getWorldType();
		return worldTypes.contains(WorldType.SEASONAL)
			|| worldTypes.contains(WorldType.DEADMAN)
			|| worldTypes.contains(WorldType.TOURNAMENT_WORLD)
			|| worldTypes.contains(WorldType.FRESH_START_WORLD)
			|| worldTypes.contains(WorldType.NOSAVE_MODE);
	}

	/**
	 * Dispatches the payload to all active exporters.
	 * Runs on the background executor thread — never on the client thread.
	 */
	private void dispatchExport(ExportPayload<? extends ExportRecord> payload)
	{
		List<DataExporter> exporters = exporterFactory.getActiveExporters();
		if (exporters.isEmpty())
		{
			log.debug("No active exporters configured, skipping export");
			return;
		}

		for (DataExporter exporter : exporters)
		{
			try
			{
				exporter.export(payload);
			}
			catch (Exception e)
			{
				log.error("Exporter {} failed", exporter.getType(), e);
			}
		}
	}
}
