package nl.inl.blacklab.server.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.inl.blacklab.perdocument.DocCounts;
import nl.inl.blacklab.perdocument.DocProperty;
import nl.inl.blacklab.perdocument.DocPropertyMultiple;
import nl.inl.blacklab.perdocument.DocResults;
import nl.inl.blacklab.server.dataobject.DataObjectMapElement;

/**
 * Represents a hits search and sort operation.
 */
public class JobFacets extends Job {

	private Map<String, DocCounts> counts;

	private DocResults docResults;

	public JobFacets(SearchManager searchMan, String userId, SearchParameters par) throws IndexOpenException {
		super(searchMan, userId, par);
	}

	@Override
	public void performSearch() throws IndexOpenException, QueryException, InterruptedException  {
		// First, execute blocking docs search.
		SearchParameters parNoGroup = par.copyWithout("group", "sort");
		JobWithDocs docsSearch = searchMan.searchDocs(userId, parNoGroup);
		waitForJobToFinish(docsSearch);

		// Now, group the docs according to the requested facets.
		docResults = docsSearch.getDocResults();
		String facets = par.getString("facets");
		if (facets == null) {
			// If no facets were specified, we shouldn't even be here.
			throw new RuntimeException("facets == null");
		}
		DocProperty propMultipleFacets = DocProperty.deserialize(facets);
		List<DocProperty> props = new ArrayList<DocProperty>();
		if (propMultipleFacets instanceof DocPropertyMultiple) {
			// Multiple facets requested
			for (DocProperty prop: (DocPropertyMultiple)propMultipleFacets) {
				props.add(prop);
			}
		} else {
			// Just a single facet requested
			props.add(propMultipleFacets);
		}

		Map<String, DocCounts> theCounts = new HashMap<String, DocCounts>();
		for (DocProperty facetBy: props) {
			DocCounts facetCounts = docResults.countBy(facetBy);
			counts.put(facetBy.serialize(), facetCounts);
		}
		counts = theCounts; // we're done, caller can use the groups now
	}

	public Map<String, DocCounts> getCounts() {
		return counts;
	}

	public DocResults getDocResults() {
		return docResults;
	}

	@Override
	public DataObjectMapElement toDataObject() {
		DataObjectMapElement d = super.toDataObject();
		d.put("number-of-doc-results", docResults == null ? -1 : docResults.size());
		d.put("number-of-facets", counts == null ? -1 : counts.size());
		return d;
	}

}