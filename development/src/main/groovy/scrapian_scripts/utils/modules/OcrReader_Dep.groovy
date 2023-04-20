package scrapian_scripts.utils.modules

import groovy.grape.Grape

/**
 * This class is only used as a dependency stub for OcrReader
 * Check ModulesFactory class for detail use case
 */
@GrabResolver(name = 'central', root = 'http://central.maven.org/maven2/')

//@Grapes([
//    @Grab(group = 'org.apache.pdfbox', module = 'pdfbox', version = '2.0.4'),
//    @GrabConfig(systemClassLoader=true, initContextClassLoader=true)
//])


class OcrReader_Dep {

  def getLoader() {
    def loader = new GroovyClassLoader()
    Grape.grab([classLoader: loader], [group: 'org.apache.pdfbox', module: 'pdfbox', version: '2.0.4'])
    return loader
  }
}