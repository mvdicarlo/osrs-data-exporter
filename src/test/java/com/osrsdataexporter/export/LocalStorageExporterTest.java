package com.osrsdataexporter.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.osrsdataexporter.model.ItemEntry;
import com.osrsdataexporter.model.BankRecord;
import com.osrsdataexporter.model.ExportPayload;
import com.osrsdataexporter.model.GroupStorageRecord;
import com.osrsdataexporter.model.InventoryRecord;
import com.osrsdataexporter.model.SkillEntry;
import com.osrsdataexporter.model.SkillsRecord;
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
			new ItemEntry(4151, "Abyssal whip", 1, true, true, 2560)
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
			new ItemEntry(4151, "Abyssal whip", 1, true, true, 2560),
			new ItemEntry(995, "Coins", 50000, false, true, 1)
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
			new ItemEntry(4151, "Abyssal whip", 3, true, true, 2560)
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
			new ItemEntry(4151, "Abyssal whip", 1, true, true, 2560)
		));
		exporter.export(firstPayload);

		ExportPayload<BankRecord> secondPayload = buildPayload(Collections.singletonList(
			new ItemEntry(995, "Coins", 99999, false, true, 1)
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

	@Test
	public void export_inventoryWritesCorrectFile() throws IOException
	{
		ExportPayload<InventoryRecord> payload = buildInventoryPayload(Arrays.asList(
			new ItemEntry(4151, "Abyssal whip", 1, true, true, 2560),
			new ItemEntry(385, "Shark", 10, true, true, 200)
		));

		exporter.export(payload);

		File outputFile = expectedInventoryOutputFile();
		assertTrue("Inventory output file should exist", outputFile.exists());
		assertEquals("inventory.json", outputFile.getName());

		String json = new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
		JsonObject root = gson.fromJson(json, JsonObject.class);

		assertEquals("INVENTORY", root.get("dataType").getAsString());
		JsonObject record = root.getAsJsonObject("record");
		assertEquals(ACCOUNT_HASH, record.get("accountHash").getAsLong());
		assertEquals(2, record.getAsJsonArray("items").size());
	}

	@Test
	public void export_inventoryAndBankWriteSeparateFiles() throws IOException
	{
		exporter.export(buildPayload(Collections.singletonList(
			new ItemEntry(995, "Coins", 50000, false, true, 1)
		)));
		exporter.export(buildInventoryPayload(Collections.singletonList(
			new ItemEntry(4151, "Abyssal whip", 1, true, true, 2560)
		)));

		assertTrue("Bank file should exist", expectedOutputFile().exists());
		assertTrue("Inventory file should exist", expectedInventoryOutputFile().exists());

		String bankJson = new String(Files.readAllBytes(expectedOutputFile().toPath()), StandardCharsets.UTF_8);
		String invJson = new String(Files.readAllBytes(expectedInventoryOutputFile().toPath()), StandardCharsets.UTF_8);

		assertEquals(995, gson.fromJson(bankJson, JsonObject.class)
			.getAsJsonObject("record").getAsJsonArray("items")
			.get(0).getAsJsonObject().get("itemId").getAsInt());
		assertEquals(4151, gson.fromJson(invJson, JsonObject.class)
			.getAsJsonObject("record").getAsJsonArray("items")
			.get(0).getAsJsonObject().get("itemId").getAsInt());
	}

	private ExportPayload<BankRecord> buildPayload(java.util.List<ItemEntry> items)
	{
		BankRecord record = new BankRecord(ACCOUNT_HASH, TIMESTAMP, items);
		return new ExportPayload<>(record);
	}

	private ExportPayload<InventoryRecord> buildInventoryPayload(java.util.List<ItemEntry> items)
	{
		InventoryRecord record = new InventoryRecord(ACCOUNT_HASH, TIMESTAMP, items);
		return new ExportPayload<>(record);
	}

	private ExportPayload<SkillsRecord> buildSkillsPayload(java.util.List<SkillEntry> skills)
	{
		SkillsRecord record = new SkillsRecord(ACCOUNT_HASH, TIMESTAMP, skills);
		return new ExportPayload<>(record);
	}

	private File expectedSkillsOutputFile()
	{
		return new File(tempFolder.getRoot(),
			"osrs-data-exporter" + File.separator + ACCOUNT_HASH + File.separator + "skills.json");
	}

	@Test
	public void export_skillsWritesCorrectFile() throws IOException
	{
		ExportPayload<SkillsRecord> payload = buildSkillsPayload(Arrays.asList(
			new SkillEntry("Attack", 83776, 70),
			new SkillEntry("Hitpoints", 1210421, 80)
		));

		exporter.export(payload);

		File outputFile = expectedSkillsOutputFile();
		assertTrue("Skills output file should exist", outputFile.exists());
		assertEquals("skills.json", outputFile.getName());

		String json = new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
		JsonObject root = gson.fromJson(json, JsonObject.class);

		assertEquals("SKILLS", root.get("dataType").getAsString());
		JsonObject record = root.getAsJsonObject("record");
		assertEquals(ACCOUNT_HASH, record.get("accountHash").getAsLong());
		assertEquals(2, record.getAsJsonArray("skills").size());
	}

	@Test
	public void export_skillsWritesCorrectSkillData() throws IOException
	{
		ExportPayload<SkillsRecord> payload = buildSkillsPayload(Collections.singletonList(
			new SkillEntry("Woodcutting", 1210421, 80)
		));

		exporter.export(payload);

		JsonObject skill = gson.fromJson(
				new String(Files.readAllBytes(expectedSkillsOutputFile().toPath()), StandardCharsets.UTF_8),
				JsonObject.class)
			.getAsJsonObject("record")
			.getAsJsonArray("skills")
			.get(0).getAsJsonObject();

		assertEquals("Woodcutting", skill.get("skillName").getAsString());
		assertEquals(1210421, skill.get("xp").getAsInt());
		assertEquals(80, skill.get("level").getAsInt());
	}

	private File expectedOutputFile()
	{
		return new File(tempFolder.getRoot(),
			"osrs-data-exporter" + File.separator + ACCOUNT_HASH + File.separator + "bank.json");
	}

	private File expectedInventoryOutputFile()
	{
		return new File(tempFolder.getRoot(),
			"osrs-data-exporter" + File.separator + ACCOUNT_HASH + File.separator + "inventory.json");
	}

	private File expectedGroupStorageOutputFile()
	{
		return new File(tempFolder.getRoot(),
			"osrs-data-exporter" + File.separator + ACCOUNT_HASH + File.separator + "group-storage.json");
	}

	private ExportPayload<GroupStorageRecord> buildGroupStoragePayload(java.util.List<ItemEntry> items)
	{
		GroupStorageRecord record = new GroupStorageRecord(ACCOUNT_HASH, TIMESTAMP, items);
		return new ExportPayload<>(record);
	}

	@Test
	public void export_groupStorageWritesCorrectFile() throws IOException
	{
		ExportPayload<GroupStorageRecord> payload = buildGroupStoragePayload(Arrays.asList(
			new ItemEntry(4151, "Abyssal whip", 1, true, true, 2560),
			new ItemEntry(385, "Shark", 25, true, true, 200)
		));

		exporter.export(payload);

		File outputFile = expectedGroupStorageOutputFile();
		assertTrue("Group storage output file should exist", outputFile.exists());
		assertEquals("group-storage.json", outputFile.getName());

		String json = new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
		JsonObject root = gson.fromJson(json, JsonObject.class);

		assertEquals("GROUP_STORAGE", root.get("dataType").getAsString());
		JsonObject record = root.getAsJsonObject("record");
		assertEquals(ACCOUNT_HASH, record.get("accountHash").getAsLong());
		assertEquals(2, record.getAsJsonArray("items").size());
	}

	@Test
	public void export_groupStorageWritesSeparateFromBank() throws IOException
	{
		exporter.export(buildPayload(Collections.singletonList(
			new ItemEntry(995, "Coins", 50000, false, true, 1)
		)));
		exporter.export(buildGroupStoragePayload(Collections.singletonList(
			new ItemEntry(4151, "Abyssal whip", 1, true, true, 2560)
		)));

		assertTrue("Bank file should exist", expectedOutputFile().exists());
		assertTrue("Group storage file should exist", expectedGroupStorageOutputFile().exists());

		String bankJson = new String(Files.readAllBytes(expectedOutputFile().toPath()), StandardCharsets.UTF_8);
		String gsJson = new String(Files.readAllBytes(expectedGroupStorageOutputFile().toPath()), StandardCharsets.UTF_8);

		assertEquals(995, gson.fromJson(bankJson, JsonObject.class)
			.getAsJsonObject("record").getAsJsonArray("items")
			.get(0).getAsJsonObject().get("itemId").getAsInt());
		assertEquals(4151, gson.fromJson(gsJson, JsonObject.class)
			.getAsJsonObject("record").getAsJsonArray("items")
			.get(0).getAsJsonObject().get("itemId").getAsInt());
	}
}
