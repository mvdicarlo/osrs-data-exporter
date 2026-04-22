package com.osrsdataexporter.datasource;

import com.osrsdataexporter.OsrsDataExporterConfig;
import com.osrsdataexporter.model.AccountContext;
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
import net.runelite.api.events.ItemContainerChanged;

/**
 * Base class for data sources backed by an {@link ItemContainer}.
 * Provides shared item resolution logic and a common trigger method.
 *
 * @param <T> the specific {@link ExportRecord} subtype this handler produces
 */
public abstract class ItemContainerDataSource<T extends ExportRecord> extends DataSourceHandler<T>
{
	private final int containerId;

	protected ItemContainerDataSource(Client client, OsrsDataExporterConfig config, DataType dataType, long debounceDelayMs, int containerId)
	{
		super(client, config, dataType, debounceDelayMs);
		this.containerId = containerId;
	}

	/**
	 * Returns the inventory container ID this data source monitors.
	 */
	public int getContainerId()
	{
		return containerId;
	}

	@Override
	public boolean canHandle(Object event)
	{
		return event instanceof ItemContainerChanged
			&& ((ItemContainerChanged) event).getContainerId() == containerId;
	}

	@Override
	public void handleEvent(
		Object event,
		AccountContext account,
		ScheduledExecutorService executor,
		Consumer<ExportPayload<? extends ExportRecord>> dispatcher)
	{
		ItemContainer container = ((ItemContainerChanged) event).getItemContainer();
		if (container == null)
		{
			return;
		}

		ExportPayload<T> payload = snapshot(account, container);
		scheduleExport(payload, executor, dispatcher);
	}

	/**
	 * Creates an export payload from the given item container.
	 */
	protected abstract ExportPayload<T> snapshot(AccountContext account, ItemContainer container);

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
