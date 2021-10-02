package com;

import com.server.TCPOperations.Database;
import com.server.RMIOperations.RMITask;
import com.server.TCPOperations.SelectionTask;
import com.server.RMIOperations.RMICallbackServiceImpl;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Scanner;

/**
 * @author Davide Chen
 */
public class ServerMain {

    public static void main (String[] args) {

        System.out.println("WORTH (WORkTogetHer) Server");
        // Upload of all persistent data
        Database data;
        try {
            data = new Database();
        } catch (IOException e) {
            System.out.println("Data initialization failed. Stack Trace: ");
            e.printStackTrace();
            return;
        }

        // creation object for callback
        RMICallbackServiceImpl callbackService;
        try {
            callbackService = new RMICallbackServiceImpl();
        } catch (RemoteException e) {
            System.out.println("RMI callback service initialization failed. Stack Trace: ");
            e.printStackTrace();
            return;
        }

        // run task user registration
        RMITask registrationTask = new RMITask(data, callbackService);
        Thread userRegistration = new Thread(registrationTask);
        userRegistration.start();

        // TCP connection management
        SelectionTask selectionTask = new SelectionTask(data, callbackService);
        Thread tcpConnection = new Thread(selectionTask);
        tcpConnection.start();

        try {
            // sleep for 1s before proceeding
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String command = "";
        Scanner in = new Scanner(System.in);
        do {
            System.out.println(CommunicationProtocol.ANSI_RESET + "\nEnter \"exit\" to terminate:");
            System.out.print("> ");
            command = in.nextLine();
        } while(!command.equals("exit"));

        selectionTask.shutdownServer();

        try {
            registrationTask.unbindRegistry();
            userRegistration.join();
            tcpConnection.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Server Shutdown, bye!");
    }
}
