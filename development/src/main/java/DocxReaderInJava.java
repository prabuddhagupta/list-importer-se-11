import java.io.File;
import java.io.FileInputStream;
import java.util.List;


import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.util.POILogFactory;
import org.apache.poi.util.POILogger;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

public class DocxReaderInJava {
    private static final POILogger LOG = POILogFactory.getLogger(DocxReaderInJava.class);
    public static void main(String[] args) {
        File file= new File("converted_file.docx");
        //File file= new File("C:\\Downloads\\Test.docx");
        String path= "\\Downloads\\Test.docx";
//        System.out.println(file.getPath());
//        System.out.println(file.getAbsolutePath());
        try {
            FileInputStream fis = new FileInputStream(file.getAbsolutePath());
            //FileInputStream fis = new FileInputStream(path);
            XWPFDocument xdoc = new XWPFDocument(OPCPackage.open(fis));
            XWPFWordExtractor extractor = new XWPFWordExtractor(xdoc);
            List<XWPFParagraph> paragraphs =  xdoc.getParagraphs();

            for(XWPFParagraph paragraph : paragraphs){
                System.out.println(paragraph.getText());
            }

            System.out.println(extractor.getText());
        } catch(Exception ex) {
            LOG.log(POILogger.ERROR, "Docx can't be loaded/rendered.", ex);
        }
    }
}