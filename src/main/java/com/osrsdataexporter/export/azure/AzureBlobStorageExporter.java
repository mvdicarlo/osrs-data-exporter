package com.osrsdataexporter.export.azure;

import com.google.gson.Gson;
import com.osrsdataexporter.export.DataExporter;
import com.osrsdataexporter.export.ExportType;
import com.osrsdataexporter.model.ExportPayload;
import com.osrsdataexporter.model.record.ExportRecord;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.util.concurrent.TimeUnit;

/**
 * Exports data to Azure Blob Storage.
 */
@Slf4j
public class AzureBlobStorageExporter implements DataExporter
{
	private static final String X_MS_VERSION = "2021-12-02";
	private static final int CONNECT_TIMEOUT_MS = 10_000;
	private static final int READ_TIMEOUT_MS = 20_000;
	private static final String PLUGIN_BLOB_PREFIX = "osrs-data-exporter";
	private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
	private static final Pattern ACCOUNT_NAME_PATTERN = Pattern.compile("^[a-z0-9]{3,24}$");

	private final Gson gson;
	private final OkHttpClient httpClient;
	private final String accountName;
	private final byte[] accountKey;
	private final String blobServiceEndpoint;
	private final String containerName;

	private volatile boolean containerReady;

	public AzureBlobStorageExporter(Gson gson, String connectionString, String containerName)
	{
		this.gson = gson;
		this.httpClient = new OkHttpClient.Builder()
			.connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
			.readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
			.writeTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
			.build();

		Map<String, String> values = parseConnectionString(connectionString);
		this.accountName = parseAccountName(values);
		this.accountKey = parseAccountKey(values);
		this.blobServiceEndpoint = resolveBlobServiceEndpoint(values, this.accountName);
		this.containerName = containerName.trim().toLowerCase(Locale.ROOT);

		if (this.containerName.isEmpty())
		{
			throw new IllegalArgumentException("Azure Blob container name cannot be empty");
		}
	}

	@Override
	public void export(ExportPayload<? extends ExportRecord> payload)
	{
		try
		{
			ensureContainerExists();
			uploadPayload(payload);
		}
		catch (Exception e)
		{
			log.error("Failed to export {} data to Azure Blob Storage for account {}",
				payload.getDataType().getIdentifier(), payload.getRecord().getAccount().getAccountHash(), e);
		}
	}

	@Override
	public ExportType getType()
	{
		return ExportType.AZURE_BLOB_STORAGE;
	}

	private void uploadPayload(ExportPayload<? extends ExportRecord> payload) throws Exception
	{
		String blobName = buildBlobName(payload);
		byte[] body = gson.toJson(payload).getBytes(StandardCharsets.UTF_8);

		String url = blobServiceEndpoint + "/" + encodePath(containerName) + "/" + encodePath(blobName);

		String requestDate = rfc1123Now();
		Map<String, String> xMsHeaders = new TreeMap<>();
		xMsHeaders.put("x-ms-blob-type", "BlockBlob");
		xMsHeaders.put("x-ms-date", requestDate);
		xMsHeaders.put("x-ms-version", X_MS_VERSION);

		String auth = sharedKeyAuthorization(
			"PUT",
			"application/json; charset=utf-8",
			Integer.toString(body.length),
			xMsHeaders,
			canonicalizedResource("/" + containerName + "/" + blobName, Collections.emptyMap())
		);

		Request request = new Request.Builder()
			.url(url)
			.put(RequestBody.create(JSON_MEDIA_TYPE, body))
			.header("Authorization", auth)
			.header("x-ms-date", requestDate)
			.header("x-ms-version", X_MS_VERSION)
			.header("x-ms-blob-type", "BlockBlob")
			.build();

		try (Response response = httpClient.newCall(request).execute())
		{
			if (response.isSuccessful())
			{
				log.debug("Exported {} data for account {} to Azure blob {}/{}",
					payload.getDataType().getIdentifier(), payload.getRecord().getAccount().getAccountHash(), containerName, blobName);
				return;
			}

			throw new IOException("Azure Blob PUT failed with HTTP "
				+ response.code() + ": " + readResponseBody(response));
		}
	}

	private synchronized void ensureContainerExists() throws Exception
	{
		if (containerReady)
		{
			return;
		}

		String url = blobServiceEndpoint + "/" + encodePath(containerName) + "?restype=container";

		String requestDate = rfc1123Now();
		Map<String, String> xMsHeaders = new TreeMap<>();
		xMsHeaders.put("x-ms-date", requestDate);
		xMsHeaders.put("x-ms-version", X_MS_VERSION);

		String auth = sharedKeyAuthorization(
			"PUT",
			"",
			"",
			xMsHeaders,
			canonicalizedResource("/" + containerName, Collections.singletonMap("restype", "container"))
		);

		Request request = new Request.Builder()
			.url(url)
			.put(RequestBody.create(null, new byte[0]))
			.header("Authorization", auth)
			.header("x-ms-date", requestDate)
			.header("x-ms-version", X_MS_VERSION)
			.build();

		try (Response response = httpClient.newCall(request).execute())
		{
			int status = response.code();
			if (status == 201 || status == 409)
			{
				containerReady = true;
				return;
			}

			throw new IOException("Azure Blob container ensure failed with HTTP "
				+ status + ": " + readResponseBody(response));
		}
	}

	private static String readResponseBody(Response response) throws IOException
	{
		return response.body() == null ? "" : response.body().string();
	}

	private String buildBlobName(ExportPayload<? extends ExportRecord> payload)
	{
		long accountHash = payload.getRecord().getAccount().getAccountHash();
		String identifier = payload.getDataType().getIdentifier();
		return PLUGIN_BLOB_PREFIX + "/" + accountHash + "/" + identifier + ".json";
	}

	private String sharedKeyAuthorization(
		String method,
		String contentType,
		String contentLength,
		Map<String, String> xMsHeaders,
		String canonicalizedResource) throws Exception
	{
		String canonicalizedHeaders = canonicalizedHeaders(xMsHeaders);

		String stringToSign = method + "\n"
			+ "\n"                 // Content-Encoding
			+ "\n"                 // Content-Language
			+ contentLength + "\n" // Content-Length (empty for 0-length in x-ms-version >= 2015-02-21)
			+ "\n"                 // Content-MD5
			+ contentType + "\n"   // Content-Type
			+ "\n"                 // Date
			+ "\n"                 // If-Modified-Since
			+ "\n"                 // If-Match
			+ "\n"                 // If-None-Match
			+ "\n"                 // If-Unmodified-Since
			+ "\n"                 // Range
			+ canonicalizedHeaders
			+ canonicalizedResource;

		Mac hmac = Mac.getInstance("HmacSHA256");
		hmac.init(new SecretKeySpec(accountKey, "HmacSHA256"));
		byte[] signatureBytes = hmac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
		String signature = Base64.getEncoder().encodeToString(signatureBytes);
		return "SharedKey " + accountName + ":" + signature;
	}

	private static String canonicalizedHeaders(Map<String, String> xMsHeaders)
	{
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : xMsHeaders.entrySet())
		{
			sb.append(entry.getKey().toLowerCase(Locale.ROOT))
				.append(':')
				.append(entry.getValue().trim())
				.append('\n');
		}
		return sb.toString();
	}

	private String canonicalizedResource(String path, Map<String, String> queryParams)
	{
		StringBuilder sb = new StringBuilder("/").append(accountName).append(path);
		if (!queryParams.isEmpty())
		{
			List<String> keys = new ArrayList<>(queryParams.keySet());
			Collections.sort(keys);
			for (String key : keys)
			{
				sb.append('\n')
					.append(key.toLowerCase(Locale.ROOT))
					.append(':')
					.append(queryParams.get(key));
			}
		}
		return sb.toString();
	}

	private static String rfc1123Now()
	{
		return DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC));
	}

	private static String parseAccountName(Map<String, String> values)
	{
		String name = require(values, "AccountName");
		if (!ACCOUNT_NAME_PATTERN.matcher(name).matches())
		{
			throw new IllegalArgumentException(
				"Azure connection string AccountName must be 3-24 lowercase alphanumeric characters");
		}
		return name;
	}

	private static byte[] parseAccountKey(Map<String, String> values)
	{
		String key = require(values, "AccountKey");
		try
		{
			return Base64.getDecoder().decode(key);
		}
		catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException("Azure connection string AccountKey is not valid Base64", e);
		}
	}

	private static String resolveBlobServiceEndpoint(Map<String, String> values, String accountName)
	{
		String blobEndpoint = values.get("BlobEndpoint");
		if (blobEndpoint != null && !blobEndpoint.trim().isEmpty())
		{
			String trimmed = stripTrailingSlash(blobEndpoint.trim());
			try
			{
				URI uri = new URI(trimmed);
				if (uri.getScheme() == null || uri.getHost() == null)
				{
					throw new URISyntaxException(trimmed, "missing scheme or host");
				}
			}
			catch (URISyntaxException e)
			{
				throw new IllegalArgumentException("Azure connection string BlobEndpoint is not a valid URL", e);
			}
			return trimmed;
		}

		String protocol = values.getOrDefault("DefaultEndpointsProtocol", "https");
		if (!"https".equals(protocol))
		{
			throw new IllegalArgumentException(
				"Azure connection string DefaultEndpointsProtocol must be 'https'");
		}
		String endpointSuffix = values.getOrDefault("EndpointSuffix", "core.windows.net");
		return protocol + "://" + accountName + ".blob." + endpointSuffix;
	}

	private static Map<String, String> parseConnectionString(String connectionString)
	{
		if (connectionString == null || connectionString.trim().isEmpty())
		{
			throw new IllegalArgumentException("Azure Blob connection string is required");
		}

		Map<String, String> values = new HashMap<>();
		String[] parts = connectionString.split(";");
		for (String part : parts)
		{
			String trimmed = part.trim();
			if (trimmed.isEmpty())
			{
				continue;
			}

			int idx = trimmed.indexOf('=');
			if (idx <= 0 || idx == trimmed.length() - 1)
			{
				continue;
			}

			String key = trimmed.substring(0, idx).trim();
			String value = trimmed.substring(idx + 1).trim();
			values.put(key, value);
		}
		return values;
	}

	private static String require(Map<String, String> values, String key)
	{
		String value = values.get(key);
		if (value == null || value.isEmpty())
		{
			throw new IllegalArgumentException("Azure connection string missing required key: " + key);
		}
		return value;
	}

	private static String stripTrailingSlash(String value)
	{
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}

	private static String encodePath(String path)
	{
		String[] segments = path.split("/");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < segments.length; i++)
		{
			if (i > 0)
			{
				sb.append('/');
			}
			sb.append(URLEncoder.encode(segments[i], StandardCharsets.UTF_8));
		}
		return sb.toString().replace("+", "%20");
	}
}
