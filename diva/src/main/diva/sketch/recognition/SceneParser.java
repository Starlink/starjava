/*
 * $Id: SceneParser.java,v 1.9 2002/08/12 06:36:59 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.recognition;

import diva.resource.DefaultBundle;
import diva.util.xml.*;
import java.util.Iterator;
import java.io.*;

/**
 * SceneParser parses an XML file representing a single interpretation
 * of a scene into a Scene data structure.  This interpretation can
 * then be used for testing purposes.  It currently has the limitation
 * that interprets all typed data as "SimpleData", because it doesn't
 * know how to handle complex data.
 *
 * @see SceneWriter
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.9 $
 * @rating Red
 */
public class SceneParser implements diva.util.ModelParser {
    /** The composite builder that is used to build this.
     */
    private CompositeBuilder _compBuilder;

    /** Build a scene parser using the system-specified composite
     * builder for
     */
    public SceneParser() throws Exception {
        DefaultBundle resources = new DefaultBundle();
        _compBuilder = new CompositeBuilder();
        _compBuilder.addBuilderDecls(new InputStreamReader(resources.getResourceAsStream(SceneBuilder.BUILDER_DECLS)));
    }

    /** Build a scene parser that uses the given builder
     * declarations to parse its typed data.
     */
    public SceneParser(CompositeBuilder builder) {
        _compBuilder = builder;
    }

    /**
     * Parse the input stream dictated by the given
     * reader intoa scene.
     */
    public Object parse(Reader in) throws java.lang.Exception  {
        XmlDocument doc = new XmlDocument();
        XmlReader reader = new XmlReader();
        reader.parse(doc, in);
        if(reader.getErrorCount() > 0) {
            throw new Exception("errors encountered during parsing");
        }
        return _compBuilder.build(doc.getRoot(), doc.getRoot().getType());
    }

    /**
     * Simple test of this class.
     */
    public static void main (String args[]) throws Exception {
        /* FIXME
        SceneParser demo = new SceneParser();
        if (args.length != 1) {
            System.err.println("java SceneParser <uri>");
            System.exit(1);
        } else {
            demo.parse(new FileReader(args[0]));
        }
        */
    }
}
