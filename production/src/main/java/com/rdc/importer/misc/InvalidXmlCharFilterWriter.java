package com.rdc.importer.misc;

import java.io.Writer;
import java.io.IOException;

/**
 * Used to filter out invalid xml characters.
 */
public class InvalidXmlCharFilterWriter extends Writer {

    private static final char REPLACEMENT_CHARACTER = 0x3f;
    private Writer writer;

    public InvalidXmlCharFilterWriter(Writer writer) {
        this.writer = writer;
    }

    public void write(int i) throws IOException {
        writer.write(handleChar(i));
    }

    public void write(char[] chars) throws IOException {
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) handleChar(chars[i]);
        }
        writer.write(chars);
    }

    public void write(char[] chars, int offset, int length) throws IOException {
        for (int i = offset; i < length + offset; i++) {
            chars[i] = (char) handleChar(chars[i]);
        }
        writer.write(chars, offset, length);
    }

    public void write(String s) throws IOException {
        write(s.toCharArray());
    }

    public void write(String s, int offset, int length) throws IOException {
        write(s.toCharArray(), offset, length);
    }

    public void flush() throws IOException {
        writer.flush();
    }

    public void close() throws IOException {
        writer.close();
    }

    private int handleChar(int charIn) {
        char character = (char) charIn;
        if (character >= 0x20 || character == 0x9 || character == 0xA || character == 0xD || character < 0x0) {
            return character;
        } else {
            return REPLACEMENT_CHARACTER;
        }
    }
}
