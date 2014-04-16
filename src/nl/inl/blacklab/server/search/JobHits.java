package nl.inl.blacklab.server.search;

import nl.inl.blacklab.search.TextPattern;

import org.apache.lucene.search.BooleanQuery.TooManyClauses;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;

/**
 * Represents a hit search operation.
 */
public class JobHits extends JobWithHits {

	/** The parsed pattern */
	protected TextPattern textPattern;

	/** The parsed filter */
	protected Filter filterQuery;

	public JobHits(SearchManager searchMan, SearchParameters par) throws IndexOpenException {
		super(searchMan, par);
	}

	@Override
	public void performSearch() throws QueryException {
		try {
			textPattern = SearchManager.parsePatt(par.get("patt"), par.get("pattlang"));
			Query q = SearchManager.parseFilter(par.get("filter"), par.get("filterlang"));
			filterQuery = q == null ? null : new QueryWrapperFilter(q);
			hits = searcher.find(textPattern, filterQuery);
		} catch (TooManyClauses e) {
			throw new QueryException("QUERY_TOO_BROAD", "Query too broad, too many matching terms. Please be more specific.", e);
		}
	}

	public TextPattern getTextPattern() {
		return textPattern;
	}

	public Filter getDocumentFilter() {
		return filterQuery;
	}

}
