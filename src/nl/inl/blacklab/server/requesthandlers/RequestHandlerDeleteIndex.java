package nl.inl.blacklab.server.requesthandlers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.inl.blacklab.exceptions.BlsException;
import nl.inl.blacklab.server.BlackLabServer;
import nl.inl.blacklab.server.search.User;

/**
 * Display the contents of the cache.
 */
public class RequestHandlerDeleteIndex extends RequestHandler {
	public RequestHandlerDeleteIndex(BlackLabServer servlet, HttpServletRequest request, User user, String indexName, String urlResource, String urlPathPart) {
		super(servlet, request, user, indexName, urlResource, urlPathPart);
	}

	@Override
	public Response handle() throws BlsException {
		if (indexName != null && indexName.length() > 0) {
			// Delete index
			try {
				searchMan.deleteUserIndex(indexName);
				return Response.status("SUCCESS", "Index deleted succesfully.", HttpServletResponse.SC_OK);
			} catch (BlsException e) {
				throw e;
			} catch (Exception e) {
				return Response.internalError(e, debugMode, 12);
			}
		}
		
		return Response.badRequest("CANNOT_CREATE_INDEX", "Could not create index '" + indexName + "'. Specify a valid name.");
	}

}
