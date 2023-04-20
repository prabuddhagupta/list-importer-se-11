package scrapian_scripts.utils.modules

import com.rdc.importer.scrapian.util.GitHubModuleLocator
import com.rdc.importer.scrapian.util.ModuleLoaderContext

import javax.tools.JavaCompiler
import javax.tools.ToolProvider
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ModulesFactory {
    protected final String revision
    private final String localModuleRef = "scrapian_scripts.utils.modules."
    private final File compiledClassDir = new File(System.getProperty("java.io.tmpdir"), "RDC_github/classes/")

    //TODO: redesign -> use ScrapianContext in the constructor and make all the getXXXX methods "context" removed.
    ModulesFactory(String version = ModuleLoaderContext.HEAD_REV) {
        revision = version
    }

    def getGenericAddressParser(context, version = revision) {
        Object[] arguments = [context, GitHubModuleLocator.getLocalPath("standard_addresses", version, false)]
        return createInstance("GenericAddressParser.groovy", version, arguments)
    }

    def getOcrReader(context, version = revision) {
        def loader = createInstance("OcrReader_Dep.groovy", version).loader
        Object[] arguments = [context]
        return createInstance("OcrReader.groovy", version, arguments, loader)
    }

    def getFxWebClient(version = revision) {
        return createInstance("FxWebClient.java", version)
    }

    def getEntityTypeDetection(context, version = revision) {
        Object[] arguments = [context, GitHubModuleLocator.getLocalPath("assets", version, false)]
        return createInstance("EntityTypeDetection.groovy", version, arguments)
    }

    private def createInstance(String scriptName, String version, arguments = null, groovyLoader = new GroovyClassLoader()) {
        def localClassRef = localModuleRef + scriptName.replaceAll(/\.[^.]+$/, "")

        if (version.equals(ModuleLoaderContext.HEAD_REV) && !ModuleLoaderContext.isRemoteEnv) {
            try {
                Class targetClass = Class.forName(localClassRef)
                if (arguments) {
                    return targetClass.newInstance(arguments)
                } else {
                    return targetClass.newInstance()
                }
            } catch (ignored) {
            }
        }

        def srcFilePath = new File(GitHubModuleLocator.getLocalPath(scriptName, version))
        if ((scriptName =~ /(?i)\.java$/).find()) {
            return loadJavaClass(srcFilePath, localClassRef, version)
        }

        def gClass = groovyLoader.parseClass(srcFilePath)
        if (arguments) {
            return gClass.newInstance(arguments)
        } else {
            return gClass.newInstance()
        }
    }

    private def loadJavaClass(File srcFile, classReference, String version) {
        //prepare
        File compiledClassDir = new File(compiledClassDir, version)
        def fileToCompile = new File(compiledClassDir, classReference.replaceAll(/\./, "/") + ".java")

        if (!fileToCompile.exists() || version.equals(ModuleLoaderContext.HEAD_REV)) {
            fileToCompile.parentFile.mkdirs()
            Files.copy(srcFile.toPath(), fileToCompile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        //compile
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, null, null, fileToCompile.getPath());

        // Load and instantiate compiled class.
        def cl = new GroovyClassLoader()
        cl.addClasspath(compiledClassDir.path)
        return cl.loadClass(classReference).newInstance()
    }
}