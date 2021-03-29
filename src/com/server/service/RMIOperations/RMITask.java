package com.server.service.RMIOperations;

import com.utils.CommunicationProtocol;
import com.server.service.TCPOperations.LocalRegistration;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * @author Davide Chen
 *
 * Task for setting up and publishing the RMI Regitry
 * of the registration and callback services
 */
public class RMITask implements Runnable {
    private final LocalRegistration registration;
    private final RMICallbackServiceImpl callbackService;

    public RMITask(LocalRegistration registration, RMICallbackServiceImpl callbackService) {
        this.registration = registration;
        this.callbackService = callbackService;
    }

    @Override
    public void run() {
        try {
            // creation of the registry on the REG_PORT port
            Registry registry = LocateRegistry.createRegistry(CommunicationProtocol.REGISTRY_PORT);

            // publishing the callback stub in the registry
            registry.bind(CommunicationProtocol.CALLBACK_SERVICE_NAME, callbackService);
            System.out.println("Callback service is now available");

            // Initialization of the RMIRegistrationService
            RMIRegistrationService registrationService = new RMIRegistrationServiceImpl(registration, callbackService);

            // publish the registration stub in the registry
            registry.bind(CommunicationProtocol.REGISTRATION_SERVICE_NAME, registrationService);
            System.out.println("Registration service is now available");
        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        }
    }
}
