package com.client;

import com.exceptions.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.data.*;
import com.server.service.RMIOperations.RMICallbackService;
import com.server.service.RMIOperations.RMIRegistrationService;
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
import java.time.LocalDateTime;
import java.util.*;

/**
 * @author Davide Chen
 *
 * All operations provided by the service
 */
public class ClientService {
    private static final int ALLOCATION_SIZE = 512*512;     // buffer allocation space
    private boolean isLogged;                               // user online status flag
    private String username;                                // to keep track of the user's username
    private final SocketChannel socket;                     // socket for connection establishment
    private final ObjectMapper mapper;                      // mapper for serialization / deserialization
    private Map<String, UserStatus> userStatus;             // users status list
    private RMICallbackNotify callbackNotify;               // callback management
    private Map<String, String> projectChatAddressAndPort;  // projects multicast andress and port
    private LocalDateTime lastListProjectsCall;             // the last time I called listProjects
    private Map<String, MulticastSocket> projectSockets;    // projects sockets
    private Map<String, ProjectChatTask> projectChats;      // projects chats manager

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

        this.userStatus = null; // it will be initialized during login
        this.callbackNotify = null; // it will be initialized during login
        this.projectChatAddressAndPort = new HashMap<>();
        this.projectSockets = new HashMap<>();
        this.isLogged = false;
        this.username = "";
        this.projectChats = new HashMap<>();
    }

    /**
     * @// TODO: togliere
     */
    public void closeConnection() {
        if (!this.isLogged) return;
        try {
            this.unregisterForCallback();
            this.socket.close();

            // shutdown threads chat receivers
            shutdownThreadPool();

        } catch (Exception e) {
            e.printStackTrace();
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

        System.out.println(result);
    }

    /**
     * TCP operations
     */
    public void login(String username, String password)
            throws UserNotExistException, AlreadyLoggedInException, WrongPasswordException, CommunicationException, IOException {
        // preparing message to send
        RequestMessage requestMessage = new RequestMessage(
                CommunicationProtocol.LOGIN_CMD,
                username,
                password
        );

        ResponseMessage response = this.sendTCPRequest(requestMessage);

        int responseCode = response.getStatusCode();
        // error cases
        if(responseCode == CommunicationProtocol.USER_NOT_EXISTS) throw new UserNotExistException();
        if(responseCode == CommunicationProtocol.LOGIN_WRONGPWD) throw new WrongPasswordException();
        if(responseCode == CommunicationProtocol.COMMUNICATION_ERROR) throw new CommunicationException();
        if(responseCode == CommunicationProtocol.LOGIN_ALREADY_LOGGED) throw new AlreadyLoggedInException();

        // everything through
        try {
            // saving server reply
            this.userStatus = this.mapper.readValue(
                    response.getResponseBody(),
                    new TypeReference<Map<String, UserStatus>>() {
                    }
            );
            // it must remain synchronized
            this.userStatus = Collections.synchronizedMap(this.userStatus);

            // instantiate callback
            this.callbackNotify = new RMICallbackNotifyImpl(this.userStatus);

            // callback registration request
            this.registerForCallback();
        } catch (IOException | NotBoundException e) {
            e.printStackTrace();
        }

        // I'm online
        this.username = username;
        this.isLogged = true;

        System.out.println(SuccessMSG.LOGIN_SUCCESSFUL + "\n");

        // retrieve users list
        this.listUsers();

        // retrieve projects list that I'm part of
        this.listProjects();

        // for each project I create a thread assigned to receive multicast chat messages
        for (Map.Entry<String, String> entry : projectChatAddressAndPort.entrySet()) {
            String projectName = entry.getKey();
            String chatAddressAndPort = entry.getValue();

            String[] tokens = chatAddressAndPort.split(":");
            String chatAddress = tokens[0];
            int port = Integer.parseInt(tokens[1]);

            MulticastSocket multicastSocket = new MulticastSocket(port);
            multicastSocket.setSoTimeout(2000);
            this.projectSockets.put(projectName, multicastSocket);
            ProjectChatTask newChat = new ProjectChatTask(multicastSocket, chatAddress);
            this.projectChats.put(projectName, newChat);
            new Thread(newChat).start();
        }
    }


    public void logout()
            throws UserNotExistException, CommunicationException {
        if(isLogged) {
            // preparing message to send
            RequestMessage requestMessage = new RequestMessage(CommunicationProtocol.LOGOUT_CMD);

            ResponseMessage response = this.sendTCPRequest(requestMessage);

            // error cases
            if (response.getStatusCode() == CommunicationProtocol.USER_NOT_EXISTS) {
                throw new UserNotExistException();
            }

            // request for de-registration to callback service
            try {
                this.unregisterForCallback();
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }

            // I'm offline
            this.username = "";
            this.isLogged = false;


            // shutdown threads chat tasks
            shutdownThreadPool();

            // invalidate list of multicast addresses and sockets
            this.projectChatAddressAndPort = new HashMap<>();
            this.projectSockets = new HashMap<>();

            System.out.println(SuccessMSG.LOGOUT_SUCCESSFUL);
        } else {
            System.err.println(ErrorMSG.NOT_LOGGED);
        }
    }

    public void listUsers() {
        System.out.println("User list with their status:");

        for (Map.Entry<String, UserStatus> entry : userStatus.entrySet()) {
            System.out.println("\t " + entry.getKey() + " - " + entry.getValue());
        }
    }

    public void listOnlineUsers() {

        System.out.println("Online user list:");

        for (Map.Entry<String, UserStatus> entry : userStatus.entrySet()) {
            if(entry.getValue() == UserStatus.ONLINE)
                System.out.println("\t " + entry.getKey() + ": " + entry.getValue());
        }
    }

    public void listProjects()
            throws CommunicationException, UserNotExistException {
        // preparing message to send
        RequestMessage requestMessage = new RequestMessage(CommunicationProtocol.LIST_PROJECTS_CMD);

        ResponseMessage response = this.sendTCPRequest(requestMessage);

        if (response.getStatusCode() == CommunicationProtocol.USER_NOT_EXISTS) {
            throw new UserNotExistException();
        }

        try {
            List<Project> projectList = this.mapper.readValue(
                    response.getResponseBody(),
                    new TypeReference<List<Project>>() {
                    }
            );

            /*
             * invalidate from the multicast address map
             * 1) all deleted projects
             * 2) all projects with the same name as deleted projects
             * of which the user was part of
             */
            List<String> projectNames = new ArrayList<>();
            for (Project project : projectList) {
                projectNames.add(project.getName());
                this.getChatAddressAndPort(project.getName());
            }

            Set<String> addressKeys = this.projectChatAddressAndPort.keySet();
            for (String addressKey : addressKeys) {
                int index = projectNames.indexOf(addressKey);
                if (index == -1) {
                    // case 1
                    this.projectChatAddressAndPort.replace(addressKey, null);
                } else {
                    // case 2
                    if (this.lastListProjectsCall != null) {
                        Project project = projectList.get(index);
                        if (this.lastListProjectsCall.isBefore(project.getCreationDateTime())) {
                            this.projectChatAddressAndPort.replace(addressKey, null);
                        }
                    }
                }
            }

            // update last call variable
            this.lastListProjectsCall = LocalDateTime.now(CommunicationProtocol.ZONE_ID);

            System.out.println("\nMy projects list:");
            if(projectNames.size() == 0) System.out.println("\t empty");
            for(String project : projectNames) {
                System.out.println(project);
            }
        } catch (IOException | UnauthorizedUserException | ProjectNotExistException e) {
            e.printStackTrace();
            throw new CommunicationException();
        }
    }

    public void createProject(String projectName)
            throws ProjectAlreadyExistException, NoSuchAddressException, CharactersNotAllowedException, CommunicationException, NoSuchPortException, UnauthorizedUserException, ProjectNotExistException, IOException {
        // preparing message to send
        RequestMessage requestMessage = new RequestMessage(
                CommunicationProtocol.CREATE_PROJECT_CMD,
                projectName
        );

        ResponseMessage response = this.sendTCPRequest(requestMessage);

        int responseCode = response.getStatusCode();
        // error cases
        if(responseCode == CommunicationProtocol.CREATEPROJECT_ALREADYEXISTS) throw new ProjectAlreadyExistException();
        if(responseCode == CommunicationProtocol.CREATEPROJECT_NOMOREADDRESSES) throw new NoSuchAddressException();
        if(responseCode == CommunicationProtocol.CREATEPROJECT_NOMOREPORTS) throw new NoSuchPortException();
        if(responseCode == CommunicationProtocol.CHARS_NOT_ALLOWED) throw new CharactersNotAllowedException();
        if(responseCode == CommunicationProtocol.COMMUNICATION_ERROR) throw new CommunicationException();

        String chatAddressAndPort = this.getChatAddressAndPort(projectName);

        String[] tokens = chatAddressAndPort.split(":");
        String chatAddress = tokens[0];
        int port = Integer.parseInt(tokens[1]);

        MulticastSocket multicastSocket = new MulticastSocket(port);
        multicastSocket.setSoTimeout(1000);
        this.projectSockets.put(projectName, multicastSocket);
        ProjectChatTask newChat = new ProjectChatTask(multicastSocket, chatAddress);
        this.projectChats.put(projectName, newChat);
        new Thread(newChat).start();

        System.out.println(SuccessMSG.PROJECT_CREATE_SUCCESS);
    }

    public void addMember(String projectName, String username)
            throws CommunicationException, ProjectNotExistException, UnauthorizedUserException, UserAlreadyMemberException, UserNotExistException, IOException {
        // preparing message to send
        RequestMessage requestMessage = new RequestMessage(
                CommunicationProtocol.ADD_MEMBER_CMD,
                projectName,
                username
        );

        ResponseMessage response = this.sendTCPRequest(requestMessage);

        int responseCode = response.getStatusCode();
        // error cases
        if(responseCode == CommunicationProtocol.PROJECT_NOT_EXISTS) throw new ProjectNotExistException();
        if(responseCode == CommunicationProtocol.UNAUTHORIZED) throw new UnauthorizedUserException();
        if(responseCode == CommunicationProtocol.ADD_MEMBER_ALREADYPRESENT) throw new UserAlreadyMemberException();
        if(responseCode == CommunicationProtocol.USER_NOT_EXISTS) throw new UserNotExistException();
        if(responseCode == CommunicationProtocol.COMMUNICATION_ERROR) throw new CommunicationException();

        System.out.println(SuccessMSG.ADD_MEMBER_SUCCESS);
    }

        public void showMembers(String projectName)
            throws CommunicationException, ProjectNotExistException, UnauthorizedUserException {
        // preparing message to send
        RequestMessage requestMessage = new RequestMessage(
                CommunicationProtocol.SHOW_MEMBERS_CMD,
                projectName
        );

        ResponseMessage response = this.sendTCPRequest(requestMessage);

        int responseCode = response.getStatusCode();
        // error cases
        if(responseCode == CommunicationProtocol.PROJECT_NOT_EXISTS) throw new ProjectNotExistException();
        if(responseCode == CommunicationProtocol.UNAUTHORIZED) throw new UnauthorizedUserException();
        if(responseCode == CommunicationProtocol.COMMUNICATION_ERROR) throw new CommunicationException();

        try {
            List<String> members = this.mapper.readValue(
                    response.getResponseBody(),
                    new TypeReference<List<String>>() {}
            );

            System.out.println(projectName + " project member list: ");
            for(String member : members) {
                System.out.println(member);
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new CommunicationException();
        }
    }

    public void showCards(String projectName)
            throws CommunicationException, ProjectNotExistException, UnauthorizedUserException {
        // preparing messagge to send
        RequestMessage requestMessage = new RequestMessage(
                CommunicationProtocol.SHOW_CARDS_CMD,
                projectName
        );

        ResponseMessage response = this.sendTCPRequest(requestMessage);

        int responseCode = response.getStatusCode();
        // error cases
        if(responseCode == CommunicationProtocol.PROJECT_NOT_EXISTS) throw new ProjectNotExistException();
        if(responseCode == CommunicationProtocol.UNAUTHORIZED) throw new UnauthorizedUserException();
        if(responseCode == CommunicationProtocol.COMMUNICATION_ERROR) throw new CommunicationException();

        try {
            Map<CardStatus, List<String>> cards = this.mapper.readValue(
                    response.getResponseBody(),
                    new TypeReference<Map<CardStatus, List<String>>>() {}
            );

            System.out.println("TODO:");
            for(String cardName : cards.get(CardStatus.TODO)) {
                System.out.println("\t" + cardName);
            }
            System.out.println("INPROGRESS:");
            for(String cardName : cards.get(CardStatus.INPROGRESS)) {
                System.out.println("\t" + cardName);
            }
            System.out.println("TOBEREVISED:");
            for(String cardName : cards.get(CardStatus.TOBEREVISED)) {
                System.out.println("\t" + cardName);
            }
            System.out.println("DONE:");
            for(String cardName : cards.get(CardStatus.DONE)) {
                System.out.println("\t" + cardName);
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new CommunicationException();
        }
    }

    public void showCard(String projectName, String cardName)
            throws CommunicationException, ProjectNotExistException, CardNotExistException, UnauthorizedUserException {
        // preparing message to send
        RequestMessage requestMessage = new RequestMessage(
                CommunicationProtocol.SHOW_CARD_CMD,
                projectName,
                cardName
        );

        ResponseMessage response = this.sendTCPRequest(requestMessage);

        int responseCode = response.getStatusCode();
        // error cases
        if(responseCode == CommunicationProtocol.PROJECT_NOT_EXISTS) throw new ProjectNotExistException();
        if(responseCode == CommunicationProtocol.CARD_NOT_EXISTS) throw new CardNotExistException();
        if(responseCode == CommunicationProtocol.UNAUTHORIZED) throw new UnauthorizedUserException();
        if(responseCode == CommunicationProtocol.COMMUNICATION_ERROR) throw new CommunicationException();

        try {
            Card card = this.mapper.readValue(
                    response.getResponseBody(),
                    new TypeReference<CardImpl>() {}
            );

            System.out.println("Card name: " + card.getName());
            System.out.println("Card descption: " + card.getDescription());
            System.out.println("Card status: " + card.getStatus());

        } catch (IOException e) {
            e.printStackTrace();
            throw new CommunicationException();
        }
    }

    public void addCard(String projectName, String cardName, String description)
            throws CommunicationException, ProjectNotExistException, UnauthorizedUserException, CardAlreadyExistsException, CharactersNotAllowedException {
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
        if(responseCode == CommunicationProtocol.PROJECT_NOT_EXISTS) throw new ProjectNotExistException();
        if(responseCode == CommunicationProtocol.UNAUTHORIZED) throw new UnauthorizedUserException();
        if(responseCode == CommunicationProtocol.CHARS_NOT_ALLOWED) throw new CharactersNotAllowedException();
        if(responseCode == CommunicationProtocol.ADD_CARD_ALREADYEXISTS) throw new CardAlreadyExistsException();
        if(responseCode == CommunicationProtocol.COMMUNICATION_ERROR) throw new CommunicationException();

        System.out.println(SuccessMSG.ADD_CARD_SUCCESS);
    }

    public void moveCard(String projectName, String cardName, CardStatus from, CardStatus to)
            throws CommunicationException, ProjectNotExistException, UnauthorizedUserException, OperationNotAllowedException, CardNotExistException {
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
        if(responseCode == CommunicationProtocol.PROJECT_NOT_EXISTS) throw new ProjectNotExistException();
        if(responseCode == CommunicationProtocol.CARD_NOT_EXISTS) throw new CardNotExistException();
        if(responseCode == CommunicationProtocol.UNAUTHORIZED) throw new UnauthorizedUserException();
        if(responseCode == CommunicationProtocol.MOVE_CARD_NOT_ALLOWED) throw new OperationNotAllowedException();
        if(responseCode == CommunicationProtocol.COMMUNICATION_ERROR) throw new CommunicationException();

        System.out.println(SuccessMSG.MOVE_CARD_SUCCESS);
    }

    public void getCardHistory(String projectName, String cardName)
            throws CommunicationException, CardNotExistException, UnauthorizedUserException, ProjectNotExistException {
        // preparing message to send
        RequestMessage requestMessage = new RequestMessage(
                CommunicationProtocol.CARD_HISTORY_CMD,
                projectName,
                cardName
        );

        ResponseMessage response = this.sendTCPRequest(requestMessage);

        int responseCode = response.getStatusCode();
        // error cases
        if(responseCode == CommunicationProtocol.PROJECT_NOT_EXISTS) throw new ProjectNotExistException();
        if(responseCode == CommunicationProtocol.CARD_NOT_EXISTS) throw new CardNotExistException();
        if(responseCode == CommunicationProtocol.UNAUTHORIZED) throw new UnauthorizedUserException();
        if(responseCode == CommunicationProtocol.COMMUNICATION_ERROR) throw new CommunicationException();

        try {
            List<Movement> movements = this.mapper.readValue(
                    response.getResponseBody(),
                    new TypeReference<List<Movement>>() {}
            );

            System.out.println("History of the " + cardName + " card of the " + projectName + " project:");
            for(Movement movement : movements) {
                System.out.println(movement);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new CommunicationException();
        }
    }

    public void readChat(String projectName) {

        LinkedList<UDPMessage> messages = projectChats.get(projectName).getMessages();

        System.out.println("----------- Reading chat ------------- ");
        if(messages.size() == 0) System.out.println("No new messages");
        else {
            int size = messages.size();
            while(size > 0) {
                UDPMessage message = messages.removeFirst();
                System.out.println(message.getAuthor() + ": " + message.getMessage());
                size--;
            }
        }
        System.out.println("------------- End chat --------------- ");
    }

    public void sendChatMsg(String projectName, String message)
            throws ChatAddressException, IOException, DatagramTooBigException, ProjectNotExistException, UnauthorizedUserException, CommunicationException {
        String chatAddressAndPort = this.projectChatAddressAndPort.get(projectName);

        if(chatAddressAndPort == null) {
            chatAddressAndPort = this.getChatAddressAndPort(projectName);
        }

        MulticastSocket multicastSocket = this.projectSockets.get(projectName);
        if (chatAddressAndPort == null || multicastSocket == null) {
            throw new ChatAddressException();
        }
        String[] tokens = chatAddressAndPort.split(":");
        String chatAddress = tokens[0];
        int port = Integer.parseInt(tokens[1]);

        InetAddress group = InetAddress.getByName(chatAddress);

        UDPMessage udpMessage = new UDPMessage(
                this.username,
                message,
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
                port
        );

        multicastSocket.send(packet);
    }

    public void cancelProject(String projectName)
            throws CommunicationException, ProjectNotExistException, UnauthorizedUserException, ProjectNotCancelableException {
        // preparing message to send
        RequestMessage requestMessage = new RequestMessage(
                CommunicationProtocol.CANCELPROJECT_CMD,
                projectName
        );

        ResponseMessage response = this.sendTCPRequest(requestMessage);

        int responseCode = response.getStatusCode();
        // error cases
        if(responseCode == CommunicationProtocol.PROJECT_NOT_EXISTS) throw new ProjectNotExistException();
        if(responseCode == CommunicationProtocol.UNAUTHORIZED) throw new UnauthorizedUserException();
        if(responseCode == CommunicationProtocol.CANCELPROJECT_NOTCANCELABLE) throw new ProjectNotCancelableException();
        if(responseCode == CommunicationProtocol.COMMUNICATION_ERROR) throw new CommunicationException();

        System.out.println(SuccessMSG.PROJECT_CANCEL_SUCCESS);
    }


    public String getChatAddressAndPort(String projectName)
            throws CommunicationException, UnauthorizedUserException, ProjectNotExistException {

        String chatAddressAndPort;

        // preparing messagge to send
        RequestMessage requestMessage = new RequestMessage(
                CommunicationProtocol.READ_CHAT_CMD,
                projectName
        );

        ResponseMessage response = this.sendTCPRequest(requestMessage);
        int responseCode = response.getStatusCode();
        // error cases
        if (responseCode == CommunicationProtocol.PROJECT_NOT_EXISTS) throw new ProjectNotExistException();
        if (responseCode == CommunicationProtocol.UNAUTHORIZED) throw new UnauthorizedUserException();
        if (responseCode == CommunicationProtocol.COMMUNICATION_ERROR) throw new CommunicationException();

        try {
            chatAddressAndPort = this.mapper.readValue(
                    response.getResponseBody(),
                    new TypeReference<String>() {}
            );

            String[] tokens = chatAddressAndPort.split(":");
            int port = Integer.parseInt(tokens[1]);
            MulticastSocket multicastSocket = new MulticastSocket(port);
            multicastSocket.setSoTimeout(1000); // receive bloccante per 1 secondo

            // save for each project its chat address and port
            this.projectChatAddressAndPort.put(projectName, chatAddressAndPort);

            // save for each project its multicast socket
            this.projectSockets.put(projectName, multicastSocket);

            return chatAddressAndPort;

        } catch (IOException e) {
            e.printStackTrace();
            throw new CommunicationException();
        }
    }

    /**
     * Terminate all threads assigned to receive multicast chat messages,
     * by breaking the endless loop of each task
     */
    public void shutdownThreadPool() {
        for(ProjectChatTask task : projectChats.values()) {
            task.terminate();
        }
    }

    /**
     * Sends a TCP request to the server and waits for a response from it
     *
     * @param requestMessage to be sent to the server
     *
     * @return response message from the server
     *
     * @throws CommunicationException if there are communication errors
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

                responseMessage.append(StandardCharsets.UTF_8.decode(readBuffer).toString());

                readBuffer.clear();
            } while (totalReaded < messageLength);

            String stringResponse = responseMessage.toString();

            ResponseMessage response = this.mapper.readValue(stringResponse, new TypeReference<ResponseMessage>() {});
            return response;
        } catch (IOException e) {
            e.printStackTrace();
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
        callbackService.registerForCallback(this.callbackNotify);
    }

    /**
     * Unregister the client from the callback service
     * */
    private void unregisterForCallback() throws RemoteException, NotBoundException {
        // reates RMI connections for the callback service
        Registry registry = LocateRegistry.getRegistry(CommunicationProtocol.REGISTRY_PORT);
        RMICallbackService callbackService =
                (RMICallbackService) registry.lookup(CommunicationProtocol.CALLBACK_SERVICE_NAME);
        // unregister from the service
        callbackService.unregisterForCallback(this.callbackNotify);
    }

}
