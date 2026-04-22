package com.osrsdataexporter.datasource;

import com.osrsdataexporter.OsrsDataExporterConfig;
import com.osrsdataexporter.model.DataType;
import com.osrsdataexporter.model.ExportPayload;
import com.osrsdataexporter.model.GroupStorageRecord;
import com.osrsdataexporter.model.ItemEntry;
import java.time.Instant;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import com.osrsdataexporter.model.AccountContext;

/**
 * Data source for Group Ironman shared storage snapshots.
 */
public class GroupStorageDataSource extends ItemContainerDataSource<GroupStorageRecord>
{
	public GroupStorageDataSource(Client client, OsrsDataExporterConfig config)
	{
		super(client, config, DataType.GROUP_STORAGE, ITEM_DEBOUNCE_DELAY_MS, InventoryID.INV_GROUP_TEMP);
	}

	@Override
	public boolean isEnabled()
	{
		return config.exportGroupStorageData();
	}

	@Override
	protected ExportPayload<GroupStorageRecord> snapshot(AccountContext account, ItemContainer container)
	{
		List<ItemEntry> entries = buildItemEntries(container.getItems());
		GroupStorageRecord record = new GroupStorageRecord(account, Instant.now(), entries);
		return new ExportPayload<>(record);
	}
}
