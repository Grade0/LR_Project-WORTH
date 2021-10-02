package com.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.CommunicationProtocol;
import com.utils.UDPMessage;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Davide Chen
 *
 * Task to manage the reception of chat messages
 */
public class ProjectChatTask implements Runnable {

    private final MulticastSocket multicastSocket;
    private final Map<String, InetAddress> chatAddresses;           // Collection.sychronizedMap
    private final Map<String, LinkedList<UDPMessage>> messages;     // ConcurrentHashMap
    volatile boolean finish = false;                                // flag for the infinity loop,
                                                                    // it will set to true when the user logs out

    public ProjectChatTask(Map<String, InetAddress> addresses) throws IOException {
        this.multicastSocket = new MulticastSocket(CommunicationProtocol.MULTICAST_GROUP_PORT);
        this.chatAddresses = addresses;
        this.messages = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        DatagramPacket packet;
        byte[] buffer;

        try {
            for(Map.Entry<String, InetAddress> chat : this.chatAddresses.entrySet()) {
                this.multicastSocket.joinGroup(chat.getValue());
                this.messages.put(chat.getKey(), new LinkedList<>());
            }
            buffer = new byte[2024];
            packet = new DatagramPacket(buffer, buffer.length);

            while (!finish) {
                try {
                    multicastSocket.setSoTimeout(1000);
                    multicastSocket.receive(packet);

                    String message = new String(
                            packet.getData(),
                            packet.getOffset(),
                            packet.getLength(),
                            StandardCharsets.UTF_8
                    );

                    UDPMessage udpMessage = new ObjectMapper().readValue(message,
                            new TypeReference<UDPMessage>() {
                            });

                    messages.get(udpMessage.getProjectName()).add(udpMessage);
                } catch (SocketTimeoutException e) {
                    //ignore
                }
            }

            multicastSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public MulticastSocket getMulticastSocket() {
        return this.multicastSocket;
    }

    public InetAddress getChatAddress(String projectName) {
        return this.chatAddresses.get(projectName);
    }

    public LinkedList<UDPMessage> getMessages(String projectName) {
        return this.messages.get(projectName);
    }

    public void joinGroup(String projectName, InetAddress address) {
        try {
            this.chatAddresses.put(projectName, address);
            this.messages.put(projectName, new LinkedList<>());
            this.multicastSocket.joinGroup(address);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void leaveGroup(String projectName) {
        try {
            InetAddress address = this.chatAddresses.remove(projectName);
            multicastSocket.leaveGroup(address);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void terminate() {
        // interrupt the infinity loop
        this.finish = true;
        try {
            for(InetAddress chatAddress : this.chatAddresses.values()) {
                // leave all the multicast group
                multicastSocket.leaveGroup(chatAddress);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isTerminated(String projectName) {
        // the project is not cancelled
        if(chatAddresses.containsKey(projectName)) return false;
        // messages.get() will not return null,
        // otherwise 'isTerminated' would not have been called
        else if (messages.get(projectName).size() == 0) {
                // project is cancelled and last messages already read
                messages.remove(projectName);
                return true;
            }
        // last messages not read yet
        return false;
    }
}
