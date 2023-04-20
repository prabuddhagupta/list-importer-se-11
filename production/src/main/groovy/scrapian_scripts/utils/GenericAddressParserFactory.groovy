package scrapian_scripts.utils

import com.rdc.importer.scrapian.ScrapianContext
import com.rdc.importer.scrapian.ScrapianEngine
import com.rdc.importer.scrapian.util.ModuleLoaderContext

import java.util.concurrent.Callable

/**
 public/protected methods could be overridden by a sub class to add more custom name/regex values in the core parser.
 */

public class GenericAddressParserFactory {  
	static String fileLocation = "https://github.com/RegDC/scripts";
	static String fileName = "GenericAddressParser.groovy";
	static getGenericAddressParser(ScrapianContext context, xlsLocation = null, scriptLocation = ""/*getScriptLocation(fileLocation,fileName,"")*/) {
		return new GenericAddressParser(context, xlsLocation);//new GroovyClassLoader().parseClass(new File(scriptLocation)).newInstance(context, xlsLocation);
	}

	static String getScriptLocation(fileLocation,fileName,version) {
		def scrape = new ScrapianEngine();
		def scriptLocation = scrape.getGitHubURLAsString(fileLocation,fileName,version);
		def scriptpath = scriptLocation.replaceAll("file:/","")
		return scriptpath;
	}

	static {
		//execute any configuration tasks
		try {
			if (ModuleLoaderContext.preActions.size() > 0) {
				for (Callable preAction : ModuleLoaderContext.preActions) {
					preAction.call();
				}

			}
		} catch (Exception ignored) {
		}
	}
}