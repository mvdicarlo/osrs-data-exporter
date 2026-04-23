package com.osrsdataexporter.datasource;

import com.osrsdataexporter.OsrsDataExporterConfig;
import com.osrsdataexporter.model.AccountContext;
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
import net.runelite.api.events.StatChanged;

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

	@Override
	public void onAccountChanged()
	{
		lastKnownXp.clear();
	}

	@Override
	public boolean canHandle(Object event)
	{
		return event instanceof StatChanged;
	}

	@Override
	public void handleEvent(
		Object event,
		AccountContext account,
		ScheduledExecutorService executor,
		Consumer<ExportPayload<? extends ExportRecord>> dispatcher)
	{
		if (!hasXpChanged((StatChanged) event))
		{
			return;
		}

		ExportPayload<SkillsRecord> payload = snapshot(account);
		scheduleExport(payload, executor, dispatcher);
	}

	/**
	 * Returns {@code true} if the event represents a real XP gain for its skill,
	 * updating the tracked value. Returns {@code false} for temporary boosts,
	 * stat drains, and HP regeneration where XP is unchanged.
	 */
	private boolean hasXpChanged(StatChanged event)
	{
		int newXp = event.getXp();
		Integer previousXp = lastKnownXp.put(event.getSkill(), newXp);
		return previousXp == null || previousXp != newXp;
	}

	private ExportPayload<SkillsRecord> snapshot(AccountContext account)
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

		SkillsRecord record = new SkillsRecord(account, Instant.now(), entries);
		return new ExportPayload<>(record);
	}
}
