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
 */
public class ProjectChatTask implements Runnable {

    private final MulticastSocket multicastSocket;
    private final InetAddress chatAddress;
    private final LinkedList<UDPMessage> messages;
    volatile boolean finish = false;


    public ProjectChatTask(MulticastSocket socket, String address) throws UnknownHostException {
        this.multicastSocket = socket;
        this.chatAddress = InetAddress.getByName(address);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean projectCancelled(UDPMessage message) {
        return message.isFromSystem() && message.getMessage().equals(CommunicationProtocol.UDP_TERMINATE_MSG);
    }

    public LinkedList<UDPMessage> getMessages() {
        return this.messages;
    }

    public void terminate() {
        this.finish = true;
    }
}
