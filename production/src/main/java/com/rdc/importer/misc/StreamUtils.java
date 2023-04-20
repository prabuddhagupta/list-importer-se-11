package com.rdc.importer.misc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import org.apache.commons.io.IOUtils;

public class StreamUtils {

    public static void closeQuietly(java.io.Writer writer) {
        try {
            writer.flush();
        } catch (Exception ignore) {
            // Ignore
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    public static void closeQuietly(OutputStream outputStream) {
        try {
            outputStream.flush();
        } catch (Exception ignore) {
            // Ignore
        } finally {
            IOUtils.closeQuietly(outputStream);
        }

    }

    public static void closeQuietly(Reader input) {
        if (input != null) {
            try {
                input.close();
            } catch (Exception e) {
                //ignore
            }
        }
    }

    public static void closeQuietly(InputStream input) {
        if (input != null) {
            try {
                input.close();
            } catch (Exception e) {
                //ignore
            }
        }
    }

    public static void copy(InputStream input, OutputStream output) throws IOException {
        IOUtils.copy(input, output);
    }

    public static void copy(Reader input, Writer output) throws IOException {
        IOUtils.copy(input, output);
    }
}
