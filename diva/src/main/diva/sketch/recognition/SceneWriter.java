/*
 * $Id: SceneWriter.java,v 1.10 2001/07/22 22:01:54 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.recognition;

import diva.util.xml.*;
import java.io.*;
import java.util.Iterator;
import diva.resource.DefaultBundle;


/**
 * SceneWriter writes a single interpretation of a scene to an output
 * stream.  This interpretation can then be read in by a SceneParser
 * for testing purposes.  It currently has the limitation that it
 * can only write interpretations that have "SimpleData" interpretations
 * of the scene, because it doesn't know how to handle complex data.
 *
 * @see SceneParser
 * @author Michael Shilman      (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.10 $
 * @rating Red
 */
public class SceneWriter {
    CompositeBuilder _compBuilder;

    /** Build a scene writer using the system-specified composite
     * builder for
     */
    public SceneWriter() throws Exception {
        DefaultBundle resources = new DefaultBundle();
        _compBuilder = new CompositeBuilder();
        _compBuilder.addBuilderDecls(new InputStreamReader(resources.getResourceAsStream(SceneBuilder.BUILDER_DECLS)));
    }
    
    /** Build a scene writer using the given composite builder for
     * mapping types to builders.
     */
    public SceneWriter(CompositeBuilder builder) {
        _compBuilder = builder;
    }
    
    /**
     * Write the single interpretation of the scene given rooted by
     * the given root to the character-output stream.  The caller is
     * responsible for closing the stream.  Throw a runtime exception
     * if it encounters typed data that is not of the type
     * SimpleData.
     */
    public void write(Scene db, SceneElement root, Writer out)
            throws Exception {
        XmlDocument doc = new XmlDocument();
        XmlWriter writer = new XmlWriter();
        XmlElement scene = new XmlElement(SceneBuilder.SCENE_TAG);
        scene.addElement(_compBuilder.generate(root));
        doc.setRoot(scene);
        writer.write(doc, out);
    }
}

