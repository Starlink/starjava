/*
 * $Id: LightweightGraphTest.java,v 1.4 2002/02/05 23:16:49 johnr Exp $
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
 * A test suite for the LightweightGraph class.
 *
 * @author John Reekie
 * @version $Revision: 1.4 $
 */
public class LightweightGraphTest extends TestSuite {

    /** The graph factory interface
     */
    public interface GraphFactory {
        public LightweightGraph createGraph ();
    }       

    /** The factory for the LightweightGraph class
     */
    public static class LightweightGraphFactory implements GraphFactory {
        public LightweightGraph createGraph () {
            return new LightweightGraph();
        }
    }

    /**
     * The unit factory
     */
    private GraphFactory factory;

    /** Constructor
     */
    public LightweightGraphTest (TestHarness harness, GraphFactory factory) {
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
	testBig();
        testNodesEdges();
        testSuccPred();
        testInOut();
        testHierarchy();
    }

    //////////////////////////////////////////////////////////// 
    ////  main

    /** Create a default test harness and run all tests on it.
     */
    public static void main (String argv[]) {
         new LightweightGraphTest(new TestHarness(),
                new LightweightGraphTest.LightweightGraphFactory()).run();
    }

    ///////////////////////////////////////////////////////////////////
    //// Test methods

    /** Perform tests on an empty graph
     */
    public void testEmpty () {
        runTestCase(new TestCase("Empty graph") {
            LightweightGraph g;

            public void init () throws Exception {
                g = factory.createGraph();
            }
            public void run () throws Exception {
                ;
            }
            public void check () throws TestFailedException {
                assertExpr(g.getEdgeCount() == 0, "Edge count != 0");
                assertExpr(g.getNodeCount() == 0, "Node count != 0");
            }
        });
    }

    /** Test a star-connected graph
     */
    public void testStarConnected () {
        runTestCase(new TestCase("Star-connected from single node") {
            LightweightGraph g;

            public void init () throws Exception {
		startTimer();
                g = factory.createGraph();
            }
            public void run () throws Exception {
		LWNode root = new BasicLWNode();
		g.addNode(root);

                for (int i = 1; i < 32; i++) {
                    LWNode n = new BasicLWNode();
		    g.addNode(n);
		    LWEdge e = new BasicLWEdge();
                    g.addEdge(e);
		    g.connect(e, root, n);
                }
		stopTimer();
            }
            public void check () throws TestFailedException {
		assertExpr(g.getNodeCount() == 32, "Node count != 32");
		assertExpr(g.getEdgeCount() == 31, "Edge count != 31");
            }
        });
    }

    /** Test a large (64 knode) graph
     */
    public void testBig () {
        runTestCase(new TestCase("Test 64 knode graph") {
            LightweightGraph g;

            public void init () throws Exception {
		startTimer();
                g = factory.createGraph();
            }
            public void run () throws Exception {
		LWNode root = new BasicLWNode();
		g.addNode(root);

                for (int i = 1; i < 65536; i++) {
                    BasicLWNode n = new BasicLWNode();
		    g.addNode(n);
		    BasicLWEdge e = new BasicLWEdge();
                    g.addEdge(e);
		    int s = i / 2;
		    g.connect(e, g.getNode(s), n);
                }
		stopTimer();
            }
            public void check () throws TestFailedException {
                assertExpr(g.getEdgeCount() == 65535, "Edge count != 65535");
                assertExpr(g.getNodeCount() == 65536, "Node count != 65536");
            }
        });
    }

    /** Create test graph 1 and check that the node and
     * edge iterators work correctly.
     */
    public void testNodesEdges () {
        runTestCase(new TestGraph1("Test node and edge iterators") {
            public void check () throws TestFailedException {
                assertExpr(iterateNames(g.nodes()).equals("abcde"), "Iterate nodes");
                assertExpr(iterateNames(g.edges()).equals("pqrstu"), "Iterate edges");
                assertExpr(iterateNames(g.roots()).equals("ab"), "Iterate roots");
            }
        });
    }

    /** Create test graph 1 and check that successor and predecessor
     * nodes are iterated properly
     */
    public void testSuccPred () {
        runTestCase(new TestGraph1("Test successor and predecessor iterators") {
            public void check () throws TestFailedException {
                assertExpr(iterateNames(g.successors(a)).equals("c"), "Successors of a");
                assertExpr(iterateNames(g.successors(b)).equals("c"), "Successors of b");
                assertExpr(iterateNames(g.successors(c)).equals("de"), "Successors of c");
                assertExpr(iterateNames(g.successors(d)).equals("e"), "Successors of d");
                assertExpr(iterateNames(g.successors(e)).equals("d"), "Successors of e");

                assertExpr(iterateNames(g.predecessors(a)).equals(""), "Predecessors of a");
                assertExpr(iterateNames(g.predecessors(b)).equals(""), "Predecessors of b");
                assertExpr(iterateNames(g.predecessors(c)).equals("ab"), "Predecessors of c");
                assertExpr(iterateNames(g.predecessors(d)).equals("ce"), "Predecessors of d");
                assertExpr(iterateNames(g.predecessors(e)).equals("cd"), "Predecessors of e");
            }
        });
    }

    /** Create test graph 1 and check that in and out edges
     * are iterated properly
     */
    public void testInOut () {
        runTestCase(new TestGraph1("Test in and out edge iterators") {
            public void check () throws TestFailedException {
                assertExpr(iterateNames(g.outEdges(a)).equals("p"), "OutEdges of a");
                assertExpr(iterateNames(g.outEdges(b)).equals("q"), "OutEdges of b");
                assertExpr(iterateNames(g.outEdges(c)).equals("rs"), "OutEdges of c");
                assertExpr(iterateNames(g.outEdges(d)).equals("t"), "OutEdges of d");
                assertExpr(iterateNames(g.outEdges(e)).equals("u"), "OutEdges of e");

                assertExpr(iterateNames(g.inEdges(a)).equals(""), "InEdges of a");
                assertExpr(iterateNames(g.inEdges(b)).equals(""), "InEdges of b");
                assertExpr(iterateNames(g.inEdges(c)).equals("pq"), "InEdges of c");
                assertExpr(iterateNames(g.inEdges(d)).equals("ru"), "InEdges of d");
                assertExpr(iterateNames(g.inEdges(e)).equals("st"), "InEdges of e");
            }
        });
    }

    /** Create test graph r and check properties of a hierarchical graph
     */
    public void testHierarchy () {
        runTestCase(new TestGraph2("Test hierarchical graph") {
            public void check () throws TestFailedException {
                assertExpr(iterateNames(g.nodes()).equals("abcdN"), "Iterate nodes");
                assertExpr(iterateNames(g.edges()).equals("pqrst"), "Iterate edges");
                assertExpr(iterateNames(g.roots()).equals("aN"), "Iterate roots");
                assertExpr(iterateNames(g.successors(N)).equals("d"), "Successors of N");
                assertExpr(iterateNames(g.predecessors(N)).equals(""), "Predecessors of N");
                assertExpr(iterateNames(g.nodes(N)).equals("bc"), "Children of N");
             }
        });
    }


    //////////////////////////////////////////////////////////// 
    ////  Utility classes

    /** A test case containing the following test graph, where the labels
     * are the 'name' attribute of the nodes and edges:
     *
     * <pre>
     * a --p-> c ---r--> d <-+
     *         ^\        |   |
     *         | \       |t  |u
     * b --q---+  \      V   |
     *             +-s-> e --+
     * </pre>
     */
    public abstract class TestGraph1 extends TestCase {
        LightweightGraph g;
            
        BasicLWNode a = new BasicLWNode();
        BasicLWNode b = new BasicLWNode();
        BasicLWNode c = new BasicLWNode();
        BasicLWNode d = new BasicLWNode();
        BasicLWNode e = new BasicLWNode();

        BasicLWEdge p = new BasicLWEdge();
        BasicLWEdge q = new BasicLWEdge();
        BasicLWEdge r = new BasicLWEdge();
        BasicLWEdge s = new BasicLWEdge();
        BasicLWEdge t = new BasicLWEdge();
        BasicLWEdge u = new BasicLWEdge();

        public TestGraph1 (String s) {
            super(s);
        }
        public void init () throws Exception {
            g = factory.createGraph();
        }
        public void run () throws Exception {
            a.setProperty("name", "a");
            b.setProperty("name", "b");
            c.setProperty("name", "c");
            d.setProperty("name", "d");
            e.setProperty("name", "e");
            
            g.addNode(a);
            g.addNode(b);
            g.addNode(c);
            g.addNode(d);
            g.addNode(e);
            
            p.setProperty("name", "p");
            q.setProperty("name", "q");
            r.setProperty("name", "r");
            s.setProperty("name", "s");
            t.setProperty("name", "t");
            u.setProperty("name", "u");

            g.addEdge(p);
            g.addEdge(q);
            g.addEdge(r);
            g.addEdge(s);
            g.addEdge(t);
            g.addEdge(u);

            g.connect(p, a, c);
            g.connect(q, b, c);
            g.connect(r, c, d);
            g.connect(s, c, e);
            g.connect(t, d, e);
            g.connect(u, e, d);
        }
    }

    /** A test case containing the following test graph, where the labels
     * are the 'name' attribute of the nodes and edges and "N" is a parent node.
     *
     * <pre>
     *          N
     *         +--------------+
     * a --p-> | b ---q--> c  |
     *         | ^         |  +-+
     *         +-+---------+--+ |
     *           |s        |r   |t
     *           |         v    |
     *           +-------+ d <--+
     * </pre>
     */
    public abstract class TestGraph2 extends TestCase {
        LightweightGraph g;
            
        BasicLWNode a = new BasicLWNode();
        BasicLWNode b = new BasicLWNode();
        BasicLWNode c = new BasicLWNode();
        BasicLWNode d = new BasicLWNode();
        BasicLWNode N = new BasicLWNode();

        BasicLWEdge p = new BasicLWEdge();
        BasicLWEdge q = new BasicLWEdge();
        BasicLWEdge r = new BasicLWEdge();
        BasicLWEdge s = new BasicLWEdge();
        BasicLWEdge t = new BasicLWEdge();

        public TestGraph2 (String s) {
            super(s);
        }
        public void init () throws Exception {
            g = factory.createGraph();
        }
        public void run () throws Exception {
            a.setProperty("name", "a");
            b.setProperty("name", "b");
            c.setProperty("name", "c");
            d.setProperty("name", "d");
            N.setProperty("name", "N");
            
            g.addNode(a);
            g.addNode(b);
            g.addNode(c);
            g.addNode(d);
            g.addNode(N);

            g.setParent(b, N);
            g.setParent(c, N);

            p.setProperty("name", "p");
            q.setProperty("name", "q");
            r.setProperty("name", "r");
            s.setProperty("name", "s");
            t.setProperty("name", "t");

            g.addEdge(p);
            g.addEdge(q);
            g.addEdge(r);
            g.addEdge(s);
            g.addEdge(t);

            g.connect(p, a, b);
            g.connect(q, b, c);
            g.connect(r, c, d);
            g.connect(s, d, b);
            g.connect(t, N, d);
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
