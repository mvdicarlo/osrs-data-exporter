package com.osrsdataexporter.model.entry;

import lombok.Value;

/**
 * A single Grand Exchange offer at the time of an initial or terminal state change.
 */
@Value
public class GrandExchangeOfferEntry
{
	/** GE slot index (0–7). */
	int slot;

	/** The item being bought or sold. */
	int itemId;

	/** Human-readable name of the item. */
	String itemName;

	/**
	 * The current state of this offer.
	 * One of: {@code BUYING}, {@code BOUGHT}, {@code SELLING}, {@code SOLD},
	 * {@code CANCELLED_BUY}, {@code CANCELLED_SELL}.
	 */
	String state;

	/** Whether this is a buy offer ({@code true}) or sell offer ({@code false}). */
	boolean buy;

	/** Price per item set by the player (coins). */
	int price;

	/** Total quantity ordered. */
	int totalQuantity;

	/** Quantity transacted so far. */
	int quantityTraded;

	/** Total coins spent (buy) or received (sell) so far. */
	int spent;
}
