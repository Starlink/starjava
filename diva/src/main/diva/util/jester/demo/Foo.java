/* Foo.java: a name path with bugs in it
 *
 * $Id: Foo.java,v 1.2 2000/05/02 00:45:31 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util.jester.demo;

/**
 * Foo: a name path with bugs in it
 *
 * @author John Reekie
 * @version $Revision: 1.2 $
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

