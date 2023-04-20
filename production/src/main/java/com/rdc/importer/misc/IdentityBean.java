// Copyright © 2008, 2009, 2014 Regulatory DataCorp, Inc. (RDC)

package com.rdc.importer.misc;

import java.io.Serializable;

public abstract class IdentityBean implements Serializable {

    static final long serialVersionUID = -3808670021292648029L;

    private Integer id;


    /** No-arg (default) ctor */
    public IdentityBean() { /*no content*/ }

    /**
     * Copy ctor
     *
     * @since <code>rdc_model-11.29.4</code>
     */
    public IdentityBean( final IdentityBean other )
    {
        if ( null == other ) {
            throw new IllegalArgumentException( "null provided for required 'other' param" );
        }

        this.setId( other.getId() );
    }


    public final Integer getId() {
        return id;
    }

    public final void setId(Integer id) {
        this.id = id;
    }

}
