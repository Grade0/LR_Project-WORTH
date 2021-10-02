package com;

import java.time.ZoneId;

/**
 * @author Davide Chen
 *
 * Set of all client-server communication details of the service
 */
public abstract class CommunicationProtocol {

    /**
     * SERVICE STANDARD PROTOCOLS
     */
    // Server IP for TCP requests
    public static final String SERVER_IP_ADDRESS = "localhost";
    // Server port for TCP requests
    public static final int SERVER_PORT = 2500;
    public static final int REGISTRY_PORT = 6789;
    public static final int MULTICAST_GROUP_PORT = 30000;
    // is the size of the buffer
    // thus I don't have to worry if the message
    // is longer than the buffer
    public static final int UDP_MSG_MAX_LEN = 2048;
    // system name (mainly used as the author of UDP messages)
    public final static String SYSTEM_NAME = "System";
    // message to be sent to the server to close the chat service
    public final static String UDP_TERMINATE_MSG = "project canceled";
    // Name of the registration service offered by RMI
    public static final String REGISTRATION_SERVICE_NAME =
            "rmi://" + SERVER_IP_ADDRESS +":" + REGISTRY_PORT + "/RegistrationService";
    // Name of the callback service offered by RMI
    public static final String CALLBACK_SERVICE_NAME =
            "rmi://" + SERVER_IP_ADDRESS +":" + REGISTRY_PORT + "/CallbackService";
    public static final int MIN_PASSWORD_LEN = 8;
    // REGEX username, only a-z, 0-9, _ allowed
    public static final String STRING_REGEX = "^[a-z0-9_]+$";
    public static final ZoneId ZONE_ID = ZoneId.of("Europe/Rome");

    /**
     * RESPONSE CODE
     */
    public static final int OP_SUCCESS = 200;
    public static final int UNKNOWN = 400; //generic error
    public static final int COMMUNICATION_ERROR = 401;
    public static final int USER_NOT_EXISTS = 402;
    public static final int PROJECT_NOT_EXISTS = 403;
    public static final int CARD_NOT_EXISTS = 404;
    public static final int UNAUTHORIZED = 405;
    public static final int CHARS_NOT_ALLOWED = 406;
    public static final int LOGIN_WRONGPWD = 407;
    public static final int LOGIN_ALREADY_LOGGED = 408;
    public static final int CREATEPROJECT_ALREADYEXISTS = 409;
    public static final int CREATEPROJECT_NOMOREADDRESSES = 410;
    public static final int CREATEPROJECT_NOMOREPORTS = 411;
    public static final int ADD_CARD_ALREADYEXISTS = 412;
    public static final int ADD_MEMBER_ALREADYPRESENT = 413;
    public static final int MOVE_CARD_NOT_ALLOWED = 414;
    public static final int CANCELPROJECT_NOTCANCELABLE = 415;
    public static final int USER_NOT_LOGGED = 416;

    /**
     * COMMANDS
     */
    public static final String REGISTER_CMD = "register";
    public static final String LOGIN_CMD = "login";
    public static final String LOGOUT_CMD = "logout";
    public static final String LIST_USERS_CMD = "list_users";
    public static final String LIST_ONLINE_USERS_CMD = "list_online_users";
    public static final String LIST_PROJECTS_CMD = "list_projects";
    public static final String CREATE_PROJECT_CMD = "create_project";
    public static final String ADD_MEMBER_CMD = "add_member";
    public static final String SHOW_MEMBERS_CMD = "show_members";
    public static final String SHOW_CARDS_CMD = "show_cards";
    public static final String SHOW_CARD_CMD = "show_card";
    public static final String ADD_CARD_CMD = "add_card";
    public static final String MOVE_CARD_CMD = "move_card";
    public static final String CARD_HISTORY_CMD = "get_card_history";
    public static final String READ_CHAT_CMD = "read_chat";
    public static final String SEND_CHAT_CMD = "send_chat";
    public static final String CANCEL_PROJECT_CMD = "cancel_project";
    public static final String EXIT_CMD = "exit";

    /**
     * TEXT COLOR
     */
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";        // for error messages
    public static final String ANSI_GREEN = "\u001B[32m";      // for the success messages
    public static final String ANSI_YELLOW = "\u001B[33m";     // for server response and system messages
    public static final String ANSI_BLUE = "\u001B[34m";       // for my messages in the chat
}
