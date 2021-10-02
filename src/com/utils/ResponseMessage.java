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
    private String responseBody2;   // second message body used only for LOGIN_CMD

    public ResponseMessage(int statusCode, String responseBody, String responseBody2) {
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.responseBody2 = responseBody2;
    }

    @JsonCreator
    private ResponseMessage() {}

    public int getStatusCode() {
        return this.statusCode;
    }

    public String getResponseBody() {
        return this.responseBody;
    }

    public String getResponseBody2() {
        return responseBody2;
    }
}
