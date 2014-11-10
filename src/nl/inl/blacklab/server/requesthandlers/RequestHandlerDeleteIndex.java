package nl.inl.blacklab.server.requesthandlers;

import javax.servlet.http.HttpServletRequest;

import nl.inl.blacklab.server.BlackLabServer;
import nl.inl.blacklab.server.dataobject.DataObject;
import nl.inl.blacklab.server.search.QueryException;

/**
 * Display the contents of the cache.
 */
public class RequestHandlerDeleteIndex extends RequestHandler {
	public RequestHandlerDeleteIndex(BlackLabServer servlet, HttpServletRequest request, String indexName, String urlResource, String urlPathPart) {
		super(servlet, request, indexName, urlResource, urlPathPart);
	}

	@Override
	public DataObject handle() throws QueryException {
		if (indexName != null && indexName.length() > 0) {
			// Delete index
			try {
				searchMan.deleteUserIndex(indexName, user);
				return DataObject.statusObject("SUCCESS", "Index deleted succesfully.");
			} catch (QueryException e) {
				throw e;
			} catch (Exception e) {
				return DataObject.errorObject("INTERNAL_ERROR", internalErrorMessage(e, debugMode, 12));
			}
		}
		
		return DataObject.errorObject("CANNOT_CREATE_INDEX", "Could not create index '" + indexName + "'. Specify a valid name.");
	}

}