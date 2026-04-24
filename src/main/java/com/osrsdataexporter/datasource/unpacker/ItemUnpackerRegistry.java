package com.osrsdataexporter.datasource.unpacker;

import com.osrsdataexporter.model.entry.ItemEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Client;

/**
 * Central registry of {@link ItemUnpacker} implementations.
 *
 * <p>During item container snapshots, each item is passed through the registry
 * to check if it is a container-like item whose inner contents can be read.
 * The first matching unpacker is used. If no unpacker matches, {@code null}
 * is returned (the item has no inner contents).</p>
 *
 * <p>Register new unpackers via {@link #register(ItemUnpacker)}.</p>
 */
public class ItemUnpackerRegistry
{
	private final List<ItemUnpacker> unpackers = new ArrayList<>();

	/**
	 * Registers an unpacker. Order determines priority — first match wins.
	 */
	public void register(ItemUnpacker unpacker)
	{
		unpackers.add(unpacker);
	}

	/**
	 * Attempts to unpack the given item. Returns the inner contents if a
	 * matching unpacker exists, or {@code null} if the item is not a container.
	 *
	 * @param itemId the item ID to check
	 * @param client the RuneLite client (must be on the client thread)
	 * @return list of inner items, or {@code null} if not a container item
	 */
	public List<ItemEntry> tryUnpack(int itemId, Client client)
	{
		for (ItemUnpacker unpacker : unpackers)
		{
			if (unpacker.handles(itemId))
			{
				List<ItemEntry> contents = unpacker.unpack(client, itemId);
				return contents.isEmpty() ? Collections.emptyList() : contents;
			}
		}
		return null;
	}
}
