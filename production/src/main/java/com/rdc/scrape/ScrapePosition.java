package com.rdc.scrape;

import java.io.Serializable;

public class ScrapePosition implements Serializable {
    private String description;
    private String fromYear;
    private String fromMonth;
    private String fromDay;
    private String toYear;
    private String toMonth;
    private String toDay;

    public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		if(description.length() > 500){
			description = description.substring(0,497) + "...";
		}
		this.description = description;
	}

	public String getFromYear() {
		return fromYear;
	}

	public void setFromYear(String fromYear) {
		this.fromYear = fromYear;
	}

	public String getFromMonth() {
		return fromMonth;
	}

	public void setFromMonth(String fromMonth) {
		this.fromMonth = fromMonth;
	}

	public String getFromDay() {
		return fromDay;
	}

	public void setFromDay(String fromDay) {
		this.fromDay = fromDay;
	}

	public String getToYear() {
		return toYear;
	}

	public void setToYear(String toYear) {
		this.toYear = toYear;
	}

	public String getToMonth() {
		return toMonth;
	}

	public void setToMonth(String toMonth) {
		this.toMonth = toMonth;
	}

	public String getToDay() {
		return toDay;
	}

	public void setToDay(String toDay) {
		this.toDay = toDay;
	}

	public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ScrapePosition pos = (ScrapePosition) o;

        if (description != null ? !description.equals(pos.description) : pos.description != null) {
            return false;
        }
        if (fromYear != null ? !fromYear.equals(pos.fromYear) : pos.fromYear != null) {
            return false;
        }
        if (fromMonth != null ? !fromMonth.equals(pos.fromMonth) : pos.fromMonth != null) {
            return false;
        }
        if (fromDay != null ? !fromDay.equals(pos.fromDay) : pos.fromDay != null) {
            return false;
        }
        if (toYear != null ? !toYear.equals(pos.toYear) : pos.toYear != null) {
            return false;
        }
        if (toMonth != null ? !toMonth.equals(pos.toMonth) : pos.toMonth != null) {
            return false;
        }
        if (toDay != null ? !toDay.equals(pos.toDay) : pos.toDay != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (description != null ? description.hashCode() : 0);
        result = 31 * result + (fromYear != null ? fromYear.hashCode() : 0);
        result = 31 * result + (fromMonth != null ? fromMonth.hashCode() : 0);
        result = 31 * result + (fromDay != null ? fromDay.hashCode() : 0);
        result = 31 * result + (toYear != null ? toYear.hashCode() : 0);
        result = 31 * result + (toMonth != null ? toMonth.hashCode() : 0);
        result = 31 * result + (toDay != null ? toDay.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ScrapePosition [description=" + description + ", fromYear=" + fromYear + ", fromMonth=" + fromMonth + ", fromDay=" + fromDay + ", toYear=" + toYear + ", toMonth=" + toMonth + ", toDay=" + toDay + "]";
    }
    
    
}
