package nl.inl.blacklab.server.dataobject;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Represents hierarchical data that can be serialized to XML or JSON.
 */
public abstract class DataObject {

	/** If set, overrides the response type (XML/JSON) for this object. */
	DataFormat overrideType = null;

	/** If true, the client may cache this response. If false, it should never cache this. */
	boolean cacheAllowed = true;

	public boolean isCacheAllowed() {
		return cacheAllowed;
	}

	public void setCacheAllowed(boolean allowCache) {
		this.cacheAllowed = allowCache;
	}

	public DataFormat getOverrideType() {
		return overrideType;
	}

	public void overrideType(DataFormat type) {
		overrideType = type;
	}

	/**
	 * Serialize the data to either XML or JSON
	 * @param out where to serialize to
	 * @param format target data format: XML or JSON
	 * @param prettyPrint if true, add newlines and indentation to output
	 * @param depth current indentation depth (default: 0)
	 * @throws IOException
	 */
	public abstract void serialize(Writer out, DataFormat format, boolean prettyPrint, int depth) throws IOException;

	/**
	 * Serialize the data to either XML or JSON
	 * @param out where to serialize to
	 * @param format target data format: XML or JSON
	 * @param prettyPrint if true, add newlines and indentation to output
	 * @throws IOException
	 */
	public void serialize(Writer out, DataFormat format, boolean prettyPrint) throws IOException {
		serialize(out, format, prettyPrint, 0);
	}

	/**
	 * Serialize the data to either XML or JSON
	 *
	 * The output will contain no newlines or whitespace.
	 *
	 * @param out where to serialize to
	 * @param format target data format: XML or JSON
	 * @throws IOException
	 */
	public void serialize(Writer out, DataFormat format) throws IOException {
		serialize(out, format, false, 0);
	}

	/**
	 * Serialize to the specified data-format, pretty-printed.
	 *
	 * @param format the format: XML or JSON.
	 * @return the pretty-printed XML or JSON representation
	 */
	public String toString(DataFormat format) {
		StringWriter sw = new StringWriter();
		try {
			this.serialize(sw, format, true);
			return sw.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/** Serialize to pretty-printed JSON. */
	@Override
	public String toString() {
		return toString(DataFormat.JSON);
	}

	/**
	 * Write the specified indent depth to the writer.
	 * @param out where to write the indent
	 * @param depth indent depth
	 * @throws IOException on write error
	 */
	void indent(Writer out, int depth) throws IOException {
		for (int i = 0; i < depth; i++) {
			out.append("  ");
		}
	}

	/**
	 * Is this a simple value (i.e. a number of string)?
	 * @return true iff this is a simple value.
	 */
	public abstract boolean isSimple();

	/**
	 * Serialize this value to a full XML, JSON document.
	 * @param rootElementName name of the XML root element (not applicable for JSON)
	 * @param out where to serialize to
	 * @param outputType the output format: XML or JSON
	 * @param prettyPrint whether or not to include newline and indents
	 * @param jsonpCallback the callback function name (only used for JSON, in which case the
	 *   response is JSONP, i.e. a Javascript consisting of a single function call with a JSON 
	 *   object as parameter)
	 * @throws IOException
	 */
	public void serializeDocument(String rootElementName, Writer out, DataFormat outputType, boolean prettyPrint, String jsonpCallback) throws IOException {
		switch (outputType) {
		case JSON:
			if (jsonpCallback != null && jsonpCallback.length() > 0)
				out.append(jsonpCallback).append("(");
			break;
		case XML:
			out.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>");
			if (prettyPrint)
				out.append("\n");
			out.append("<").append(rootElementName).append(">");
			if (prettyPrint)
				out.append("\n");
			break;
		}
		serialize(out, outputType, prettyPrint, outputType == DataFormat.XML ? 1 : 0);
		switch (outputType) {
		case JSON:
			if (jsonpCallback != null && jsonpCallback.length() > 0)
				out.append(");");
			break;
		case XML:
			out.append("</").append(rootElementName).append(">");
			if (prettyPrint)
				out.append("\n");
			break;
		}
	}

	/**
	 * Construct a simple status response object.
	 *
	 * Status response indicates the server is carrying out the request
	 * and will have results later.
	 *
	 * @param code (string) status code
	 * @param msg the message
	 * @param checkAgainMs advice for how long to wait before asking again (ms) (if 0, don't include this)
	 * @return the data object representing the error message
	 */
	public static DataObject statusObject(String code, String msg, int checkAgainMs) {
		DataObjectMapElement status = new DataObjectMapElement();
		status.put("code", new DataObjectString(code));
		status.put("message", new DataObjectString(msg));
		if (checkAgainMs != 0)
			status.put("check-again-ms", checkAgainMs);
		DataObjectMapElement rv = new DataObjectMapElement();
		rv.put("status", status);
		rv.setCacheAllowed(false); // status should never be cached
		return rv;
	}

	/**
	 * Construct a simple error response object.
	 *
	 * @param code (string) error code
	 * @param msg the error message
	 * @return the data object representing the error message
	 */
	public static DataObject errorObject(String code, String msg) {
		DataObjectMapElement error = new DataObjectMapElement();
		error.put("code", new DataObjectString(code));
		error.put("message", new DataObjectString(msg));
		DataObjectMapElement rv = new DataObjectMapElement();
		rv.put("error", error);
		return rv;
	}

	public static DataObject from(String value) {
		return new DataObjectString(value);
	}

	public static DataObject from(int value) {
		return new DataObjectNumber(value);
	}

	public static DataObject from(long value) {
		return new DataObjectNumber(value);
	}

	public static DataObject from(double value) {
		return new DataObjectNumber(value);
	}

	public static DataObject from(boolean value) {
		return value ? DataObjectBoolean.TRUE : DataObjectBoolean.FALSE;
	}

}
