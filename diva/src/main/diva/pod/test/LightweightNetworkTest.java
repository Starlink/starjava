/*
 * $Id: LightweightNetworkTest.java,v 1.3 2002/02/05 23:17:16 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.pod.test;

import diva.util.*;
import diva.util.jester.*;
import diva.util.java2d.*;

import diva.pod.lwgraph.*;

import java.util.Iterator;

/**
 * A test suite for the LightweightNetwork class.
 *
 * @author John Reekie
 * @version $Revision: 1.3 $
 */
public class LightweightNetworkTest extends TestSuite {

    /** The factory for the LightweightNetwork class
     */
    public static class LightweightNetworkFactory implements LightweightGraphTest.GraphFactory {
        public LightweightGraph createGraph () {
            return new LightweightNetwork();
        }
    }

    /**
     * The unit factory
     */
    private LightweightGraphTest.GraphFactory factory;

    /** Constructor
     */
    public LightweightNetworkTest (TestHarness harness, LightweightGraphTest.GraphFactory factory) {
        setTestHarness(harness);
        setFactory(factory);
        this.factory = factory;
    }

    /** runAll()
     */
    public void runAll () {
	new LightweightGraphTest(getTestHarness(),new LightweightNetworkFactory()).runSuite();
	runSuite();
    }

    /**
     * runSuite()
     */
    public void runSuite () {
        testNodesEdges();
        testSuccPred();
        testInOut();
    }

    //////////////////////////////////////////////////////////// 
    ////  main

    /** Create a default test harness and run all tests on it.
     */
    public static void main (String argv[]) {
         new LightweightNetworkTest(new TestHarness(),
                new LightweightNetworkTest.LightweightNetworkFactory()).run();
    }

    ///////////////////////////////////////////////////////////////////
    //// Test methods


    /** Create test network 1 and check that the node and
     * edge iterators work correctly.
     */
    public void testNodesEdges () {
        runTestCase(new TestNetwork1("Test node, edge, and port iterators") {
            public void check () throws TestFailedException {
                startTimer();
                assertExpr(iterateNames(g.nodes()).equals("abc"), "Iterate nodes");
                assertExpr(iterateNames(g.ports(a)).equals("i"), "Iterate ports of a");
                assertExpr(iterateNames(g.ports(b)).equals("jkn"), "Iterate ports of b");
                assertExpr(iterateNames(g.ports(c)).equals("lm"), "Iterate ports of c");
                assertExpr(iterateNames(g.edges()).equals("pqr"), "Iterate edges");
                assertExpr(iterateNames(g.roots()).equals("a"), "Iterate roots");
                stopTimer();
            }
        });
    }

    /** Create test network 1 and check that successor and predecessor
     * nodes are iterated properly
     */
    public void testSuccPred () {
        runTestCase(new TestNetwork1("Test successor and predecessor iterators") {
            public void check () throws TestFailedException {
                assertExpr(iterateNames(g.successors(a)).equals("b"), "Successors of a");
                assertExpr(iterateNames(g.successors(b)).equals("c"), "Successors of b");
                assertExpr(iterateNames(g.successors(c)).equals("b"), "Successors of c");

                assertExpr(iterateNames(g.predecessors(a)).equals(""), "Predecessors of a");
                assertExpr(iterateNames(g.predecessors(b)).equals("ac"), "Predecessors of b");
                assertExpr(iterateNames(g.predecessors(c)).equals("b"), "Predecessors of c");
            }
        });
    }

    /** Create test network 1 and check that in and out edges
     * are iterated properly
     */
    public void testInOut () {
        runTestCase(new TestNetwork1("Test in and out edge iterators") {
            public void check () throws TestFailedException {
                assertExpr(iterateNames(g.outEdges(a)).equals("p"), "OutEdges of a");
                assertExpr(iterateNames(g.outEdges(b)).equals("q"), "OutEdges of b");
                assertExpr(iterateNames(g.outEdges(c)).equals("r"), "OutEdges of c");

                assertExpr(iterateNames(g.inEdges(a)).equals(""), "InEdges of a");
                assertExpr(iterateNames(g.inEdges(b)).equals("pr"), "InEdges of b");
                assertExpr(iterateNames(g.inEdges(c)).equals("q"), "InEdges of c");
            }
        });
    }


    //////////////////////////////////////////////////////////// 
    ////  Utility classes

    /** A test case containing the following test network, where the labels
     * are the 'name' attribute of the nodes, ports (in parens), and edges.
     *
     * <pre>
     * a (i) --p-> (j) b (k) --------+
     *                (n)            |
     *                 ^             | q
     *                 | r           |
     *                 |             |
     *                 +-(m) c (l) <-+
     * </pre>
     */
    public abstract class TestNetwork1 extends TestCase {
        LightweightNetwork g;
            
        BasicLWNode a = new BasicLWNode();
        BasicLWNode b = new BasicLWNode();
        BasicLWNode c = new BasicLWNode();

        BasicLWPort i = new BasicLWPort();
        BasicLWPort j = new BasicLWPort();
        BasicLWPort k = new BasicLWPort();
        BasicLWPort l = new BasicLWPort();
        BasicLWPort m = new BasicLWPort();
        BasicLWPort n = new BasicLWPort();

        BasicLWEdge p = new BasicLWEdge();
        BasicLWEdge q = new BasicLWEdge();
        BasicLWEdge r = new BasicLWEdge();

        public TestNetwork1 (String s) {
            super(s);
        }
        public void init () throws Exception {
            g = (LightweightNetwork) factory.createGraph();
        }
        public void run () throws Exception {
            a.setProperty("name", "a");
            b.setProperty("name", "b");
            c.setProperty("name", "c");
            
            g.addNode(a);
            g.addNode(b);
            g.addNode(c);

            i.setProperty("name", "i");
            j.setProperty("name", "j");
            k.setProperty("name", "k");
            l.setProperty("name", "l");
            m.setProperty("name", "m");
            n.setProperty("name", "n");

            g.addPort(a, i);
            g.addPort(b, j);
            g.addPort(b, k);
            g.addPort(c, l);
            g.addPort(c, m);
            g.addPort(b, n);

            p.setProperty("name", "p");
            q.setProperty("name", "q");
            r.setProperty("name", "r");

            g.addEdge(p);
            g.addEdge(q);
            g.addEdge(r);

            g.connect(p, i, j);
            g.connect(q, k, l);
            g.connect(r, m, n);
        }
    }

    //////////////////////////////////////////////////////////// 
    ////  Utility methods

    /** Given an iterator over PropertyContainers, return the
     * string consisting of the concatenated "name" attributes
     */
    public String iterateNames (Iterator i) {
        StringBuffer s = new StringBuffer("");
        while (i.hasNext()) {
            PropertyContainer pc = (PropertyContainer) i.next();
            s.append((String) pc.getProperty("name"));
        }
        return s.toString();
    }
}
