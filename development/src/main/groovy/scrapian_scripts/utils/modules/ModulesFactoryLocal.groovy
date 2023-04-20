package scrapian_scripts.utils.modules

//This is just a local proxy class for ModulesFactory (loaded via Github loader)
// so that local script development IDEs could easily get the necessary references

class ModulesFactoryLocal extends ModulesFactory {

    ModulesFactoryLocal(String version) {
        super(version)
    }

    FxWebClient getFxWebClient(version = revision) {
        return new FxWebClient()
    }

    GenericAddressParser getGenericAddressParser(context, version = revision) {
        return new GenericAddressParser(context)
    }

    OcrReader getOcrReader(context, version = revision) {
        return new OcrReader(context)
    }

    OCRRestAPI getOCRRestAPI(context, version = revision) {
        return new OCRRestAPI(context)

    }

    OCRSpaceReader getOCRSpaceReader(context, version = revision) {
        return new OCRSpaceReader(context)
    }

    EntityTypeDetection getEntityTypeDetection(context, version = revision) {
        return new EntityTypeDetection(context)
    }
}