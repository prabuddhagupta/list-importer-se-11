package com.se.rdc.utils;

public class StringToUnicode {

  // Similar implementation in : http://app.live-start.com/magic-status/char.htm

  public static void main(String[] args) {
    StringToUnicode su = new StringToUnicode();
    su.stringToUnicodeHtmlCodes("-–— – ");
    su.htmlDecimalValueToChar("&#8211;|233");
    su.unicodeHexStrToChar("\\u0072");
  }

  /**
   * In format: &#digit; OR only digit (base 10); separate by "|"
   * */
  public void htmlDecimalValueToChar(String s) {
    System.out.println("\n==== Char value for : " + s + " ==");
    String[] aStr = s.replaceAll("[&#;\\s]+", "").split("\\s*\\|\\s*");
    for (String str : aStr) {
      if (str.length() > 0) {
        Character c = (char) Integer.parseInt(str);
        System.out.println(str + " -> " + c);
      }
    }
  }

  /**
   * In format: \\uXXXX OR any hex digit; separate by "|"
   * */
  public void unicodeHexStrToChar(String s) {
    System.out.println("\n==== Char value for : " + s + " ==");
    String[] aStr = s.replaceAll("[u\\\\s]+", "").split("\\s*\\|\\s*");
    for (String str : aStr) {
      if (str.length() > 0) {
        Character c = (char) Integer.parseInt(str, 16);
        System.out.println(s + " -> " + c);
      }
    }
  }

  /**
   * In format: any string
   * */
  public void stringToUnicodeHtmlCodes(String str) {
    char[] ch = str.toCharArray();
    System.out.println("\n==== Unicode-Html values for :" + str + " ==");
    for (char c : ch) {
      int i = (int) c;
      String val = intToUnicodeHex(i);
      System.out.println(c + " -> " + val + " | " + "&#" + i + ";");
    }
  }

  private String intToUnicodeHex(int i) {
    String val = Integer.toHexString(i);
    if (val.length() == 2) {
      val = "00" + val;
    } else if (val.length() == 3) {
      val = "0" + val;
    } else if (val.length() == 1) {
      val = "000" + val;
    }
    val = "\\u" + val;
    return val;
  }

}
