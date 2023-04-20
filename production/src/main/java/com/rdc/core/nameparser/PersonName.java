package com.rdc.core.nameparser;


public class PersonName {
    private String prefix;
    private String firstname;
    private String middlename;
    private String lastname;
    private String suffix;

    public PersonName() {}

    public PersonName(String lastname) {this(null, lastname);}

    public PersonName(String firstname, String lastname) {
        this(firstname, null, lastname);
    }

    public PersonName(String firstname, String middlename, String lastname) {
        this(firstname, middlename, lastname, null);
    }

    public PersonName(String firstname, String middlename,
                      String lastname, String suffix) {
        this(null, firstname, middlename, lastname, suffix);
    }

    public PersonName(String prefix, String firstname, String middlename,
                      String lastname, String suffix) {
        this.prefix = prefix;
        this.firstname = firstname;
        this.middlename = middlename;
        this.lastname = lastname;
        this.suffix = suffix;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getMiddlename() {
        return middlename;
    }

    public void setMiddlename(String middlename) {
        this.middlename = middlename;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public String toString() {
        final String s =
            (prefix     != null ? prefix     + " " : "") +
            (firstname  != null ? firstname  + " " : "") +
            (middlename != null ? middlename + " " : "") +
            (lastname   != null ? lastname   + " " : "") +
            (suffix     != null ? suffix           : "");
        return s.trim();
    }
}

