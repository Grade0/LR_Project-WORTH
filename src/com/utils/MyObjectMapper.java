package com.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.text.SimpleDateFormat;

/**
 * @author Davide Chen
 *
 * ObjectMapper with extend functionality
 */
public class MyObjectMapper extends ObjectMapper {

    public MyObjectMapper() {
        super();

        // enable indentation
        this.enable(SerializationFeature.INDENT_OUTPUT);

        // date formatting
        this.registerModule(new JavaTimeModule());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm");
        this.setDateFormat(dateFormat);

        // don't print date as timestamp
        this.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

}
