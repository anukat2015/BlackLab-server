package nl.inl.blacklab.server.requesthandlers;

import javax.servlet.http.HttpServletRequest;

import nl.inl.blacklab.search.indexstructure.AltDesc;
import nl.inl.blacklab.search.indexstructure.ComplexFieldDesc;
import nl.inl.blacklab.search.indexstructure.FieldType;
import nl.inl.blacklab.search.indexstructure.IndexStructure;
import nl.inl.blacklab.search.indexstructure.PropertyDesc;
import nl.inl.blacklab.search.Searcher;
import nl.inl.blacklab.server.BlackLabServer;
import nl.inl.blacklab.server.dataobject.DataObject;
import nl.inl.blacklab.server.dataobject.DataObjectMapAttribute;
import nl.inl.blacklab.server.dataobject.DataObjectMapElement;
import nl.inl.blacklab.server.search.IndexOpenException;

import org.apache.log4j.Logger;

/**
 * Get information about the structure of an index.
 */
public class RequestHandlerIndexStructure extends RequestHandler {
	@SuppressWarnings("hiding")
	private static final Logger logger = Logger.getLogger(RequestHandlerIndexStructure.class);

	public RequestHandlerIndexStructure(BlackLabServer servlet, HttpServletRequest request, String indexName, String urlResource, String urlPathPart) {
		super(servlet, request, indexName, urlResource, urlPathPart);
	}

	@Override
	public DataObject handle() throws IndexOpenException {
		debug(logger, "REQ struct: " + indexName);

		Searcher searcher = searchMan.getSearcher(indexName);
		IndexStructure struct = searcher.getIndexStructure();

		// Complex fields
		DataObjectMapAttribute doComplexFields = new DataObjectMapAttribute("complex-field", "name");
		for (String name: struct.getComplexFields()) {
			ComplexFieldDesc fieldDesc = struct.getComplexFieldDesc(name);
			DataObjectMapElement doComplexField = new DataObjectMapElement();
			doComplexField.put("has-content-store", fieldDesc.hasContentStore());
			doComplexField.put("has-xml-tags", fieldDesc.hasXmlTags());
			doComplexField.put("has-length-tokens", fieldDesc.hasLengthTokens());
			doComplexField.put("main-property", fieldDesc.getMainProperty().getName());
			DataObjectMapAttribute doProps = new DataObjectMapAttribute("property", "name");
			for (String propName: fieldDesc.getProperties()) {
				PropertyDesc propDesc = fieldDesc.getPropertyDesc(propName);
				DataObjectMapElement doProp = new DataObjectMapElement();
				doProp.put("has-forward-index", propDesc.hasForwardIndex());
				DataObjectMapAttribute doAlts = new DataObjectMapAttribute("alternative", "name");
				for (String altName: propDesc.getAlternatives()) {
					AltDesc altDesc = propDesc.getAlternativeDesc(altName);
					DataObjectMapElement doAlt = new DataObjectMapElement();
					doAlt.put("type", altDesc.getType().toString());
					doAlt.put("has-offsets", altDesc == propDesc.getOffsetsAlternative());
					doAlts.put(altName, doAlt);
				}
				doProp.put("alternative", doAlts);
				doProps.put(propName, doProp);
			}
			doComplexField.put("properties", doProps);
			doComplexFields.put(name, doComplexField);
		}

		// Metadata fields
		DataObjectMapAttribute doMetaFields = new DataObjectMapAttribute("metadata-field", "name");
		for (String name: struct.getMetadataFields()) {
			FieldType type = struct.getMetadataFieldDesc(name).getType();
			DataObjectMapElement doMetaField = new DataObjectMapElement();
			doMetaField.put("type", type.toString());
			doMetaFields.put(name, doMetaField);
		}

		// Assemble response
		DataObjectMapElement response = new DataObjectMapElement();
		response.put("index-name", indexName);
		response.put("document-title-field", struct.titleField());
		response.put("complex-fields", doComplexFields);
		response.put("metadata-fields", doMetaFields);

		return response;
	}

}
