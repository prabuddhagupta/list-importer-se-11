package com.rdc.scrape;

import java.io.Serializable;

public class ScrapeDeceased implements Serializable {
    private String year;
    private String month;
    private String day;
    private Boolean dead;

    public ScrapeDeceased() {
        super();
    }

    /**
     * @param date MM/dd/yyyy 
     */
    public ScrapeDeceased(String date, Boolean dead) {
    	this.dead = dead;
        String[] datePart = date.split("/");
        if (datePart != null && datePart.length == 3) {
            this.year = datePart[2];
            if (!datePart[0].equals("-")) {
                this.month = datePart[0];
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

    public Boolean getDead() {
        return dead;
    }

    public void setDead(Boolean dead) {
        this.dead = dead;
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

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ScrapeDeceased scrapeDob = (ScrapeDeceased) o;

        if (dead != null ? !dead.equals(scrapeDob.dead) : scrapeDob.dead != null) {
            return false;
        }
        if (day != null ? !day.equals(scrapeDob.day) : scrapeDob.day != null) {
            return false;
        }
        if (month != null ? !month.equals(scrapeDob.month) : scrapeDob.month != null) {
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
        result = 31 * result + (dead != null ? dead.hashCode() : 0);
        return result;
    }
}
