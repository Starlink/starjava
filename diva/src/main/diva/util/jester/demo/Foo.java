/* Foo.java: a name path with bugs in it
 *
 * $Id: Foo.java,v 1.3 2001/07/22 22:02:10 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util.jester.demo;

/**
 * Foo: a name path with bugs in it
 *
 * @author John Reekie
 * @version $Revision: 1.3 $
 */
public class Foo {
    StringBuffer path;

    public void append (String element) {
        if (path == null) {
            path = new StringBuffer();
        } else {
            path.append("/");
        }
        path.append(element);
    }

    public String getPath () {
        return path.toString();
    }
}


