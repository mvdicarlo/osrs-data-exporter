package com.osrsdataexporter.model.entry;

import lombok.Value;

/**
 * Represents a single skill entry in a skills snapshot.
 */
@Value
public class SkillEntry
{
	/**
	 * The name of the skill (e.g. "Attack", "Woodcutting").
	 */
	String skillName;

	/**
	 * Total accumulated XP in this skill.
	 */
	int xp;

	/**
	 * The real (unmodified) level of this skill (1-99).
	 */
	int level;
}
