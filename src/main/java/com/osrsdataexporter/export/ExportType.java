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
	LOCAL_STORAGE,

	/**
	 * Export to Azure Blob Storage using connection-string authentication.
	 */
	AZURE_BLOB_STORAGE
}
