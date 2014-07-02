package nl.inl.blacklab.server.requesthandlers;

import javax.servlet.http.HttpServletRequest;

import nl.inl.blacklab.perdocument.DocGroup;
import nl.inl.blacklab.perdocument.DocGroups;
import nl.inl.blacklab.perdocument.DocProperty;
import nl.inl.blacklab.perdocument.DocResult;
import nl.inl.blacklab.perdocument.DocResults;
import nl.inl.blacklab.perdocument.DocResultsWindow;
import nl.inl.blacklab.search.Hit;
import nl.inl.blacklab.search.Hits;
import nl.inl.blacklab.search.IndexStructure;
import nl.inl.blacklab.search.Kwic;
import nl.inl.blacklab.search.Searcher;
import nl.inl.blacklab.search.grouping.HitPropValue;
import nl.inl.blacklab.server.BlackLabServer;
import nl.inl.blacklab.server.dataobject.DataObject;
import nl.inl.blacklab.server.dataobject.DataObjectContextList;
import nl.inl.blacklab.server.dataobject.DataObjectList;
import nl.inl.blacklab.server.dataobject.DataObjectMapElement;
import nl.inl.blacklab.server.search.IndexOpenException;
import nl.inl.blacklab.server.search.Job;
import nl.inl.blacklab.server.search.JobDocsGrouped;
import nl.inl.blacklab.server.search.JobDocsTotal;
import nl.inl.blacklab.server.search.JobDocsWindow;
import nl.inl.blacklab.server.search.QueryException;
import nl.inl.blacklab.server.search.SearchCache;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;

/**
 * Request handler for the doc results.
 */
public class RequestHandlerDocs extends RequestHandler {
	@SuppressWarnings("hiding")
	private static final Logger logger = Logger.getLogger(RequestHandlerDocs.class);

	public RequestHandlerDocs(BlackLabServer servlet, HttpServletRequest request, String indexName, String urlResource, String urlPathPart) {
		super(servlet, request, indexName, urlResource, urlPathPart);
	}

	@Override
	public DataObject handle() throws IndexOpenException, QueryException, InterruptedException {
		debug(logger, "REQ docs: " + searchParam);

		// Do we want to view a single group after grouping?
		String groupBy = searchParam.get("group");
		if (groupBy == null)
			groupBy = "";
		String viewGroup = searchParam.get("viewgroup");
		if (viewGroup == null)
			viewGroup = "";
		Job search;
		JobDocsGrouped searchGrouped = null;
		JobDocsWindow searchWindow = null;
		DocResultsWindow window;
		DocGroup group = null;
		JobDocsTotal total = null;
		boolean block = getBoolParameter("block");
		if (groupBy.length() > 0 && viewGroup.length() > 0) {

			// TODO: clean up, do using JobHitsGroupedViewGroup or something (also cache sorted group!)

			// Yes. Group, then show hits from the specified group
			search = searchGrouped = searchMan.searchDocsGrouped(getUserId(), searchParam);
			if (block) {
				search.waitUntilFinished(SearchCache.MAX_SEARCH_TIME_SEC * 1000);
				if (!search.finished())
					return DataObject.errorObject("SEARCH_TIMED_OUT", "Search took too long, cancelled.");
			}

			// If search is not done yet, indicate this to the user
			if (!search.finished()) {
				return DataObject.statusObject("WORKING", "Searching, please wait...", servlet.getSearchManager().getCheckAgainAdviceMs(search));
			}

			// Search is done; construct the results object
			DocGroups groups = searchGrouped.getGroups();

			HitPropValue viewGroupVal = null;
			viewGroupVal = HitPropValue.deserialize(groups.getOriginalDocResults().getOriginalHits(), viewGroup);
			if (viewGroupVal == null)
				return DataObject.errorObject("ERROR_IN_GROUP_VALUE", "Parameter 'viewgroup' has an illegal value: " + viewGroup);

			group = groups.getGroup(viewGroupVal);
			if (group == null)
				return DataObject.errorObject("GROUP_NOT_FOUND", "Group not found: " + viewGroup);

			String sortBy = searchParam.get("sort");
			DocProperty sortProp = sortBy != null && sortBy.length() > 0 ? DocProperty.deserialize(sortBy) : null;
			DocResults docsSorted;
			if (sortProp != null) {
				docsSorted = group.getResults();
				docsSorted.sort(sortProp, false);
			} else
				docsSorted = group.getResults();

			int first = searchParam.getInteger("first");
			int number = searchParam.getInteger("number");
			window = docsSorted.window(first, number);

		} else {
			// Regular set of hits (no grouping first)

			search = searchWindow = searchMan.searchDocsWindow(getUserId(), searchParam);
			if (block) {
				search.waitUntilFinished(SearchCache.MAX_SEARCH_TIME_SEC * 1000);
				if (!search.finished())
					return DataObject.errorObject("SEARCH_TIMED_OUT", "Search took too long, cancelled.");
			}

			// Also determine the total number of hits (nonblocking)
			total = searchMan.searchDocsTotal(getUserId(), searchParam);

			// If search is not done yet, indicate this to the user
			if (!search.finished()) {
				return DataObject.statusObject("WORKING", "Searching, please wait...", servlet.getSearchManager().getCheckAgainAdviceMinimumMs());
			}

			window = searchWindow.getWindow();
		}

		// Search is done; construct the results object
		Searcher searcher = search.getSearcher();
		IndexStructure struct = searcher.getIndexStructure();

		// The hits and document info
		DataObjectList docList = new DataObjectList("doc");
		for (DocResult result: window) {
			// Doc info (metadata, etc.)
			Document document = result.getDocument();
			DataObjectMapElement docInfo = getDocumentInfo(indexName, struct, document);

			// Snippets
			Hits hits = result.getHits(5); // TODO: make num. snippets configurable
			DataObjectList doSnippetList = new DataObjectList("snippet");
			for (Hit hit: hits) {
				Kwic c = hits.getKwic(hit);
				DataObjectMapElement hitMap = new DataObjectMapElement();
				hitMap.put("left", new DataObjectContextList(c.getProperties(), c.getLeft()));
				hitMap.put("match", new DataObjectContextList(c.getProperties(), c.getMatch()));
				hitMap.put("right", new DataObjectContextList(c.getProperties(), c.getRight()));
				doSnippetList.add(hitMap);
			}

			// Find pid
			String pid = searchMan.getDocumentPid(indexName, result.getDocId(), document);

			// Combine all
			DataObjectMapElement docMap = new DataObjectMapElement();
			docMap.put("doc-pid", pid);
			docMap.put("number-of-hits", result.getNumberOfHits());
			docMap.put("doc-info", docInfo);
			docMap.put("snippets", doSnippetList);

			docList.add(docMap);
		}

		// The summary (done last because the count might be done by this time)
		DataObjectMapElement summary = new DataObjectMapElement();
		DocResults docs = searchWindow != null ? total.getDocResults() : group.getResults();
		Hits hits = docs.getOriginalHits();
		boolean done = hits == null ? true : hits.doneFetchingHits();
		summary.put("search-param", searchParam.toDataObject());
		summary.put("search-time", search.executionTimeMillis());
		if (total != null)
			summary.put("count-time", total.executionTimeMillis());
		summary.put("still-counting", !done);
		if (searchGrouped == null && hits != null) {
			summary.put("number-of-hits", hits.countSoFarHitsCounted());
			summary.put("number-of-hits-retrieved", hits.countSoFarHitsRetrieved());
		}
		if (hits != null || group != null) {
			summary.put("number-of-docs", hits == null ? group.getResults().size() : hits.countSoFarDocsCounted());
			summary.put("number-of-docs-retrieved", hits == null ? group.getResults().size() : hits.countSoFarDocsRetrieved());
		} else {
			// TODO: DocResults.countSoFarDocsCounted/Retrieved?
			summary.put("number-of-docs", docs.size());
		}
		summary.put("window-first-result", window.first());
		summary.put("requested-window-size", searchParam.getInteger("number"));
		summary.put("actual-window-size", window.size());
		summary.put("window-has-previous", window.hasPrevious());
		summary.put("window-has-next", window.hasNext());

		// Assemble all the parts
		DataObjectMapElement response = new DataObjectMapElement();
		response.put("summary", summary);
		response.put("docs", docList);

		return response;
	}


}