package com.osrsdataexporter.model;

import java.time.Instant;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * A snapshot of all items in a Group Ironman shared storage at a point in time.
 * Extends {@link ExportRecord} to inherit account and timestamp context.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class GroupStorageRecord extends ExportRecord
{
	/**
	 * The list of item entries in the shared storage at the time of the snapshot.
	 */
	List<ItemEntry> items;

	public GroupStorageRecord(long accountHash, Instant timestamp, List<ItemEntry> items)
	{
		super(accountHash, timestamp, DataType.GROUP_STORAGE);
		this.items = items;
	}
}
