package scrapian_scripts.utils.modules

import com.rdc.importer.scrapian.ScrapianContext
import java.util.regex.Pattern

class EntityTypeDetection {
  private ScrapianContext context;
  private String fileLocation = "../assets/config/"
  protected List extraTokenList;


  EntityTypeDetection(ScrapianContext context, fileLocation = null) {
    this.context = context
    if (fileLocation) {
      this.fileLocation = fileLocation.replaceAll(/(?<!\/)$/, "/")
    }
    processTokenFile()

  }

  def processTokenFile() {
    def text = context.invoke([url: 'file://' + new File(fileLocation + "orgTokensCleaned.txt").getCanonicalPath(), cache: false]);
    text = text.split("\n")
    extraTokenList = text.getValue();

  }

  //will be updated later if any modification requires.
  def detectEntityType(def entityName) {
    def type = ""

    extraTokenList.each {def tokenVal ->
      tokenVal = Pattern.quote(tokenVal.toString())
      if (entityName =~ /(?i)\b$tokenVal\b/) {
        type = "O"
        return type
      } else if (entityName =~ /(?m)(?!^)\b(\w{1,2}\.\s*\w{1,2}\b)+/) {   //All dot words ex:s.a. de c.v. |s.r.l.
        type = "O"
        return type
      } else if (entityName =~ /(?i)(?:\d+|&|\w+\.com)/) {   //all numbers,url ex:m23 limited|abc.com
        type = "O"
        return type
      }
    }

    if (!type) {
      type = "P"
    }
    return type

  }

  /**
   * @param file
   Existing orgtokenfile (ie:"orgTokensCleaned.txt") that is uploaded in git
   * @param extraTokenList
   when we will add extra token in existing file
   * @return
   Modified sorted file
   */

  def addTokensintoFile(File file, def extraTokenList = []) {
    def lines;
    // if(extraTokenList.size()>0) {
    extraTokenList.each {
      file.append('\n' + it.toString().toUpperCase())
    }
    //}

    lines = file.readLines()
    lines = lines.unique()
    lines = lines.sort()
    //sort by word length
    Collections.sort(lines, new Comparator<String>() {
      @Override
      public int compare(String left, String right) {
        return left.length().compareTo(right.length())

      }

    });
    //write modified data into a existing file
    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
    for (line in lines) {
      if (line.trim()) {
        bw.write(line)
        bw.newLine();
      }

    }
    bw.close()

  }
}



