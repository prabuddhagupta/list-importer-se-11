//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.01.21 at 02:43:08 PM EST 
//


package com.rdc.importer.thirdparty.sdn;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="IDRegDocumentID" use="required" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" />
 *       &lt;attribute name="DeltaAction" type="{http://www.un.org/sanctions/1.0}DeltaActionSchemaType" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "IDRegDocumentReference")
public class IDRegDocumentReference {

    @XmlAttribute(name = "IDRegDocumentID", required = true)
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger idRegDocumentID;
    @XmlAttribute(name = "DeltaAction")
    protected DeltaActionSchemaType deltaAction;

    /**
     * Gets the value of the idRegDocumentID property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getIDRegDocumentID() {
        return idRegDocumentID;
    }

    /**
     * Sets the value of the idRegDocumentID property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setIDRegDocumentID(BigInteger value) {
        this.idRegDocumentID = value;
    }

    /**
     * Gets the value of the deltaAction property.
     * 
     * @return
     *     possible object is
     *     {@link DeltaActionSchemaType }
     *     
     */
    public DeltaActionSchemaType getDeltaAction() {
        return deltaAction;
    }

    /**
     * Sets the value of the deltaAction property.
     * 
     * @param value
     *     allowed object is
     *     {@link DeltaActionSchemaType }
     *     
     */
    public void setDeltaAction(DeltaActionSchemaType value) {
        this.deltaAction = value;
    }

}
