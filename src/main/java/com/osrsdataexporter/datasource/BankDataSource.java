package com.osrsdataexporter.datasource;

import com.osrsdataexporter.OsrsDataExporterConfig;
import com.osrsdataexporter.model.BankRecord;
import com.osrsdataexporter.model.DataType;
import com.osrsdataexporter.model.ExportPayload;
import com.osrsdataexporter.model.ItemEntry;
import java.time.Instant;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;

/**
 * Data source for bank snapshots.
 */
public class BankDataSource extends ItemContainerDataSource<BankRecord>
{
	public BankDataSource(Client client, OsrsDataExporterConfig config)
	{
		super(client, config, DataType.BANK, ITEM_DEBOUNCE_DELAY_MS, InventoryID.BANK);
	}

	@Override
	public boolean isEnabled()
	{
		return config.exportBankData();
	}

	@Override
	protected ExportPayload<BankRecord> snapshot(long accountHash, ItemContainer container)
	{
		List<ItemEntry> entries = buildItemEntries(container.getItems());
		BankRecord record = new BankRecord(accountHash, Instant.now(), entries);
		return new ExportPayload<>(record);
	}
}
