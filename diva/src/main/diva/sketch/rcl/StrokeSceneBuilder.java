/*
 * $Id: StrokeSceneBuilder.java,v 1.2 2001/07/22 22:01:51 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.rcl;

import diva.util.xml.*;
import diva.sketch.recognition.StrokeRecognizer;
import diva.sketch.recognition.StrokeSceneRecognizer;
import java.util.List;

/**
 * Build and generate a StrokeSceneRecognizer to and
 * from XML.
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating Red
 */
public class StrokeSceneBuilder extends AbstractXmlBuilder {
    /**
     * Build a StrokeSceneRecognizer from XML.
     */
    public Object build(XmlElement elt, String type)
            throws Exception {
        List children = elt.getChildList();
        if(children.size() == 0 || children.size() > 1) {
            String err = "VotingStrokeBuilder: requires 1 child recognizer";
            throw new IllegalArgumentException(err);
        }
        XmlElement child = (XmlElement)children.get(0);
        StrokeRecognizer sr = (StrokeRecognizer)
            getDelegate().build(child,child.getType());
        return new StrokeSceneRecognizer(sr);
    }

    /**
     * Generate a StrokeSceneRecognizer to XML.
     */
    public XmlElement generate(Object in) throws Exception {
        StrokeSceneRecognizer ssr = (StrokeSceneRecognizer)in;
        XmlElement out = new XmlElement(ssr.getClass().getName());
        out.addElement(getDelegate().generate(ssr.getStrokeRecognzer()));
        return out;
    }
}

