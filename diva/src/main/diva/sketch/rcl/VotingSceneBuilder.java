/*
 * $Id: VotingSceneBuilder.java,v 1.3 2001/07/22 22:01:52 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.rcl;

import diva.sketch.recognition.SceneRecognizer;
import diva.sketch.recognition.VotingSceneRecognizer;
import java.util.List;
import java.util.Iterator;
import java.lang.reflect.Constructor;
import diva.util.xml.*;

/**
 * Build a voting scene recognizer that votes among the children of
 * the given element.
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public class VotingSceneBuilder extends AbstractXmlBuilder {
    /** The n-highest attribute tag.
     */
    public static final String N_HIGHEST = "nHighest";

    /** The min-confidence attribute tag.
     */
    public static final String MIN_CONFIDENCE = "minConfidence";
    
    /**
     * Build a voting scene recognizer that votes among the children
     * of the given element.  If the given element has no children,
     * throw an IllegalArgumentException.
     */
    public Object build(XmlElement elt, String type)
            throws Exception {
        List children = elt.getChildList();
        if(children.size() == 0) {
            String err = "VotingSceneBuilder: requires 1 child recognizer";
            throw new IllegalArgumentException(err);
        }
        SceneRecognizer[] childArray =
            new SceneRecognizer[children.size()];
        for(int i = 0; i < children.size(); i++) {
            XmlElement child = (XmlElement)children.get(i);
            childArray[i] = (SceneRecognizer)
                (getDelegate().build(child,child.getType()));
        }
        VotingSceneRecognizer out = new VotingSceneRecognizer(childArray);
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

    /** Generate an XML element for the given VotingSceneRecognizer
     */
    public XmlElement generate(Object in) throws Exception {
        VotingSceneRecognizer vsr = (VotingSceneRecognizer)in;
        XmlElement out = new XmlElement(getClass().getName());
        out.setAttribute(N_HIGHEST, Integer.toString(vsr.getNHighest()));
        out.setAttribute(MIN_CONFIDENCE, Double.toString(vsr.getMinConfidence()));
        for(Iterator i = vsr.children().iterator(); i.hasNext(); ) {
            out.addElement(getDelegate().generate(i.next()));
        }
        return out;
    }
}

