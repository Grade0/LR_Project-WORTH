package com.client;

import com.data.UserStatus;
import com.CommunicationProtocol;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

/**
 * @author Davide Chen
 *
 * Implementation of the client-side callback service
 */
public class RMICallbackNotifyImpl extends UnicastRemoteObject implements RMICallbackNotify {
    private final Map<String, UserStatus> userStatus;
    private final ProjectChatTask projectChats;

    public RMICallbackNotifyImpl(Map<String, UserStatus> userStatus,
                                 ProjectChatTask projectChats) throws RemoteException {
        super();
        this.userStatus = userStatus;
        this.projectChats = projectChats;
    }

    @Override
    public synchronized void notifyUserStatus(String username, UserStatus status) throws RemoteException {
        // If the map previously contained a mapping for the key,
        // the old value is replaced by the specified value.
        this.userStatus.put(username, status);
    }

    @Override
    public synchronized void notifyNewProject(String projectName, String chatAddress) throws RemoteException {
        try {
            this.projectChats.joinGroup(projectName, InetAddress.getByName(chatAddress));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(CommunicationProtocol.ANSI_YELLOW + "\nSystem ntf: You have been added to a new project: " + projectName);
        System.out.print(CommunicationProtocol.ANSI_RESET + "> ");

    }

    @Override
    public synchronized void notifyCloseClient() throws RemoteException {
        System.out.println(CommunicationProtocol.ANSI_RED + "\nSystem ntf: Server is offline, please close the application");
        System.out.print(CommunicationProtocol.ANSI_RESET + "> ");
    }

    @Override
    public synchronized void leaveMulticastGroup(String projectName) {
        projectChats.leaveGroup(projectName);
    }
}
