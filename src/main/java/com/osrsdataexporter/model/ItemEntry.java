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

	/**
	 * Whether this item is members-only.
	 */
	boolean members;

	/**
	 * Whether this item can be traded between players on the Grand Exchange or directly.
	 */
	boolean tradeable;

	/**
	 * The base store price of this item in coins.
	 * High alchemy value can be derived as {@code floor(price * 0.6)},
	 * and low alchemy value as {@code floor(price * 0.4)}.
	 */
	int price;
}
