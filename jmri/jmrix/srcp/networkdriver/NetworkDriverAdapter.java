// NetworkDriverAdapter.java

package jmri.jmrix.srcp.networkdriver;

import jmri.jmrix.srcp.SRCPMessage;
import jmri.jmrix.srcp.SRCPPortController;
import jmri.jmrix.srcp.SRCPProgrammer;
import jmri.jmrix.srcp.SRCPProgrammerManager;
import jmri.jmrix.srcp.SRCPTrafficController;

import java.io.*;
import java.net.*;
import java.util.Vector;

/**
 * Implements SerialPortAdapter for the SRCP system network connection.
 * <P>This connects
 * an SRCP server (daemon) via a telnet connection.
 * Normally controlled by the NetworkDriverFrame class.
 *
 * @author	Bob Jacobsen   Copyright (C) 2001, 2002, 2003, 2008
 * @version	$Revision: 1.1 $
 */
public class NetworkDriverAdapter extends SRCPPortController {

    /**
     * set up all of the other objects to operate with an SRCP command
     * station connected to this port
     */
    public void configure() {
        // connect to the traffic controller
        SRCPTrafficController.instance().connectPort(this);

        jmri.InstanceManager.setProgrammerManager(
                new SRCPProgrammerManager(
                    new SRCPProgrammer()));

        jmri.InstanceManager.setPowerManager(new jmri.jmrix.srcp.SRCPPowerManager());

        jmri.InstanceManager.setTurnoutManager(new jmri.jmrix.srcp.SRCPTurnoutManager());

		jmri.InstanceManager.setThrottleManager(new jmri.jmrix.srcp.SRCPThrottleManager());

        // Create an instance of the consist manager.  Make sure this
        // happens AFTER the programmer manager to override the default   
        // consist manager.
        // jmri.InstanceManager.setConsistManager(new jmri.jmrix.srcp.SRCPConsistManager());

        jmri.InstanceManager.setCommandStation(new jmri.jmrix.srcp.SRCPCommandStation());

        // start the connection
        SRCPTrafficController.instance().sendSRCPMessage(new SRCPMessage("GO\n"), null);
        // mark OK for menus
        jmri.jmrix.srcp.ActiveFlag.setActive();
    }

    private Thread sinkThread;

    // base class methods for the SRCPPortController interface
    public DataInputStream getInputStream() {
        if (!opened) {
            log.error("getInputStream called before load(), stream not available");
        }
        try {
            return new DataInputStream(socket.getInputStream());
        } catch (java.io.IOException ex1) {
            log.error("Exception getting input stream: "+ex1);
            return null;
        }
    }

    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            opened = true;
        } catch (Exception e) {
            log.error("error opening SRCP network connection: "+e);
        }
    }

    public DataOutputStream getOutputStream() {
        if (!opened) log.error("getOutputStream called before load(), stream not available");
        try {
            return new DataOutputStream(socket.getOutputStream());
        }
     	catch (java.io.IOException e) {
            log.error("getOutputStream exception: "+e);
     	}
     	return null;
    }

    public boolean status() {return opened;}

    // private control members
    private boolean opened = false;

    static public NetworkDriverAdapter instance() {
        if (mInstance == null) mInstance = new NetworkDriverAdapter();
        return mInstance;
    }
    static NetworkDriverAdapter mInstance = null;

    Socket socket;

    public Vector getPortNames() {
        log.error("Unexpected call to getPortNames");
        return null;
    }
    public String openPort(String portName, String appName)  {
        log.error("Unexpected call to openPort");
        return null;
    }
    public String[] validBaudRates() {
        log.error("Unexpected call to validBaudRates");
        return null;
    }

    static org.apache.log4j.Category log = org.apache.log4j.Category.getInstance(NetworkDriverAdapter.class.getName());

}
