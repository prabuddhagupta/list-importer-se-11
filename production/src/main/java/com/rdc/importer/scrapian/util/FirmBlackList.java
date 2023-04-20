package com.rdc.importer.scrapian.util;

public class FirmBlackList {
    private Integer bizUnitId;
    private String firmNumber;
    private String listName;
    private Boolean stopSearchOnThisSource;
    private Boolean onlyAlertThisSource;

    public Integer getBizUnitId() {
        return bizUnitId;
    }

    public void setBizUnitId(Integer bizUnitId) {
        this.bizUnitId = bizUnitId;
    }

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public Boolean getStopSearchOnThisSource() {
        return stopSearchOnThisSource;
    }

    public void setStopSearchOnThisSource(Boolean stopSearchOnThisSource) {
        this.stopSearchOnThisSource = stopSearchOnThisSource;
    }

    public String getFirmNumber() {
        return firmNumber;
    }

    public void setFirmNumber(String firmNumber) {
        this.firmNumber = firmNumber;
    }

    public Boolean getOnlyAlertThisSource() {
        return onlyAlertThisSource;
    }

    public void setOnlyAlertThisSource(Boolean onlyAlertThisSource) {
        this.onlyAlertThisSource = onlyAlertThisSource;
    }

    @Override
    public String toString() {
        return "FirmBlackList{" +
                "firmNumber='" + firmNumber + '\'' +
                ", listName='" + listName + '\'' +
                '}';
    }
}