/*
 * $Id: All.java,v 1.2 2001/07/22 22:01:55 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.test;

import diva.util.jester.*;
import java.awt.*;


/**
 * All the tests in this directory.
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating Red
 */
public class All extends TestSuite {
    /** Use the given test harness to run the tests.
     */
    public All (TestHarness harness) {
        setTestHarness(harness);
    }

    /**
     * Run each of the tests.
     */
    public void runSuite () {
        // Test sketch demo for sketching and saving and loading
        new JSketchSuite(getTestHarness()).run();
    }

    /** Create a default test harness and
     * run all tests on it.
     */
    public static void main (String argv[]) {
        new All(new TestHarness()).run();
        System.exit(0);
    }
}




