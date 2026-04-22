package com.osrsdataexporter.model;

import java.time.Instant;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * A snapshot of all skill levels and XP for a player at a point in time.
 * Extends {@link ExportRecord} to inherit account and timestamp context.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class SkillsRecord extends ExportRecord
{
	/**
	 * The list of skill entries at the time of the snapshot.
	 */
	List<SkillEntry> skills;

	public SkillsRecord(AccountContext account, Instant timestamp, List<SkillEntry> skills)
	{
		super(account, timestamp, DataType.SKILLS);
		this.skills = skills;
	}
}
