package com.se.rdc.core;

import com.google.common.io.Files;
import com.rdc.importer.scrapian.ScrapianDataList;
import com.rdc.importer.scrapian.ScrapianScript;
import com.rdc.importer.scrapian.resolver.ScrapianResolver;
import org.apache.commons.collections.map.HashedMap;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ScrapianResolverSE implements ScrapianResolver {
    private final String ORG_TOKEN_LIST = "org_tokens";
    private final String ORG_CONTAIN_LIST = "org_contains";
    private final String[] cmdArgs;
    private List orgTokenList;

    public ScrapianResolverSE(String[] cmdArgs) {
        this.cmdArgs = cmdArgs;
    }

    public ScrapianResolverSE() {
        this.cmdArgs = new String[0];
    }

    @Override public ScrapianScript resolveScript(String scriptName)
        throws Exception {
        ScrapianScript scrapianScript = new ScrapianScript();

        if (cmdArgs.length > 0) {
            HashedMap params = new HashedMap(3);

            for (int i = 1; i < cmdArgs.length; i++) {
                String[] splitParamArr = cmdArgs[i].split("=");
                params.put(splitParamArr[0], splitParamArr[1]);
            }

            scrapianScript.setUrl(new URL(cmdArgs[0]));
            scrapianScript.setParams(params);
        }

        return scrapianScript;
    }

    @Override public ScrapianDataList resolveDataList(String dataListName)
        throws Exception {
        ScrapianDataList dataList = new ScrapianDataList();
        dataList.setKey(dataListName);

        if (dataListName.equals(ORG_TOKEN_LIST)) {
            dataList.setDataList(getOrgTokenList());

        } else {
            dataList.setDataList(new ArrayList<>());
        }

        return dataList;
    }

    private List getOrgTokenList() throws IOException {
        if (orgTokenList == null) {
            orgTokenList = Files.readLines(
                new File("../assets/config/predefined_org_tokens.txt"), UTF_8);
        }

        return orgTokenList;
    }
}