package com.utils;

import com.exceptions.NoSuchPortException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Davide Chen
 *
 * Port manager for multicast addresses
 * Generates port from 30000 to 65535
 */
public abstract class PortManager {
    private static int port = 30000;
    private static final int MAX_PORT = 65535;
    private static final List<Integer> freePorts = new ArrayList<>();

    public synchronized static int getPort() throws NoSuchPortException {
        if (!freePorts.isEmpty()) {
            return freePorts.remove(0);
        }
        if (port == MAX_PORT)
            throw new NoSuchPortException();
        int toReturn = port;
        port++;
        return toReturn;
    }

    public synchronized static void freePort(int port) {
        if (port < MAX_PORT)
            freePorts.add(port);
    }

}