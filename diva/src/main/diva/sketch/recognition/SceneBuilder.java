/*
 * $Id: SceneBuilder.java,v 1.4 2001/08/27 22:16:41 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.recognition;

import diva.util.xml.*;
import java.util.List;
import java.util.Iterator;
import java.io.StringWriter;
import java.io.IOException;

/**
 * Builder class for scenes and scene elements and simple
 * data.  FIXME - more docs
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 * @rating Red
 */
public class SceneBuilder extends AbstractXmlBuilder {
    public static final String BUILDER_DECLS = "sceneBuilders";

    private Scene _db;

    /**
     * Indicates the file contains a scene.
     */
    public static final String SCENE_TAG = "scene";
    
    /**
     * Indicates a scene element.
     */
    public static final String COMPOSITE_ELEMENT_TAG = "compositeElement";

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
     * The name of the root element.
     */
    public static final String ROOT_NAME = "root";

    /**
     * Indicates the confidence of a scene element.
     */
    public static final String CONFIDENCE_TAG = "confidence";

    /**
     * Indicates the stroke path for a stroke element.
     */
    public static final String POINTS_TAG = "points";
    
    /**
     * "scene" => return a scene database
     * "compositeElement" => return a CompositeElement object
     * "strokeElement" => return a StrokeElement object
     */
    public Object build(XmlElement elt, String type) 
            throws Exception {
        if(isScene(type)) {
            return buildScene(elt, type);
        }
        else if(isComposite(type)) {
            return buildCompositeElement(elt, type);
        }
        else if(isStroke(type)) {
            return buildStrokeElement(elt, type);
        }
        String err = "Unknown type: " + type;
        throw new Exception(err);
    }

    private boolean isComposite(String type) {
        return type.equals(COMPOSITE_ELEMENT_TAG) ||
            type.equals("diva.sketch.recognition.BasicScene$CompositeElt");
    }

    private boolean isStroke(String type) {
        return type.equals(STROKE_ELEMENT_TAG) ||
            type.equals("diva.sketch.recognition.BasicScene$StrokeElt");
    }

    private boolean isScene(String type) {
        return type.equals(SCENE_TAG) ||
            type.equals("diva.sketch.recognition.BasicScene");
    }
    
    public Scene buildScene(XmlElement elt, String type)
            throws Exception {
        _db = new BasicScene();
        for(Iterator i = elt.elements(); i.hasNext(); ) {
            XmlElement child = (XmlElement)i.next();
            if(isComposite(child.getType())) {
                buildCompositeElement(child, child.getType());
            }
            else {
                String err = "Unknown type: " + child.getType();
                throw new Exception(err);
            }
        }
        return _db;
    }
    
    /**
     * Given a composite element represented by its parsed XML equivalent,
     * first build all of its children in the database, then build it
     * in the database.
     */
    public CompositeElement buildCompositeElement(XmlElement elt, String type)
            throws Exception {
        SceneElement[] children = new SceneElement[elt.elementCount()-1];
        int i = 0;
        TypedData data = null;
        for(Iterator cs = elt.elements(); cs.hasNext(); ) {
            XmlElement child = (XmlElement)cs.next();
            if(isStroke(child.getType())) {
                children[i++] = buildStrokeElement(child, child.getType());
            }
            else if(isComposite(child.getType())) {
                children[i++] = buildCompositeElement(child, child.getType());
            }
            else {
                data = (TypedData)getDelegate().build(child, child.getType());
            }
        }
        String conf = elt.getAttribute(CONFIDENCE_TAG);
        double confidence = (conf == null) ? 1 : Double.parseDouble(conf);
        String[] names = childNames(elt);
        return _db.addComposite(data, confidence, children, names);
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
            if(child.getType().equals(COMPOSITE_ELEMENT_TAG)) {
                out[j++] = child.getAttribute(NAME_TAG);
            }
        }
        return out;
    }
    
    /**
     * Build the given elemnt into a stroke elem
     */
    public StrokeElement buildStrokeElement(XmlElement elt, String type) {
        String points = elt.getAttribute(POINTS_TAG);
        TimedStroke stroke = SSTrainingParser.parsePoints(points);
        return _db.addStroke(stroke);
    }

    public XmlElement generate(Object in) throws Exception {
        if(in instanceof CompositeElement) {
            return generateComposite((CompositeElement)in, ROOT_NAME);
        }
        else if(in instanceof StrokeElement) {
            return generateStroke((StrokeElement)in);
        }
        String err = "Unknown object type: " + in;
        throw new Exception(err);
    }

    public XmlElement generateComposite(CompositeElement in, String name)
            throws Exception {
        XmlElement out = new XmlElement(COMPOSITE_ELEMENT_TAG);
        out.setAttribute(CONFIDENCE_TAG, Double.toString(in.getConfidence()));
        out.setAttribute(NAME_TAG, name);
        out.addElement(getDelegate().generate(in.getData()));
        List children = in.children();
        List childNames = in.childNames();
        for(int i = 0; i < children.size(); i++) {
            SceneElement child = (SceneElement)children.get(i);
            if(child instanceof StrokeElement) {
                out.addElement(generateStroke((StrokeElement)child));
            }
            else {
                out.addElement(generateComposite((CompositeElement)child,
                        (String)childNames.get(i)));
            }
        }
        return out;
    }

    public XmlElement generateStroke(StrokeElement in) throws IOException {
        XmlElement out = new XmlElement(STROKE_ELEMENT_TAG);
        StringWriter points = new StringWriter();
        SSTrainingWriter.writeStroke(in.getStroke(), points);
        out.setAttribute(POINTS_TAG, points.toString());
        return out;
    }
}

