package com.osrsdataexporter;

import java.util.Set;
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

	/** Config keys that control export targets — used to detect runtime changes. */
	Set<String> EXPORT_TARGET_KEYS = Set.of(
		"enableLocalStorage",
		"enableAzureBlobStorage",
		"azureBlobConnectionString",
		"azureBlobContainerName"
	);

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
		return false;
	}

	@ConfigItem(
		keyName = "exportGroupStorageData",
		name = "Export Group Storage Data",
		description = "Export a snapshot of Group Ironman shared storage whenever it is updated.",
		section = dataSourcesSection,
		position = 3
	)
	default boolean exportGroupStorageData()
	{
		return true;
	}

	@ConfigItem(
		keyName = "exportEquipmentData",
		name = "Export Equipment Data",
		description = "Export a snapshot of worn equipment, including combat stats, whenever gear changes.",
		section = dataSourcesSection,
		position = 4
	)
	default boolean exportEquipmentData()
	{
		return true;
	}

	@ConfigItem(
		keyName = "exportGrandExchangeData",
		name = "Export Grand Exchange Data",
		description = "Export a record for each GE offer when it is placed, completed, or cancelled.",
		section = dataSourcesSection,
		position = 5
	)
	default boolean exportGrandExchangeData()
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

	@ConfigItem(
		keyName = "enableAzureBlobStorage",
		name = "Enable Azure Blob Storage",
		description = "Upload exported JSON payloads to Azure Blob Storage.",
		section = exportTargetsSection,
		position = 1
	)
	default boolean enableAzureBlobStorage()
	{
		return false;
	}

	@ConfigItem(
		keyName = "azureBlobConnectionString",
		name = "Azure Blob Connection String",
		description = "Connection string for the Azure Storage account.",
		section = exportTargetsSection,
		position = 2,
		secret = true
	)
	default String azureBlobConnectionString()
	{
		return "";
	}

	@ConfigItem(
		keyName = "azureBlobContainerName",
		name = "Azure Blob Container",
		description = "Container name used for exported data blobs.",
		section = exportTargetsSection,
		position = 3
	)
	default String azureBlobContainerName()
	{
		return "osrs-data-exporter";
	}
}
