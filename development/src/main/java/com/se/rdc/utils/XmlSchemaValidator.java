package com.se.rdc.utils;

import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class XmlSchemaValidator {

  private String xmlFile = "";
  private String schemaFile = "";

  public XmlSchemaValidator(String xmlFile, String schemaFile) {
    this.xmlFile = xmlFile;
    this.schemaFile = schemaFile;
  }

  public static void main(String[] args) throws FileNotFoundException, SAXException{
    XmlSchemaValidator xml = new XmlSchemaValidator("./assets/output/ny_gov_insurance.groovy.xml", "./assets/doc/ScrapeEntity.xsd");
    xml.validate();
  }
  
  public void validate() throws FileNotFoundException, SAXException {

    InputStream input = new FileInputStream(xmlFile);
    InputStream inputSchema = new FileInputStream(schemaFile);
    SchemaFactory factory = SchemaFactory
        .newInstance("http://www.w3.org/2001/XMLSchema");
    Schema schema = factory.newSchema(new StreamSource(inputSchema));

    Validator validator = schema.newValidator();
    try {
      validator.validate(new StreamSource(input));
      System.out.println("XML Schema validation successful!");
    } catch (Exception e) {
      System.out.println("Failed XML Schema validation.");
      e.printStackTrace();
    }
  }
}
