package com.osrsdataexporter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

/**
 * Configuration interface for the OSRS Data Exporter plugin.
 * Settings are persisted across client launches via RuneLite's ConfigManager.
 */
@ConfigGroup(OsrsDataExporterConfig.CONFIG_GROUP)
public interface OsrsDataExporterConfig extends Config
{
	String CONFIG_GROUP = "osrsdataexporter";

	@ConfigSection(
		name = "Data Sources",
		description = "Toggle which data types are exported.",
		position = 0
	)
	String dataSourcesSection = "dataSources";

	@ConfigSection(
		name = "Export Targets",
		description = "Configure where data is exported to.",
		position = 1
	)
	String exportTargetsSection = "exportTargets";

	@ConfigItem(
		keyName = "exportBankData",
		name = "Export Bank Data",
		description = "Export a snapshot of bank contents whenever the bank is updated.",
		section = dataSourcesSection,
		position = 0
	)
	default boolean exportBankData()
	{
		return true;
	}

	@ConfigItem(
		keyName = "exportInventoryData",
		name = "Export Inventory Data",
		description = "Export a snapshot of inventory contents whenever the inventory is updated.",
		section = dataSourcesSection,
		position = 1
	)
	default boolean exportInventoryData()
	{
		return true;
	}

	@ConfigItem(
		keyName = "exportSkillsData",
		name = "Export Skills Data",
		description = "Export a snapshot of skill levels and XP whenever a skill changes.",
		section = dataSourcesSection,
		position = 2
	)
	default boolean exportSkillsData()
	{
		return true;
	}

	@ConfigItem(
		keyName = "enableLocalStorage",
		name = "Enable Local Storage",
		description = "Save exported data as JSON files in the .runelite/osrs-data-exporter directory.",
		section = exportTargetsSection,
		position = 0
	)
	default boolean enableLocalStorage()
	{
		return true;
	}
}
