package com.osrsdataexporter.model.record;

import com.osrsdataexporter.model.AccountContext;
import com.osrsdataexporter.model.DataType;
import com.osrsdataexporter.model.entry.ItemEntry;
import java.time.Instant;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * A snapshot of all items in a player's bank at a point in time.
 * Extends {@link ExportRecord} to inherit account and timestamp context.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class BankRecord extends ExportRecord
{
	/**
	 * The list of item entries in the bank at the time of the snapshot.
	 */
	List<ItemEntry> items;

	public BankRecord(AccountContext account, Instant timestamp, List<ItemEntry> items)
	{
		super(account, timestamp, DataType.BANK);
		this.items = items;
	}
}
