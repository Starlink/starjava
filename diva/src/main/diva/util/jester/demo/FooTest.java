/*
 * $Id: FooTest.java,v 1.3 2001/07/22 22:02:10 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util.jester.demo;
import diva.util.jester.*;

/**
 * A unit test suite for Foo.
 *
 * @author John Reekie      (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 */
public class FooTest extends TestSuite {

  /** The unit factory
   */
  private FooFactory _factory;

  /** Constructor
   */
  public FooTest (TestHarness harness, FooFactory factory) {
    setTestHarness(harness);
    setFactory(factory);
    _factory = factory;
  }

  /** runSuite()
   */
  public void runSuite () {
    test1();
    test2();
    test3();
    test4();
  }

  ///////////////////////////////////////////////////////////////////
  //// Test methods

  /** Comment this
   */
  public void test1 () {
    runTestCase(new FooTestCase("Foo-1") {
      public void run () throws Exception {
	//
      }
      public void check () throws TestFailedException {
	check("");
      }
    });
  }

  /** Comment this
   */
  public void test2 () {
    runTestCase(new FooTestCase("Foo-2") {
      public void run () throws Exception {
	f.append("foo");
      }
     public void check () throws TestFailedException {
       check("foo");
     }
    });
  }

  /** Comment this
   */
  public void test3 () {
    runTestCase(new FooTestCase("Foo-3") {
      public void run () throws Exception {
	f.append("foo");
	f.append("bar");
      }
     public void check () throws TestFailedException {
       check("foo/bar");
     }
    });
  }

  /** Comment this
   */
  public void test4 () {
    runTestCase(new FooTestCase("Foo-4") {
      public void run () throws Exception {
	f.append("foo");
	f.append("");
      }
     public void check () throws TestFailedException {
       check("foo");
     }
    });
  }

  ///////////////////////////////////////////////////////////////////
  //// main

  public static void main (String argv[]) {
    new FooTest(new TestHarness(), new FooFactory()).run();
  }

  //////////////////////////////////////////////////////////// 
  //// Test cases

  abstract class FooTestCase extends TestCase {
    public Foo f = (Foo) _factory.create();
    public FooTestCase (String name) {
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

  public static class FooFactory {
    public Object create() {
      return new Foo();
    }
  }
}


