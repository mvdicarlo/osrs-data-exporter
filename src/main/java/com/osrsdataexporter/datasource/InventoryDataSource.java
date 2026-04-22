package com.osrsdataexporter.datasource;

import com.osrsdataexporter.OsrsDataExporterConfig;
import com.osrsdataexporter.model.DataType;
import com.osrsdataexporter.model.ExportPayload;
import com.osrsdataexporter.model.InventoryRecord;
import com.osrsdataexporter.model.ItemEntry;
import java.time.Instant;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;

/**
 * Data source for inventory snapshots.
 */
public class InventoryDataSource extends ItemContainerDataSource<InventoryRecord>
{
	public InventoryDataSource(Client client, OsrsDataExporterConfig config)
	{
		super(client, config, DataType.INVENTORY, 2000);
	}

	@Override
	public boolean isEnabled()
	{
		return config.exportInventoryData();
	}

	@Override
	protected ExportPayload<InventoryRecord> snapshot(long accountHash, ItemContainer container)
	{
		List<ItemEntry> entries = buildItemEntries(container.getItems());
		InventoryRecord record = new InventoryRecord(accountHash, Instant.now(), entries);
		return new ExportPayload<>(DataType.INVENTORY, record);
	}
}
