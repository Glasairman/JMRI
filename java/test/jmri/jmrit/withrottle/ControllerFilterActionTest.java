package jmri.jmrit.withrottle;

import jmri.util.JUnitUtil;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test simple functioning of ControllerFilterAction
 *
 * @author	Paul Bender Copyright (C) 2016
 */
public class ControllerFilterActionTest extends TestCase {

    public void testCtor() {
        ControllerFilterAction panel = new ControllerFilterAction();
        Assert.assertNotNull("exists", panel );
    }

    // from here down is testing infrastructure
    public ControllerFilterActionTest(String s) {
        super(s);
    }

    // Main entry point
    static public void main(String[] args) {
        String[] testCaseName = {"-noloading", ControllerFilterActionTest.class.getName()};
        junit.swingui.TestRunner.main(testCaseName);
    }

    // test suite from all defined tests
    public static Test suite() {
        TestSuite suite = new TestSuite(ControllerFilterActionTest.class);
        return suite;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        apps.tests.Log4JFixture.setUp();
    }
    
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        apps.tests.Log4JFixture.tearDown();
    }
}
