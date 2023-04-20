package com.rdc.importer.misc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Exception that has ability to hold multiple Validation Exceptions.
 */
public class ValidationException extends Exception implements Serializable {

    private List<String> messages = new ArrayList<String>();

    public ValidationException(String message) {
        messages.add(message);
    }

    public ValidationException() {
    }

    public void addValidationMessage(String message) {
        messages.add(message);
    }

    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        for (String message : messages) {
            sb.append(message).append("\n");
        }
        return sb.toString();
    }

    public List<String> getMessages() {
        return messages;
    }
}
