package com.osrsdataexporter.datasource.unpacker;

import com.osrsdataexporter.model.ItemEntry;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;

/**
 * Unpacks the contents of a Rune Pouch by reading rune type and quantity varbits.
 *
 * <p>The Rune Pouch stores up to six rune types and their quantities across
 * dedicated varbit pairs. The rune type varbit encodes an index that is
 * mapped to a specific rune item ID via a fixed lookup table.</p>
 */
public class RunePouchUnpacker implements ItemUnpacker
{
	/** Varbit IDs for the rune type stored in each pouch slot. */
	private static final int[] RUNE_TYPE_VARBITS = {
		VarbitID.RUNE_POUCH_TYPE_1,
		VarbitID.RUNE_POUCH_TYPE_2,
		VarbitID.RUNE_POUCH_TYPE_3,
		VarbitID.RUNE_POUCH_TYPE_4,
		VarbitID.RUNE_POUCH_TYPE_5,
		VarbitID.RUNE_POUCH_TYPE_6,
	};

	/** Varbit IDs for the quantity stored in each pouch slot. */
	private static final int[] RUNE_AMOUNT_VARBITS = {
		VarbitID.RUNE_POUCH_QUANTITY_1,
		VarbitID.RUNE_POUCH_QUANTITY_2,
		VarbitID.RUNE_POUCH_QUANTITY_3,
		VarbitID.RUNE_POUCH_QUANTITY_4,
		VarbitID.RUNE_POUCH_QUANTITY_5,
		VarbitID.RUNE_POUCH_QUANTITY_6,
	};

	/**
	 * Mapping from rune type varbit value to rune item ID.
	 * Index 0 represents an empty slot and is skipped.
	 */
	private static final int[] RUNE_ITEM_IDS = {
		0,                    // 0: empty
		ItemID.AIRRUNE,       // 1
		ItemID.WATERRUNE,     // 2
		ItemID.EARTHRUNE,     // 3
		ItemID.FIRERUNE,      // 4
		ItemID.MINDRUNE,      // 5
		ItemID.CHAOSRUNE,     // 6
		ItemID.DEATHRUNE,     // 7
		ItemID.BLOODRUNE,     // 8
		ItemID.COSMICRUNE,    // 9
		ItemID.LAWRUNE,       // 10
		ItemID.NATURERUNE,    // 11
		ItemID.ASTRALRUNE,    // 12
		ItemID.MISTRUNE,      // 13
		ItemID.MUDRUNE,       // 14
		ItemID.DUSTRUNE,      // 15
		ItemID.LAVARUNE,      // 16
		ItemID.STEAMRUNE,     // 17
		ItemID.SMOKERUNE,     // 18
		ItemID.WRATHRUNE,     // 19
		ItemID.SOULRUNE,      // 20
		ItemID.SUNFIRERUNE,   // 21 — added Varlamore update
	};

	/** Item IDs for all known Rune Pouch variants. */
	private static final int[] RUNE_POUCH_ITEM_IDS = {
		ItemID.BH_RUNE_POUCH,
		ItemID.BH_RUNE_POUCH_TROUVER,
		ItemID.DIVINE_RUNE_POUCH,
		ItemID.DIVINE_RUNE_POUCH_TROUVER,
	};

	@Override
	public boolean handles(int itemId)
	{
		for (int pouchId : RUNE_POUCH_ITEM_IDS)
		{
			if (itemId == pouchId)
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public List<ItemEntry> unpack(Client client, int containerItemId)
	{
		List<ItemEntry> contents = new ArrayList<>();

		for (int i = 0; i < RUNE_TYPE_VARBITS.length; i++)
		{
			int runeIndex = client.getVarbitValue(RUNE_TYPE_VARBITS[i]);
			int amount = client.getVarbitValue(RUNE_AMOUNT_VARBITS[i]);

			if (runeIndex <= 0 || amount <= 0 || runeIndex >= RUNE_ITEM_IDS.length)
			{
				continue;
			}

			int itemId = RUNE_ITEM_IDS[runeIndex];
			ItemComposition comp = client.getItemDefinition(itemId);
			contents.add(new ItemEntry(itemId, comp.getName(), amount,
				comp.isMembers(), comp.isTradeable(), comp.getPrice(), null, containerItemId));
		}

		return contents;
	}
}
