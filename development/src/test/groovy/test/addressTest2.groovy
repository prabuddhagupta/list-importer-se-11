package test

import com.se.rdc.core.ScrapianContextSE
import scrapian_scripts.utils.GenericAddressParserFactory

def addr = "1001 LOUISIANA STREET, HOUSTON, TX, 77002, sdfshg";
def addressParser = GenericAddressParserFactory.getGenericAddressParser(new ScrapianContextSE())
addressParser.reloadData()
def addrMap = addressParser.parseAddress([text: addr, force_country: true])
println addrMap;