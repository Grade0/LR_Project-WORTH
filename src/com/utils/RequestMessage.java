package com.utils;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * @author Davide Chen
 *
 * Identifies a request message that a client sends to the server
 */
public class RequestMessage implements Serializable {
    private String command;
    private List<String> arguments;

    public RequestMessage(String command, String... arguments) {
        this.command = command;
        this.arguments = Arrays.asList(arguments);
    }

    @JsonCreator
    private RequestMessage() {}

    public String getCommand() {
        return this.command;
    }

    public List<String> getArguments() {
        return this.arguments;
    }
}
