package com.server.TCPOperations;

import com.CommunicationProtocol;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.RMIOperations.RMICallbackServiceImpl;
import com.data.*;
import com.exceptions.*;
import com.utils.*;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author Davide Chen
 *
 * Task for managing the client's requests using a NIO selector
 */
public class SelectionTask implements Runnable {
    private static final int ALLOCATION_SIZE = 2048;        // size (in bytes) per allocation of a ByteBuffer
    private final TCPOperations data;                       // application data
    private final ObjectMapper mapper;                      // mapper used for Jackson serialization / deserialization
    private final RMICallbackServiceImpl callbackService;   // callback service
    private volatile boolean terminated;

    public SelectionTask(TCPOperations data, RMICallbackServiceImpl callbackService) {
        this.data = data;
        this.callbackService = callbackService;

        this.mapper = new MyObjectMapper();
        this.terminated = false;
    }

    public void run() {
        ServerSocketChannel serverChannel;
        Selector selector;

        try {
            serverChannel = ServerSocketChannel.open();
            InetSocketAddress address = new InetSocketAddress(
                    CommunicationProtocol.SERVER_IP_ADDRESS,
                    CommunicationProtocol.SERVER_PORT
            );
            ServerSocket serverSocket = serverChannel.socket();
            serverSocket.bind(address);
            serverChannel.configureBlocking(false); // non-blocking socket server
            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT); // register server to accept connections
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        while (!terminated) {
            try {
                selector.select(1000);
                Set<SelectionKey> readyKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = readyKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) { // server ready to accept connection
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        try {
                            SocketChannel client = server.accept(); // non-blocking

                            client.configureBlocking(false); // non-blocking client socket

                            // get ready for reading from client
                            client.register(selector, SelectionKey.OP_READ);
                        } catch (IOException e) {
                            e.printStackTrace();
                            server.close();
                            return;
                        }
                    } else if (key.isReadable()) {
                        // client wrote on channel, I'm ready to read it
                        SocketChannel client = (SocketChannel) key.channel();

                        Attachment attachment = (Attachment) key.attachment();

                        // the first time the attachment will be null
                        // if so I have to instantiate it
                        if (attachment == null) {
                            attachment = new Attachment();
                        }

                        // buffer allocation
                        ByteBuffer buffer = ByteBuffer.allocate(ALLOCATION_SIZE);

                        // read message from channel
                        int byteReaded;
                        int totalReaded = 0;
                        int messageLength = -1;
                        StringBuilder messageReceived = new StringBuilder();
                        try {
                            do {
                                byteReaded = client.read(buffer);
                                if (byteReaded == -1) break;
                                totalReaded += byteReaded;

                                buffer.flip();

                                // saving message length
                                if (messageLength == -1)
                                    messageLength = buffer.getInt();

                                messageReceived.append(StandardCharsets.UTF_8.decode(buffer));

                                buffer.clear();
                            } while (totalReaded < messageLength);
                        } catch (IOException e) {
                            // when the client abruptly breaks the connection with the server
                            // a SocketException is caught

                            // if the user was online, I have to log out
                            String username = attachment.getUsername();
                            if (username != null) {
                                try {
                                    data.logout(username);
                                    callbackService.notifyUsers(username, UserStatus.OFFLINE);
                                } catch (UserNotExistException e1) {
                                    e1.printStackTrace();
                                }
                            }

                            key.cancel();
                            client.close();
                            continue;
                        }

                        // get the request message
                        RequestMessage requestMessage = this.mapper.readValue(
                                messageReceived.toString(),
                                new TypeReference<RequestMessage>() {}
                        );


                        String command = requestMessage.getCommand();
                        List<String> arguments = requestMessage.getArguments();

                        // preparing the response code
                        int responseCode = CommunicationProtocol.UNKNOWN;
                        // preparing the response body
                        String responseBody = null;
                        String responseBody2 = null;

                        // depending on the command, there will be different behaviors
                        switch (command) {
                            case CommunicationProtocol.LOGIN_CMD: {
                                // check number of parameters
                                if (arguments.size() != 2) {
                                    responseCode = CommunicationProtocol.COMMUNICATION_ERROR;
                                    break;
                                }

                                String username = arguments.get(0);
                                String hash = arguments.get(1);
                                try {
                                    data.login(username, hash);

                                    // response body: list of users and their status
                                    Map<String, UserStatus> userStatus = data.getUserStatus();
                                    // response body 2: list of his/her projects
                                    Map<String, InetAddress> chatsAddresses = new Hashtable<>();
                                    for(Project project : data.listProjects(username)) {
                                        String projectName = project.getName();
                                        String chatAddress = data.getProjectChatAddress(projectName);
                                        InetAddress group = InetAddress.getByName(chatAddress);
                                        chatsAddresses.put(projectName, group);
                                    }

                                    responseBody = this.mapper.writeValueAsString(userStatus);
                                    responseBody2 = this.mapper.writeValueAsString(chatsAddresses);

                                    // notifies users that the user 'username' is now online
                                    callbackService.notifyUsers(username, UserStatus.ONLINE);

                                    // insert the username in the attachment
                                    attachment.setUsername(username);
                                } catch (UserNotExistException e) {
                                    responseCode = CommunicationProtocol.USER_NOT_EXISTS;
                                } catch (AlreadyLoggedInException e) {
                                    responseCode = CommunicationProtocol.LOGIN_ALREADY_LOGGED;
                                } catch (WrongPasswordException e) {
                                    responseCode = CommunicationProtocol.LOGIN_WRONGPWD;
                                } catch (ProjectNotExistException e) {
                                    //impossible to happen
                                }
                                break;
                            }
                            case CommunicationProtocol.LOGOUT_CMD: {
                                // get the user who made the request
                                String username = attachment.getUsername();
                                if (username != null) {
                                    try {
                                        data.logout(username);

                                        // notify other users that 'username' is offline
                                        callbackService.notifyUsers(username, UserStatus.OFFLINE);

                                        attachment.setUsername(null);
                                    } catch (UserNotExistException e) {
                                        responseCode = CommunicationProtocol.USER_NOT_EXISTS;
                                    }
                                }
                                break;
                            }
                            case CommunicationProtocol.LIST_PROJECTS_CMD: {
                                // get the user who made the request
                                String username = attachment.getUsername();
                                if (username != null) {
                                    try {
                                        List<Project> projects = data.listProjects(username);
                                        responseBody = this.mapper.writeValueAsString(projects);
                                    } catch (UserNotExistException e) {
                                        responseCode = CommunicationProtocol.USER_NOT_EXISTS;
                                    }
                                }
                                else {
                                    responseCode = CommunicationProtocol.USER_NOT_LOGGED;
                                }
                                break;
                            }
                            case CommunicationProtocol.CREATE_PROJECT_CMD: {
                                // check number of parameters
                                if (arguments.size() != 1) {
                                    responseCode = CommunicationProtocol.COMMUNICATION_ERROR;
                                    break;
                                }

                                // get the user who made the request
                                String username = attachment.getUsername();
                                if(username == null) {
                                    responseCode = CommunicationProtocol.USER_NOT_LOGGED;
                                    break;
                                }

                                String projectName = arguments.get(0);
                                if (!projectName.matches(CommunicationProtocol.STRING_REGEX)) {
                                    responseCode = CommunicationProtocol.CHARS_NOT_ALLOWED;
                                    break;
                                }

                                try {
                                    data.createProject(projectName, username);

                                    String chatAddress = data.getProjectChatAddress(projectName);
                                    responseBody = this.mapper.writeValueAsString(chatAddress);
                                } catch (ProjectAlreadyExistException e) {
                                    responseCode = CommunicationProtocol.CREATEPROJECT_ALREADYEXISTS;
                                } catch (NoSuchAddressException e) {
                                    responseCode = CommunicationProtocol.CREATEPROJECT_NOMOREADDRESSES;
                                } catch (NoSuchPortException e) {
                                    responseCode = CommunicationProtocol.CREATEPROJECT_NOMOREPORTS;
                                } catch (ProjectNotExistException e) {
                                    //impossible to happen
                                }
                                break;
                            }
                            case CommunicationProtocol.ADD_MEMBER_CMD: {
                                // check number of parameters
                                if (arguments.size() != 2) {
                                    responseCode = CommunicationProtocol.COMMUNICATION_ERROR;
                                    break;
                                }

                                // get the user who made the request
                                String username = attachment.getUsername();
                                if(username == null) {
                                    responseCode = CommunicationProtocol.USER_NOT_LOGGED;
                                    break;
                                }

                                String projectName = arguments.get(0);
                                String userToAdd = arguments.get(1);
                                try {
                                    data.addMember(projectName, userToAdd, username);

                                    String chatAddress = data.getProjectChatAddress(projectName);
                                    // notifies user 'username' that he/she is now a member of the project 'projectName'
                                    callbackService.notifyProject(userToAdd, projectName, chatAddress);
                                } catch (ProjectNotExistException e) {
                                    responseCode = CommunicationProtocol.PROJECT_NOT_EXISTS;
                                } catch (UnauthorizedUserException e) {
                                    responseCode = CommunicationProtocol.UNAUTHORIZED;
                                } catch (UserAlreadyMemberException e) {
                                    responseCode = CommunicationProtocol.ADD_MEMBER_ALREADYPRESENT;
                                } catch (UserNotExistException e) {
                                    responseCode = CommunicationProtocol.USER_NOT_EXISTS;
                                }

                                break;
                            }
                            case CommunicationProtocol.SHOW_MEMBERS_CMD: {
                                // check number of parameters
                                if (arguments.size() != 1) {
                                    responseCode = CommunicationProtocol.COMMUNICATION_ERROR;
                                    break;
                                }

                                // get the user who made the request
                                String username = attachment.getUsername();
                                if(username == null) {
                                    responseCode = CommunicationProtocol.USER_NOT_LOGGED;
                                    break;
                                }

                                String projectName = arguments.get(0);

                                try {
                                    List<String> members = data.showMembers(projectName, username);
                                    responseBody = this.mapper.writeValueAsString(members);
                                } catch (ProjectNotExistException e) {
                                    responseCode = CommunicationProtocol.PROJECT_NOT_EXISTS;
                                } catch (UnauthorizedUserException e) {
                                    responseCode = CommunicationProtocol.UNAUTHORIZED;
                                }
                                break;
                            }
                            case CommunicationProtocol.SHOW_CARDS_CMD: {
                                // check number of parameters
                                if (arguments.size() != 1) {
                                    responseCode = CommunicationProtocol.COMMUNICATION_ERROR;
                                    break;
                                }
                                // get the user who made the request
                                String username = attachment.getUsername();
                                if(username == null) {
                                    responseCode = CommunicationProtocol.USER_NOT_LOGGED;
                                    break;
                                }

                                String projectName = arguments.get(0);
                                try {
                                    Map<CardStatus, List<String>> cards = data.showCards(projectName, username);
                                    responseBody = this.mapper.writeValueAsString(cards);
                                } catch (ProjectNotExistException e) {
                                    responseCode = CommunicationProtocol.PROJECT_NOT_EXISTS;
                                } catch (UnauthorizedUserException e) {
                                    responseCode = CommunicationProtocol.UNAUTHORIZED;
                                }
                                break;
                            }
                            case CommunicationProtocol.SHOW_CARD_CMD: {
                                // check the number of parameters
                                if (arguments.size() != 2) {
                                    responseCode = CommunicationProtocol.COMMUNICATION_ERROR;
                                    break;
                                }

                                // get the user who made the request
                                String username = attachment.getUsername();
                                if(username == null) {
                                    responseCode = CommunicationProtocol.USER_NOT_LOGGED;
                                    break;
                                }

                                String projectName = arguments.get(0);
                                String cardName = arguments.get(1);

                                try {
                                    Card card = data.showCard(projectName, cardName, username);
                                    // serialize interface elements only
                                    responseBody = this.mapper.writerFor(Card.class).writeValueAsString(card);
                                } catch (ProjectNotExistException e) {
                                    responseCode = CommunicationProtocol.PROJECT_NOT_EXISTS;
                                } catch (UnauthorizedUserException e) {
                                    responseCode = CommunicationProtocol.UNAUTHORIZED;
                                } catch (CardNotExistException e) {
                                    responseCode = CommunicationProtocol.CARD_NOT_EXISTS;
                                }
                                break;
                            }
                            case CommunicationProtocol.ADD_CARD_CMD: {
                                // check number of parameters
                                if (arguments.size() != 3) {
                                    responseCode = CommunicationProtocol.COMMUNICATION_ERROR;
                                    break;
                                }
                                // get the user who made the request
                                String username = attachment.getUsername();
                                if(username == null) {
                                    responseCode = CommunicationProtocol.USER_NOT_LOGGED;
                                    break;
                                }

                                String projectName = arguments.get(0);
                                String cardName = arguments.get(1);
                                String description = arguments.get(2);

                                // check: cardName must respect the string regex
                                if (!cardName.matches(CommunicationProtocol.STRING_REGEX)) {
                                    responseCode = CommunicationProtocol.CHARS_NOT_ALLOWED;
                                    break;
                                }

                                try {
                                    data.addCard(projectName, cardName, description, username);
                                } catch (ProjectNotExistException e) {
                                    responseCode = CommunicationProtocol.PROJECT_NOT_EXISTS;
                                } catch (UnauthorizedUserException e) {
                                    responseCode = CommunicationProtocol.UNAUTHORIZED;
                                } catch (CardAlreadyExistsException e) {
                                    responseCode = CommunicationProtocol.ADD_CARD_ALREADYEXISTS;
                                }
                                break;
                            }
                            case CommunicationProtocol.MOVE_CARD_CMD: {
                                // check number of parameters
                                if (arguments.size() != 4) {
                                    responseCode = CommunicationProtocol.COMMUNICATION_ERROR;
                                    break;
                                }
                                // get the user who made the request
                                String username = attachment.getUsername();
                                if(username == null) {
                                    responseCode = CommunicationProtocol.USER_NOT_LOGGED;
                                    break;
                                }

                                String projectName = arguments.get(0);
                                String cardName = arguments.get(1);
                                CardStatus from = CardStatus.retriveFromString(
                                        arguments.get(2)
                                );
                                CardStatus to = CardStatus.retriveFromString(
                                        arguments.get(3)
                                );

                                // check that 'from' and 'to' are not null
                                if (from == null || to == null) {
                                    responseCode = CommunicationProtocol.COMMUNICATION_ERROR;
                                    break;
                                }

                                try {
                                    data.moveCard(projectName, cardName, from, to, username);

                                    // operation succeed
                                    // the server notifies all users in the project chat
                                    try {
                                        String chatAddress = data.getProjectChatAddress(projectName);
                                        InetAddress group = InetAddress.getByName(chatAddress);
                                        DatagramSocket socket = new DatagramSocket();

                                        UDPMessage udpMessage = new UDPMessage(
                                                CommunicationProtocol.SYSTEM_NAME,
                                                username + " moved card '" + cardName +
                                                        "' from " + from.name() + " to " + to.name(),
                                                projectName,
                                                true
                                        );
                                        byte[] byteMessage = this.mapper.writeValueAsBytes(udpMessage);
                                        DatagramPacket packet = new DatagramPacket(
                                                byteMessage,
                                                byteMessage.length,
                                                group,
                                                CommunicationProtocol.MULTICAST_GROUP_PORT
                                        );
                                        socket.send(packet);

                                    } catch (ProjectNotExistException e) {
                                        e.printStackTrace();
                                    }

                                } catch (ProjectNotExistException e) {
                                    responseCode = CommunicationProtocol.PROJECT_NOT_EXISTS;
                                } catch (UnauthorizedUserException e) {
                                    responseCode = CommunicationProtocol.UNAUTHORIZED;
                                } catch (CardNotExistException e) {
                                    responseCode = CommunicationProtocol.CARD_NOT_EXISTS;
                                } catch (OperationNotAllowedException e) {
                                    responseCode = CommunicationProtocol.MOVE_CARD_NOT_ALLOWED;
                                }
                                break;
                            }
                            case CommunicationProtocol.CARD_HISTORY_CMD: {
                                // check number of parameters
                                if (arguments.size() != 2) {
                                    responseCode = CommunicationProtocol.COMMUNICATION_ERROR;
                                    break;
                                }

                                // get the user who made the request
                                String username = attachment.getUsername();
                                if(username == null) {
                                    responseCode = CommunicationProtocol.USER_NOT_LOGGED;
                                    break;
                                }

                                String projectName = arguments.get(0);
                                String cardName = arguments.get(1);

                                try {
                                    List<Movement> cardHistory = data.getCardHistory(projectName, cardName, username);
                                    responseBody = this.mapper.writeValueAsString(cardHistory);
                                } catch (ProjectNotExistException e) {
                                    responseCode = CommunicationProtocol.PROJECT_NOT_EXISTS;
                                } catch (UnauthorizedUserException e) {
                                    responseCode = CommunicationProtocol.UNAUTHORIZED;
                                } catch (CardNotExistException e) {
                                    responseCode = CommunicationProtocol.CARD_NOT_EXISTS;
                                }
                                break;
                            }

                            case CommunicationProtocol.CANCEL_PROJECT_CMD: {
                                // check number of parameters
                                if (arguments.size() != 1) {
                                    responseCode = CommunicationProtocol.COMMUNICATION_ERROR;
                                    break;
                                }
                                // get the user who made the request
                                String username = attachment.getUsername();
                                if(username == null) {
                                    responseCode = CommunicationProtocol.USER_NOT_LOGGED;
                                    break;
                                }

                                String projectName = arguments.get(0);
                                try {
                                    // the server notifies all users in the project chat
                                    // that the project has been deleted
                                    String chatAddress = data.getProjectChatAddress(projectName);
                                    InetAddress group = InetAddress.getByName(chatAddress);
                                    DatagramSocket socket = new DatagramSocket();

                                    // retrieval of the list of members to terminate the project chat
                                    List<String> members = data.showMembers(projectName, username);

                                    // delete project
                                    data.cancelProject(projectName, username);

                                    UDPMessage udpMessage = new UDPMessage(
                                            CommunicationProtocol.SYSTEM_NAME,
                                            CommunicationProtocol.UDP_TERMINATE_MSG,
                                            projectName,
                                            true
                                    );

                                    byte[] byteMessage = this.mapper.writeValueAsBytes(udpMessage);
                                    DatagramPacket packet = new DatagramPacket(
                                            byteMessage,
                                            byteMessage.length,
                                            group,
                                            CommunicationProtocol.MULTICAST_GROUP_PORT
                                    );

                                    socket.send(packet);

                                    // terminate
                                    callbackService.terminateChat(projectName, members);

                                } catch (ProjectNotExistException e) {
                                    responseCode = CommunicationProtocol.PROJECT_NOT_EXISTS;
                                } catch (UnauthorizedUserException e) {
                                    responseCode = CommunicationProtocol.UNAUTHORIZED;
                                } catch (ProjectNotCancelableException e) {
                                    responseCode = CommunicationProtocol.CANCELPROJECT_NOTCANCELABLE;
                                }
                                break;
                            }

                            case CommunicationProtocol.EXIT_CMD: {
                                // get the user who made the request
                                String username = attachment.getUsername();

                                // in case the user is still online
                                if (username != null) {
                                    try {
                                        data.logout(username);
                                        callbackService.notifyUsers(username, UserStatus.OFFLINE);
                                    } catch (UserNotExistException e1) {
                                        e1.printStackTrace();
                                    }
                                }

                                key.cancel();
                                client.close();
                                continue;
                            }
                        }

                        // if code still unidentified => success
                        // as there are cases where they don't return responseCode
                        if (responseCode == CommunicationProtocol.UNKNOWN) {
                            responseCode = CommunicationProtocol.OP_SUCCESS;
                        }

                        // preparing reply message
                        ResponseMessage response = new ResponseMessage(
                                responseCode,
                                responseBody,
                                responseBody2
                        );

                        // serialize it and put it in the buffer
                        byte[] byteResponse = this.mapper.writeValueAsBytes(response);
                        // get the message length
                        messageLength = byteResponse.length;
                        buffer = ByteBuffer.allocate(Integer.BYTES + messageLength);
                        // insert length and message
                        buffer.putInt(messageLength).put(byteResponse);
                        buffer.flip();

                        // save the buffer in the attachment
                        attachment.setBuffer(buffer);

                        // get ready for writing
                        client.register(selector, SelectionKey.OP_WRITE, attachment);
                    } else if (key.isWritable()) {
                        // client waiting for writing to channel
                        SocketChannel client = (SocketChannel) key.channel();

                        Attachment attachment = (Attachment) key.attachment();

                        ByteBuffer buffer = attachment.getBuffer();

                        try {
                            client.write(buffer);
                        } catch (IOException e) {
                            client.close();
                        }
                        if (!buffer.hasRemaining())
                            buffer.clear();

                        // get ready for next reading from client
                        client.register(selector, SelectionKey.OP_READ, attachment);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            selector.close();
            serverChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdownServer() {
        callbackService.notifyServerDown();
        this.terminated = true;
    }
}
