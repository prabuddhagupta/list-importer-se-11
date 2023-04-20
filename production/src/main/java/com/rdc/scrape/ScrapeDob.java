package com.rdc.scrape;

import java.io.Serializable;

public class ScrapeDob implements Serializable {
    private String year;
    private String month;
    private String day;
    private Boolean circa;
    private String startYear;
    private String endYear;

    public ScrapeDob() {
        super();
    }

    /**
     * @param date MM/dd/yyyy - note circa years are -/-/yyyy format.
     */
    public ScrapeDob(String date) {
        String[] datePart = date.split("/");
        if (datePart != null && datePart.length == 3) {
            this.year = datePart[2];
            if (datePart[0].equals("-") || datePart[1].equals("-")) {
                this.circa = true;
            } 
            if(!datePart[0].equals("-")){
                this.month = datePart[0];
            }
            if(!datePart[1].equals("-")){
                this.day = datePart[1];
            }           
        } else {
            throw new RuntimeException("Invalid date[" + date + "]");
        }
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public Boolean getCirca() {
        return circa;
    }

    public void setCirca(Boolean circa) {
        this.circa = circa;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getStartYear() {
        return startYear;
    }

    public void setStartYear(String startYear) {
        this.startYear = startYear;
    }

    public String getEndYear() {
        return endYear;
    }

    public void setEndYear(String endYear) {
        this.endYear = endYear;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ScrapeDob scrapeDob = (ScrapeDob) o;

        if (circa != null ? !circa.equals(scrapeDob.circa) : scrapeDob.circa != null) {
            return false;
        }
        if (day != null ? !day.equals(scrapeDob.day) : scrapeDob.day != null) {
            return false;
        }
        if (endYear != null ? !endYear.equals(scrapeDob.endYear) : scrapeDob.endYear != null) {
            return false;
        }
        if (month != null ? !month.equals(scrapeDob.month) : scrapeDob.month != null) {
            return false;
        }
        if (startYear != null ? !startYear.equals(scrapeDob.startYear) : scrapeDob.startYear != null) {
            return false;
        }
        if (year != null ? !year.equals(scrapeDob.year) : scrapeDob.year != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (year != null ? year.hashCode() : 0);
        result = 31 * result + (month != null ? month.hashCode() : 0);
        result = 31 * result + (day != null ? day.hashCode() : 0);
        result = 31 * result + (circa != null ? circa.hashCode() : 0);
        result = 31 * result + (startYear != null ? startYear.hashCode() : 0);
        result = 31 * result + (endYear != null ? endYear.hashCode() : 0);
        return result;
    }
}
