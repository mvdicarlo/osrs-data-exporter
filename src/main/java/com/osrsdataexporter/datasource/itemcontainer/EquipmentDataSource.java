package com.osrsdataexporter.datasource.itemcontainer;

import com.osrsdataexporter.OsrsDataExporterConfig;
import com.osrsdataexporter.datasource.ItemContainerDataSource;
import com.osrsdataexporter.datasource.unpacker.ItemUnpackerRegistry;
import com.osrsdataexporter.model.AccountContext;
import com.osrsdataexporter.model.DataType;
import com.osrsdataexporter.model.entry.EquipmentItemEntry;
import com.osrsdataexporter.model.record.EquipmentRecord;
import com.osrsdataexporter.model.entry.EquipmentStatsEntry;
import com.osrsdataexporter.model.ExportPayload;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;

/**
 * Data source for equipment snapshots.
 * Captures the items currently worn by the player, including combat and defence stats.
 */
public class EquipmentDataSource extends ItemContainerDataSource<EquipmentRecord>
{
	private final ItemManager itemManager;

	public EquipmentDataSource(Client client, OsrsDataExporterConfig config, ItemManager itemManager, ItemUnpackerRegistry unpackerRegistry)
	{
		super(client, config, DataType.EQUIPMENT, ITEM_DEBOUNCE_DELAY_MS, InventoryID.WORN, unpackerRegistry);
		this.itemManager = itemManager;
	}

	@Override
	public boolean isEnabled()
	{
		return config.exportEquipmentData();
	}

	@Override
	protected ExportPayload<EquipmentRecord> snapshot(AccountContext account, ItemContainer container)
	{
		List<EquipmentItemEntry> entries = buildEquipmentEntries(container.getItems());
		EquipmentRecord record = new EquipmentRecord(account, Instant.now(), entries);
		return new ExportPayload<>(record);
	}

	private List<EquipmentItemEntry> buildEquipmentEntries(Item[] items)
	{
		List<EquipmentItemEntry> entries = new ArrayList<>();

		for (int slot = 0; slot < items.length; slot++)
		{
			Item item = items[slot];
			int id = item.getId();
			int quantity = item.getQuantity();

			if (id <= 0 || quantity == 0)
			{
				continue;
			}

			ItemComposition composition = client.getItemDefinition(id);
			EquipmentStatsEntry stats = resolveStats(id, slot);

			entries.add(new EquipmentItemEntry(
				id,
				composition.getName(),
				quantity,
				composition.isMembers(),
				composition.isTradeable(),
				composition.getPrice(),
				stats
			));
		}

		return entries;
	}

	private EquipmentStatsEntry resolveStats(int itemId, int slot)
	{
		ItemStats itemStats = itemManager.getItemStats(itemId);
		if (itemStats == null || !itemStats.isEquipable())
		{
			return null;
		}

		ItemEquipmentStats eq = itemStats.getEquipment();
		if (eq == null)
		{
			return null;
		}

		return EquipmentStatsEntry.from(eq, slot);
	}
}
