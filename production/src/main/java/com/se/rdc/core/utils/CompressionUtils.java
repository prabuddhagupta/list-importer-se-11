package com.se.rdc.core.utils;

import java.io.*;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class CompressionUtils {

    public static byte[] compress(Object object) throws IOException {
        return compress(object, Deflater.DEFAULT_COMPRESSION);
    }

    public static byte[] compress(Object object, int compressionLevel) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream objectWriter = new ObjectOutputStream(bos);
        objectWriter.writeObject(object);
        objectWriter.close();

        return compress(bos.toByteArray(), compressionLevel);
    }

    public static byte[] compress(byte[] data) throws IOException {
        return compress(data, Deflater.DEFAULT_COMPRESSION);
    }

    public static byte[] compress(byte[] data, int compressionLevel) throws IOException {
        Deflater deflater = new Deflater(compressionLevel);
        deflater.setInput(data);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        deflater.finish();

        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer); // returns the generated code... index
            outputStream.write(buffer, 0, count);
        }

        outputStream.close();
        byte[] output = outputStream.toByteArray();

        return output;
    }

    public static byte[] decompress(byte[] data) throws IOException, DataFormatException {
        Inflater inflater = new Inflater();
        inflater.setInput(data);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);

        byte[] buffer = new byte[1024];
        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        byte[] output = outputStream.toByteArray();

        return output;
    }

    public static Object decompressToObject(byte[] data) throws IOException, ClassNotFoundException, DataFormatException {
        data = decompress(data);

        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInput in = new ObjectInputStream(bis);
        Object o = in.readObject();
        in.close();

        return o;
    }

}