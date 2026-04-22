package com.osrsdataexporter;

import com.google.gson.Gson;
import com.google.inject.Provides;
import com.osrsdataexporter.datasource.BankDataSource;
import com.osrsdataexporter.datasource.DataSourceHandler;
import com.osrsdataexporter.datasource.InventoryDataSource;
import com.osrsdataexporter.datasource.SkillsDataSource;
import com.osrsdataexporter.export.DataExporter;
import com.osrsdataexporter.export.DataExporterFactory;
import com.osrsdataexporter.model.ExportPayload;
import com.osrsdataexporter.model.ExportRecord;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.WorldType;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

/**
 * OSRS Data Exporter plugin.
 *
 * <p>Captures account data (bank, inventory, and skills) and exports it
 * to configurable targets via the adapter/factory pattern. All export I/O
 * is performed off the client thread to avoid any impact on game performance.</p>
 *
 * <p>Each data source is encapsulated in its own {@link DataSourceHandler}
 * with independent debounce timers. The plugin routes events and manages lifecycle.</p>
 */
@Slf4j
@PluginDescriptor(
	name = "OSRS Data Exporter",
	description = "Exports account data (bank, inventory, skills, etc.) to external storage targets.",
	tags = {"bank", "inventory", "skills", "export", "data"}
)
public class OsrsDataExporterPlugin extends Plugin
{
	private static final String EXECUTOR_THREAD_NAME = "osrs-data-exporter";

	@Inject
	private Client client;

	@Inject
	private OsrsDataExporterConfig config;

	@Inject
	private Gson gson;

	private ScheduledExecutorService executor;
	private DataExporterFactory exporterFactory;

	private final List<DataSourceHandler<?>> dataSources = new ArrayList<>();
	private BankDataSource bankDataSource;
	private InventoryDataSource inventoryDataSource;
	private SkillsDataSource skillsDataSource;

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

		bankDataSource = new BankDataSource(client, config);
		inventoryDataSource = new InventoryDataSource(client, config);
		skillsDataSource = new SkillsDataSource(client, config);
		dataSources.add(bankDataSource);
		dataSources.add(inventoryDataSource);
		dataSources.add(skillsDataSource);

		log.info("OSRS Data Exporter started");
	}

	@Override
	protected void shutDown()
	{
		dataSources.forEach(DataSourceHandler::shutdown);
		dataSources.clear();
		bankDataSource = null;
		inventoryDataSource = null;
		skillsDataSource = null;

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

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (isSeasonalWorld())
		{
			return;
		}

		long accountHash = client.getAccountHash();
		if (accountHash == -1)
		{
			return;
		}

		if (event.getContainerId() == InventoryID.BANK)
		{
			bankDataSource.handleContainerChange(
				event.getItemContainer(), accountHash, executor, this::dispatchExport);
		}
		else if (event.getContainerId() == InventoryID.INV)
		{
			inventoryDataSource.handleContainerChange(
				event.getItemContainer(), accountHash, executor, this::dispatchExport);
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (isSeasonalWorld())
		{
			return;
		}

		long accountHash = client.getAccountHash();
		if (accountHash == -1)
		{
			return;
		}

		skillsDataSource.handleStatChanged(accountHash, executor, this::dispatchExport);
	}

	/**
	 * Checks whether the player is on a seasonal or temporary game mode world.
	 * Data from these worlds is non-permanent and should not be exported.
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
