package com.rdc.importer.scrapian.transform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.rdc.importer.scrapian.model.ScrapianSource;
import com.rdc.importer.scrapian.model.ListSource;
import com.rdc.importer.scrapian.model.StringSource;
import com.rdc.importer.scrapian.util.RegexUtils;

public class RegexMatchesTransformer implements ScrapianTransformer {

    public ListSource<ListSource<StringSource>> transform(ScrapianSource scrapianSource, Map<String, Object> parameters) throws Exception {
        String regex = (String) parameters.get("regex");
        List<List<String>> matches = RegexUtils.extractMatches(regex, scrapianSource.serialize());
        if (matches.size() == 0) {
            return null;
        } else {
            List<ListSource<StringSource>> matchGroupSources = new ArrayList<ListSource<StringSource>>();
            for (List<String> matchGroup : matches) {
                List<StringSource> matchGroupSource = new ArrayList<StringSource>();
                for (String match : matchGroup) {
                    matchGroupSource.add(new StringSource(match));
                }
                matchGroupSources.add(new ListSource<StringSource>(matchGroupSource));
            }
            return new ListSource<ListSource<StringSource>>(matchGroupSources);
        }
    }
}