/*
 * $Id: JSketchSuite.java,v 1.4 2001/10/04 20:17:45 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.test;
import diva.sketch.*;
import diva.util.jester.*;
import diva.sketch.demo.SketchDemo;
import java.awt.event.InputEvent;
import java.io.FileReader;

/**
 * Suite of tests for the sketch demo.
 *
 * <ul>
 *
 * <li> Load a sketch, save it,
 * and compare the files to make sure they are identical.  Tests
 * saving, loading, and the integrity of the sketch model.
 *
 * <li> Record a set of mouse events.  Instantiate a sketch window,
 * but don't show it.  Send the mouse events to the window.  Check
 * that the model contains the correct stroke.
 *
 * </ul>
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 * @rating Red
 */
public class JSketchSuite extends TestSuite {
    /** 
     */
    public JSketchSuite (TestHarness harness) {
        setTestHarness(harness);
    }

    /**
     * Test load & save, and test sketch, save, and load.
     */
    public void runSuite () {
        testDraw();
    }

    public void testLoadSave() {
        //FIXME - add file equals utility function to
        //        jester package
    }

    /** Instantiate a JSketch, read mouse events from a
     * file, play the events into the window, and check
     * to make sure that the model has the correct number
     * of strokes.
     */
    public void testDraw() {
        runTestCase(new TestCase("Simple drawing") {
                JSketch _sketch = new JSketch();
                public void run() throws Exception {
                    //read mouse events from file
                    EventParser parser = new EventParser(_sketch);
                    EventPlayer player = new EventPlayer(_sketch);
                    InputEvent[] stream =
                        parser.parseEvents(new FileReader("simpleSketch.xml"));
                    player.play(stream);
                }
                public void check() throws TestFailedException {
                    SketchController bsc = _sketch.getSketchPane().getSketchController();
                    int nsymbols = bsc.getSketchModel().getSymbolCount();
                    assertExpr(nsymbols == 3,
                            "Expected 3 symbols, got " + nsymbols);
                }
            });
    }

    public void testSaveLoad() {
    }
}


