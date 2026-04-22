package com.osrsdataexporter.export;

import com.google.gson.Gson;
import com.osrsdataexporter.OsrsDataExporterConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory responsible for creating and managing {@link DataExporter} instances.
 *
 * <p>Maintains a registry of active exporters keyed by {@link ExportType}.
 * Exporters are initialized based on plugin configuration. The active exporter
 * list is cached on init/shutdown to avoid per-export allocation, and uses
 * a volatile field for safe cross-thread visibility.</p>
 */
@Slf4j
public class DataExporterFactory
{
	private final Map<ExportType, DataExporter> exporters = new EnumMap<>(ExportType.class);
	private final OsrsDataExporterConfig config;
	private final Gson gson;

	private volatile List<DataExporter> cachedActiveExporters = Collections.emptyList();

	public DataExporterFactory(OsrsDataExporterConfig config, Gson gson)
	{
		this.config = config;
		this.gson = gson;
	}

	/**
	 * Initializes exporters based on the current plugin configuration.
	 * Should be called on plugin startup.
	 */
	public void init()
	{
		exporters.clear();

		if (config.enableLocalStorage())
		{
			LocalStorageExporter localExporter = new LocalStorageExporter(gson);
			exporters.put(localExporter.getType(), localExporter);
			log.debug("Registered LocalStorageExporter");
		}

		cachedActiveExporters = Collections.unmodifiableList(new ArrayList<>(exporters.values()));
	}

	/**
	 * Returns the cached unmodifiable list of all currently active exporters.
	 *
	 * @return list of active exporters
	 */
	public List<DataExporter> getActiveExporters()
	{
		return cachedActiveExporters;
	}

	/**
	 * Clears all registered exporters. Called on plugin shutdown.
	 */
	public void shutdown()
	{
		exporters.clear();
		cachedActiveExporters = Collections.emptyList();
	}
}
