/*
 * $Id: DocumentFactory.java,v 1.4 2000/05/02 00:44:34 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.gui;

import java.io.File;
import java.net.URL;
import java.util.Iterator;

/**
 * DocumentFactory is an factory interface that creates Document
 * objects. It is used by the Open action to create a new document in
 * response to user selection of a file or URL.
 *
 * @author John Reekie (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 */
public interface DocumentFactory {

    /** Create a new empty document.
     */
    public Document createDocument (Application app); 

    /** Create a new document based on the given URL.  Typically, this
     * method will parse the contents of the URL and create a Document
     * object containing the parsed form of those contents.
     */
    public Document createDocument (Application app, URL url); 

    /** Create a new document based on the given file path.
     * Typically, this method will parse the contents of the file and
     * create a Document object containing the parsed form of those
     * contents.
     */
    public Document createDocument (Application app, File file); 
}

