package com.utils;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.Serializable;

/**
 * @author Davide Chen
 *
 * Identifies a response message that the server sends to the client
 */
public class ResponseMessage implements Serializable {
    private int statusCode;         // status code resulting from the operation
    private String responseBody;    // message body (can be null)

    public ResponseMessage(int statusCode, String responseBody) {
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    @JsonCreator
    private ResponseMessage() {}

    public int getStatusCode() {
        return this.statusCode;
    }

    public String getResponseBody() {
        return this.responseBody;
    }
}
