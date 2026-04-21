package com.osrsdataexporter;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class OsrsDataExporterPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(OsrsDataExporterPlugin.class);
		RuneLite.main(args);
	}
}
