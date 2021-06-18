package com.data;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Davide Chen
 */

public class CardImpl implements Serializable, Card {
    private String name;
    private String description;
    private CardStatus status;
    private List<Movement> movements;

    public CardImpl(String name, String desc) {
        this.name = name;
        this.description = desc;
        this.status = CardStatus.TODO;
        this.movements = new ArrayList<>();
    }

    @JsonCreator
    private CardImpl() {}


    public String getName() {
        return this.name;
    }


    public String getDescription() {
        return this.description;
    }


    public CardStatus getStatus() {
        return this.status;
    }

    public List<Movement> getMovements() {
        return this.movements;
    }

    public void changeStatus(CardStatus newStatus) {
        Movement mov = new Movement(this.status, newStatus);
        this.status = newStatus;
        this.movements.add(mov);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || o.getClass() != this.getClass()) return false;
        return this.name.equals(((CardImpl)o).getName());
    }
}
