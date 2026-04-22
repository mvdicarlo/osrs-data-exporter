package com.osrsdataexporter.export;

import com.google.gson.Gson;
import com.osrsdataexporter.OsrsDataExporterConfig;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataExporterFactoryTest
{
	private OsrsDataExporterConfig config;
	private Gson gson;
	private DataExporterFactory factory;

	@Before
	public void setUp()
	{
		config = mock(OsrsDataExporterConfig.class);
		gson = new Gson();
		factory = new DataExporterFactory(config, gson);

		when(config.enableAzureBlobStorage()).thenReturn(false);
	}

	@Test
	public void init_withLocalStorageEnabled_registersExporter()
	{
		when(config.enableLocalStorage()).thenReturn(true);

		factory.init();

		List<DataExporter> exporters = factory.getActiveExporters();
		assertEquals(1, exporters.size());
		assertEquals(ExportType.LOCAL_STORAGE, exporters.get(0).getType());
	}

	@Test
	public void init_withLocalStorageDisabled_registersNoExporters()
	{
		when(config.enableLocalStorage()).thenReturn(false);

		factory.init();

		assertTrue(factory.getActiveExporters().isEmpty());
	}

	@Test
	public void init_reinitializes_reflectsNewConfig()
	{
		when(config.enableLocalStorage()).thenReturn(true);
		factory.init();
		assertEquals(1, factory.getActiveExporters().size());

		when(config.enableLocalStorage()).thenReturn(false);
		factory.init();
		assertTrue(factory.getActiveExporters().isEmpty());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void getActiveExporters_returnsUnmodifiableList()
	{
		when(config.enableLocalStorage()).thenReturn(true);
		factory.init();

		factory.getActiveExporters().clear();
	}

	@Test
	public void shutdown_clearsAllExporters()
	{
		when(config.enableLocalStorage()).thenReturn(true);
		factory.init();
		assertEquals(1, factory.getActiveExporters().size());

		factory.shutdown();
		assertTrue(factory.getActiveExporters().isEmpty());
	}

	@Test
	public void init_withAzureEnabledAndValidConfig_registersAzureExporter()
	{
		when(config.enableLocalStorage()).thenReturn(false);
		when(config.enableAzureBlobStorage()).thenReturn(true);
		when(config.azureBlobConnectionString()).thenReturn(
			"DefaultEndpointsProtocol=https;AccountName=testaccount;AccountKey=dGVzdGtleQ==;EndpointSuffix=core.windows.net");
		when(config.azureBlobContainerName()).thenReturn("osrs-data-exporter");

		factory.init();

		List<DataExporter> exporters = factory.getActiveExporters();
		assertEquals(1, exporters.size());
		assertEquals(ExportType.AZURE_BLOB_STORAGE, exporters.get(0).getType());
	}

	@Test
	public void init_withAzureEnabledAndEmptyConnectionString_registersNoAzureExporter()
	{
		when(config.enableLocalStorage()).thenReturn(false);
		when(config.enableAzureBlobStorage()).thenReturn(true);
		when(config.azureBlobConnectionString()).thenReturn("");
		when(config.azureBlobContainerName()).thenReturn("osrs-data-exporter");

		factory.init();

		assertTrue(factory.getActiveExporters().isEmpty());
	}

	@Test
	public void init_withLocalAndAzureEnabled_registersBothExporters()
	{
		when(config.enableLocalStorage()).thenReturn(true);
		when(config.enableAzureBlobStorage()).thenReturn(true);
		when(config.azureBlobConnectionString()).thenReturn(
			"DefaultEndpointsProtocol=https;AccountName=testaccount;AccountKey=dGVzdGtleQ==;EndpointSuffix=core.windows.net");
		when(config.azureBlobContainerName()).thenReturn("osrs-data-exporter");

		factory.init();

		List<DataExporter> exporters = factory.getActiveExporters();
		assertEquals(2, exporters.size());
		assertTrue(exporters.stream().anyMatch(e -> e.getType() == ExportType.LOCAL_STORAGE));
		assertTrue(exporters.stream().anyMatch(e -> e.getType() == ExportType.AZURE_BLOB_STORAGE));
	}
}
