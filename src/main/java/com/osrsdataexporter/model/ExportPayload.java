package com.osrsdataexporter.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Generic payload wrapper for exporting records to adapters.
 * The data type is derived from the record at construction time,
 * ensuring the pairing can never be inconsistent.
 *
 * @param <T> the specific {@link ExportRecord} subtype being exported
 */
@Getter
@EqualsAndHashCode
@ToString
public class ExportPayload<T extends ExportRecord>
{
	/**
	 * The type of data being exported, derived from the record.
	 * Serialized into JSON for consumers that need routing metadata.
	 */
	private final DataType dataType;

	/**
	 * The record containing the actual data to export.
	 */
	private final T record;

	public ExportPayload(T record)
	{
		this.record = record;
		this.dataType = record.getDataType();
	}
}
