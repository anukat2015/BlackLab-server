package nl.inl.blacklab.server;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.inl.blacklab.server.dataobject.DataFormat;
import nl.inl.blacklab.server.exceptions.InternalServerError;
import nl.inl.util.ExUtil;

import org.apache.log4j.Logger;

public class ServletUtil {
	private static final Logger logger = Logger.getLogger(ServletUtil.class);

	/**
	 * Returns the value of a servlet parameter
	 * @param request the request object
	 * @param name
	 *            name of the parameter
	 * @return value of the paramater
	 */
	private static String getParameter(HttpServletRequest request, String name) {
		return request.getParameter(name);
	}

	/**
	 * Returns the value of a servlet parameter, or the default value
	 * @param request the request object
	 *
	 * @param name
	 *            name of the parameter
	 * @param defaultValue
	 *            default value
	 * @return value of the paramater
	 */
	public static int getParameter(HttpServletRequest request, String name, int defaultValue) {
		final String stringToParse = getParameter(request, name, "" + defaultValue);
		try {
			return Integer.parseInt(stringToParse);
		} catch (NumberFormatException e) {
			logger.info("Could not parse parameter '" + name + "', value '" + stringToParse
					+ "'. Using default (" + defaultValue + ")");
			return defaultValue;
		}
	}

	/**
	 * Returns the value of a servlet parameter, or the default value
	 * @param request the request object
	 *
	 * @param name
	 *            name of the parameter
	 * @param defaultValue
	 *            default value
	 * @return value of the paramater
	 */
	public static boolean getParameter(HttpServletRequest request, String name, boolean defaultValue) {
		String defStr = defaultValue ? "true" : "false";
		String value = getParameter(request, name, defStr);
		if (value.equalsIgnoreCase("true"))
			return true;
		if (value.equalsIgnoreCase("false"))
			return false;

		logger.warn("Illegal value '" + value + "' given for boolean parameter '" + name
				+ "'. Using default (" + defStr + ")");
		return defaultValue;
	}

	/**
	 * Returns the value of a servlet parameter, or the default value
	 * @param request the request object
	 * @param name
	 *            name of the parameter
	 * @param defaultValue
	 *            default value
	 * @return value of the paramater
	 */
	public static String getParameter(HttpServletRequest request, String name, String defaultValue) {
		String value = getParameter(request, name);
		if (value == null || value.length() == 0)
			value = defaultValue; // default action
		return value;
	}

	/**
	 * Returns the type of content the user would like as output (HTML, CSV, ...)
	 * This is based on the "outputformat" parameter.
	 *
	 * TODO: Also support HTTP Accept header!
	 *
	 * @param request the request object
	 * @param defaultFormat what to return if not specified
	 * @return the type of content the user would like
	 */
	public static DataFormat getOutputType(HttpServletRequest request, DataFormat defaultFormat) {
		// See if jsonp callback parameter specified. If so, we want JSON (the "P" part is handled elsewhere)
		String jsonpCallback = getParameter(request, "jsonp", "").toLowerCase();
		if (jsonpCallback.length() > 0) {
			return DataFormat.JSON;
		}

		// See if there was an explicit outputformat parameter. If so, use that.
		String outputTypeString = getParameter(request, "outputformat", "").toLowerCase();
		if (outputTypeString.length() > 0) {
			return getOutputTypeFromString(outputTypeString, defaultFormat);
		}

		// No explicit parameter. Check if the Accept header contains either json or xml
		String accept = request.getHeader("Accept");
		//logger.debug("Accept: " + accept);
		if (accept != null && accept.length() > 0) {
			if (accept.contains("json"))
				return DataFormat.JSON;
			if (accept.contains("xml"))
				return DataFormat.XML;
			if (accept.contains("javascript"))
				return DataFormat.JSON;
		}

		// Unspecified. Use the configured default format.
		return defaultFormat;
	}

	/**
	 * Returns the desired content type for the output.
	 * This is based on the "outputformat" parameter.
	 * @param outputType the request object
	 * @return the MIME content type
	 */
	public static String getContentType(DataFormat outputType) {
		if (outputType == DataFormat.XML)
			return "application/xml";
		return "application/json";
	}

	/**
	 * Translate the string value for outputType to the enum OutputType value.
	 *
	 * @param typeString
	 *            the outputType string
	 * @param defaultValue what to use if neither matches
	 * @return the OutputType enum value
	 */
	public static DataFormat getOutputTypeFromString(String typeString, DataFormat defaultValue) {
		if (typeString.equalsIgnoreCase("xml"))
			return DataFormat.XML;
		if (typeString.equalsIgnoreCase("json"))
			return DataFormat.JSON;
		logger.warn("Onbekend outputtype gevraagd: " + typeString);
		return defaultValue;
	}

	/**
	 * Get a PrintStream for writing the response
	 * @param responseObject the response object
	 * @return the PrintStream
	 */
	public static PrintStream getPrintStream(HttpServletResponse responseObject) {
		try {
			return new PrintStream(responseObject.getOutputStream(), true, "utf-8");
		} catch (Exception e) {
			throw ExUtil.wrapRuntimeException(e);
		}
	}

	/** Output character encoding */
	static final String OUTPUT_ENCODING = "UTF-8";

	/** The HTTP date format, to use for the cache header */
	static DateFormat httpDateFormat;

	// Initialize the HTTP date format
	static {
		httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	public static void writeNoCacheHeaders(HttpServletResponse response) {
		writeCacheHeaders(response, 0);
	}

	/**
	 * Write cache headers for the configured cache time.
	 * @param response the response object to write the headers to
	 * @param cacheTimeSeconds how long to cache the response
	 */
	public static void writeCacheHeaders(HttpServletResponse response, int cacheTimeSeconds) {
		if (cacheTimeSeconds > 0) {
			// Cache page for specified time
			GregorianCalendar cal = new GregorianCalendar();
			cal.add(Calendar.SECOND, cacheTimeSeconds);
			String expires;
			synchronized (httpDateFormat) {
				expires = httpDateFormat.format(cal.getTime());
			}
			response.setHeader("Expires", expires);
			response.setHeader("Cache-Control", "PUBLIC, max-age=" + cacheTimeSeconds);
		} else {
			// Don't cache this page
			response.setHeader("Expires", "0");
			response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
			response.setHeader("Pragma", "no-cache");
		}
	}

	/**
	 * Write the HTTP headers for the response, including caching, IE8 XSS protection (off!),
	 * encoding, save as file.
	 * @param request the request object
	 * @param response the response object
	 * @param defaultFormat default output format to use
	 * @param noCache
	 *            if true, outputs headers to make sure the page isn't cached.
	 */
	@SuppressWarnings("unused")
	private void writeResponseHeaders(HttpServletRequest request, HttpServletResponse response, DataFormat defaultFormat) {
		boolean noCache = false;

		// For the login page: set the no-cache headers for the response,
		// so he doesn't see the login page again after the user has logged in.
		if (noCache) {
			response.setHeader("Pragma", "No-cache");
			response.setHeader("Cache-Control", "no-cache");
			response.setDateHeader("Expires", 1);
		}

		// Make sure IE8 doesn't do "smart" XSS prevention
		// (i.e. mangle our URLs and scripts for us)
		response.setIntHeader("X-XSS-Protection", 0);

		// Set the content headers for the response
		response.setCharacterEncoding(OUTPUT_ENCODING);
		response.setContentType(ServletUtil.getContentType(getOutputType(request, defaultFormat)));
	}

	public static String internalErrorMessage(int code) {
		return "An internal error occurred. Please contact the administrator. Error code: " + code + ".";
	}

	public static String internalErrorMessage(Exception e, boolean debugMode, int code) {
		if (debugMode) {
			if (e instanceof InternalServerError)
				return internalErrorMessage(e.getMessage(), debugMode, code);
			return internalErrorMessage(e.getClass().getName() + ": " + e.getMessage(), debugMode, code);
		}
		return ServletUtil.internalErrorMessage(code);
	}

	public static String internalErrorMessage(String message, boolean debugMode, int code) {
		if (debugMode) {
			return message + " (Internal error code " + code + ")";
		}
		return ServletUtil.internalErrorMessage(code);
	}

	/**
	 * Returns the path info and query string (if any) of the request URL
	 *
	 * @param request the servlet request
	 *
	 * @return the path and query string (if any)
	 */
	public static String getPathAndQueryString(HttpServletRequest request) {
		String pathInfo = request.getPathInfo();
		if (pathInfo == null)
			pathInfo = "";
		String queryString = request.getQueryString();
		if (queryString == null)
			queryString = "";
		else
			queryString = "?" + queryString;
		return request.getServletPath() + pathInfo + queryString;
	}

	/**
	 * Returns the servlte's base URL, including the context path
	 *
	 * @param request the servlet request
	 *
	 * @return the base URL, e.g. http://myserver:8080/myroot
	 */
	public static String getServletBaseUrl(HttpServletRequest request) {
		int port = request.getLocalPort();
		String optPort = port == 80 ? "" : ":" + port;
		return request.getScheme() + "://" + request.getServerName() + optPort + request.getContextPath();
	}

	/**
	 * Returns the complete request URL
	 *
	 * @param request the servlet request
	 *
	 * @return the complete request URL
	 */
	public static String getRequestUrl(HttpServletRequest request) {
		return getServletBaseUrl(request) + getPathAndQueryString(request);
	}

	public static String shortenIpv6(String longAddress) {
		return longAddress.replaceFirst("(^|:)(0+(:|$)){2,8}", "::").replaceAll("(:|^)0+([0-9A-Fa-f])", "$1$2");
	}


}
