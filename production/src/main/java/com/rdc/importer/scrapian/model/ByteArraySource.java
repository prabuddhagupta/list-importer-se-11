package com.rdc.importer.scrapian.model;

import org.apache.commons.codec.binary.Base64;

public class ByteArraySource implements ScrapianSource {
    private byte[] value;

    public ByteArraySource(byte[] value) {
        this.value = value;
    }

    public byte[] getValue() {
        return value;
    }

    public String serialize() throws Exception {
        return new String(Base64.encodeBase64(value));
    }
}
