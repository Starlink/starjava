/*
 * $Id: XmlUtilities.java,v 1.2 2001/07/22 22:02:14 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.util.xml;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.*;
import java.util.*;

/**
 * A collection of utility methods for XML-related
 * operations.
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 */
public class XmlUtilities {
    /**
     * Given a file name in the current working directory,
     * complete it and turn it into a URL.
     */
    public static final String makeAbsoluteURL(String url) throws MalformedURLException {
        URL baseURL;
        System.out.println("orig url: " + url);
        
        String currentDirectory = System.getProperty("user.dir");

        String fileSep = System.getProperty("file.separator");
        String file = currentDirectory.replace(fileSep.charAt(0), '/') + '/';
        if (file.charAt(0) != '/') {
            file = "/" + file;
        }
        System.out.println("new url: " + file);
        baseURL = new URL("file", null, file);
        return new URL(baseURL,url).toString();
    }
}


