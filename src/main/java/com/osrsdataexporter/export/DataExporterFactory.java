package com.osrsdataexporter.export;

import com.google.gson.Gson;
import com.osrsdataexporter.OsrsDataExporterConfig;
import com.osrsdataexporter.export.azure.AzureBlobStorageExporter;
import com.osrsdataexporter.export.local.LocalStorageExporter;
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
	private volatile List<String> lastInitErrors = Collections.emptyList();

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
		List<String> errors = new ArrayList<>();

		if (config.enableLocalStorage())
		{
			LocalStorageExporter localExporter = new LocalStorageExporter(gson);
			exporters.put(localExporter.getType(), localExporter);
			log.debug("Registered LocalStorageExporter");
		}

		if (config.enableAzureBlobStorage())
		{
			String connectionString = config.azureBlobConnectionString();
			String containerName = config.azureBlobContainerName();

			if (connectionString == null || connectionString.trim().isEmpty())
			{
				String msg = "Azure Blob export enabled but connection string is empty";
				log.warn("{}; exporter not registered", msg);
				errors.add(msg);
			}
			else
			{
				try
				{
					AzureBlobStorageExporter azureExporter =
						new AzureBlobStorageExporter(gson, connectionString, containerName);
					exporters.put(azureExporter.getType(), azureExporter);
					log.debug("Registered AzureBlobStorageExporter");
				}
				catch (IllegalArgumentException e)
				{
					String msg = "Invalid Azure Blob config: " + e.getMessage();
					log.error("{}; exporter not registered", msg, e);
					errors.add(msg);
				}
			}
		}

		cachedActiveExporters = Collections.unmodifiableList(new ArrayList<>(exporters.values()));
		lastInitErrors = Collections.unmodifiableList(errors);
	}

	/**
	 * Returns error messages produced by the most recent {@link #init()} call.
	 * Intended for surfacing to the user (e.g. game chat) after startup or config change.
	 */
	public List<String> getLastInitErrors()
	{
		return lastInitErrors;
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
		lastInitErrors = Collections.emptyList();
	}
}
