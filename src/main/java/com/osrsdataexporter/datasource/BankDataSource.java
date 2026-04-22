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

/**
 * Data source for bank snapshots.
 */
public class BankDataSource extends ItemContainerDataSource<BankRecord>
{
	private static final long DEBOUNCE_DELAY_MS = 2000;

	private final OsrsDataExporterConfig config;

	public BankDataSource(Client client, OsrsDataExporterConfig config)
	{
		super(client, DataType.BANK, DEBOUNCE_DELAY_MS);
		this.config = config;
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
		return new ExportPayload<>(DataType.BANK, record);
	}
}
