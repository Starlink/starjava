/*
 * $Id: SceneClassBuilder.java,v 1.3 2001/07/22 22:01:51 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.rcl;

import diva.sketch.recognition.SceneRecognizer;
import diva.sketch.recognition.VotingSceneRecognizer;
import java.util.List;
import java.util.Map;
import java.util.Map;
import java.lang.reflect.Constructor;
import diva.util.xml.*;

/**
 * Build a scene recognizer specified by the given class name that
 * contains a single scene recognizer child.  Ignore the param and
 * config parameters--subclasses can special-case these.
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public class SceneClassBuilder extends AbstractXmlBuilder {
    /**
     * Build a scene recognizer or a stroke recognizer given
     * a list of child recognizers, a map of string (key,
     * value) attribute pairs, and a string cnofiguration.
     */
    public Object build(XmlElement elt, String type) 
            throws Exception {
        List children = elt.getChildList();
        if(children.size() > 1) {
            String err = "SimpleSceneBuilder: requires 1 child recognizer";
            throw new IllegalArgumentException(err);
        }
        Class c = Class.forName(type);
        Constructor[] cons = c.getConstructors();
        for(int i = 0; i < cons.length; i++) {
            Class[] sig = cons[i].getParameterTypes();
            if(sig.length == 1 && sig[0].equals(SceneRecognizer.class)) {
                Object[] args = new Object[1];
                XmlElement child = (XmlElement)children.get(0);
                args[0] = getDelegate().build(child, child.getType());
                System.out.println("args[0] = " + args[0]);
                return cons[i].newInstance(args);
            }
        }
        String err = "SimpleSceneBuilder: no valid constructors: " + type;
        throw new IllegalArgumentException(err);
    }
}

