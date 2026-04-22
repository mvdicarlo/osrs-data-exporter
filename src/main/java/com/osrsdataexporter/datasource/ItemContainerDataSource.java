package com.osrsdataexporter.datasource;

import com.osrsdataexporter.OsrsDataExporterConfig;
import com.osrsdataexporter.model.DataType;
import com.osrsdataexporter.model.ExportPayload;
import com.osrsdataexporter.model.ExportRecord;
import com.osrsdataexporter.model.ItemEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;

/**
 * Base class for data sources backed by an {@link ItemContainer}.
 * Provides shared item resolution logic and a common trigger method.
 *
 * @param <T> the specific {@link ExportRecord} subtype this handler produces
 */
public abstract class ItemContainerDataSource<T extends ExportRecord> extends DataSourceHandler<T>
{
	protected ItemContainerDataSource(Client client, OsrsDataExporterConfig config, DataType dataType, long debounceDelayMs)
	{
		super(client, config, dataType, debounceDelayMs);
	}

	/**
	 * Handles an item container change. Checks if enabled,
	 * snapshots the container, and schedules a debounced export.
	 */
	public void handleContainerChange(
		ItemContainer container,
		long accountHash,
		ScheduledExecutorService executor,
		Consumer<ExportPayload<? extends ExportRecord>> dispatcher)
	{
		if (!isEnabled())
		{
			return;
		}

		ExportPayload<T> payload = snapshot(accountHash, container);
		scheduleExport(payload, executor, dispatcher);
	}

	/**
	 * Creates an export payload from the given item container.
	 */
	protected abstract ExportPayload<T> snapshot(long accountHash, ItemContainer container);

	/**
	 * Converts raw items into {@link ItemEntry} records,
	 * resolving composition data from the cache.
	 */
	protected List<ItemEntry> buildItemEntries(Item[] items)
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
}
