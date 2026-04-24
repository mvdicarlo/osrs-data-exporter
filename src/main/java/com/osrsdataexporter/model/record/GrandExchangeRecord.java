package com.osrsdataexporter.model.record;

import com.osrsdataexporter.model.AccountContext;
import com.osrsdataexporter.model.DataType;
import com.osrsdataexporter.model.entry.GrandExchangeOfferEntry;
import java.time.Instant;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * A snapshot of all known Grand Exchange slot states at the time an offer changes.
 * Extends {@link ExportRecord} to inherit account and timestamp context.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class GrandExchangeRecord extends ExportRecord
{
	/**
	 * All slots with a known state at the time of this snapshot.
	 * Empty slots (EMPTY state) are excluded.
	 */
	List<GrandExchangeOfferEntry> offers;

	public GrandExchangeRecord(AccountContext account, Instant timestamp, List<GrandExchangeOfferEntry> offers)
	{
		super(account, timestamp, DataType.GRAND_EXCHANGE);
		this.offers = offers;
	}
}
