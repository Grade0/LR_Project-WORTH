package com.client;

import com.CommunicationProtocol;
import com.exceptions.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.data.*;
import com.server.RMIOperations.RMICallbackService;
import com.server.RMIOperations.RMIRegistrationService;
import com.utils.*;

import javax.naming.CommunicationException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author Davide Chen
 *
 * All operations provided by the service
 */
public class ClientService {
    private static final int ALLOCATION_SIZE = 2048;     // buffer allocation space
    private boolean isLogged;                               // user online status flag
    private String username;                                // to keep track of the user's username
    private final SocketChannel socket;                     // socket for connection establishment
    private final ObjectMapper mapper;                      // mapper for serialization / deserialization
    private Map<String, UserStatus> userStatus;             // users status list
    private RMICallbackNotify callbackNotify;               // callback management
    private ProjectChatTask projectChats;                   // projects chats manager

    // set up the client's connection to the server
    public ClientService() throws IOException {
        // opens TCP connection with server
        this.socket = SocketChannel.open();
        InetSocketAddress address = new InetSocketAddress(
                CommunicationProtocol.SERVER_IP_ADDRESS,
                CommunicationProtocol.SERVER_PORT
        );
        this.socket.connect(address); // blocking-mode for the client

        this.mapper = new MyObjectMapper();

        // the following data will be initialized/updated during the login
        this.userStatus = null;
        this.callbackNotify = null;
        this.projectChats = null;
        this.isLogged = false;
        this.username = "";
    }

    public void closeConnection() throws IOException {
        try {
            if (this.isLogged) {
                // terminate the chat task
                this.projectChats.terminate();

                this.unregisterForCallback();
            }
            // preparing message to send
            RequestMessage requestMessage = new RequestMessage(CommunicationProtocol.EXIT_CMD);
            // could throw CommunicationException due to server down
            this.sendTCPRequest(requestMessage);
        } catch (Exception e) {
            // ignore
        } finally {
            // unexport remote object
            // and close communication socket
            UnicastRemoteObject.unexportObject(callbackNotify, true);
            this.socket.close();
        }
    }

    /**
     * Registration via RMI
     */
    public void register(String username, String password)
            throws RemoteException, NotBoundException, CharactersNotAllowedException,
            UsernameNotAvailableException, PasswordTooShortException {
        // creates RMI connection for the registration service
        Registry registry = LocateRegistry.getRegistry(CommunicationProtocol.REGISTRY_PORT);
        RMIRegistrationService regService =
                (RMIRegistrationService) registry.lookup(CommunicationProtocol.REGISTRATION_SERVICE_NAME);
        // call to the RMI service
        String result = regService.register(username, password);

        System.out.println(CommunicationProtocol.ANSI_GREEN + result);
    }

    /**
     * TCP operations
     */
    public void login(String username, String password)
            throws UserNotExistException, AlreadyLoggedInException, WrongPasswordException, CommunicationException, IOException, UserNotLoggedException {
        if(!isLogged) {
            // preparing message to send
            RequestMessage requestMessage = new RequestMessage(
                    CommunicationProtocol.LOGIN_CMD,
                    username,
                    password
            );

            ResponseMessage response = this.sendTCPRequest(requestMessage);

            int responseCode = response.getStatusCode();
            // error cases
            if (responseCode == CommunicationProtocol.USER_NOT_EXISTS) throw new UserNotExistException();
            if (responseCode == CommunicationProtocol.LOGIN_WRONGPWD) throw new WrongPasswordException();
            if (responseCode == CommunicationProtocol.COMMUNICATION_ERROR) throw new CommunicationException();
            if (responseCode == CommunicationProtocol.LOGIN_ALREADY_LOGGED) throw new AlreadyLoggedInException();

            // everything went through
            // I'm online
            this.username = username;
            this.isLogged = true;

            System.out.println(CommunicationProtocol.ANSI_GREEN + SuccessMSG.LOGIN_SUCCESSFUL + "\n");

            try {
                // saving server reply
                this.userStatus = this.mapper.readValue(
                        response.getResponseBody(),
                        new TypeReference<Map<String, UserStatus>>() {
                        }
                );

                Map<String, InetAddress> chatAddresses = this.mapper.readValue(
                        response.getResponseBody2(),
                        new TypeReference<Hashtable<String, InetAddress>>() {

                        }
                );

                // they must remain synchronized
                this.userStatus = Collections.synchronizedMap(this.userStatus);
                chatAddresses = Collections.synchronizedMap(chatAddresses);

                // initiate the projectChat thread
                this.projectChats = new ProjectChatTask(chatAddresses);
                new Thread(projectChats).start();

                // instantiate callback
                this.callbackNotify = new RMICallbackNotifyImpl(this.userStatus, this.projectChats);

                // callback registration request
                this.registerForCallback();

            } catch (IOException | NotBoundException e) {
                e.printStackTrace();
            }

            // retrieve users list
            this.listUsers();

        } else {
            System.out.println(CommunicationProtocol.ANSI_RED + ErrorMSG.SOMEONE_ALREADY_LOGGED + this.username);
        }
    }


    public void logout(String username)
            throws UserNotExistException, CommunicationException {
        if(isLogged) {
            if(this.username.equals(username)) {
                // preparing message to send
                RequestMessage requestMessage = new RequestMessage(CommunicationProtocol.LOGOUT_CMD);

                ResponseMessage response = this.sendTCPRequest(requestMessage);

                // error cases
                if (response.getStatusCode() == CommunicationProtocol.USER_NOT_EXISTS) {
                    throw new UserNotExistException();
                }

                // request for de-registration to callback service
                // and unexport remote object
                try {
                    this.unregisterForCallback();
                    UnicastRemoteObject.unexportObject(callbackNotify, true);
                } catch (RemoteException | NotBoundException e) {
                    e.printStackTrace();
                }

                // I'm offline
                this.username = "";
                this.isLogged = false;

                // terminate the chat task
                this.projectChats.terminate();

                System.out.println(CommunicationProtocol.ANSI_GREEN + SuccessMSG.LOGOUT_SUCCESSFUL);
            } else {
                System.out.println(CommunicationProtocol.ANSI_RED + ErrorMSG.NOT_LOGGED_AS + username);
            }
        } else {
            System.out.println(CommunicationProtocol.ANSI_RED + ErrorMSG.NOT_LOGGED);
        }
    }

    public void listUsers() {
        if(isLogged) {
            System.out.println(CommunicationProtocol.ANSI_YELLOW + "User list with their status:");

            for (Map.Entry<String, UserStatus> entry : userStatus.entrySet()) {
                System.out.println("- " + entry.getKey() + ": " + entry.getValue());
            }
        } else {
            System.out.println(CommunicationProtocol.ANSI_RED + ErrorMSG.NOT_LOGGED);
        }
    }

    public void listOnlineUsers() {
        if(isLogged) {
            System.out.println(CommunicationProtocol.ANSI_RESET + "Online user list:");

            for (Map.Entry<String, UserStatus> entry : userStatus.entrySet()) {
                if (entry.getValue() == UserStatus.ONLINE)
                    System.out.println("- " + entry.getKey() + ": " + entry.getValue());
            }
        } else {
            System.out.println(CommunicationProtocol.ANSI_RED + ErrorMSG.NOT_LOGGED);
        }
    }

    public void listProjects()
            throws CommunicationException, UserNotExistException, UserNotLoggedException {
        if(isLogged) {
            // preparing message to send
            RequestMessage requestMessage = new RequestMessage(CommunicationProtocol.LIST_PROJECTS_CMD);

            ResponseMessage response = this.sendTCPRequest(requestMessage);

            if (response.getStatusCode() == CommunicationProtocol.USER_NOT_EXISTS) throw new UserNotExistException();


            try {
                List<Project> projectList = this.mapper.readValue(
                        response.getResponseBody(),
                        new TypeReference<List<Project>>() {
                        }
                );

                System.out.println(CommunicationProtocol.ANSI_YELLOW + "\nMy projects list:");
                if (projectList.size() == 0) System.out.println("\t empty");
                else {
                    for (Project project : projectList) {
                        System.out.println("- " + project.getName());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new CommunicationException();
            }
        } else {
            System.out.println(CommunicationProtocol.ANSI_RED + ErrorMSG.NOT_LOGGED);
        }
    }

    public void createProject(String projectName)
            throws ProjectAlreadyExistException, NoSuchAddressException, CharactersNotAllowedException, CommunicationException, NoSuchPortException, UnauthorizedUserException, ProjectNotExistException, IOException, UserNotLoggedException {
        if(isLogged) {
            // preparing message to send
            RequestMessage requestMessage = new RequestMessage(
                    CommunicationProtocol.CREATE_PROJECT_CMD,
                    projectName
            );

            ResponseMessage response = this.sendTCPRequest(requestMessage);

            int responseCode = response.getStatusCode();
            // error cases
            if (responseCode == CommunicationProtocol.CREATEPROJECT_ALREADYEXISTS) throw new ProjectAlreadyExistException();
            if (responseCode == CommunicationProtocol.CREATEPROJECT_NOMOREADDRESSES) throw new NoSuchAddressException();
            if (responseCode == CommunicationProtocol.CREATEPROJECT_NOMOREPORTS) throw new NoSuchPortException();
            if (responseCode == CommunicationProtocol.CHARS_NOT_ALLOWED) throw new CharactersNotAllowedException();
            if (responseCode == CommunicationProtocol.COMMUNICATION_ERROR) throw new CommunicationException();

            String chatAddress = this.mapper.readValue(
                    response.getResponseBody(),
                    new TypeReference<String>() {

                    }
            );

            projectChats.joinGroup(projectName, InetAddress.getByName(chatAddress));

            System.out.println(CommunicationProtocol.ANSI_GREEN + SuccessMSG.PROJECT_CREATE_SUCCESS);
        } else {
            System.out.println(CommunicationProtocol.ANSI_RED + ErrorMSG.NOT_LOGGED);
        }
    }

    public void addMember(String projectName, String username)
            throws CommunicationException, ProjectNotExistException, UnauthorizedUserException, UserAlreadyMemberException, UserNotExistException, IOException {
        if(isLogged) {
            // preparing message to send
            RequestMessage requestMessage = new RequestMessage(
                    CommunicationProtocol.ADD_MEMBER_CMD,
                    projectName,
                    username
            );

            ResponseMessage response = this.sendTCPRequest(requestMessage);

            int responseCode = response.getStatusCode();
            // error cases
            if (responseCode == CommunicationProtocol.PROJECT_NOT_EXISTS) throw new ProjectNotExistException();
            if (responseCode == CommunicationProtocol.UNAUTHORIZED) throw new UnauthorizedUserException();
            if (responseCode == CommunicationProtocol.ADD_MEMBER_ALREADYPRESENT) throw new UserAlreadyMemberException();
            if (responseCode == CommunicationProtocol.USER_NOT_EXISTS) throw new UserNotExistException();
            if (responseCode == CommunicationProtocol.COMMUNICATION_ERROR) throw new CommunicationException();

            System.out.println(CommunicationProtocol.ANSI_GREEN + SuccessMSG.ADD_MEMBER_SUCCESS);
        } else {
            System.out.println(CommunicationProtocol.ANSI_RED + ErrorMSG.NOT_LOGGED);
        }
    }

    public void showMembers(String projectName)
            throws CommunicationException, ProjectNotExistException, UnauthorizedUserException {
        if(isLogged) {
            // preparing message to send
            RequestMessage requestMessage = new RequestMessage(
                    CommunicationProtocol.SHOW_MEMBERS_CMD,
                    projectName
            );

            ResponseMessage response = this.sendTCPRequest(requestMessage);

            int responseCode = response.getStatusCode();
            // error cases
            if (responseCode == CommunicationProtocol.PROJECT_NOT_EXISTS) throw new ProjectNotExistException();
            if (responseCode == CommunicationProtocol.UNAUTHORIZED) throw new UnauthorizedUserException();
            if (responseCode == CommunicationProtocol.COMMUNICATION_ERROR) throw new CommunicationException();

            try {
                List<String> members = this.mapper.readValue(
                        response.getResponseBody(),
                        new TypeReference<List<String>>() {
                        }
                );

                System.out.println(CommunicationProtocol.ANSI_YELLOW + "Project \"" + projectName + "\" member-list: ");
                for (String member : members) {
                    System.out.println("- " + member);
                }

            } catch (IOException e) {
                e.printStackTrace();
                throw new CommunicationException();
            }
        } else {
            System.out.println(CommunicationProtocol.ANSI_RED + ErrorMSG.NOT_LOGGED);
        }
    }

    public void showCards(String projectName)
            throws CommunicationException, ProjectNotExistException, UnauthorizedUserException {
        if(isLogged) {
            // preparing messagge to send
            RequestMessage requestMessage = new RequestMessage(
                    CommunicationProtocol.SHOW_CARDS_CMD,
                    projectName
            );

            ResponseMessage response = this.sendTCPRequest(requestMessage);

            int responseCode = response.getStatusCode();
            // error cases
            if (responseCode == CommunicationProtocol.PROJECT_NOT_EXISTS) throw new ProjectNotExistException();
            if (responseCode == CommunicationProtocol.UNAUTHORIZED) throw new UnauthorizedUserException();
            if (responseCode == CommunicationProtocol.COMMUNICATION_ERROR) throw new CommunicationException();

            try {
                Map<CardStatus, List<String>> cards = this.mapper.readValue(
                        response.getResponseBody(),
                        new TypeReference<Map<CardStatus, List<String>>>() {
                        }
                );

                System.out.println(CommunicationProtocol.ANSI_YELLOW + "TODO:");
                for (String cardName : cards.get(CardStatus.TODO)) {
                    System.out.println("\t" + cardName);
                }
                System.out.println("INPROGRESS:");
                for (String cardName : cards.get(CardStatus.INPROGRESS)) {
                    System.out.println("\t" + cardName);
                }
                System.out.println("TOBEREVISED:");
                for (String cardName : cards.get(CardStatus.TOBEREVISED)) {
                    System.out.println("\t" + cardName);
                }
                System.out.println("DONE:");
                for (String cardName : cards.get(CardStatus.DONE)) {
                    System.out.println("\t" + cardName);
                }

            } catch (IOException e) {
                e.printStackTrace();
                throw new CommunicationException();
            }
        } else {
            System.out.println(CommunicationProtocol.ANSI_RED + ErrorMSG.NOT_LOGGED);
        }
    }

    public void showCard(String projectName, String cardName)
            throws CommunicationException, ProjectNotExistException, CardNotExistException, UnauthorizedUserException {
        if(isLogged) {
            // preparing message to send
            RequestMessage requestMessage = new RequestMessage(
                    CommunicationProtocol.SHOW_CARD_CMD,
                    projectName,
                    cardName
            );

            ResponseMessage response = this.sendTCPRequest(requestMessage);

            int responseCode = response.getStatusCode();
            // error cases
            if (responseCode == CommunicationProtocol.PROJECT_NOT_EXISTS) throw new ProjectNotExistException();
            if (responseCode == CommunicationProtocol.CARD_NOT_EXISTS) throw new CardNotExistException();
            if (responseCode == CommunicationProtocol.UNAUTHORIZED) throw new UnauthorizedUserException();
            if (responseCode == CommunicationProtocol.COMMUNICATION_ERROR) throw new CommunicationException();

            try {
                Card card = this.mapper.readValue(
                        response.getResponseBody(),
                        new TypeReference<CardImpl>() {
                        }
                );

                System.out.println(CommunicationProtocol.ANSI_YELLOW + "Card name: " + card.getName());
                System.out.println("Card description: " + card.getDescription());
                System.out.println("Card status: " + card.getStatus());

            } catch (IOException e) {
                e.printStackTrace();
                throw new CommunicationException();
            }
        } else {
            System.out.println(CommunicationProtocol.ANSI_RED + ErrorMSG.NOT_LOGGED);
        }
    }

    public void addCard(String projectName, String cardName, String description)
            throws CommunicationException, ProjectNotExistException, UnauthorizedUserException, CardAlreadyExistsException, CharactersNotAllowedException {
        if(isLogged) {
            // preparing message to send
            RequestMessage requestMessage = new RequestMessage(
                    CommunicationProtocol.ADD_CARD_CMD,
                    projectName,
                    cardName,
                    description
            );

            ResponseMessage response = this.sendTCPRequest(requestMessage);

            int responseCode = response.getStatusCode();
            // error cases
            if (responseCode == CommunicationProtocol.PROJECT_NOT_EXISTS) throw new ProjectNotExistException();
            if (responseCode == CommunicationProtocol.UNAUTHORIZED) throw new UnauthorizedUserException();
            if (responseCode == CommunicationProtocol.CHARS_NOT_ALLOWED) throw new CharactersNotAllowedException();
            if (responseCode == CommunicationProtocol.ADD_CARD_ALREADYEXISTS) throw new CardAlreadyExistsException();
            if (responseCode == CommunicationProtocol.COMMUNICATION_ERROR) throw new CommunicationException();

            System.out.println(CommunicationProtocol.ANSI_GREEN + SuccessMSG.ADD_CARD_SUCCESS);
        } else {
            System.out.println(CommunicationProtocol.ANSI_RED + ErrorMSG.NOT_LOGGED);
        }
    }

    public void moveCard(String projectName, String cardName, CardStatus from, CardStatus to)
            throws CommunicationException, ProjectNotExistException, UnauthorizedUserException, OperationNotAllowedException, CardNotExistException {
        if(isLogged) {

            if(!this.isValid(from) || !this.isValid(to)) throw new OperationNotAllowedException();

            // preparing message to send
            RequestMessage requestMessage = new RequestMessage(
                    CommunicationProtocol.MOVE_CARD_CMD,
                    projectName,
                    cardName,
                    from.name(),
                    to.name()
            );

            ResponseMessage response = this.sendTCPRequest(requestMessage);

            int responseCode = response.getStatusCode();
            // error cases
            if (responseCode == CommunicationProtocol.PROJECT_NOT_EXISTS) throw new ProjectNotExistException();
            if (responseCode == CommunicationProtocol.CARD_NOT_EXISTS) throw new CardNotExistException();
            if (responseCode == CommunicationProtocol.UNAUTHORIZED) throw new UnauthorizedUserException();
            if (responseCode == CommunicationProtocol.MOVE_CARD_NOT_ALLOWED) throw new OperationNotAllowedException();
            if (responseCode == CommunicationProtocol.COMMUNICATION_ERROR) throw new CommunicationException();

            System.out.println(CommunicationProtocol.ANSI_GREEN + SuccessMSG.MOVE_CARD_SUCCESS);
        } else {
            System.out.println(CommunicationProtocol.ANSI_RED + ErrorMSG.NOT_LOGGED);
        }
    }

    public void getCardHistory(String projectName, String cardName)
            throws CommunicationException, CardNotExistException, UnauthorizedUserException, ProjectNotExistException {
        if(isLogged) {
            // preparing message to send
            RequestMessage requestMessage = new RequestMessage(
                    CommunicationProtocol.CARD_HISTORY_CMD,
                    projectName,
                    cardName
            );

            ResponseMessage response = this.sendTCPRequest(requestMessage);

            int responseCode = response.getStatusCode();
            // error cases
            if (responseCode == CommunicationProtocol.PROJECT_NOT_EXISTS) throw new ProjectNotExistException();
            if (responseCode == CommunicationProtocol.CARD_NOT_EXISTS) throw new CardNotExistException();
            if (responseCode == CommunicationProtocol.UNAUTHORIZED) throw new UnauthorizedUserException();
            if (responseCode == CommunicationProtocol.COMMUNICATION_ERROR) throw new CommunicationException();

            try {
                List<Movement> movements = this.mapper.readValue(
                        response.getResponseBody(),
                        new TypeReference<List<Movement>>() {
                        }
                );

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                System.out.println(CommunicationProtocol.ANSI_YELLOW + "History of the " + cardName + " card of the " + projectName + " project:");
                if(movements.size() == 0) System.out.println("The card is in TODO status and has not yet been moved by anyone");
                else {
                    for (Movement movement : movements) {
                        System.out.println("from: " + movement.getFrom());
                        System.out.println("to:   " + movement.getTo());
                        System.out.println("when: " + movement.getWhen().format(formatter) + "\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new CommunicationException();
            }
        } else {
            System.out.println(CommunicationProtocol.ANSI_RED + ErrorMSG.NOT_LOGGED);
        }
    }

    public void readChat(String projectName) {
        if(isLogged) {

            LinkedList<UDPMessage> messages = projectChats.getMessages(projectName);
            if (messages != null && !projectChats.isTerminated(projectName)) {
                System.out.println(CommunicationProtocol.ANSI_RESET + "----------- Reading chat ------------- ");
                if (messages.size() == 0) System.out.println("No new messages");
                else {
                    int size = messages.size();
                    while (size > 0) {
                        UDPMessage message = messages.removeFirst();
                        if(message.isFromSystem())
                            System.out.println(CommunicationProtocol.ANSI_YELLOW + message.getAuthor() + ": " + message.getMessage());
                        else if (message.getAuthor().equals(this.username))
                            System.out.println(CommunicationProtocol.ANSI_BLUE + message.getAuthor() + ": " + message.getMessage());
                        else System.out.println(CommunicationProtocol.ANSI_RESET + message.getAuthor() + ": " + message.getMessage());
                        size--;
                    }
                }
                System.out.println(CommunicationProtocol.ANSI_RESET + "------------- End chat --------------- ");
            } else {
                System.out.println(CommunicationProtocol.ANSI_RED + ErrorMSG.PROJECT_NOT_EXISTS);
            }
        } else {
            System.out.println(CommunicationProtocol.ANSI_RED + ErrorMSG.NOT_LOGGED);
        }
    }

    public void sendChatMsg(String projectName, String message)
            throws ChatAddressException, IOException, DatagramTooBigException, ProjectNotExistException, UnauthorizedUserException, CommunicationException {
        if(isLogged) {
            InetAddress group = projectChats.getChatAddress(projectName);
            if(group == null) throw new ProjectNotExistException();

            MulticastSocket multicastSocket = projectChats.getMulticastSocket();

            UDPMessage udpMessage = new UDPMessage(
                    this.username,
                    message,
                    projectName,
                    false
            );
            byte[] byteMessage = this.mapper.writeValueAsBytes(udpMessage);

            // check message size
            if (byteMessage.length >= CommunicationProtocol.UDP_MSG_MAX_LEN)
                throw new DatagramTooBigException();

            DatagramPacket packet = new DatagramPacket(
                    byteMessage,
                    byteMessage.length,
                    group,
                    CommunicationProtocol.MULTICAST_GROUP_PORT
            );
            try {
                multicastSocket.send(packet);
            } catch (IOException e) {
                System.out.println(CommunicationProtocol.ANSI_RED + ErrorMSG.PROJECT_NOT_EXISTS);
                return;
            }
            System.out.println(CommunicationProtocol.ANSI_GREEN + "Message sent");
        } else {
            System.out.println(CommunicationProtocol.ANSI_RED + ErrorMSG.NOT_LOGGED);
        }
    }

    public void cancelProject(String projectName)
            throws CommunicationException, ProjectNotExistException, UnauthorizedUserException, ProjectNotCancelableException {
        if(isLogged) {
            // preparing message to send
            RequestMessage requestMessage = new RequestMessage(
                    CommunicationProtocol.CANCEL_PROJECT_CMD,
                    projectName
            );

            ResponseMessage response = this.sendTCPRequest(requestMessage);

            int responseCode = response.getStatusCode();
            // error cases
            if (responseCode == CommunicationProtocol.PROJECT_NOT_EXISTS) throw new ProjectNotExistException();
            if (responseCode == CommunicationProtocol.UNAUTHORIZED) throw new UnauthorizedUserException();
            if (responseCode == CommunicationProtocol.CANCELPROJECT_NOTCANCELABLE)
                throw new ProjectNotCancelableException();
            if (responseCode == CommunicationProtocol.COMMUNICATION_ERROR) throw new CommunicationException();

            System.out.println(CommunicationProtocol.ANSI_GREEN + SuccessMSG.PROJECT_CANCEL_SUCCESS);
        } else {
            System.out.println(CommunicationProtocol.ANSI_RED + ErrorMSG.NOT_LOGGED);
        }
    }

    /**
     * Called by MOVE_CARD_CMD to verify
     * if the 'from' or 'to' value are valid
     *
     * @param test value to be verify
     * @return true if test is a valid value
     */
    private boolean isValid(CardStatus test) {
        for(CardStatus status : CardStatus.values()) {
            if(status == test) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sends a TCP request to the server and waits for a response from it
     *
     * @param requestMessage to be sent to the server
     *
     * @return response message from the server
     *
     * @throws CommunicationException if there are communication errors with the server
     * */
    private ResponseMessage sendTCPRequest(RequestMessage requestMessage) throws CommunicationException {
        try {
            // convert messages to string
            String stringRequest = this.mapper.writeValueAsString(requestMessage);

            // preparing message
            byte[] byteMessage = stringRequest.getBytes(StandardCharsets.UTF_8);

            // get the message size
            int messageLength = byteMessage.length;

            // write the message and its length in the buffer
            ByteBuffer sendBuffer = ByteBuffer.allocate(Integer.BYTES + messageLength);
            sendBuffer.putInt(messageLength).put(byteMessage);
            sendBuffer.flip();
            while(sendBuffer.hasRemaining())
                socket.write(sendBuffer);
            sendBuffer.clear();

            // waits the response from the server
            ByteBuffer readBuffer = ByteBuffer.allocate(ALLOCATION_SIZE);
            int byteReaded;
            int totalReaded = 0;
            messageLength = -1;
            StringBuilder responseMessage = new StringBuilder();
            do {
                byteReaded = socket.read(readBuffer);
                // if there are errors, throw exception
                if (byteReaded == -1)
                    throw new CommunicationException();

                totalReaded += byteReaded;

                readBuffer.flip();

                // save message size
                if (messageLength == -1)
                    messageLength = readBuffer.getInt();

                responseMessage.append(StandardCharsets.UTF_8.decode(readBuffer));

                readBuffer.clear();
            } while (totalReaded < messageLength);

            String stringResponse = responseMessage.toString();

            ResponseMessage response = this.mapper.readValue(stringResponse, new TypeReference<ResponseMessage>() {});
            return response;
        } catch (IOException e) {
            // exception thrown if there are communication problems with the server

            //e.printStackTrace();
            throw new CommunicationException();
        }
    }

    /*
     * Callback RMI operations
     */

    /**
     * Register the client for the callback service
     * */
    private void registerForCallback() throws RemoteException, NotBoundException {
        // creates RMI connections for the callback service
        Registry registry = LocateRegistry.getRegistry(CommunicationProtocol.REGISTRY_PORT);
        RMICallbackService callbackService =
                (RMICallbackService) registry.lookup(CommunicationProtocol.CALLBACK_SERVICE_NAME);
        // register to the service
        callbackService.registerForCallback(this.username, this.callbackNotify);
    }

    /**
     * Unregister the client from the callback service
     * */
    private void unregisterForCallback() throws RemoteException, NotBoundException {
        // creates RMI connections for the callback service
        Registry registry = LocateRegistry.getRegistry(CommunicationProtocol.REGISTRY_PORT);
        RMICallbackService callbackService =
                (RMICallbackService) registry.lookup(CommunicationProtocol.CALLBACK_SERVICE_NAME);
        // unregister from the service
        callbackService.unregisterForCallback(this.username);
    }

}
