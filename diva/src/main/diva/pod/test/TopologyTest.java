/*
 * $Id: TopologyTest.java,v 1.4 2002/01/16 01:27:31 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.pod.test;
import diva.util.jester.*;
import diva.util.java2d.*;

import diva.pod.lwgraph.*;

/**
 * A test suite for the primitive Topology and Traversal classes.
 *
 * @author John Reekie
 * @version $Revision: 1.4 $
 */
public class TopologyTest extends TestSuite {

    /** The topology factory interface
     */
    public interface ICFactory {
        public Topology createTopology ();
        public Traversal createTraversal (Topology ic);
    }

    /** The basic factory for the Topology and Traversal classes
     */
    public static class TopologyFactory implements ICFactory {
        public Topology createTopology () {
            return new Topology();
        }
        public Traversal createTraversal (Topology ic) {
            return new Traversal(ic);
        }
    }

    /**
     * The unit factory
     */
    private TopologyFactory factory;

    /** Constructor
     */
    public TopologyTest (TestHarness harness, TopologyFactory factory) {
        setTestHarness(harness);
        setFactory(factory);
        this.factory = factory;
    }

    /**
     * runSuite()
     */
    public void runSuite () {
        testEmpty();
        testStarConnected();
        testReverseStarConnected();
        testDeletion();
        testBig();
    }

    //////////////////////////////////////////////////////////// 
    ////  main

    /** Create a default test harness and run all tests on it.
     */
    public static void main (String argv[]) {
         new TopologyTest(new TestHarness(),
                new TopologyTest.TopologyFactory()).run();
    }

    ///////////////////////////////////////////////////////////////////
    //// Test methods

    /** Perform tests on empty graph....
     */
    public void testEmpty () {
        runTestCase(new TestCase("Empty topology") {
            Topology ic;
            Traversal tv;

            public void init () throws Exception {
                ic = factory.createTopology();
                tv = factory.createTraversal(ic);
            }
            public void run () throws Exception {
                ;
            }
            public void check () throws TestFailedException {
                assertExpr(ic.getMaxEdgeId() == 0, "Max edge id != 0");
                assertExpr(ic.getMaxNodeId() == 0, "Max node id != 0");
                assertExpr(tv.getRootCount() == 0, "Root count != 0");
           }
        });
    }

    public void testStarConnected () {
        runTestCase(new TestCase("Star-connected from single node") {
            Topology ic;
            Traversal tv;

            public void init () throws Exception {
		startTimer();
                ic = factory.createTopology();
            }
            public void run () throws Exception {
                for (int i = 1, j = 0; i < 32; i++, j++) {
                    ic.connect(j, 0, i);
                }
                tv = factory.createTraversal(ic);
		stopTimer();
            }
            public void check () throws TestFailedException {
                assertExpr(ic.getMaxEdgeId() == 30, "Max edge id != 30");
                assertExpr(ic.getMaxNodeId() == 31, "Max node id != 31");

                assertExpr(tv.getEdgeCount(0) == 31, "Out edge count != 31");
                assertExpr(tv.getEdgeCount(1) == 0, "Out edge count != 0");
                assertExpr(tv.getNodeCount() == 32, "Node count != 32");
                assertExpr(tv.getRootCount() == 1, "Root count != 1");
            }
        });
    }

    public void testReverseStarConnected () {
        runTestCase(new TestCase("Star-connected and reversed") {
            Topology ic;
            Traversal tv;

            public void init () throws Exception {
		startTimer();
                ic = factory.createTopology();
            }
            public void run () throws Exception {
                for (int i = 1, j = 0; i < 32; i++, j++) {
                    ic.connect(j, 0, i);
                }
                ic.reverse();
                tv = factory.createTraversal(ic);
		stopTimer();
            }
            public void check () throws TestFailedException {
                assertExpr(ic.getMaxEdgeId() == 30, "Max edge id != 30");
                assertExpr(ic.getMaxNodeId() == 31, "Max node id != 31");

                assertExpr(tv.getEdgeCount(0) == 0, "Edge count != 0");
                assertExpr(tv.getEdgeCount(1) == 1, "Edge count != 1");
                assertExpr(tv.getNodeCount() == 32, "Node count != 32");
                assertExpr(tv.getRootCount() == 31, "Root count != 31");
            }
        });
    }

    public void testDeletion () {
        runTestCase(new TestCase("Star-connected with edges deleted") {
            Topology ic;
            Traversal tv;

            public void init () throws Exception {
                ic = factory.createTopology();
            }
            public void run () throws Exception {
                for (int i = 1, j = 0; i < 32; i++, j++) {
                    ic.connect(j, 0, i);
                }
                ic.removeEdge(0);
                ic.removeEdge(15);
                ic.removeEdge(30);
                tv = factory.createTraversal(ic);
            }
            public void check () throws TestFailedException {
                assertExpr(ic.getMaxEdgeId() == 30, "Max edge id != 30");
                assertExpr(ic.getMaxNodeId() == 31, "Max node id != 31");

                assertExpr(tv.getEdgeCount(0) == 28, "Out edge count != 28");
                assertExpr(tv.getEdgeCount(1) == 0, "Out edge count != 0");
                assertExpr(tv.getNodeCount() == 29, "Node count != 29");
                assertExpr(tv.getRootCount() == 1, "Root count != 1");
            }
        });
    }

    /** Test a large (64 knode) graph
     */
    public void testBig () {
        runTestCase(new TestCase("Test 64 knode graph") {
            Topology ic;
            Traversal tv;

            public void init () throws Exception {
		startTimer();
                ic = factory.createTopology();
            }
            public void run () throws Exception {
                for (int i = 1, j = 0; i < 65536; i++, j++) {
                    ic.connect(j, i / 2, i);
                }
                tv = factory.createTraversal(ic);
		stopTimer();
            }
            public void check () throws TestFailedException {
                assertExpr(tv.getNodeCount() == 65536, "Node count != 65536");

		// FIXME FIXME
                // assertExpr(tv.getRootCount() == 1, "Root count != 1");
            }
        });
    }
}
