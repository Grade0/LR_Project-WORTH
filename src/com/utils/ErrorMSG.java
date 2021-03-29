package com.utils;

/**
 * @author Davide Chen
 *
 * System error messages
 */
public class ErrorMSG {

    public static final String GENERIC_ERROR = "Sorry, we encountered an error.\n" +
            "If the problem persists, please close and restart the application";

    public static final String CONNECTION_ERROR = "Sorry, we encountered some connection problems.\n" +
            "If the problem persists, please close and restart the application";

    public static final String CONNECTION_REFUSED = "Sorry, but the connection seems to be refused by the server";

    public static final String REGISTRY_NOT_BOUND = "Sorry, the lookup registry seems has no associated binding";

    public static final String PASSWORD_TOO_SHORT =
            "The entered password is too short.\n" +
                    "Insert a password with at least 8 characters";

    public static final String PASSWORD_WRONG = "Entered password is wrong";

    public static final String CHARACTERS_NOT_ALLOWED =
            "We're sorry, but this field contains some not allowed characters.\n" +
                    "It should contain only lower alphanumeric characters and _";

    public static final String EMPTY_FIELD = "Some fields seems to be empty or blank.\n";

    public static String USERNAME_NOT_AVAILABLE = "We're sorry, but the username you've chosen is not available.\n" +
            "Please try with another one";

    public static final String USERNAME_NOT_EXISTS = "This username does not exists";

    public static final String USER_ALREADY_LOGGED = "Sorry, but seems that you're already logged-in" +
            " from another terminal.";

    public static final String CARD_NOT_EXISTS = "This card not exists.";

    public static final String OPERATION_NOT_ALLOWED = "Sorry, but seems that you're trying" +
            " to do an illegal operation.";

    public static final String CARD_ALREADY_EXISTS = "This card can't be added because " +
            "it already exists in the project.";

    public static final String NO_SUCH_ADDRESS = "Sorry, but the project can't be created because" +
            " we passed the maximum number of chat addresses.";

    public static final String NO_SUCH_PORT = "Sorry, but the project can't be created because" +
            " we passed the maximum number of ports.";

    public static final String PROJECT_ALREADY_EXISTS = "The project can't be created because" +
            " already exists another project with this name.";

    public static final String PROJECT_NOT_CANCELABLE = "The project can't be cancelled because" +
            " there are some tasks to do yet.";

    public static final String PROJECT_NOT_EXISTS = "You're trying to access" +
            " to a not existing project. Maybe it has been canceled while" +
            " you were trying to modify it.";

    public static final String UNAUTHORIZED_USER = "It seems that you are not authorized to access this data.";

    public static final String USER_ALREADY_MEMBER = "The user is already part in this project.";

    public static final String UNOBTAINABLE_ADDRESS = "Sorry, but we can't send this message.\n" +
            "Seems like there's no saved chat address.\n" +
            "Try again, if the problem persists, restart the application";

    public static final String DATAGRAM_TOO_BIG = "The message you're trying to send is too big.";

    public static final String NOT_LOGGED = "Sorry, but seems that you're not logged yet";
}
