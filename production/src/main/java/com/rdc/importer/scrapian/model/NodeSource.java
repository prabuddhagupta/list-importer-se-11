package com.rdc.importer.scrapian.model;

import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;

public class NodeSource implements ScrapianSource {

    private Node value;

    public NodeSource(Node value) {
        this.value = value;
    }

    public Node getValue() {
        return value;
    }

    public String serialize() throws TransformerException {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        StringWriter sw = new StringWriter();
        t.transform(new DOMSource(value), new StreamResult(sw));
        return sw.toString();
    }

    public String toString() {
        try {
            return serialize();
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    public int hashCode() {
        String value = toString();
        if (value != null) {
            return value.hashCode();
        }
        return super.hashCode();
    }

    public boolean equals(Object that) {
        if (that instanceof NodeSource) {
            String thisToString = this.toString();
            String thatToString = that.toString();
            return thisToString != null && thatToString != null && thisToString.equals(thatToString);
        }
        return false;
    }
}
