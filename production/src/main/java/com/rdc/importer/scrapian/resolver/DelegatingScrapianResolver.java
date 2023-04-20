package com.rdc.importer.scrapian.resolver;

import com.rdc.importer.scrapian.ScrapianDataList;
import com.rdc.importer.scrapian.ScrapianScript;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;

@Component("scrapianResolver")
public class DelegatingScrapianResolver implements ScrapianResolver {
    private ScrapianResolver scrapianResolver;
    private Map<String, ScrapianResolver> scrapianResolvers;

    @Resource
    public void setScrapianResolvers(Map<String, ScrapianResolver> scrapianResolvers) {
        this.scrapianResolvers = scrapianResolvers;
    }

    @PostConstruct
    public void init() {
        if ("classpath".equals(System.getProperty("scrapian.resolver"))) {
            scrapianResolver = scrapianResolvers.get("classpath");
        } else {
            scrapianResolver = scrapianResolvers.get("svn");
        }
    }

    public ScrapianScript resolveScript(String scriptName) throws Exception {
        return getResolver().resolveScript(scriptName);
    }

    public ScrapianDataList resolveDataList(String dataListName) throws Exception {
        return getResolver().resolveDataList(dataListName);
    }

    public ScrapianResolver getResolver() {
        return scrapianResolver;
    }
}
