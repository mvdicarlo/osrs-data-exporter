package com.osrsdataexporter.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Abstract base class for all exportable records.
 * Every record is associated with an account (via accountHash) and carries
 * a consistent timestamp representing when the export action was triggered.
 */
@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public abstract class ExportRecord
{
	/**
	 * The unique account hash identifying the player.
	 * Obtained via {@code client.getAccountHash()}.
	 */
	private final long accountHash;

	/**
	 * The timestamp when the export action was triggered.
	 * All records from the same export action share this timestamp.
	 */
	private final Instant timestamp;
}
