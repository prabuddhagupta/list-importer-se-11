package com.rdc.importer.scrapian.transform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.rdc.importer.scrapian.model.ScrapianSource;
import com.rdc.importer.scrapian.model.ListSource;
import com.rdc.importer.scrapian.model.StringSource;
import com.rdc.importer.scrapian.util.RegexUtils;

public class RegexMatchTransformer implements ScrapianTransformer {

    public ScrapianSource transform(ScrapianSource scrapianSource, Map<String, Object> parameters) throws Exception {
        String regex = (String) parameters.get("regex");

        List<List<String>> matches = RegexUtils.extractMatches(regex, scrapianSource.serialize());
        if (matches.size() == 0) {
            return null;
        } else if (matches.size() == 1) {
            List<String> groups = matches.iterator().next();
            // if only 1 group, then groups weren't used, just return match.
            if (groups.size() == 1) { // no groups
                return new StringSource(groups.iterator().next());
            } else { // return the list of groups found by match

                List<StringSource> sources = new ArrayList<StringSource>();
                for (String group : groups) {
                    sources.add(new StringSource(group));
                }
                return new ListSource<StringSource>(sources);
            }

        } else {
            throw new Exception("Regex returned more than 1 result - returned [" + matches.size() + "]");
        }
    }
}