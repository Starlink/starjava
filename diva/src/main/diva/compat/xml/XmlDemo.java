/*
 * $Id: XmlDemo.java,v 1.1 2002/05/26 07:39:59 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.compat.xml;

import java.net.URL;
import java.io.*;
import java.util.*;

/**
 * A simple program that you can run to illustrate the operation
 * of the files in this package.
 *
 * @author John Reekie (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 */
public class XmlDemo {

    public static void main (String argv[]) {
        File file = new File("xml1.xml");
        XmlReader reader;
        XmlWriter writer;
        XmlDocument document;
        String xmlout;

        // Construct a document and parse it
        System.out.println("We are going to parse from " + file + ".");
        System.out.println("The parser will be set to verbose mode\n");

        document = new XmlDocument(file);
        reader = new XmlReader();
        reader.setVerbose(true);
        try {
            reader.parse(document);
        } catch (Exception e) {
            e.printStackTrace();
        }
        int errors = reader.getErrorCount();
        int warnings = reader.getWarningCount();

        System.err.println("Completed: " + errors + " errors, " + warnings + " warnings");
        if (errors > 0) {
            System.err.println("Unrecoverable errors in XML. Stop.");
            return;
        }

        // Now print it
        System.out.println("\nHaving parsed the document, we will print it out.");
        System.out.println("Here is the DTD:\n");

        writer = new XmlWriter();
        Writer out = new OutputStreamWriter(System.out);
        try {
            writer.writeDTD(document, out);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("\nHere is the XML:\n");
        try {
            writer.write(document, out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


