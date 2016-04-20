package jmri.util.exceptionhandler;

import jmri.util.JUnitAppender;
import jmri.util.SwingTestCase;
import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * Tests for the jmri.util.UncaughtExceptionHandler class.
 *
 * @author	Bob Jacobsen Copyright 2010
 */
public class UncaughtExceptionHandlerTest extends SwingTestCase {

    private Thread.UncaughtExceptionHandler defaultExceptionHandler;
    
    public void testThread() throws Exception {
        Thread t = new Thread() {
            public void run() {
                // The deref has to be on line 22 or test will fail
                deref(null);
            }

            void deref(Object o) {
                // The NPE has to be on line 27 or test will fail
                o.toString();
            }
        };

        t.start();
        jmri.util.JUnitUtil.releaseThread(this);
        JUnitAppender.assertErrorMessage("Uncaught Exception caught by jmri.util.exceptionhandler.UncaughtExceptionHandler");
    }

    boolean caught = false;
    public void testSwing() throws Exception {
        Runnable r = new Runnable() {
            public void run() {
                deref(null);
            }

            void deref(Object o) {
                o.toString();
            }
        };
        try {
            javax.swing.SwingUtilities.invokeAndWait(r);
        } catch (java.lang.reflect.InvocationTargetException e) {
            caught = true;
        }
        jmri.util.JUnitUtil.waitFor(()->{return caught;}, "threw exception");
        // emits no logging, as the UncaughtExceptionHandlerTest handler isn't invoked
    }

    // from here down is testing infrastructure
    public UncaughtExceptionHandlerTest(String s) {
        super(s);
    }

    // Main entry point
    static public void main(String[] args) {
        String[] testCaseName = {UncaughtExceptionHandlerTest.class.getName()};
        junit.swingui.TestRunner.main(testCaseName);
    }

    // test suite from all defined tests
    public static Test suite() {
        TestSuite suite = new TestSuite(UncaughtExceptionHandlerTest.class);
        return suite;
    }

    // The minimal setup for log4J
    protected void setUp() throws Exception {
        apps.tests.Log4JFixture.setUp();
        this.defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        Thread.setDefaultUncaughtExceptionHandler(this.defaultExceptionHandler);
        apps.tests.Log4JFixture.tearDown();
    }
}
