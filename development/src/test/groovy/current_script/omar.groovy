package current_script

import com.rdc.importer.scrapian.util.ModuleLoader
import com.se.rdc.core.ScrapianContextSE

context = new ScrapianContextSE()
url = "http://www.africau.edu/images/default/sample.pdf"
factoryRevision = "519ae4bb0ae45b0152e0eaefe281525b60ebaa1e"
moduleFactory = ModuleLoader.getFactory(factoryRevision)
ic = moduleFactory.getOcrReader(context)
println ic.getText(url, [force_ocr: true, cache: false])

//addressParser = new GenericAddressParser_bckup(context)
//token = addressParser.tokens
////addressParser.reloadData()
//Set sxt = []
//def addr = "705 Hickory, PO Box 64, Stanton, NE 68779"
//println addressParser.parseAddress([text: addr, force_country: true])
//
//new File("/media/omar/Projects/projects/office_project/RDCScrapper/output/reports/us_ne_sex_offenders/addr_1.txt").eachLine {
//  def s = it.split("--->")
//  def addrMap = addressParser.parseAddress([text: s[3], force_country: true])
//  def city = addrMap[GenericAddressParser_bckup.TOKENS.CITY].toString().toLowerCase()
//  if (city != s[1]) {
//    sxt.add(s[3]+"-->"+city+"->"+addrMap.toString())
//  }
//}
//
//sxt.each{
//  println "no match: " +it
//}

// 705 Hickory, PO Box 64, Stanton, NE 68779
// 303 Nevada St, Clearwater, NE 68726
// 211 Missouri, Alliance, NE 69301

def invoke(url, isPost = false, isBinary = false, cache = true, postParams = [:], headersMap = [:], cleanSpaceChar = false, tidy = false, miscData = [:]) {
  Map data = [url: url, tidy: tidy, headers: headersMap, cache: cache, clean: cleanSpaceChar]
  if (isPost) {
    data.type = "POST"
    data.params = postParams
  }
  data.putAll(miscData)

  try {
    if (isBinary) {
      return context.invokeBinary(data)
    } else {
      return context.invoke(data)
    }

  } catch (InterruptedException ignored) {
    System.err.println("Interrupted exception for: " + url)

  } catch (e) {
//    def executor = tasker.getExecutorService()
//    if (executor instanceof ThreadPoolExecutor) {
//      ((ThreadPoolExecutor) executor).setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
//    }
//    executor.shutdownNow()
//    System.err.println("Error invoking: " + url)
//    throw new Exception(e)
  }
}