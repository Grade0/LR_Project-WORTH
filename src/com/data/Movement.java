package com.data;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @author Davide Chen
 *
 * Represents a movement of a card
 */
public class Movement implements Serializable {
    private CardStatus from;        // original state
    private CardStatus to;          // state of arrival
    private LocalDateTime when;     // when it's happened

    @JsonCreator
    private Movement() {}

    public Movement(CardStatus from, CardStatus to) {
        this.from = from;
        this.to = to;

        //Get current date time
        this.when = LocalDateTime.now(ZoneId.systemDefault());
    }

    public CardStatus getFrom() {
        return from;
    }

    public CardStatus getTo() {
        return to;
    }

    public String getWhen() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return this.when.format(formatter);
    }

}
