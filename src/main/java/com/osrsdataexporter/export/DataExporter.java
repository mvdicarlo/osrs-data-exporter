package com.osrsdataexporter.export;

import com.osrsdataexporter.model.ExportPayload;
import com.osrsdataexporter.model.ExportRecord;

/**
 * Interface for all data export adapters.
 * Implementations handle persisting export payloads to a specific target
 * (e.g. local filesystem, remote API, database).
 *
 * <p>All {@code export()} calls are invoked off the client thread,
 * so implementations may perform blocking I/O safely.</p>
 */
public interface DataExporter
{
	/**
	 * Export the given payload to the target destination.
	 *
	 * @param payload the data payload to export
	 */
	void export(ExportPayload<? extends ExportRecord> payload);

	/**
	 * Returns the export type this adapter handles.
	 *
	 * @return the export type identifier
	 */
	ExportType getType();
}
