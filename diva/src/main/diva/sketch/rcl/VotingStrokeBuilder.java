/*
 * $Id: VotingStrokeBuilder.java,v 1.3 2001/07/22 22:01:52 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.rcl;

import diva.sketch.recognition.StrokeRecognizer;
import diva.sketch.recognition.VotingStrokeRecognizer;
import java.util.List;
import java.util.Iterator;
import java.lang.reflect.Constructor;
import diva.util.xml.*;

/**
 * Build a voting stroke recognizer that votes among the
 * given list of children.
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public class VotingStrokeBuilder extends AbstractXmlBuilder {
    /** The n-highest attribute tag.
     */
    public static final String N_HIGHEST = "nHighest";

    /** The min-confidence attribute tag.
     */
    public static final String MIN_CONFIDENCE = "minConfidence";
    
    /**
     * Build a voting stroke recognizer that votes among the given
     * list of children.  If the children list is empty, throw an
     * IllegalArgumentException.
     */
    public Object build(XmlElement elt, String type)
            throws Exception {
        List children = elt.getChildList();
        if(children.size() == 0) {
            String err = "VotingStrokeBuilder: requires 1 child recognizer";
            throw new IllegalArgumentException(err);
        }
        StrokeRecognizer[] childArray =
            new StrokeRecognizer[children.size()];
        for(int i = 0; i < children.size(); i++) {
            XmlElement child = (XmlElement)children.get(i);
            childArray[i] = (StrokeRecognizer)
                (getDelegate().build(child,child.getType()));
        }
        VotingStrokeRecognizer out = new VotingStrokeRecognizer(childArray);
        String nHighest = elt.getAttribute(N_HIGHEST);
        if(nHighest != null) {
            out.setNHighest(Integer.parseInt(nHighest));
        }
        String minConfidence = elt.getAttribute(MIN_CONFIDENCE);
        if(minConfidence != null) {
            out.setMinConfidence(Double.parseDouble(minConfidence));
        }
        return out;
    }

    /** Generate an XML element for the given VotingStrokeRecognizer
     */
    public XmlElement generate(Object in) throws Exception {
        VotingStrokeRecognizer vsr = (VotingStrokeRecognizer)in;
        XmlElement out = new XmlElement(getClass().getName());
        out.setAttribute(N_HIGHEST, Integer.toString(vsr.getNHighest()));
        out.setAttribute(MIN_CONFIDENCE, Double.toString(vsr.getMinConfidence()));
        for(Iterator i = vsr.children().iterator(); i.hasNext(); ) {
            out.addElement(getDelegate().generate(i.next()));
        }
        return out;
    }
}

