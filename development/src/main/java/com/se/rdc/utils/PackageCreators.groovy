package com.se.rdc.utils

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.regex.Matcher


/**
 * Created by Omar on 11/30/2015.
 */

def pc = new PackageCreator();
pc.runScript();

public class PackageCreator{

  def sourceFolder = "t";
  def rdcListFile = "assets/doc/rdc_original_package.txt";
  def pkgListTxt = "";
  def currentPath = "";

  PackageCreator() {
    currentPath = new File("").getAbsolutePath().replaceAll(/(.*[\\\/]src[\\\/]).*/, '$1')
    pkgListTxt = new File(currentPath.replaceAll(/\bsrc\b.*$/, "") + rdcListFile).getText("UTF-8");
  }

  def runScript() {
    File[] fList = getAllSourceFiles();
    fList.each {
      moveToRdcPakgDir(it);
    }
  }

  def getAllSourceFiles() {
    def sf = new File(currentPath + sourceFolder);

    return sf.listFiles();
  }

  def moveToRdcPakgDir(File fileName) {
    Matcher fMatch = pkgListTxt =~ /\bsrc\b[\/\\]([^\n]+${fileName.name})/
    if (fMatch.find()) {
      def target = new File(currentPath + fMatch.group(1).replaceAll(/[^\/\\]+$/, ''));
      target.mkdirs();
      target = new File(currentPath + fMatch.group(1));
      Files.move(fileName.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
      def newPkg = fMatch.group(1).replaceAll(/^.*?\bjava\b.|.[^\/\\]+$/, '').replaceAll(/[\/\\]/, '.')+";"
      def updatedData = target.getText("UTF-8").replaceAll(/(?m)^([^\S\n]*\bpackage\b\s+).*/, '$1'+newPkg);
      target.write(updatedData);
    }
  }
}
