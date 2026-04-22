package com.osrsdataexporter.model;

import lombok.Value;

/**
 * Identifies the player account associated with an export.
 *
 * <p>Bundles the stable numeric account hash with the human-readable
 * character name, providing a single extensible identity object that
 * all export records and handlers can carry.</p>
 */
@Value
public class AccountContext
{
	/** Stable numeric account identifier from {@code client.getAccountHash()}. */
	long accountHash;

	/**
	 * In-game character name from {@code client.getLocalPlayer().getName()}.
	 * May be {@code null} if the player object is not yet loaded.
	 */
	String characterName;
}
