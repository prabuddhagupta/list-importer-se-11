package com.rdc.importer.scrapian.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import com.rdc.importer.scrapian.model.ListSource;
import com.rdc.importer.scrapian.model.ScrapianSource;
import com.rdc.importer.scrapian.model.StringSource;
import com.rdc.importer.scrapian.util.RegexUtils;
import org.apache.commons.lang.StringUtils;

public class ElementSeekTransformer implements ScrapianTransformer {

    public ScrapianSource transform(ScrapianSource inputSource, Map<String, Object> parameters) throws Exception {
        String startText = (String) parameters.get("startText");
        String endText = (String) parameters.get("endText");
        String element = (String) parameters.get("element");
        Boolean greedyText = (Boolean) parameters.get("greedyText");
        Boolean greedy = (Boolean) parameters.get("greedy");
        Boolean keepEmptyXHtmlElements = (Boolean) parameters.get("keepEmptyXHTMLElements");

        if(keepEmptyXHtmlElements == null) {
            keepEmptyXHtmlElements = false;
        }
        
        if (greedyText == null) {
            greedyText = true;
        }

        if (greedy == null) {
            greedy = false;
        }

        String textRegex = "";
        if (startText != null) {
            textRegex = RegexUtils.escapeRegex(startText) + (greedyText ? "(?:.*)" : "(?:.*?)");
        }

        if (endText != null) {
            if (startText == null) {
                textRegex = (greedyText ? "(?:.*)" : "(?:.*?)");
            }
            textRegex += RegexUtils.escapeRegex(endText);
        }

        ScrapianSource targetSource = inputSource;
        if (StringUtils.isNotBlank(textRegex)) {
            Matcher matcher = RegexUtils.getMatcher(textRegex, targetSource.toString());
            if (matcher.find()) {
                String match = matcher.group(0);
                targetSource = new StringSource(match);
            } else {
                return null;
            }
        }

        Map<String, Object> params = new HashMap<String, Object>();

        if(!keepEmptyXHtmlElements) {
            params.put("regex", "<" + element + "(?:[^>]*)" + (greedy ? "(?:.*)" : "(?:.*?)") + "</" + element + ">");
        } else {
            // Add ability to capture empty XHTML elements. we need them in certain cases.
            params.put("regex", "<" + element + "[\\s[>]][^/]" + (greedy ? "(?:.*)" : "(?:.*?)") + "</" + element + ">|<" + element + " />");
        }

        ListSource<ListSource<StringSource>> listSource = new RegexMatchesTransformer().transform(targetSource, params);

        if (listSource != null) {
            List<StringSource> matches = new ArrayList<StringSource>();
            for (ListSource<StringSource> source : listSource.getValue()) {
                matches.add(source.getValue().iterator().next());
            }
            return new ListSource<StringSource>(matches);
        }

        return null;
    }
}
