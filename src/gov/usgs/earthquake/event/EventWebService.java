package gov.usgs.earthquake.event;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * A wrapper around the Event Web Service.
 */
public class EventWebService {

	/** ISO8601 date formatting object. */
	SimpleDateFormat ISO8601_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

	/** Base URL to the event web service. */
	private final URL serviceURL;

	/**
	 * Construct an EventWebService.
	 * 
	 * @param serviceURL
	 */
	public EventWebService(final URL serviceURL) {
		this.serviceURL = serviceURL;
	}

	/**
	 * Convert an EventQuery object into an EventWebService URL, using a
	 * specific return format.
	 * 
	 * @param query
	 *            the query.
	 * @param format
	 *            the format.
	 * @return a URL for query and format.
	 * @throws MalformedURLException
	 */
	public URL getURL(final EventQuery query, final Format format)
			throws MalformedURLException {

		// fill hashmap with parameters
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("alertlevel", query.getAlertLevel());
		params.put("catalog", query.getCatalog());
		params.put("contributor", query.getContributor());
		params.put("endtime", getISO8601Date(query.getEndTime()));
		params.put("eventid", query.getEventId());
		params.put("eventtype", query.getEventType());
		params.put("format", format == null ? query.getFormat() : format);
		params.put("includeallmagnitudes", query.getIncludeAllMagnitudes());
		params.put("includeallorigins", query.getIncludeAllOrigins());
		params.put("includearrivals", query.getIncludeArrivals());
		params.put("kmlanimated", query.getKmlAnimated());
		params.put("kmlcolorby", query.getKmlColorBy());
		params.put("latitude", query.getLatitude());
		params.put("limit", query.getLimit());
		params.put("longitude", query.getLongitude());
		params.put("magnitudetype", query.getMagnitudeType());
		params.put("maxcdi", query.getMaxCdi());
		params.put("maxdepth", query.getMaxDepth());
		params.put("maxgap", query.getMaxGap());
		params.put("maxlatitude", query.getMaxLatitude());
		params.put("maxlongitude", query.getMaxLongitude());
		params.put("maxmagnitude", query.getMaxMagnitude());
		params.put("maxmmi", query.getMaxMmi());
		params.put("maxradius", query.getMaxRadius());
		params.put("maxsig", query.getMaxSig());
		params.put("mincdi", query.getMinCdi());
		params.put("mindepth", query.getMinDepth());
		params.put("minfelt", query.getMinFelt());
		params.put("mingap", query.getMinGap());
		params.put("minlatitude", query.getMinLatitude());
		params.put("minlongitude", query.getMinLongitude());
		params.put("minmagnitude", query.getMinMagnitude());
		params.put("minmmi", query.getMinMmi());
		params.put("minradius", query.getMinRadius());
		params.put("minsig", query.getMinSig());
		params.put("offset", query.getOffset());
		params.put("orderby", query.getOrderBy());
		params.put("producttype", query.getProductType());
		params.put("reviewstatus", query.getReviewStatus());
		params.put("starttime", getISO8601Date(query.getStartTime()));
		params.put("updatedafter", getISO8601Date(query.getUpdatedAfter()));

		String queryString = getQueryString(params);
		return new URL(serviceURL, "query" + queryString);
	}

	/**
	 * Request events from the event web service.
	 * 
	 * @param query
	 *            query describing events to return.
	 * @return list of events.
	 * @throws Exception
	 *             if any occur.
	 */
	public List<JSONEvent> getEvents(final EventQuery query) throws Exception {
		InputStream result = getInputStream(getURL(query, Format.GEOJSON));
		try {
			return parseJSONEventCollection(result);
		} finally {
			try {
				result.close();
			} catch (Exception e) {
				// ignore
			}
		}
	}

	/**
	 * Parse the response from event web service into an array of JSONEvent
	 * objects.
	 * 
	 * @param input
	 *            input stream response from event web service.
	 * @return list of parsed events
	 * @throws Exception
	 *             if format is unexpected.
	 */
	public List<JSONEvent> parseJSONEventCollection(final InputStream input)
			throws Exception {
		JSONParser parser = new JSONParser();

		// parse feature collection into objects
		JSONObject collection = JSONUtil.getJSONObject(parser
				.parse(new InputStreamReader(input)));
		if (collection == null) {
			throw new Exception("Expected feature collection");
		}
		JSONArray features = JSONUtil.getJSONArray(collection.get("features"));
		if (features == null) {
			throw new Exception("Expected features");
		}

		// parse features into eventss
		ArrayList<JSONEvent> events = new ArrayList<JSONEvent>(features.size());
		Iterator<?> iter = features.iterator();
		while (iter.hasNext()) {
			JSONObject next = JSONUtil.getJSONObject(iter.next());
			if (next == null) {
				throw new Exception("Expected feature");
			}
			events.add(new JSONEvent(next));
		}

		return events;

	}

	/**
	 * Utility method to encode a Date using ISO8601, when not null.
	 * 
	 * @param date
	 *            date to encode.
	 * @return iso8601 encoded date, or null if date is null.
	 */
	public String getISO8601Date(final Date date) {
		if (date == null) {
			return null;
		}
		return ISO8601_FORMAT.format(date);
	}

	/**
	 * Open an InputStream, attempting to use gzip compression.
	 * 
	 * @param url
	 *            url to open
	 * @return opened InputStream, ready to be read.
	 * @throws IOException
	 */
	public InputStream getInputStream(final URL url) throws IOException {
		// request gzip
		URLConnection conn = url.openConnection();
		conn.addRequestProperty("Accept-encoding", "gzip");
		InputStream in = conn.getInputStream();

		// ungzip response
		if (conn.getContentEncoding().equalsIgnoreCase("gzip")) {
			in = new GZIPInputStream(in);
		}

		return in;
	}

	/**
	 * Utility method to build a query string from a map of parameters.
	 * 
	 * @param params
	 *            the params, and keys with null values are omitted.
	 * @return query string containing params.
	 */
	public String getQueryString(final HashMap<String, Object> params) {
		StringBuffer buf = new StringBuffer();
		boolean first = true;

		Iterator<String> iter = params.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			Object value = params.get(key);

			if (value != null) {
				if (first) {
					buf.append("?");
					first = false;
				} else {
					buf.append("&");
				}
				buf.append(key).append("=").append(value.toString());
			}
		}
		return buf.toString();
	}

}
