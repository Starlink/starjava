/*
 * $Id: All.java,v 1.5 2000/05/02 00:45:33 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util.test;
import diva.util.jester.*;

import java.awt.*;


/**
 * All the tests in this directory.
 *
 * @author John Reekie (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.5 $
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
        // Test Shape utilities 
        new ShapeUtilitiesTest(getTestHarness()).run();

        // Test XML
        new XMLElementTest(getTestHarness()).run();
        new XMLParserTest(getTestHarness()).run();
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



