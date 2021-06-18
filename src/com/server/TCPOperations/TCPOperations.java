package com.server.TCPOperations;

import com.data.*;
import com.exceptions.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Davide Chen
 *
 * Interface that declares all the TCP operations that the server will have to perform
 */
public interface TCPOperations {

    /**
     * Operation that allows the user to log-in
     *
     * @param username of the user
     * @param password of the user
     *
     * @throws UserNotExistException if the user doesn't exist
     * @throws AlreadyLoggedInException if the user is already logged-in
     * @throws WrongPasswordException if the password is wrong
     */
    void login(String username, String password)
            throws UserNotExistException, AlreadyLoggedInException, WrongPasswordException;

    /**
     * Operation that allows the user to log-out
     *
     * @param username of the user
     *
     * @throws UserNotExistException if the user doesn't exist
     */
    void logout(String username) throws UserNotExistException;

    /**
     * Shows the list of projects the user is a part of
     *
     * @param username of the user
     *
     * @throws UserNotExistException if the user doesn't exist
     *
     * @return list of user's projects
     */
    List<Project> listProjects(String username) throws UserNotExistException;

    /**
     * Create a new project with the name projectName. The user who creates it becomes a member
     *
     * @param projectName name of the project
     * @param whoRequest user who request the operation
     *
     * @throws ProjectAlreadyExistException if the project with that name already exists
     * @throws NoSuchAddressException if there're no more multicast addresses available
     * @throws NoSuchPortException if there're no more ports available
     * @throws IOException if there are errors in saving the project
     */
    void createProject(String projectName, String whoRequest) throws ProjectAlreadyExistException, NoSuchAddressException, IOException, NoSuchPortException;

    /**
     * Adds a member to the project
     *
     * @param projectName name of the project
     * @param username to add to the project
     * @param whoRequest user who requested the operation
     *
     * @throws ProjectNotExistException if the project doesn't exist
     * @throws UnauthorizedUserException if the user does not have the necessary permissions
     * @throws UserAlreadyMemberException if the user is already part of the project
     * @throws UserNotExistException if the user doesn't exist
     * @throws IOException if there are errors in saving the project
     */
    void addMember(String projectName, String username, String whoRequest)
            throws ProjectNotExistException, UnauthorizedUserException, UserAlreadyMemberException, UserNotExistException, IOException;

    /**
     * Show project members
     *
     * @param projectName name of the project
     * @param whoRequest user who requested the operation
     *
     * @throws ProjectNotExistException if the project doesn't exist
     * @throws UnauthorizedUserException if the user does not have the necessary permissions
     *
     * @return list of project members
     */
    List<String> showMembers(String projectName, String whoRequest)
            throws ProjectNotExistException, UnauthorizedUserException;

    /**
     * Show the cards of the project
     *
     * @param projectName name of the project
     * @param whoRequest user who requested the operation
     *
     * @throws ProjectNotExistException if the project doesn't exist
     * @throws UnauthorizedUserException if the user does not have the necessary permissions
     *
     * @return list for all the cards of the project
     */
    Map<CardStatus, List<String>> showCards(String projectName, String whoRequest)
            throws ProjectNotExistException, UnauthorizedUserException;

    /**
     * Show a project specific card
     *
     * @param projectName name of the project
     * @param cardName name of the card
     * @param whoRequest user who requested the operation
     *
     * @throws ProjectNotExistException if the project doesn't exist
     * @throws UnauthorizedUserException if the user does not have the necessary permissions
     * @throws CardNotExistException if the card doesn't exist
     *
     * @return card of the project with the name cardName, without its movement history
     */
    Card showCard(String projectName, String cardName, String whoRequest)
            throws ProjectNotExistException, UnauthorizedUserException, CardNotExistException;

    /**
     * Add a card to the project
     *
     * @param projectName name of the project
     * @param cardName name of the card
     * @param description associated to the card
     * @param whoRequest user who requested the operation
     *
     * @throws ProjectNotExistException if the project doesn't exist
     * @throws UnauthorizedUserException if the user does not have the necessary permissions
     * @throws CardAlreadyExistsException if the card already exists in the project
     * @throws IOException if there are errors in saving the project
     *
     */
    void addCard(String projectName, String cardName, String description, String whoRequest)
            throws ProjectNotExistException, UnauthorizedUserException, CardAlreadyExistsException, IOException;

    /**
     * Move a project card from a state to an another
     *
     * @param projectName name of the project
     * @param cardName name of the card
     * @param from the origin state
     * @param to the target state
     * @param whoRequest user who requested the operation
     *
     * @throws ProjectNotExistException if the project doesn't exist
     * @throws UnauthorizedUserException if the user does not have the necessary permissions
     * @throws CardNotExistException if the card doesn't exist
     * @throws OperationNotAllowedException if the card is not in the from state or the operation violates the constraints
     * @throws IOException if there are errors in saving the project
     */
    void moveCard(String projectName, String cardName, CardStatus from, CardStatus to, String whoRequest)
            throws ProjectNotExistException, UnauthorizedUserException, CardNotExistException, OperationNotAllowedException, IOException;

    /**
     * show the movement history of a card
     *
     * @param projectName name of the project
     * @param cardName name of the card
     * @param whoRequest user who requested the operation
     *
     * @throws ProjectNotExistException if the project doesn't exist
     * @throws UnauthorizedUserException if the user does not have the necessary permissions
     * @throws CardNotExistException if the card doesn't exist
     *
     * @return lista dei movimenti della card
     */
    List<Movement> getCardHistory(String projectName, String cardName, String whoRequest)
            throws ProjectNotExistException, UnauthorizedUserException, CardNotExistException;

    /**
     * Get the multicast address of the project chat
     *
     * @param projectName name of the project
     * @param whoRequest user who requested the operation
     *
     * @throws ProjectNotExistException if the project doesn't exist
     * @throws UnauthorizedUserException if the user does not have the necessary permissions
     *
     * @return address and port of the project chat
     */
    String readChat(String projectName, String whoRequest)
            throws ProjectNotExistException, UnauthorizedUserException;

    /**
     * Delete the project
     *
     * @param projectName name of the project
     * @param whoRequest user who requested the operation
     *
     * @throws ProjectNotExistException if the project doesn't exist
     * @throws UnauthorizedUserException if the user does not have the necessary permissions
     * @throws ProjectNotCancelableException if not all cards are in the DONE state
     *
     */
    void cancelProject(String projectName, String whoRequest)
            throws ProjectNotExistException, UnauthorizedUserException, ProjectNotCancelableException;

    /**
     * Get the status of all users
     *
     * @return map of users and their status (online, offline)
     */
    Map<String, UserStatus> getUserStatus();

    /**
     * Get the multicast address of the project projectName
     *
     * @param projectName name of the project
     *
     * @throws ProjectNotExistException if the project doesn't exist
     *
     * @return multicast address of the project
     */
    String getProjectChatAddress(String projectName) throws ProjectNotExistException;

    /**
     * Get the port of the project projectName
     *
     * @param projectName name of the project
     *
     * @return port of the project
     *
     * @throws ProjectNotExistException if the project doesn't exist
     *
     */
    int getProjectChatPort(String projectName) throws ProjectNotExistException;
}
