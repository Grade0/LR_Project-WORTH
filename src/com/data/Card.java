package com.data;

/**
 * @author Davide Chen
 *
 * Card interface without movement history
 */
public interface Card {

    String getName();

    String getDescription();

    CardStatus getStatus();

}
