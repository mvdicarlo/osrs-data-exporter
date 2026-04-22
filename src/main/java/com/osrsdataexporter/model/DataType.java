package com.osrsdataexporter.model;

/**
 * Enumerates the types of data that can be exported.
 * Each value represents a distinct data source (e.g. bank, inventory)
 * and defines the identifier used for storage routing (file names, table names, etc.).
 */
public enum DataType
{
	BANK("bank"),
	INVENTORY("inventory"),
	SKILLS("skills"),
	GROUP_STORAGE("group-storage");

	private final String identifier;

	DataType(String identifier)
	{
		this.identifier = identifier;
	}

	/**
	 * Returns the string identifier used for storage routing
	 * (e.g. file names, API endpoints, table names).
	 *
	 * @return the data type identifier
	 */
	public String getIdentifier()
	{
		return identifier;
	}
}
