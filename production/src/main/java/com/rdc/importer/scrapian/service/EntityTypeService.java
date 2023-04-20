package com.rdc.importer.scrapian.service;

import com.rdc.importer.scrapian.ScrapianDataList;
import com.rdc.importer.scrapian.resolver.ScrapianResolver;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class EntityTypeService {
    private final String ORG_TOKEN_LIST = "org_tokens";
    private final String ORG_CONTAIN_LIST = "org_contains";
    private ScrapianResolver scrapianResolver;
    private List<String> orgTokens;
    private List<String> orgContains;

    @Resource
    public void setScrapianResolver(ScrapianResolver scrapianResolver) {
        this.scrapianResolver = scrapianResolver;
    }

    public String determineEntityType(String entityName) throws Exception {
        return determineEntityType(entityName, null);
    }

    public String determineEntityType(String entityName, List<String> additionalOrgTokens) throws Exception {
        String entity = entityName.toUpperCase();
        for (String suffix : getOrgContains()) {
            if (entity.contains(suffix)) {
                return "O";
            }
        }
        List nameParts = Arrays.asList(entity.replaceAll("[.|,|\\(|\\)]", "").split(" "));
        for (String defaultOrgTokens : getOrgTokens()) {
            if (nameParts.contains(defaultOrgTokens)) {
                return "O";
            }
        }
        if (additionalOrgTokens != null) {
            for (String orgToken : additionalOrgTokens) {
                if (nameParts.contains(orgToken.toUpperCase())) {
                    return "O";
                }
            }
        }
        return "P";
    }

    private List<String> getOrgTokens() throws Exception {
        if (this.orgTokens == null) {
            this.orgTokens = this.scrapianResolver.resolveDataList("org_tokens").getDataList();
        }
        return this.orgTokens;
    }

    private List<String> getOrgContains() throws Exception {
        if (this.orgContains == null) {
            this.orgContains = this.scrapianResolver.resolveDataList("org_contains").getDataList();
        }
        return this.orgContains;
    }
}
