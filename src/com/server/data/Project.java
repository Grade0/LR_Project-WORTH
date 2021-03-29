package com.server.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.exceptions.*;
import com.utils.CommunicationProtocol;
import com.utils.MulticastAddressManager;
import com.utils.PortManager;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Davide Chen
 *
 * Data structure representing a project
 */
public class Project implements Serializable {

    private String name;
    private List<String> members;
    private LocalDateTime creationDateTime;

    //the multicast chat address and port is not persistent
    //when the project is retrieved by the server, it is assigned a new address
    @JsonIgnore
    private String chatAddress;
    @JsonIgnore
    private int chatPort;

    // the 4 status list
    private Map<CardStatus, List<String>> statusLists;

    // each card of the project is serialized in a separate file
    @JsonIgnore
    private List<CardImpl> cards;

    @JsonCreator
    public Project() {}

    public Project(String projectName, String creator) throws NoSuchAddressException, NoSuchPortException {
        this.name = projectName;
        this.members = new ArrayList<>();
        this.members.add(creator);
        this.creationDateTime = LocalDateTime.now(CommunicationProtocol.ZONE_ID);
        this.chatAddress = MulticastAddressManager.getAddress();
        this.chatPort = PortManager.getPort();
        this.statusLists = new HashMap<>();
        CardStatus[] values = CardStatus.values();
        for (CardStatus status : values) {
            this.statusLists.put(status, new ArrayList<>());
        }
        this.cards = new ArrayList<>();
    }

    /**
     * this method is called by the server to initialize
     * the multicast address of the project during the deserialization phase
     * it can't be called by anyone else
     *
     */
    public void initChatAddress(String address) throws AlreadyInitialedException {
        if (this.chatAddress != null)
            throw new AlreadyInitialedException();
        this.chatAddress = address;
    }

    /**
     * this method is called by the server to initialize
     * the multicast port of the project during the deserialization phase
     * it can't be called by anyone else
     *
     */
    public void initChatPort(int port) {
        this.chatPort = port;
    }

    /**
     * this method is called by the server to initialize
     * the list of previously loaded cards
     * it can't be called by anyone else
     *
     */
    public void initCardList(List<CardImpl> cards) throws AlreadyInitialedException {
        if (this.cards != null)
            throw new AlreadyInitialedException();
        this.cards = cards;
    }

    public String getName() {
        return this.name;
    }

    public List<String> getMembers() {
        return this.members;
    }

    public LocalDateTime getCreationDateTime() {
        return this.creationDateTime;
    }

    public String getChatAddress() {
        return this.chatAddress;
    }

    public int getChatPort() {
        return this.chatPort;
    }

    public Map<CardStatus, List<String>> getStatusLists() {
        return this.statusLists;
    }

    public CardImpl getCard(String cardName) throws CardNotExistException {
        CardImpl temp = new CardImpl(cardName, "");
        int index = this.cards.indexOf(temp);
        if (index == -1)
            throw new CardNotExistException();
        return this.cards.get(index);
    }

    @JsonIgnore
    public List<CardImpl> getAllCards() {
        return this.cards;
    }

    public List<CardImpl> getCardList(CardStatus status) {
        List<CardImpl> toReturn = new ArrayList<>();
        for (CardImpl card : this.cards) {
            if (statusLists.get(status).contains(card.getName())) {
                toReturn.add(card);
            }
        }
        return toReturn;
    }

    public void moveCard (String cardName, CardStatus from, CardStatus to)
            throws OperationNotAllowedException, CardNotExistException {
        // check if the move is allowed
        if (!this.moveIsAllowed(from, to))
            throw new OperationNotAllowedException();

        // check if the card exists
        CardImpl temp = new CardImpl(cardName, "");
        if (!this.cards.contains(temp))
            throw new CardNotExistException();

        List<String> fromList = this.statusLists.get(from);
        List<String> toList = this.statusLists.get(to);
        if (!fromList.contains(cardName))
            throw new OperationNotAllowedException();

        fromList.remove(cardName);
        toList.add(cardName);

        // updates the status and add movement to the history
        CardImpl thisCard = this.cards.get(this.cards.indexOf(temp));
        thisCard.changeStatus(to);
    }

    public void addCard(CardImpl card) throws CardAlreadyExistsException {
        if (this.cards.contains(card))
            throw new CardAlreadyExistsException();
        this.cards.add(card);
        this.statusLists.get(CardStatus.TODO).add(card.getName());
    }

    public void addMember(String user) throws UserAlreadyMemberException {
        if (this.members.contains(user))
            throw new UserAlreadyMemberException();
        this.members.add(user);
    }

    // method to check if a project is cancelable
    @JsonIgnore
    public boolean isCancelable() {
        CardStatus[] values = CardStatus.values();
        for (CardStatus status : values) {
            if (status != CardStatus.DONE)
                if (!this.statusLists.get(status).isEmpty())
                    return false;
        }
        return true;
    }

    // method to check if a move is allowed
    private boolean moveIsAllowed(CardStatus from, CardStatus to) {
        if (from == to) return false;
        // da _todo posso andare solo in inprogress
        if (from == CardStatus.TODO && (to != CardStatus.INPROGRESS))
            return false;
        // da inprogress non posso andare in _todo
        if (from == CardStatus.INPROGRESS && (to == CardStatus.TODO))
            return false;
        // da toberevised non posso andare in _todo
        if (from == CardStatus.TOBEREVISED && (to == CardStatus.TODO))
            return false;
        // da done non posso andare da nessuna parte
        if (from == CardStatus.DONE)
            return false;
        return true;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) return true;
        if (o == null || o.getClass() != this.getClass()) return false;
        return this.name.equals(((Project)o).getName());
    }

}