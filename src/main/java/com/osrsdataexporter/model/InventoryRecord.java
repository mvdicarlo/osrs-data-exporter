package com.osrsdataexporter.model;

import java.time.Instant;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * A snapshot of all items in a player's inventory at a point in time.
 * Extends {@link ExportRecord} to inherit account and timestamp context.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class InventoryRecord extends ExportRecord
{
	/**
	 * The list of item entries in the inventory at the time of the snapshot.
	 */
	List<ItemEntry> items;

	public InventoryRecord(long accountHash, Instant timestamp, List<ItemEntry> items)
	{
		super(accountHash, timestamp);
		this.items = items;
	}
}
