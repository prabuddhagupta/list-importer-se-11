package test

import com.rdc.importer.scrapian.ScrapianContext
import com.se.rdc.core.ScrapianContextSE
import scrapian_scripts.utils.GenericAddressParserFactory

ScrapianContext context = new ScrapianContextSE()

context.cached_data.init()

def addr = "4666 FARIES PKWY, DECATUR,IL,62526";
addressParser = GenericAddressParserFactory.getGenericAddressParser(context)
addressParser.reloadData()
def addrMap = addressParser.parseAddress([text: addr, force_country: true])
println addrMap;

context.cached_data.close()

