// FacelessApp.java

package apps;

import jmri.InstanceManager;

/**
 * A simple example of a "Faceless" (no gui) application
 *
 * <hr>
 * This file is part of JMRI.
 * <P>
 * JMRI is free software; you can redistribute it and/or modify it under 
 * the terms of version 2 of the GNU General Public License as published 
 * by the Free Software Foundation. See the "COPYING" file for a copy
 * of this license.
 * <P>
 * JMRI is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License 
 * for more details.
 * <P>
 * @author	Bob Jacobsen   Copyright 2003, 2005, 2007
 * @version     $Revision: 1.7 $
 */
public class FacelessApp {
	static String name = "Faceless App";

    // Main entry point
    public static void main(String args[]) {

        initLog4J();
        log.info(apps.Apps.startupInfo(name));
        
        new FacelessApp(args);   // start the application class itself

        log.debug("main initialization done");
        
        // You could put your own code here,
        // for example.  The layout connection
        // is working at this point.
    }

	/**
	 * Static method to return a standard program id.
	 * Used for logging startup, etc.
	 */
    static public String startupInfo(String program) {
        return (program+" version "+jmri.Version.name()
                +" starts under Java "+System.getProperty("java.version","<unknown>"));
    }

	/**
	 * Static method to get Log4J working before the
	 * rest of JMRI starts up.
	 */
    static protected void initLog4J() {
        // initialize log4j - from logging control file (lcf) only
        // if can find it!
        String logFile = "default.lcf";
        try {
            if (new java.io.File(logFile).canRead()) {
                org.apache.log4j.PropertyConfigurator.configure(logFile);
            } else {
                org.apache.log4j.BasicConfigurator.configure();
                org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.ERROR);
            }
        }
        catch (java.lang.NoSuchMethodError e) { log.error("Exception starting logging: "+e); }
    }

	/**
	 * Constructor starts the JMRI application running, and then
	 * returns.
	 */
    public FacelessApp(String[] args) {

        // Load from preference file, by default the DecoderPro
        // one so you can use DecoderPro to load the preferences values.
        //    setConfigFilename("DecoderProConfig2.xml", args);
        //    loadFile();

		// load directly via code
		codeConfig(args);
		
		// and here we're up and running!
		
    }


	protected void codeConfig(String[] args) {
		jmri.jmrix.SerialPortAdapter adapter =  jmri.jmrix.lenz.li100.LI100Adapter.instance();

		String portName = "/dev/cu.USA28X1b1P1.1";
		String baudRate = "9600";
		//String option1Setting = null;
		//String option2Setting = null;
		
		adapter.setPort(portName);
		adapter.configureBaudRate(baudRate);
		//if (option1Setting !=null) adapter.configureOption1(option1Setting);
		//if (option2Setting !=null) adapter.configureOption2(option2Setting);

		adapter.openPort(portName, "JMRI app");
        adapter.configure();

        InstanceManager.setConfigureManager(new jmri.configurexml.ConfigXmlManager());


	}
	

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(FacelessApp.class.getName());
}


