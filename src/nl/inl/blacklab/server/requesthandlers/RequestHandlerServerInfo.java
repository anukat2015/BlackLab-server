package nl.inl.blacklab.server.requesthandlers;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import nl.inl.blacklab.server.BlackLabServer;
import nl.inl.blacklab.server.dataobject.DataObject;
import nl.inl.blacklab.server.dataobject.DataObjectMapAttribute;
import nl.inl.blacklab.server.dataobject.DataObjectMapElement;
import nl.inl.blacklab.server.search.SearchCache;

/**
 * Get information about this BlackLab server.
 */
public class RequestHandlerServerInfo extends RequestHandler {

	public RequestHandlerServerInfo(BlackLabServer servlet, HttpServletRequest request, String indexName, String urlResource, String urlPathPart) {
		super(servlet, request, indexName, urlResource, urlPathPart);
	}

	@Override
	public DataObject handle() {
		logger.debug("REQ serverinfo");

		Collection<String> indices = searchMan.getAvailableIndices();
		DataObjectMapAttribute doIndices = new DataObjectMapAttribute("index", "name");
		for (String indexName: indices) {
			DataObjectMapElement doIndex = new DataObjectMapElement();
			doIndex.put("pid-field", searchMan.getIndexPidField(indexName));

			doIndices.put(indexName, doIndex);
		}

		SearchCache cache = searchMan.getCache();
		DataObjectMapElement doCache = new DataObjectMapElement();
		doCache.put("max-size-bytes", cache.getMaxSizeBytes());
		doCache.put("max-number-of-searches", cache.getMaxNumberOfSearches());
		doCache.put("max-search-age-sec", cache.getMaxSearchAgeSec());
		doCache.put("size-bytes", cache.getSizeBytes());
		doCache.put("number-of-searches", cache.getNumberOfSearches());

		DataObjectMapElement response = new DataObjectMapElement();
		response.put("available-indices", doIndices);
		response.put("cache", doCache);

		return response;
	}

}
