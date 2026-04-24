package com.osrsdataexporter;

import com.google.gson.Gson;
import com.google.inject.Provides;
import com.osrsdataexporter.datasource.itemcontainer.BankDataSource;
import com.osrsdataexporter.datasource.DataSourceHandler;
import com.osrsdataexporter.datasource.itemcontainer.EquipmentDataSource;
import com.osrsdataexporter.datasource.GrandExchangeDataSource;
import com.osrsdataexporter.datasource.itemcontainer.GroupStorageDataSource;
import com.osrsdataexporter.datasource.itemcontainer.InventoryDataSource;
import com.osrsdataexporter.datasource.SkillsDataSource;
import com.osrsdataexporter.datasource.unpacker.ItemUnpackerRegistry;
import com.osrsdataexporter.datasource.unpacker.RunePouchUnpacker;
import com.osrsdataexporter.export.DataExporter;
import com.osrsdataexporter.export.DataExporterFactory;
import com.osrsdataexporter.model.AccountContext;
import com.osrsdataexporter.model.ExportPayload;
import com.osrsdataexporter.model.record.ExportRecord;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.ChatMessageType;
import net.runelite.api.WorldType;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

/**
 * OSRS Data Exporter plugin.
 *
 * <p>Captures account data (bank, inventory, skills, and group storage) and exports it
 * to configurable targets via the adapter/factory pattern. All export I/O
 * is performed off the client thread to avoid any impact on game performance.</p>
 *
 * <p>Each data source is encapsulated in its own {@link DataSourceHandler}
 * with independent debounce timers. The plugin routes events and manages lifecycle.</p>
 */
@Slf4j
@PluginDescriptor(
	name = "OSRS Data Exporter",
	description = "Exports account data (bank, inventory, skills, group storage, etc.) to external storage targets.",
	tags = {"bank", "inventory", "skills", "group ironman", "export", "data"}
)
public class OsrsDataExporterPlugin extends Plugin
{
	private static final String EXECUTOR_THREAD_NAME = "osrs-data-exporter";
	private static final long EXECUTOR_SHUTDOWN_TIMEOUT_SECS = 2;

	@Inject
	private Client client;

	@Inject
	private OsrsDataExporterConfig config;

	@Inject
	private Gson gson;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ItemManager itemManager;

	private ScheduledExecutorService executor;
	private DataExporterFactory exporterFactory;
	private final List<DataSourceHandler<?>> dataSources = new ArrayList<>();
	private long lastSeenAccountHash = -1;

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
		reportInitErrors();

		ItemUnpackerRegistry unpackerRegistry = new ItemUnpackerRegistry();
		unpackerRegistry.register(new RunePouchUnpacker());

		dataSources.add(new BankDataSource(client, config, unpackerRegistry));
		dataSources.add(new InventoryDataSource(client, config, unpackerRegistry));
		dataSources.add(new GroupStorageDataSource(client, config, unpackerRegistry));
		dataSources.add(new SkillsDataSource(client, config));
		dataSources.add(new EquipmentDataSource(client, config, itemManager, unpackerRegistry));
		dataSources.add(new GrandExchangeDataSource(client, config));

		log.info("OSRS Data Exporter started");
	}

	@Override
	protected void shutDown()
	{
		dataSources.forEach(DataSourceHandler::shutdown);
		dataSources.clear();
		lastSeenAccountHash = -1;

		if (executor != null)
		{
			executor.shutdown();
			try
			{
				if (!executor.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECS, TimeUnit.SECONDS))
				{
					executor.shutdownNow();
				}
			}
			catch (InterruptedException e)
			{
				executor.shutdownNow();
				Thread.currentThread().interrupt();
			}
			executor = null;
		}

		if (exporterFactory != null)
		{
			exporterFactory.shutdown();
			exporterFactory = null;
		}

		log.info("OSRS Data Exporter stopped");
	}

	@Provides
	OsrsDataExporterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OsrsDataExporterConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!OsrsDataExporterConfig.CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}

		if (OsrsDataExporterConfig.EXPORT_TARGET_KEYS.contains(event.getKey()))
		{
			log.debug("Export target config changed ({}), reinitializing exporters", event.getKey());
			exporterFactory.init();
			reportInitErrors();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		routeEvent(event);
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		routeEvent(event);
	}

	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
	{
		routeEvent(event);
	}

	/**
	 * Routes a RuneLite event to the first data source that can handle it.
	 * Guards against seasonal worlds and unauthenticated sessions.
	 */
	private void routeEvent(Object event)
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

		if (accountHash != lastSeenAccountHash)
		{
			log.debug("Account changed ({} -> {}), resetting handler state", lastSeenAccountHash, accountHash);
			lastSeenAccountHash = accountHash;
			dataSources.forEach(DataSourceHandler::onAccountChanged);
		}

		Player localPlayer = client.getLocalPlayer();
		String characterName = localPlayer != null ? localPlayer.getName() : null;
		AccountContext account = new AccountContext(accountHash, characterName);

		for (DataSourceHandler<?> handler : dataSources)
		{
			if (handler.isEnabled() && handler.canHandle(event))
			{
				handler.handleEvent(event, account, executor, this::dispatchExport);
				return;
			}
		}
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
				notifyExportFailure(exporter, e);
			}
		}
	}

	/**
	 * Posts a game-chat message describing an export failure so the user is aware
	 * without needing to read client logs. Marshalled onto the client thread.
	 */
	private void notifyExportFailure(DataExporter exporter, Exception e)
	{
		String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
		String message = "[OSRS Data Exporter] " + exporter.getType() + " export failed: " + reason;
		postChatMessage(message);
	}

	/**
	 * Posts any errors raised during the most recent exporter initialization to game chat.
	 */
	private void reportInitErrors()
	{
		for (String error : exporterFactory.getLastInitErrors())
		{
			postChatMessage("[OSRS Data Exporter] " + error);
		}
	}

	private void postChatMessage(String message)
	{
		clientThread.invokeLater(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null));
	}
}
