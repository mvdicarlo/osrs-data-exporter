package com.osrsdataexporter.datasource.unpacker;

import com.osrsdataexporter.model.entry.ItemEntry;
import java.util.List;
import net.runelite.api.Client;

/**
 * Reads the inner contents of a container-like item (e.g. Rune Pouch, Looting Bag).
 *
 * <p>Implementations are registered in {@link ItemUnpackerRegistry} and called
 * automatically during item container snapshots. Each unpacker is responsible
 * for a specific set of item IDs and knows how to read their contents from
 * the client (varbits, item containers, etc.).</p>
 *
 * <p>All methods must be called on the client thread.</p>
 */
public interface ItemUnpacker
{
	/**
	 * Returns {@code true} if this unpacker can read the contents of the given item.
	 */
	boolean handles(int itemId);

	/**
	 * Reads the current contents of the container item from the client.
	 *
	 * @param client          the RuneLite client
	 * @param containerItemId the item ID of the container being unpacked
	 * @return the items inside the container; empty list if the container is empty
	 */
	List<ItemEntry> unpack(Client client, int containerItemId);
}
