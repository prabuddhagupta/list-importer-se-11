package com.se.rdc.core;

import com.rdc.importer.scrapian.ExportReport;
import com.se.rdc.core.utils.AppConstant;
import net.sf.ehcache.Cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ExportReportSE {

    public void toReport(Cache sEntities, String fullScriptPath,
                         String csvFilePath) {
        String csvFile = csvFilePath;
        csvFilePath = AppConstant.REPORT_DIR + csvFilePath + "/";

        //create non-existing directories
        new File(csvFilePath).mkdirs();

        try {
            //Using reflection to modify the logger field
            Class innerClazz = Class.forName(
                    "com.rdc.importer.scrapian.ExportReport$ExpRunnable");
            Constructor<?> constructor = innerClazz.getDeclaredConstructor(
                    ExportReport.class);
            constructor.setAccessible(true);

            //and pass instance of Outer class as first argument
            Object o = constructor.newInstance(new ExportReport());
            Method method = o.getClass().getDeclaredMethod("toReport", Cache.class,
                    String.class, String.class, String.class);
            method.setAccessible(true);
            method.invoke(o, sEntities, fullScriptPath, csvFilePath, csvFile);

            //search file
            ZipOutputStream zipOutputStream = getZipOutputStream(csvFilePath);
            zipOutputStream.setLevel(Deflater.BEST_COMPRESSION);

            //Add script in the zip
            String scriptName = fullScriptPath.replaceAll(".*(?:/|\\\\)", "");
            addFileToZipStream(zipOutputStream, scriptName,
                    fullScriptPath.replaceAll(".*?:/*(?=/)", ""));

            //add xml file to the zip
            String xmlFile = csvFile + ".xml";
            addFileToZipStream(zipOutputStream, xmlFile,
                    new File(csvFilePath + xmlFile).getAbsolutePath());

            zipOutputStream.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void addFileToZipStream(ZipOutputStream zipOutputStream,
                                    String fileName, String filePath) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(fileName));
        FileInputStream in = new FileInputStream(filePath);

        int len;
        byte[] buffer = new byte[1024];
        while ((len = in.read(buffer)) > 0) {
            zipOutputStream.write(buffer, 0, len);
        }

        in.close();
        zipOutputStream.closeEntry();
    }

    private ZipOutputStream getZipOutputStream(String directoryName)
            throws IOException {
        File directory = new File(directoryName);
        ZipOutputStream zipOutputStream = null;
        String timeStamp = new SimpleDateFormat("E-MM-dd-yyyy").format(new Date());

        // get all the files from a directory
        File[] fList = directory.listFiles();

        //delete prev main zip|csv files
        File validZip = null;
        long latestTimeStamp = 0;
        for (File file : fList) {
            String name = file.getName();

            if (name.matches("(?i)^.*(?:" + timeStamp + ".zip|_\\d+\\.csv)$")) {
                file.delete();
            }

            if (name.matches("(?i)^.*_\\d{5,}\\.zip$")) {
                long cv = Long.parseLong(
                        name.replaceAll("(?i)^.*_(\\d{5,})\\.zip$", "$1"));
                if (cv > latestTimeStamp) {
                    if (validZip != null) {
                        validZip.delete();
                    }
                    latestTimeStamp = cv;
                    validZip = file;

                } else {
                    file.delete();
                }
            }
        }

        //Now create zip out stream
        if (validZip != null) {
            String fileName = validZip.getName().replaceAll("\\d+", timeStamp);
            File renameFile = new File(directoryName + fileName);
            validZip.renameTo(renameFile);
            zipOutputStream = returnFile(renameFile);
        }

        return zipOutputStream;
    }

    private ZipOutputStream returnFile(File zipFile) throws IOException {
        File tempFile = File.createTempFile(zipFile.getName(), ".zip",
                zipFile.getParentFile());
        tempFile.delete();

        boolean renameOk = zipFile.renameTo(tempFile);
        if (!renameOk) {
            throw new RuntimeException(
                    "Could not rename the file " + zipFile.getAbsolutePath() + " to " + tempFile.getAbsolutePath());
        }
        byte[] buf = new byte[1024];

        ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));

        ZipEntry entry = zin.getNextEntry();
        while (entry != null) {
            String name = entry.getName();
            // Add ZIP entry to output stream.
            out.putNextEntry(new ZipEntry(name));
            // Transfer bytes from the ZIP file to the output file
            int len;
            while ((len = zin.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            entry = zin.getNextEntry();
        }
        zin.close();
        tempFile.delete();

        return out;
    }
}
