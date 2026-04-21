package com.osrsdataexporter.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.osrsdataexporter.model.BankItemEntry;
import com.osrsdataexporter.model.BankRecord;
import com.osrsdataexporter.model.DataType;
import com.osrsdataexporter.model.ExportPayload;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LocalStorageExporterTest
{
	private static final long ACCOUNT_HASH = 123456789L;
	private static final Instant TIMESTAMP = Instant.parse("2026-04-21T12:00:00Z");

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private Gson gson;
	private LocalStorageExporter exporter;

	@Before
	public void setUp()
	{
		gson = new GsonBuilder()
			.registerTypeAdapter(Instant.class,
				(JsonSerializer<Instant>) (src, type, ctx) -> new JsonPrimitive(src.toString()))
			.create();
		exporter = new LocalStorageExporter(gson, tempFolder.getRoot());
	}

	@Test
	public void getType_returnsLocalStorage()
	{
		assertEquals(ExportType.LOCAL_STORAGE, exporter.getType());
	}

	@Test
	public void export_createsDirectoryAndWritesFile() throws IOException
	{
		ExportPayload<BankRecord> payload = buildPayload(Collections.singletonList(
			new BankItemEntry(4151, "Abyssal whip", 1)
		));

		exporter.export(payload);

		File outputFile = expectedOutputFile();
		assertTrue("Output file should exist", outputFile.exists());

		String json = new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
		assertFalse("JSON should not be empty", json.isEmpty());
	}

	@Test
	public void export_writesCorrectJsonStructure() throws IOException
	{
		ExportPayload<BankRecord> payload = buildPayload(Arrays.asList(
			new BankItemEntry(4151, "Abyssal whip", 1),
			new BankItemEntry(995, "Coins", 50000)
		));

		exporter.export(payload);

		String json = new String(Files.readAllBytes(expectedOutputFile().toPath()), StandardCharsets.UTF_8);
		JsonObject root = gson.fromJson(json, JsonObject.class);

		assertEquals("BANK", root.get("dataType").getAsString());
		JsonObject record = root.getAsJsonObject("record");
		assertEquals(ACCOUNT_HASH, record.get("accountHash").getAsLong());
		assertEquals(2, record.getAsJsonArray("items").size());
	}

	@Test
	public void export_writesCorrectItemData() throws IOException
	{
		ExportPayload<BankRecord> payload = buildPayload(Collections.singletonList(
			new BankItemEntry(4151, "Abyssal whip", 3)
		));

		exporter.export(payload);

		String json = new String(Files.readAllBytes(expectedOutputFile().toPath()), StandardCharsets.UTF_8);
		JsonObject item = gson.fromJson(json, JsonObject.class)
			.getAsJsonObject("record")
			.getAsJsonArray("items")
			.get(0).getAsJsonObject();

		assertEquals(4151, item.get("itemId").getAsInt());
		assertEquals("Abyssal whip", item.get("itemName").getAsString());
		assertEquals(3, item.get("quantity").getAsInt());
	}

	@Test
	public void export_overwritesExistingFile() throws IOException
	{
		ExportPayload<BankRecord> firstPayload = buildPayload(Collections.singletonList(
			new BankItemEntry(4151, "Abyssal whip", 1)
		));
		exporter.export(firstPayload);

		ExportPayload<BankRecord> secondPayload = buildPayload(Collections.singletonList(
			new BankItemEntry(995, "Coins", 99999)
		));
		exporter.export(secondPayload);

		String json = new String(Files.readAllBytes(expectedOutputFile().toPath()), StandardCharsets.UTF_8);
		JsonObject item = gson.fromJson(json, JsonObject.class)
			.getAsJsonObject("record")
			.getAsJsonArray("items")
			.get(0).getAsJsonObject();

		assertEquals(995, item.get("itemId").getAsInt());
		assertEquals("Coins", item.get("itemName").getAsString());
	}

	@Test
	public void export_emptyBank_writesEmptyItemsList() throws IOException
	{
		ExportPayload<BankRecord> payload = buildPayload(Collections.emptyList());

		exporter.export(payload);

		String json = new String(Files.readAllBytes(expectedOutputFile().toPath()), StandardCharsets.UTF_8);
		JsonObject record = gson.fromJson(json, JsonObject.class).getAsJsonObject("record");
		assertEquals(0, record.getAsJsonArray("items").size());
	}

	@Test
	public void export_createsAccountSpecificDirectory()
	{
		ExportPayload<BankRecord> payload = buildPayload(Collections.emptyList());

		exporter.export(payload);

		File accountDir = new File(tempFolder.getRoot(),
			"osrs-data-exporter" + File.separator + ACCOUNT_HASH);
		assertTrue("Account directory should exist", accountDir.exists());
		assertTrue("Account directory should be a directory", accountDir.isDirectory());
	}

	@Test
	public void export_usesCorrectFileName()
	{
		ExportPayload<BankRecord> payload = buildPayload(Collections.emptyList());

		exporter.export(payload);

		File outputFile = expectedOutputFile();
		assertEquals("bank.json", outputFile.getName());
	}

	private ExportPayload<BankRecord> buildPayload(java.util.List<BankItemEntry> items)
	{
		BankRecord record = new BankRecord(ACCOUNT_HASH, TIMESTAMP, items);
		return new ExportPayload<>(DataType.BANK, record);
	}

	private File expectedOutputFile()
	{
		return new File(tempFolder.getRoot(),
			"osrs-data-exporter" + File.separator + ACCOUNT_HASH + File.separator + "bank.json");
	}
}
