/* Bar.java: an extended name path without bugs in it
 *
 * $Id: Bar.java,v 1.3 2001/07/22 22:02:09 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util.jester.demo;

/**
 *  Bar: an extended name path without bugs in i
 *
 * @author John Reekie
 * @version $Revision: 1.3 $
 */
public class Bar extends Foo {

  public void up () {
    String p = path.toString();
    int i = p.lastIndexOf('/');
    path = new StringBuffer(p.substring(i-2));
  }

    public String getPath () {
        if (path == null) {
            return "./";
        } else {
            return path.toString();
        }
    }
}


