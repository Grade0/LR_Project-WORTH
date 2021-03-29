package com.utils;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @author Davide Chen
 *
 * Structure of a UDP message sent in the chat
 */
public class UDPMessage {
    private String author;          // name of the sender of the message
    private String message;         // message to send
    private boolean fromSystem;     // flag that checks if it's a system message

    public UDPMessage(String author, String message, boolean isFromSystem) {
        this.author = author;
        this.message = message;
        this.fromSystem = isFromSystem;
    }

    @JsonCreator
    private UDPMessage() {}

    public String getAuthor() {
        return author;
    }

    public String getMessage() {
        return message;
    }

    public boolean isFromSystem() {
        return fromSystem;
    }
}
