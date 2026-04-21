package com.osrsdataexporter.model;

import lombok.Value;

/**
 * Represents a single item entry in a bank snapshot.
 * Each entry captures the item's identity, name, quantity, and category
 * at the time the bank was read.
 */
@Value
public class BankItemEntry
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
	 * The stack size / quantity of this item in the bank.
	 */
	int quantity;
}
