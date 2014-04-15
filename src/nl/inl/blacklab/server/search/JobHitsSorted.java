package nl.inl.blacklab.server.search;

import nl.inl.blacklab.search.Hits;
import nl.inl.blacklab.search.grouping.HitProperty;

/**
 * Represents a hits search and sort operation.
 */
public class JobHitsSorted extends JobWithHits {

	public JobHitsSorted(SearchManager searchMan, SearchParameters par) throws IndexOpenException {
		super(searchMan, par);
	}

	@Override
	public void performSearch() throws IndexOpenException, QueryException, InterruptedException  {
		// First, execute blocking hits search.
		SearchParameters parNoSort = par.copyWithout("sort");
		JobWithHits hitsSearch = searchMan.searchHits(parNoSort, true);

		// Now, sort the hits.
		Hits hitsUnsorted = hitsSearch.getHits();
		String sortBy = par.get("sort");
		if (sortBy == null)
			sortBy = "";
		boolean reverse = false;
		if (sortBy.length() > 0 && sortBy.charAt(0) == '-') {
			reverse = true;
			sortBy = sortBy.substring(1);
		}
		HitProperty sortProp = HitProperty.deserialize(hitsUnsorted, sortBy);
		if (sortProp == null)
			throw new QueryException("UNKNOWN_SORT_PROPERTY", "Unknown sort property '" + sortBy + "'");
		hits = hitsUnsorted.sortedBy(sortProp, reverse);
	}

}
