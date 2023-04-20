package com.rdc.importer.scrapian.service;

/**
 * Created with IntelliJ IDEA.
 * User: jwarren
 * Date: 9/9/13
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class FTPCopyFailedException extends Exception {
    private int replyCode;

    public FTPCopyFailedException(String message, int replyCode) {
        super(message);
        this.replyCode = replyCode;
    }

    public int getReplyCode() {
        return replyCode;
    }
}
