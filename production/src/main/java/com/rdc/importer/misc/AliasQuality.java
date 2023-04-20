// Copyright © 2008, 2009, 2013, 2014 Regulatory DataCorp, Inc. (RDC)

package com.rdc.importer.misc;

import java.util.Arrays;

import java.io.Serializable;

import com.rdc.importer.misc.IdentityBean;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

public class AliasQuality extends IdentityBean implements Serializable {
    static final long serialVersionUID = 3992390574014927980L;
    private String code;
    private String name;
    private boolean active;

    public AliasQuality() { /*no content*/ }

    public AliasQuality( final AliasQuality other )
    {
        super( other );

        if ( null == other ) {
            throw new IllegalArgumentException( "null provided for required 'other' param" );
        }

        this.setCode( other.getCode() );
        this.setName( other.getName() );

        this.setActive( other.isActive() );
    }


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AliasQuality that = (AliasQuality) o;
        return code.equals(that.code);
    }

    public int hashCode() {
        return code.hashCode();
    }
}
