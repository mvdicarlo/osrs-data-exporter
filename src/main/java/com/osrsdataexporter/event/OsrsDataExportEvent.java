package com.osrsdataexporter.event;

import java.util.Map;
import lombok.Value;

/**
 * Event posted to RuneLite's {@code EventBus} after data has been exported.
 *
 * <p>Designed for cross-plugin consumption — all fields use JDK types only,
 * so subscribers do not need a compile-time dependency on this plugin's
 * internal models.</p>
 *
 * <p><strong>Threading:</strong> This event is posted on the background
 * export thread, not the client thread. Subscribers that need client-thread
 * access should use {@code ClientThread.invokeLater()}.</p>
 *
 * <p>Example subscription from another plugin:</p>
 * <pre>{@code
 * @Subscribe
 * public void onOsrsDataExportEvent(OsrsDataExportEvent event)
 * {
 *     if ("bank".equals(event.getDataType()))
 *     {
 *         Map<String, Object> record = (Map<String, Object>) event.getData().get("record");
 *         // process bank data...
 *     }
 * }
 * }</pre>
 */
@Value
public class OsrsDataExportEvent
{
	/**
	 * The data type identifier (e.g. "bank", "inventory", "skills", "group-storage").
	 */
	String dataType;

	/**
	 * The account hash of the player whose data was exported.
	 */
	long accountHash;

	/**
	 * The exported data as a plain map, mirroring the JSON structure.
	 * Contains "dataType" (String) and "record" (Map) keys.
	 * No dependency on internal model classes.
	 */
	Map<String, Object> data;
}
