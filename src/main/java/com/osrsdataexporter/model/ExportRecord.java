package com.osrsdataexporter.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Abstract base class for all exportable records.
 * Every record carries account identity and a timestamp representing
 * when the export action was triggered.
 */
@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public abstract class ExportRecord
{
	/**
	 * Account identity (hash + character name) for the player who owns this record.
	 */
	private final AccountContext account;

	/**
	 * The timestamp when the export action was triggered.
	 * All records from the same export action share this timestamp.
	 */
	private final Instant timestamp;

	/**
	 * The data type this record represents.
	 * Used by {@link ExportPayload} to derive routing metadata.
	 */
	private final DataType dataType;
}
