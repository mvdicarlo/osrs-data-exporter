package com.osrsdataexporter.datasource;

import com.osrsdataexporter.OsrsDataExporterConfig;
import com.osrsdataexporter.model.DataType;
import com.osrsdataexporter.model.ExportPayload;
import com.osrsdataexporter.model.ExportRecord;
import com.osrsdataexporter.model.SkillEntry;
import com.osrsdataexporter.model.SkillsRecord;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import net.runelite.api.Client;
import net.runelite.api.Skill;

/**
 * Data source for skills snapshots. Uses a longer debounce than item
 * containers because XP ticks fire very rapidly during combat or skilling.
 *
 * <p>Only triggers an export when actual XP changes — temporary boosts,
 * stat drains, and HP regeneration are ignored.</p>
 */
public class SkillsDataSource extends DataSourceHandler<SkillsRecord>
{
	private final Map<Skill, Integer> lastKnownXp = new EnumMap<>(Skill.class);

	public SkillsDataSource(Client client, OsrsDataExporterConfig config)
	{
		super(client, config, DataType.SKILLS, SKILLS_DEBOUNCE_DELAY_MS);
	}

	@Override
	public boolean isEnabled()
	{
		return config.exportSkillsData();
	}

	/**
	 * Handles a stat change. Only schedules an export if real XP has changed
	 * for at least one skill — temporary boosts and drains are ignored.
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

		if (!hasXpChanged())
		{
			return;
		}

		ExportPayload<SkillsRecord> payload = snapshot(accountHash);
		scheduleExport(payload, executor, dispatcher);
	}

	/**
	 * Checks whether any skill's XP has changed since the last check,
	 * and updates the tracked values.
	 */
	private boolean hasXpChanged()
	{
		boolean changed = false;
		for (Skill skill : Skill.values())
		{
			int currentXp = client.getSkillExperience(skill);
			Integer previousXp = lastKnownXp.put(skill, currentXp);
			if (previousXp == null || previousXp != currentXp)
			{
				changed = true;
			}
		}
		return changed;
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
		return new ExportPayload<>(record);
	}
}
