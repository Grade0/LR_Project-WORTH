package com.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utils.CommunicationProtocol;
import com.utils.UDPMessage;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

/**
 * @author Davide Chen
 *
 * Task to manage the reception of chat messages
 */
public class ProjectChatTask implements Runnable {

    private final MulticastSocket multicastSocket;
    private final InetAddress chatAddress;
    private final int port;
    private final LinkedList<UDPMessage> messages;
    volatile boolean finish = false;
    volatile boolean lastMessageIsRead = false;


    public ProjectChatTask(String address, int port) throws IOException {
        this.multicastSocket = new MulticastSocket(port);
        this.chatAddress = InetAddress.getByName(address);
        this.port = port;
        this.messages = new LinkedList<>();
    }

    @Override
    public void run() {
        DatagramPacket packet;
        byte[] buffer;

        try {
            buffer = new byte[1024];
            multicastSocket.joinGroup(this.chatAddress);
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

                    if (this.projectCancelled(udpMessage)) {
                        finish = true;
                    }

                    messages.add(udpMessage);
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
        return multicastSocket;
    }

    public InetAddress getChatAddress() {
        return chatAddress;
    }

    public int getPort() {
        return port;
    }

    public LinkedList<UDPMessage> getMessages() {
        return this.messages;
    }

    public boolean projectCancelled(UDPMessage message) {
        return message.isFromSystem() && message.getMessage().equals(CommunicationProtocol.UDP_TERMINATE_MSG);
    }

    public void terminate() {
        this.finish = true;
    }

    public boolean isTerminated() {

        if(!finish) return false;

        //if the project is cancelled
        //and the latest messages have not yet been read
        if(finish && !lastMessageIsRead) {
            lastMessageIsRead = true;
            return false;
        }

        return true;
    }
}
