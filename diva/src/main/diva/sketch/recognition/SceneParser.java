/*
 * $Id: SceneParser.java,v 1.6 2000/08/04 01:43:59 michaels Exp $
 *
 * Copyright (c) 1998 The Regents of the University of California.
 * All rights reserved.  See the file COPYRIGHT for details.
 */
package diva.sketch.recognition;

import diva.util.xml.*;
import java.util.Iterator;
import java.io.FileReader;
import java.io.Reader;

/**
 * SceneParser parses an XML file representing a single interpretation
 * of a scene into a Scene data structure.  This interpretation can
 * then be used for testing purposes.  It currently has the limitation
 * that interprets all typed data as "SimpleData", because it doesn't
 * know how to handle complex data.
 *
 * @see SceneWriter
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.6 $
 * @rating Red
 */
public class SceneParser implements diva.util.ModelParser {
    /**
     * The public identity of the sketch dtd file.
     */
    public static final String PUBLIC_ID = "-//UC Berkeley//DTD scene 1//EN";

    /**
     * The URL where the DTD is stored.
     */
    public static final String DTD_URL = "http://www.gigascale.org/diva/dtd/scene.dtd";

    /**
     * The DTD for sketch files.
     */
    public static final String DTD_1 =
    "<!ELEMENT scene (sceneElement)> <!ELEMENT sceneElement (sceneElement+|strokeElement)> <!ATTLIST sceneElement confidence CDATA \"1.0\" name CDATA #REQUIRED type CDATA #REQUIRED> <!ELEMENT strokeElement EMPTY> <!ATTLIST strokeElement points CDATA #REQUIRED>";

    /**
     * Indicates the file contains a scene.
     */
    public static final String SCENE_TAG = "scene";
    
    /**
     * Indicates a scene element.
     */
    public static final String SCENE_ELEMENT_TAG = "sceneElement";

    /**
     * Indicates a stroke element.
     */
    public static final String STROKE_ELEMENT_TAG = "strokeElement";
    
    /**
     * Indicates the type of scene element.
     */
    public static final String TYPE_TAG = "type";

    /**
     * Indicates the name of scene element in a composite element.
     */
    public static final String NAME_TAG = "name";

    /**
     * Indicates the confidence of a scene element.
     */
    public static final String CONFIDENCE_TAG = "confidence";

    /**
     * Indicates the stroke path for a stroke element.
     */
    public static final String POINTS_TAG = "points";

    /**
     * Parse the input stream dictated by the given
     * reader intoa scene.
     */
    public Object parse(Reader in) throws java.lang.Exception  {
        XmlDocument doc = new XmlDocument();
        doc.setDTDPublicID(PUBLIC_ID);
        doc.setDTD(DTD_1);
        XmlReader reader = new XmlReader();
        reader.parse(doc, in);
        if(reader.getErrorCount() > 0) {
            throw new Exception("errors encountered during parsing");
        }
        XmlElement scene = doc.getRoot();
        if(!scene.getType().equals(SCENE_TAG)) {
            throw new Exception("no scene");
        }
        XmlElement sceneElt = (XmlElement)scene.elements().next();
        Scene db = new BasicScene();
        buildSceneElement(db, sceneElt);
        return db;
    }

    /**
     * Given a scene element represented by its parsed XML equivalent,
     * first build all of its children in the database, then build
     * it in the database.
     */
    private SceneElement buildSceneElement(Scene db, XmlElement eltXml) {
        if(eltXml.getType().equals(STROKE_ELEMENT_TAG)) {
            TimedStroke stroke = TrainingParser.parsePoints(eltXml.getAttribute(POINTS_TAG));
            return db.addStroke(stroke);
        }
        else {
            SceneElement[] children = new SceneElement[eltXml.elementCount()];
            int i = 0;
            for(Iterator cs = eltXml.elements(); cs.hasNext(); ) {
                children[i++] = buildSceneElement(db, (XmlElement)cs.next());
            }
            String type = eltXml.getAttribute(TYPE_TAG);
            String conf = eltXml.getAttribute(CONFIDENCE_TAG);
            double confidence;
            if(conf == null) {
                confidence = 1;
            }
            else {
                Double tmp = Double.valueOf(conf);
                confidence = tmp.doubleValue();
            }
            String[] names = childNames(eltXml);
            return db.addComposite(new SimpleData(type), confidence,
                    children, names);
        }
    }

    /**
     * Return the array of child names given an xml element
     * that represents a composite scene element.
     */
    private String[] childNames(XmlElement elt) {
        String[] out = new String[elt.elementCount()];
        int j = 0;
        for(Iterator i = elt.elements(); i.hasNext(); ) {
            XmlElement child = (XmlElement)i.next();
            out[j++] = child.getAttribute(NAME_TAG);
        }
        return out;
    }

    /**
     * Simple test of this class.
     */
    public static void main (String args[]) throws Exception {
        SceneParser demo = new SceneParser();
        if (args.length != 1) {
            System.err.println("java SceneParser <uri>");
            System.exit(1);
        } else {
            demo.parse(new FileReader(args[0]));
        }
    }
}
