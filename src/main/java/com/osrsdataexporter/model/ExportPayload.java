package com.osrsdataexporter.model;

import lombok.Value;

/**
 * Generic payload wrapper for exporting records to adapters.
 * Carries the record data along with metadata about the data type,
 * allowing adapters to route data to the correct storage location
 * (e.g. file name, table, or API endpoint).
 *
 * @param <T> the specific {@link ExportRecord} subtype being exported
 */
@Value
public class ExportPayload<T extends ExportRecord>
{
	/**
	 * The type of data being exported.
	 * Used by adapters to determine how/where to persist the data.
	 */
	DataType dataType;

	/**
	 * The record containing the actual data to export.
	 */
	T record;
}
