package nl.inl.blacklab.server.requesthandlers;

import javax.servlet.http.HttpServletRequest;

import nl.inl.blacklab.search.Hits;
import nl.inl.blacklab.search.grouping.HitGroup;
import nl.inl.blacklab.search.grouping.HitGroups;
import nl.inl.blacklab.server.BlackLabServer;
import nl.inl.blacklab.server.dataobject.DataObject;
import nl.inl.blacklab.server.dataobject.DataObjectList;
import nl.inl.blacklab.server.dataobject.DataObjectMapElement;
import nl.inl.blacklab.server.search.IndexOpenException;
import nl.inl.blacklab.server.search.JobHitsGrouped;
import nl.inl.blacklab.server.search.QueryException;
import nl.inl.blacklab.server.search.SearchCache;

/**
 * Request handler for grouped hit results.
 */
public class RequestHandlerHitsGrouped extends RequestHandler {
	//private static final Logger logger = Logger.getLogger(RequestHandlerHitset.class);

	public RequestHandlerHitsGrouped(BlackLabServer servlet, HttpServletRequest request, String indexName, String urlResource, String urlPathPart) {
		super(servlet, request, indexName, urlResource, urlPathPart);
	}

	@Override
	public DataObject handle() throws IndexOpenException, QueryException, InterruptedException {
		logger.debug("REQ hitsgrouped: " + searchParam);

		// Get the window we're interested in
		JobHitsGrouped search = searchMan.searchHitsGrouped(searchParam);
		if (getBoolParameter("block")) {
			search.waitUntilFinished(SearchCache.MAX_SEARCH_TIME_SEC * 1000);
			if (!search.finished())
				return DataObject.errorObject("SEARCH_TIMED_OUT", "Search took too long, cancelled.");
		}

		// If search is not done yet, indicate this to the user
		if (!search.finished()) {
			return DataObject.statusObject("WORKING", "Searching, please wait...", servlet.getSearchManager().getDefaultCheckAgainAdviceMs());
		}

		// Search is done; construct the results object
		HitGroups groups = search.getGroups();

		DataObjectList doGroups = null;
		// The list of groups found
		// TODO paging..?
		doGroups = new DataObjectList("group");
		int first = getIntParameter("first");
		int number = getIntParameter("number");
		int i = 0;
		for (HitGroup group: groups) {
			if (i >= first && i < first + number) {
				DataObjectMapElement doGroup = new DataObjectMapElement();
				doGroup.put("identity", group.getIdentity().serialize());
				doGroup.put("identity-human-readable", group.getIdentity().toString());
				doGroup.put("size", group.size());
				doGroups.add(doGroup);
			}
			i++;
		}

		// The summary
		DataObjectMapElement summary = new DataObjectMapElement();
		Hits hits = search.getHits();
		summary.put("search-time", search.executionTimeMillis());
		summary.put("still-counting", false);
		summary.put("number-of-hits", hits.countSoFarHitsCounted());
		summary.put("number-of-hits-retrieved", hits.countSoFarHitsRetrieved());
		summary.put("number-of-docs", hits.countSoFarDocsCounted());
		summary.put("number-of-docs-retrieved", hits.countSoFarDocsRetrieved());
		summary.put("number-of-groups", groups.numberOfGroups());
		summary.put("window-first-result", first);
		summary.put("window-size", doGroups.size());
		summary.put("window-has-previous", first > 0);
		summary.put("window-has-next", first + number < groups.numberOfGroups());

		// Assemble all the parts
		DataObjectMapElement response = new DataObjectMapElement();
		response.put("summary", summary);
		response.put("groups", doGroups);
		/*response.put("hits", hitList);
		response.put("docinfos", docInfos);*/

		return response;
	}

}
