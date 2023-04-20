package com.se.rdc.utils


def fmt = new OrgTypeFormatter();
fmt.showFormatedForm();

class OrgTypeFormatter{

  private String tokenFile = "../../../../../../../../output/tokens.txt";
  private int wordsPerRowTh = 15;
  private TreeSet<String> set = new TreeSet<String>();

  public setFile(String file){
    this.tokenFile = file;
  }

  private void sortTokens(){
    String txt = new File(tokenFile).getText("UTF-8");
    txt.split(/\s*\n+\s*/).each{
      it= it.trim();
      if(it){
        set.add(it);
      }
    }
  }

  def showFormatedForm(){
    sortTokens();
    int itr = 0;
    def rowStr = "if( name =~ /(?i)\\b(?:"

    if(set.size() > 0){
      def initChr = set.first()[0]
      for(String curWord : set){

        if(itr < wordsPerRowTh || curWord =~ /(?i)^${initChr}/){
          rowStr += curWord + "|";
        }else{
          rowStr += ")\\b/ ){\n  return \"O\";\n}else if( name =~ /(?i)\\b(?:" + curWord+"|";
          itr = 0;
        }

        initChr = curWord[0];
        itr++;
      }
      rowStr = rowStr.replaceAll(/\|$/,")\\\\b/ ){\n  return \"O\";\n}").replaceAll(/\|(\s*\))/,'$1');
      println rowStr;
    }else{
      println "Token list is empty!"
    }
  }
}
