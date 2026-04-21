package com.osrsdataexporter.model;

import lombok.Value;

/**
 * Represents a single item entry in an item container snapshot.
 * Used for both bank and inventory exports.
 */
@Value
public class ItemEntry
{
	/**
	 * The item's unique ID as defined by the game engine.
	 */
	int itemId;

	/**
	 * The human-readable name of the item, resolved from the item composition cache.
	 */
	String itemName;

	/**
	 * The stack size / quantity of this item.
	 */
	int quantity;
}
