package com.osrsdataexporter.export;

/**
 * Enumerates the available export target types.
 * Each value represents a distinct storage or transport mechanism
 * that a {@link DataExporter} adapter can implement.
 */
public enum ExportType
{
	/**
	 * Export to the local filesystem under the RuneLite data directory.
	 */
	LOCAL_STORAGE
}
