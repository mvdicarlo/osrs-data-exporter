package com.osrsdataexporter.export.local;

import com.google.gson.Gson;
import com.osrsdataexporter.export.DataExporter;
import com.osrsdataexporter.export.ExportType;
import com.osrsdataexporter.model.DataType;
import com.osrsdataexporter.model.ExportPayload;
import com.osrsdataexporter.model.record.ExportRecord;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

/**
 * Exports data as JSON files to the local filesystem.
 *
 * <p>Files are written to:
 * {@code ~/.runelite/osrs-data-exporter/{accountHash}/{exportType}.json}</p>
 *
 * <p>This exporter is always called off the client thread, so file I/O
 * will not block the game.</p>
 */
@Slf4j
public class LocalStorageExporter implements DataExporter
{
	private static final String PLUGIN_DIR_NAME = "osrs-data-exporter";
	private static final String FILE_EXTENSION = ".json";

	private final Gson gson;
	private final File baseDir;

	public LocalStorageExporter(Gson gson)
	{
		this(gson, RuneLite.RUNELITE_DIR);
	}

	LocalStorageExporter(Gson gson, File baseDir)
	{
		this.gson = gson;
		this.baseDir = baseDir;
	}

	@Override
	public void export(ExportPayload<? extends ExportRecord> payload)
	{
		long accountHash = payload.getRecord().getAccount().getAccountHash();
		DataType dataType = payload.getDataType();
		String identifier = dataType.getIdentifier();

		File accountDir = new File(baseDir, PLUGIN_DIR_NAME + File.separator + accountHash);
		if (!accountDir.exists() && !accountDir.mkdirs())
		{
			log.error("Failed to create export directory: {}", accountDir.getAbsolutePath());
			return;
		}

		File outputFile = new File(accountDir, identifier + FILE_EXTENSION);

		try (OutputStreamWriter writer = new OutputStreamWriter(
			new FileOutputStream(outputFile), StandardCharsets.UTF_8))
		{
			gson.toJson(payload, writer);
			log.debug("Exported {} data for account {} to {}", identifier, accountHash, outputFile.getAbsolutePath());
		}
		catch (IOException e)
		{
			log.error("Failed to export {} data for account {}", identifier, accountHash, e);
		}
	}

	@Override
	public ExportType getType()
	{
		return ExportType.LOCAL_STORAGE;
	}
}
