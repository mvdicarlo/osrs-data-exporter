package com.osrsdataexporter.model.record;

import com.osrsdataexporter.model.AccountContext;
import com.osrsdataexporter.model.DataType;
import com.osrsdataexporter.model.entry.EquipmentItemEntry;
import java.time.Instant;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * A snapshot of all items currently worn by the player.
 * Extends {@link ExportRecord} to inherit account and timestamp context.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class EquipmentRecord extends ExportRecord
{
	/**
	 * The list of equipped items at the time of the snapshot.
	 * Each entry includes item metadata and combat/defence stats where available.
	 */
	List<EquipmentItemEntry> items;

	public EquipmentRecord(AccountContext account, Instant timestamp, List<EquipmentItemEntry> items)
	{
		super(account, timestamp, DataType.EQUIPMENT);
		this.items = items;
	}
}
