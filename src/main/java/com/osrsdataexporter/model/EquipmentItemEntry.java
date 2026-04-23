package com.osrsdataexporter.model;

import lombok.Value;

/**
 * A single item in the equipment snapshot.
 * Extends item metadata with combat/defence stats where available.
 */
@Value
public class EquipmentItemEntry
{
	int itemId;
	String itemName;
	int quantity;
	boolean members;
	boolean tradeable;
	int price;

	/**
	 * Combat and defensive stats for this item.
	 * {@code null} if the item has no equipment stats (e.g. an ammo stack with no explicit stats).
	 */
	EquipmentStats stats;
}
