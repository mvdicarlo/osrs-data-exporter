package com.osrsdataexporter.datasource;

import com.osrsdataexporter.OsrsDataExporterConfig;
import com.osrsdataexporter.model.DataType;
import com.osrsdataexporter.model.ExportPayload;
import com.osrsdataexporter.model.ExportRecord;
import com.osrsdataexporter.model.SkillEntry;
import com.osrsdataexporter.model.SkillsRecord;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import net.runelite.api.Client;
import net.runelite.api.Skill;

/**
 * Data source for skills snapshots. Uses a longer debounce than item
 * containers because XP ticks fire very rapidly during combat or skilling.
 */
public class SkillsDataSource extends DataSourceHandler<SkillsRecord>
{
	public SkillsDataSource(Client client, OsrsDataExporterConfig config)
	{
		super(client, config, DataType.SKILLS, 5000);
	}

	@Override
	public boolean isEnabled()
	{
		return config.exportSkillsData();
	}

	/**
	 * Handles a stat change. Checks if enabled,
	 * snapshots all skills, and schedules a debounced export.
	 */
	public void handleStatChanged(
		long accountHash,
		ScheduledExecutorService executor,
		Consumer<ExportPayload<? extends ExportRecord>> dispatcher)
	{
		if (!isEnabled())
		{
			return;
		}

		ExportPayload<SkillsRecord> payload = snapshot(accountHash);
		scheduleExport(payload, executor, dispatcher);
	}

	private ExportPayload<SkillsRecord> snapshot(long accountHash)
	{
		Skill[] skills = Skill.values();
		List<SkillEntry> entries = new ArrayList<>(skills.length);

		for (Skill skill : skills)
		{
			entries.add(new SkillEntry(
				skill.getName(),
				client.getSkillExperience(skill),
				client.getRealSkillLevel(skill)
			));
		}

		SkillsRecord record = new SkillsRecord(accountHash, Instant.now(), entries);
		return new ExportPayload<>(DataType.SKILLS, record);
	}
}
