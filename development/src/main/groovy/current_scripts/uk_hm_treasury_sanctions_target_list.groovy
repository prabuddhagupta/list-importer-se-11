package current_scripts

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.model.StringSource
import com.rdc.importer.scrapian.util.ModuleLoader
import com.rdc.rdcmodel.model.RelationshipType
import com.rdc.scrape.ScrapeAddress
import com.rdc.scrape.ScrapeEntity
import com.rdc.scrape.ScrapeEntityAssociation
import com.rdc.scrape.ScrapeEvent
import org.w3c.dom.Document
import org.w3c.dom.*
import org.xml.sax.InputSource

import javax.xml.parsers.DocumentBuilderFactory

import javax.xml.transform.Result
import javax.xml.transform.Source
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

context.setup([connectionTimeout: 200000, socketTimeout: 250000, retryCount: 1, multithread: true, userAgent: "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36"])
context.session.encoding = "UTF-8" //change it according to web page's encoding
context.session.escape = true

UK_Sanctions_List script = new UK_Sanctions_List(context)
script.initParsing()


class UK_Sanctions_List {

    final addressParser
    final entityType
    final def ocrReader
    ScrapianContext context

    final def moduleFactory = ModuleLoader.getFactory("6f3c2f4bbd013bb2a37e5a24530c559eb7736c72")

    final def root = 'https://www.maine.gov/ag'
    final def FILENAME = '/home/sekh/Documents/uk_sanction_list.xml'
    def xmlUrl = 'https://assets.publishing.service.gov.uk/government/uploads/system/uploads/attachment_data/file/1058116/UK_Sanctions_List.xml'

    def xlsxUrl2
    def xlsxUrl1

    UK_Sanctions_List(context) {
        ocrReader = moduleFactory.getOcrReader(context)
        entityType = moduleFactory.getEntityTypeDetection(context)
        addressParser = moduleFactory.getGenericAddressParser(context)
        addressParser.updateCities([US: ["Lake Buena Vista", "Washington", "Dorval", "Latham", "West Des Moines", "Worcester", "Miami Gardens", "Arlington", "Franklin Park", "St. Petersburg", "Worcester", "Shepherdstown", "Overland Park"]])
        addressParser.updateCities([IL: ["Ra'anana"]])
        addressParser.updateStates([US: ["D.C."]])
        addressParser.updateStates([CA: ["QC"]])
        addressParser.updateStates([IL: ["Israel"]])

        this.context = context
    }

    def initParsing() {

        def doc = processXmlTDoc(xmlUrl)

        System.out.println(
            "Root element: "
                + doc.getDocumentElement().getNodeName());


        def nodeList = doc.getElementsByTagName("Designation")

        println nodeList.getLength()

        for (int i = 1; i < nodeList.getLength(); i++) {
            def node = nodeList.item(i)


            // println node.getChildNodes()

            def lastUpdated = getTextByTagName(node, "LastUpdated")
            println lastUpdated

            def nameNodes = nodeListByTagName(node, "Names")
            // println nameNodes

            for (def nameChild : nameNodes) {
                // println nameChild
                if (nameChild.getNodeType()
                    == Node.ELEMENT_NODE) {
                    def childElement = (Element) nameChild;
                    System.out.println(
                        "Name: "
                            + childElement
                            .getTextContent())
                }
            }


            def addressNodes = nodeListByTagName(node, "Addresses")
            println addressNodes

            for (def addressChild : addressNodes) {
                // println nameChild
                if (addressChild.getNodeType()
                    == Node.ELEMENT_NODE) {
                    def childElement = (Element) addressChild;
                    System.out.println(
                        "Address: "
                            + childElement
                            .getTextContent())
                }
            }

            /*def inputSourceNode = transformXMLfromNodeOrDoc(nameNodes)

            def nameDoc = db.parse(inputSourceNode)
            nameDoc.getDocumentElement().normalize()
            System.out.println(
                "Name Root element: "
                    + nameDoc.getDocumentElement().getNodeName());

            def nameList = nameDoc.getElementsByTagName("Name")

            for (int j = 0; j < nameList.getLength(); j++) {
                def namesNode = nameList.item(j)

                if (namesNode.getNodeType()
                    == Node.ELEMENT_NODE) {
                    def nElement = (Element) namesNode;
                    System.out.println(
                        "Name: "
                            + nElement
                            //.getElementsByTagName("Name")
                            //.item(0)
                            .getTextContent())
                }
            }*/
            println("=======================================================================")
        }
    }


    def getTextByTagName(Node node, String tagName) {

        if (node.getNodeType()
            == Node.ELEMENT_NODE) {
            def tElement = (Element) node;

            def text = tElement
                .getElementsByTagName(tagName)
                .item(0)
                .getTextContent();

            return text
        }
    }

    /*Use for Hierarchy Node or Tag element*/

    def nodeListByTagName(Node node, String tagName) {

        if (node.getNodeType() == Node.ELEMENT_NODE && ((Element) node).getElementsByTagName("Addresses").length > 0) {

            def nodeList = ((Element) node)
                .getElementsByTagName(tagName)
                .item(0)
                .getChildNodes()

            return nodeList
        }
    }

    def transformXMLfromNodeOrDoc(Node nameNode) {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Source xmlSource = new DOMSource(nameNode);
        Result outputTarget = new StreamResult(outputStream);
        TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
        InputStream inputSourceNode = new ByteArrayInputStream(outputStream.toByteArray());

        return inputSourceNode
    }

    def processXmlTDoc(String url) {
        def dbf = DocumentBuilderFactory.newInstance();

        // we are creating an object of builder to parse
        // the  xml file.
        def db = dbf.newDocumentBuilder()
        def doc = db.parse(new InputSource(new URL(url).openStream()))
        /*here normalize method Puts all Text nodes in
        the full depth of the sub-tree underneath this
        Node, including attribute nodes, into a "normal"
        form where only structure separates
        Text nodes, i.e., there are neither adjacent
        Text nodes nor empty Text nodes. */
        doc.getDocumentElement().normalize();
        return doc

    }
}