package com.rdc.importer.scrapian.resolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

public class ScrapianConfig {
    private List<Script> scripts = new ArrayList<Script>();
    private List<DataList> dataLists = new ArrayList<DataList>();

    public static ScrapianConfig create(InputStream input) throws IOException, SAXException {
        Digester digester = new Digester();
        digester.addObjectCreate("scrapian", ScrapianConfig.class);
        digester.addObjectCreate("scrapian/script", Script.class);
        digester.addSetProperties("scrapian/script");

        digester.addObjectCreate("scrapian/script/param", ScriptParam.class);
        digester.addSetProperties("scrapian/script/param");
        digester.addSetNext("scrapian/script/param", "addParam");

        digester.addSetNext("scrapian/script", "addScript");

        digester.addObjectCreate("scrapian/datalist", DataList.class);
        digester.addSetProperties("scrapian/datalist");
        digester.addSetNext("scrapian/datalist", "addDataList");

        return (ScrapianConfig) digester.parse(input);
    }


    public String getUrlForScript(String scriptName) {
        for (Script script : scripts) {
            if (script.getKey().equals(scriptName)) {
                return script.getUrl();
            }
        }
        return null;
    }

    public String getUrlForDataList(String dataListName) {
        for (DataList dataList : dataLists) {
            if (dataList.getKey().equals(dataListName)) {
                return dataList.getUrl();
            }
        }
        return null;
    }

    public Map<String, String> getParamsForScript(String scriptName) {
        for (Script script : scripts) {
            if (script.getKey().equals(scriptName)) {
                return script.getParams();
            }
        }
        return new HashMap<String, String>();
    }

    public void addDataList(DataList dataList) {
        dataLists.add(dataList);
    }

    public void addScript(Script script) {
        scripts.add(script);
    }

    public static class Script {
        private String key;
        private String url;
        private Map<String, String> params = new HashMap<String, String>();

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void addParam(ScriptParam param) {
            params.put(param.getName(), param.getValue());
        }

        public Map<String, String> getParams() {
            return params;
        }
    }

    public static class DataList {
        private String key;
        private String url;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class ScriptParam {
        private String name;
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
