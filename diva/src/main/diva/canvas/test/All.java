/*
 * $Id: All.java,v 1.11 2000/05/02 00:43:36 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.canvas.test;
import diva.util.jester.*;

import java.awt.*;


/**
 * All the tests in this directory.
 *
 * @author John Reekie (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.11 $
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
        // Test concrete figures
        new ConcreteFigures(getTestHarness()).run();

        // Canvas tests
        new JCanvasTest(
                getTestHarness(),
                new JCanvasTest.CanvasFactory()).run();
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



