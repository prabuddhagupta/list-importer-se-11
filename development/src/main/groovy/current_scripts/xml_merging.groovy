package current_scripts

import groovy.util.slurpersupport.GPathResult;
import groovy.xml.StreamingMarkupBuilder;

// If there is no match in the products/categories file, use this empty node
def emptyCategoriesXML = ''


// Uses a SAX parser, less memory and overhead than a DOM parser (XmlParser)
// parse() method returns a GPathResult, which allows you to traverse and
// manipulate  an XML file or snippet using dot (.) notation.
def xs = new XmlSlurper()
// Main products file with SKU list
//def productsSKUs = xs.parse(new File('/home/sekh/Documents/RDCScrapper/output/india_mca_disqualified_directors_two.xml'))
//def productsSKUs = xs.parse(new File('/home/sekh/Documents/RDCScrapper/output/ProductSKU.xml'))

def xmlFile1 = new File("/home/sekh/Documents/RDCScrapper/output/india_mca_disqualified_directors_two.xml")
def xmlFile2 = new File("/home/sekh/Documents/RDCScrapper/output/india_mca_disqualified_directors_2_hyderabad.xml")
File output = new File('/home/sekh/Downloads/Productsa.xml')

//def getItems = {xml -> xml.'**'.findAll { it.name() == 'entity' }}

def pXml1 = new XmlSlurper(false,false).parse(xmlFile1)
def pXml2 = new XmlSlurper(false,false).parse(xmlFile2)


///println groovy.xml.XmlUtil.serialize(pXml1)


//getItems(pXml2).collect {pXml1.appendNode(it) }
pXml2.'**'.findAll{it.name() == 'entity'}.collect{ pXml1.appendNode(it)}

def content = groovy.xml.XmlUtil.serialize(pXml1)

//Write to file.
output.newWriter().withWriter { w ->
    w << content
}
/*
// Products file with list of categories
//def productsCategories = xs.parse(new File('/home/sekh/Documents/RDCScrapper/output/india_mca_disqualified_directors_2_hyderabad.xml'))
def productsCategories = xs.parse(new File('/home/sekh/Documents/RDCScrapper/output/ProductsCategory.xml'))

def emptyCategories = xs.parseText(emptyCategoriesXML)
// Output file
File output = new File('/Downloads/Productsa.xml')

// Store category nodes into a Map for fast retrieval later. Key is product ID.
// Note: if you're using Talend, you can't statically type this and must use this:
// def productsCategoriesMap = new HashMap<String, GPathResult>()
HashMap<String, GPathResult> productsCategoriesMap = new HashMap<String, GPathResult>()

// Loop through all the products. Note that the root category is products
// (plural), but the GPathResult you get from XmlSlurper assumes you're already
// in the root category. That's why it's not productsCategories.products.product.each
productsCategories.product.each {
    // Note you must put the id in a String (Groovy style shown here)
    // in order to have a String key.
    productsCategoriesMap["${it.@id}"] = it
}

// This allows you to use a DSL to write the file. Note that you are not
// actually doing the work specified in the closure until you start writing it.
new StreamingMarkupBuilder().bind {
    // mkp is a special markup namespace for use within this closure. There
    // are other methods as well, see the docs.
    mkp.xmlDeclaration(["version":"1.0", "encoding":"UTF-16LE"])
    // My root category
    products {
        // Loop through each product and append (insert) the categories
        // node to the product node with the same product id.
        productsSKUs.product.each {
            if (productsCategoriesMap["${it.@id}"] != null) {
                it.appendNode(productsCategoriesMap["${it.@id}"].sites)
            } else {
                it.appendNode(emptyCategories)
            }
            // Note this is not System.out, it merely ensures the
            // GPathResult is printed when written.
            out << it
        }
    }
// Here we actually write the file, executing the above closure.
// Note how I specify the character set to match the declaration.
} .writeTo(output.newWriter("UTF-16LE"))*/
