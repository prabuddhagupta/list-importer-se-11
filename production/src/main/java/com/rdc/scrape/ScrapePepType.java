package com.rdc.scrape;

import java.io.Serializable;

public class ScrapePepType implements Serializable {
    private String type;
    private String level;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ScrapePepType pepType = (ScrapePepType) o;

        if (type != null ? !type.equals(pepType.type) : pepType.type != null) {
            return false;
        }
        if (level != null ? !level.equals(pepType.level) : pepType.level != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (type != null ? type.hashCode() : 0);
        result = 31 * result + (level != null ? level.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ScrapePepType [type=" + type + ", level=" + level  + "]";
    }
}