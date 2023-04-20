package com.rdc.importer.scrapian.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.rdc.scrape.CachedEntities;

public class ScrapianCacheUtil {

     public static void deleteCachedEntities(String cachedEntityDirectory,String sourceKey){
        String cachedEntityFileStr=cachedEntityDirectory+sourceKey+".obj";
        File cachedFile = new File(cachedEntityFileStr);
        cachedFile.delete();
    }
     
     /*Eliminates history but keeps entities but clears the list*/
     public static void resetCachedEntities(String cachedEntityDirectory,String sourceKey) throws IOException, ClassNotFoundException{
         String cachedEntityFileStr=cachedEntityDirectory+sourceKey+".obj";
         ObjectInputStream ois=new ObjectInputStream(new FileInputStream(cachedEntityFileStr));
         CachedEntities cachedEntities=(CachedEntities) ois.readObject();
         ois.close();
         cachedEntities.getHistoryList().clear();
         ObjectOutputStream oss=new ObjectOutputStream(new FileOutputStream(cachedEntityFileStr));
         oss.writeObject(cachedEntities);
         oss.close();

     }
}
