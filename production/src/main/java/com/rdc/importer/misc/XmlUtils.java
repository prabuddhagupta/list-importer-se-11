package com.rdc.importer.misc;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XmlUtils {

    private static Map<String, XPathExpression> xPathExpressionMap = Collections.synchronizedMap(new HashMap<String, XPathExpression>());
    private static ThreadLocal<DocumentBuilder> documentBuilderThreadLocal = new ThreadLocal<DocumentBuilder>();
    private static ThreadLocal<XPathFactory> xpathFactoryThreadLocal = new ThreadLocal<XPathFactory>();
    private static final Logger logger = LogManager.getLogger(XmlUtils.class);

    /**
     * Performs the following transformation for escaping xml.
     * & -> &amp;
     * < -> &lt;
     * > -> &gt;
     * " -> &#034;
     * ' -> &#039;
     */
    public static String escapeXml(String xml) {
        StringBuffer xmlEscaped = new StringBuffer();
        char[] xmlChar = xml.toCharArray();
        for (char c : xmlChar) {
            switch (c) {
                case '&':
                    xmlEscaped.append("&amp;");
                    break;
                case '<':
                    xmlEscaped.append("&lt;");
                    break;
                case '>':
                    xmlEscaped.append("&gt;");
                    break;
                case '"':
                    xmlEscaped.append("&quot;");
                    break;
                case '\'':
                    xmlEscaped.append("&apos;");
                    break;
                default:
                    xmlEscaped.append(c);
            }
        }
        return xmlEscaped.toString();
    }


    public static void serializeToWriter(Node node, Writer writer, boolean format) throws Exception {
        TransformerFactory transFactory = TransformerFactory.newInstance();
        if (format) {
            try {
                transFactory.setAttribute("indent-number", 2);
            } catch (Throwable t) {
                logger.info("WARN: " + t.getMessage());
            }
        }
        Transformer transformer = transFactory.newTransformer();
        if (format) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        }
        transformer.transform(new DOMSource(node), new StreamResult(writer));
    }

    public static String serializeToString(Node node) throws Exception {
        StringWriter writer = new StringWriter();
        serializeToWriter(node, writer, true);
        return writer.toString();
    }

    public static DocumentType createDocumentType(String qualifiedName, String publicId, String systemId) throws Exception {
        return getDocumentBuilder().getDOMImplementation().createDocumentType(qualifiedName, publicId, systemId);
    }

    public static Document createDocument(Reader content) throws Exception {
        return createDocument(new InputSource(content));
    }

    public static Document createDocument(InputStream content) throws Exception {
        return createDocument(new InputSource(content));
    }

    public static Document createDocument(InputStream content, String systemIdBase) throws Exception {
        return getDocumentBuilder().parse(content, systemIdBase);
    }

    public static Document createDocument(InputSource content) throws Exception {
        return getDocumentBuilder().parse(content);
    }

    public static Document createDocument() throws Exception {
        return getDocumentBuilder().newDocument();
    }

    public static Node selectNode(String xpath, Node node) throws Exception {
        return (Node) getXPathExpression(xpath).evaluate(node, XPathConstants.NODE);
    }

    public static String selectNodeValue(String xpath, Node node) throws Exception {
        Node selectedNode = selectNode(xpath, node);
        if (selectedNode != null) {
            return selectedNode.getNodeValue();
        }
        return null;
    }

    public static void transform(Transformer transformer, Reader input, Result result) throws Exception {
        transform(transformer, new StreamSource(input), result);
    }

    public static void transform(Transformer transformer, Source source, Result result) throws Exception {
        transformer.transform(source, result);
    }

    public static String transform(Transformer transformer, Node input) throws Exception {
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(input), new StreamResult(writer));
        return writer.toString();
    }

    public static Transformer createTransformer(Templates template) throws Exception {
        Transformer transformer = template.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setErrorListener(new XmlUtils.SimpleErrorListener());
        return transformer;
    }

    public static NodeList selectNodeList(String xpath, Node node) throws Exception {
        return (NodeList) getXPathExpression(xpath).evaluate(node, XPathConstants.NODESET);
    }

    public static Templates createTemplates(InputStream stream) throws Exception {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        return tfactory.newTemplates(new StreamSource(stream));
    }

    public static Document createDocument(String path) throws Exception {
        return createDocument(new InputSource(path));
    }

    private static class SimpleErrorListener implements ErrorListener, ErrorHandler {
        public void error(TransformerException exception) throws TransformerException {
            throw exception;
        }

        public void fatalError(TransformerException exception) throws TransformerException {
            throw exception;
        }

        public void warning(TransformerException exception) throws TransformerException {
            throw exception;
        }

        public void error(SAXParseException exception) throws SAXException {
            throw exception;
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }

        public void warning(SAXParseException exception) throws SAXException {
            throw exception;
        }
    }

    public static class ClasspathEntityResolver implements EntityResolver {

        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            if (systemId != null) {
                // attempt to resolve it via classpath, otherwise just let xml parser resolve it by returning null.
                int index = 0;
                if (systemId.lastIndexOf("/") != -1) {
                    index = systemId.lastIndexOf("/");
                }
                String resourceName = systemId.substring(index);
                InputStream resourceStream = XmlUtils.class.getResourceAsStream(resourceName);
                if (resourceStream != null) {
                    return new InputSource(resourceStream);
                } else {
                    resourceStream = XmlUtils.class.getResourceAsStream("/dtd" + resourceName);
                }
                if (resourceStream != null) {
                    return new InputSource(resourceStream);
                }
            }
            return null;
        }
    }


    private static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {

        DocumentBuilder builder = documentBuilderThreadLocal.get();
        if (builder == null) {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            builder.setEntityResolver(new XmlUtils.ClasspathEntityResolver());
            documentBuilderThreadLocal.set(builder);
        }
        return builder;
    }

    private static XPathFactory getXPathFactory() throws Exception {
        XPathFactory factory = xpathFactoryThreadLocal.get();
        if (factory == null) {
            factory = XPathFactory.newInstance();
            xpathFactoryThreadLocal.set(factory);
        }
        return factory;
    }


    private static XPathExpression getXPathExpression(String expression) throws Exception {
        XPathExpression xPathExpression = xPathExpressionMap.get(expression);
        if (xPathExpression == null) {
            xPathExpression = getXPathFactory().newXPath().compile(expression);
            xPathExpressionMap.put(expression, xPathExpression);
        }
        return xPathExpression;
    }

    public static Schema createSchema(InputStream inputStream) throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        return factory.newSchema(new StreamSource(inputStream));
    }
}
