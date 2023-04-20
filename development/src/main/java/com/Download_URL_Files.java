package com;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class Download_URL_Files {

    public static void main(String[] args) {
        String url = "http://kfi.ky.gov/Documents/Crossroad%20Community%20Church%20Inc%20Brian%20Tome%202020-AH-0026.pdf";

        try {
            //downloadUsingNIO(url, "/Users/pankaj/sitemap.xml");

            downloadUsingStream(url, "/home/mahadi/Downloads/pdfDownload/Kentucky.pdf");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void downloadUsingStream(String urlStr, String file) throws IOException{
        URL url = new URL(urlStr);
        BufferedInputStream bis = new BufferedInputStream(url.openStream());
        FileOutputStream fis = new FileOutputStream(file);
        byte[] buffer = new byte[1024];
        int count=0;
        while((count = bis.read(buffer,0,1024)) != -1)
        {
            fis.write(buffer, 0, count);
        }
        fis.close();
        bis.close();
    }

    private static void downloadUsingNIO(String urlStr, String file) throws IOException {
        URL url = new URL(urlStr);
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(file);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
        rbc.close();
    }

}
/*

import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class Download_URL_Files {
    public static void main(String[] args) {
        URLConnection uc;
        // Make sure that this directory exists
        String dirName = "/home/mahadi/Downloads";

        try {
            URL url = new URL("https://www.oid.ok.gov/wp-content/uploads/2020/02/NP-Order-20-01.pdf");
            uc = url.openConnection();
            uc.addRequestProperty("User-Agent",
                "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
            saveFileFromUrlWithCommonsIO(dirName + "NP-Order-20-01.pdf", url);
            System.out.println("finished");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Using Java IO
    public static void saveFileFromUrlWithJavaIO(String fileName, String fileUrl)
        throws MalformedURLException, IOException {
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        try {
            in = new BufferedInputStream(new URL(fileUrl).openStream());
            fout = new FileOutputStream(fileName);
            byte data[] = new byte[1024];
            int count;
            while ((count = in.read(data, 0, 1024)) != -1) {
                fout.write(data, 0, count);
            }
        } finally {
            if (in != null)
                in.close();
            if (fout != null)
                fout.close();
        }
    }

    // Using Commons IO library
    // Available at http://commons.apache.org/io/download_io.cgi
    public static void saveFileFromUrlWithCommonsIO(String fileName, URL fileUrl) throws MalformedURLException, IOException {
        FileUtils.copyURLToFile(fileUrl, new File(fileName));
    }
}
*/

