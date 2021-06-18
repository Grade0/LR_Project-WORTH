package com.server.TCPOperations;

import java.nio.ByteBuffer;

/**
 * @author Davide Chen
 *
 * Attachment of a Selector's key
 */
public class Attachment {
    private String username;    // username of the online user
    private ByteBuffer buffer;  // buffer associated with the key

    public Attachment() {
        username = null;
        buffer = null;
    }

    public String getUsername() {
        return this.username;
    }

    public ByteBuffer getBuffer() {
        return this.buffer;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }
}