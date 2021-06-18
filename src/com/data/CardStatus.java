package com.data;

import java.io.Serializable;

/**
 * @author Davide Chen
 *
 * Represents the possible status of a card
 */
public enum CardStatus implements Serializable {
    TODO,
    INPROGRESS,
    TOBEREVISED,
    DONE;

    /**
     * Get CardStatus from a string
     *
     * @param stringStatus string indicating the status
     *
     * @return Cardstatus referring to the string, null if there is none
     */
    public static CardStatus retriveFromString(String stringStatus) {
        if(stringStatus != null) {
            for (CardStatus cardStatus : CardStatus.values()) {
                if (cardStatus.name().equalsIgnoreCase(stringStatus))
                    return cardStatus;
            }
        }
        return null;
    }
}
