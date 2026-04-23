package com.osrsdataexporter.datasource;

import com.osrsdataexporter.OsrsDataExporterConfig;
import com.osrsdataexporter.model.AccountContext;
import com.osrsdataexporter.model.DataType;
import com.osrsdataexporter.model.ExportPayload;
import com.osrsdataexporter.model.ExportRecord;
import com.osrsdataexporter.model.GrandExchangeOfferEntry;
import com.osrsdataexporter.model.GrandExchangeRecord;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.events.GrandExchangeOfferChanged;

/**
 * Data source for Grand Exchange offer snapshots.
 *
 * <p>Exports a record whenever an offer reaches an initial state (placed)
 * or a terminal state (completed or cancelled). Partial fill updates are ignored.</p>
 *
 * <p>No debounce is used — each state transition is a discrete, meaningful event.
 * RuneLite replays current slot states on login, so terminal states missed while
 * offline are captured on next login.</p>
 */
public class GrandExchangeDataSource extends DataSourceHandler<GrandExchangeRecord>
{
	/** Tracks the last known state per slot so each export reflects all active slots. */
	private final Map<Integer, GrandExchangeOfferEntry> slotMap = new HashMap<>();

	public GrandExchangeDataSource(Client client, OsrsDataExporterConfig config)
	{
		super(client, config, DataType.GRAND_EXCHANGE, ITEM_DEBOUNCE_DELAY_MS);
	}

	@Override
	public boolean isEnabled()
	{
		return config.exportGrandExchangeData();
	}

	@Override
	public void onAccountChanged()
	{
		slotMap.clear();
	}

	@Override
	public boolean canHandle(Object event)
	{
		return event instanceof GrandExchangeOfferChanged;
	}

	@Override
	public void handleEvent(
		Object event,
		AccountContext account,
		ScheduledExecutorService executor,
		Consumer<ExportPayload<? extends ExportRecord>> dispatcher)
	{
		GrandExchangeOfferChanged offerChanged = (GrandExchangeOfferChanged) event;
		GrandExchangeOffer offer = offerChanged.getOffer();
		int slot = offerChanged.getSlot();

		if (offer.getState() == GrandExchangeOfferState.EMPTY)
		{
			slotMap.remove(slot);
			return;
		}

		if (!isExportableState(offer.getState()))
		{
			return;
		}

		String itemName = client.getItemDefinition(offer.getItemId()).getName();
		GrandExchangeOfferEntry entry = new GrandExchangeOfferEntry(
			slot,
			offer.getItemId(),
			itemName,
			offer.getState().name(),
			isBuyOffer(offer.getState()),
			offer.getPrice(),
			offer.getTotalQuantity(),
			offer.getQuantitySold(),
			offer.getSpent()
		);
		slotMap.put(slot, entry);

		ExportPayload<GrandExchangeRecord> payload = snapshot(account);
		scheduleExport(payload, executor, dispatcher);
	}

	private ExportPayload<GrandExchangeRecord> snapshot(AccountContext account)
	{
		List<GrandExchangeOfferEntry> offers = new ArrayList<>(slotMap.values());
		GrandExchangeRecord record = new GrandExchangeRecord(account, Instant.now(), offers);
		return new ExportPayload<>(record);
	}

	/**
	 * Returns {@code true} for initial states (offer placed) and terminal states
	 * (offer completed or cancelled). EMPTY and in-progress partial fills are excluded.
	 */
	private boolean isExportableState(GrandExchangeOfferState state)
	{
		switch (state)
		{
			case BUYING:
			case SELLING:
			case BOUGHT:
			case SOLD:
			case CANCELLED_BUY:
			case CANCELLED_SELL:
				return true;
			default:
				return false;
		}
	}

	private boolean isBuyOffer(GrandExchangeOfferState state)
	{
		return state == GrandExchangeOfferState.BUYING
			|| state == GrandExchangeOfferState.BOUGHT
			|| state == GrandExchangeOfferState.CANCELLED_BUY;
	}
}
