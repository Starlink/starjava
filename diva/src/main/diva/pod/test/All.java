/*
 * $Id: All.java,v 1.4 2002/01/15 05:06:23 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.pod.test;
import diva.util.jester.*;

/**
 * All the tests in this directory.
 *
 * @author John Reekie (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 */
public class All extends TestSuite {

    /** Constructor
     */
    public All (TestHarness harness) {
        setTestHarness(harness);
    }

    /**
     * runSuite()
     */
    public void runSuite () {
       // Test topology
        new TopologyTest(getTestHarness(),
                new TopologyTest.TopologyFactory()).run();

        // Test lightweight graph
        new LightweightGraphTest(getTestHarness(),
                new LightweightGraphTest.LightweightGraphFactory()).run();

        // Test lightweight network
        new LightweightNetworkTest(getTestHarness(),
                new LightweightNetworkTest.LightweightNetworkFactory()).run();
    }

    //////////////////////////////////////////////////////////// 
    ////  main

    /** Create a default test harness and
     * run all tests on it.
     */
    public static void main (String argv[]) {
        new All(new TestHarness()).run();
    }
}
