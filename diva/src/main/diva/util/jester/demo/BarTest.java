/*
 * $Id: BarTest.java,v 1.3 2001/07/22 22:02:10 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util.jester.demo;
import diva.util.jester.*;

/**
 * A unit test suite for Bar.
 *
 * @author John Reekie      (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 */
public class BarTest extends TestSuite {

  // The unit factory
  private BarFactory _factory;

  /** Constructor
   */
  public BarTest (TestHarness harness, BarFactory factory) {
      setTestHarness(harness);
      setFactory(factory);
      this._factory = factory;
  }

  /** runSuite()
   */
  public void runSuite () {
    test1();
  }

  /** runAll()
   */
  public void runAll () {
    new FooTest(getTestHarness(),_factory).runSuite();
    runSuite();
  }

  ///////////////////////////////////////////////////////////////////
  //// Test methods

    public void test1 () {
        runTestCase(new BarTestCase("Bar-1") {
            public void run () throws Exception {
                f.append("foo");
                f.append("bar");
                f.up();
            }
            public void check () throws TestFailedException {
                check("foo");
            }
        });
    }

  ///////////////////////////////////////////////////////////////////
  //// main

  public static void main (String argv[]) {
    new BarTest(new TestHarness(), new BarFactory()).run();
  }

  //////////////////////////////////////////////////////////// 
  //// Test cases

  abstract class BarTestCase extends TestCase {
    public Bar f = (Bar) _factory.create();
    public BarTestCase (String name) {
      super(name);
    }
    public void check (String p) throws TestFailedException {
       if (!f.getPath().equals(p)) {
	 fail("!f.getPath.equals(\"" + p + "\") (" + f.getPath() + ")");
       }
    }
  }

  //////////////////////////////////////////////////////////// 
  //// Factories

  public static class BarFactory extends FooTest.FooFactory {
    public Object create() {
      return new Bar();
    }
  }
}



