/*
 * $Id: SketchParser.java,v 1.18 2002/08/12 06:36:58 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch;

import diva.sketch.recognition.TimedStroke;
import diva.sketch.recognition.SSTrainingParser;
import diva.util.ModelParser;
import diva.compat.xml.*;
import java.awt.Color;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;

/**
 * SketchParser parses a sketch file in XML format (.sk) and produces
 * a SketchModel data structure.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.18 $
 */
public class SketchParser implements ModelParser {
    /**
     * The public identity of the sketch dtd file.
     */
    public static final String PUBLIC_ID = "-//UC Berkeley//DTD sketch 1//EN";

    /**
     * The URL where the DTD is stored.
     */
    public static final String DTD_URL = "http://www.gigascale.org/diva/dtd/sketch.dtd";

    /**
     * The DTD for sketch files.
     */
    public static final String DTD_1 =
    "<!ELEMENT sketchModel (composite|stroke)+> <!ELEMENT composite (composite|stroke)+> <!ELEMENT stroke EMPTY> <!ATTLIST stroke outline CDATA \"0 0 0\" fill CDATA \"null\" linewidth CDATA #REQUIRED points CDATA #REQUIRED>";
    
    /**
     * Indicate a sketch model.
     */
    public static final String MODEL_TAG = "sketchModel";
    
    /**
     * Indicate a stroke symbol.
     */
    public static final String STROKE_TAG = "stroke";

    /**
     * Indicate a stroke element.
     */
    public static final String COMPOSITE_TAG = "composite";
    
    /**
     * Indicate the outline to draw the gesture with.
     */
    public static final String OUTLINE_TAG = "outline";

    /**
     * Indicate the fill to draw the gesture with.
     */
    public static final String FILL_TAG = "fill";

    /**
     * Indicate the line width to draw the gesture with.
     */
    public static final String LINEWIDTH_TAG = "linewidth";
    
    /**
     * Parse the input stream dictated by the given reader into a
     * sketch model.
     */
    public Object parse(Reader in) throws java.lang.Exception  {
        XmlDocument doc = new XmlDocument();
        doc.setDTDPublicID(PUBLIC_ID);
        doc.setDTD(DTD_1);
        XmlReader reader = new XmlReader();
	reader.setVerbose(true);
        reader.parse(doc, in);
        if(reader.getErrorCount() > 0) {
            throw new Exception("errors encountered during parsing");
        }
        XmlElement sketch = doc.getRoot();
        if(!sketch.getType().equals(MODEL_TAG)) {
            throw new Exception("no model");
        }
        SketchModel model = new SketchModel();
        for(Iterator i = sketch.elements(); i.hasNext(); ) {
            XmlElement symbolElt = (XmlElement)i.next();
            model.addSymbol(buildSymbol(symbolElt));
        }
        return model;
    }


    /**
     * Return the color object indicated by "val".
     */
    public static Color parseColor(String val){
        if(val.equalsIgnoreCase("null")) {
            return null;
        }
        
        float fval[] = new float[3];
        int beginIndex = 0;
        int endIndex = 0;
        String space = " ";
        String sub;

        endIndex = val.indexOf(space, beginIndex);
        sub = val.substring(beginIndex, endIndex);
        fval[0] = Float.valueOf(sub).floatValue();
        beginIndex = endIndex+1;

        endIndex = val.indexOf(space, beginIndex);
        sub = val.substring(beginIndex, endIndex);
        fval[1] = Float.valueOf(sub).floatValue();
        beginIndex = endIndex+1;

        sub = val.substring(beginIndex);
        fval[2] = Float.valueOf(sub).floatValue();
        return new Color(fval[0], fval[1], fval[2]);
    }

    /**
     * Given a symbol represented by its parsed XML equivalent, first
     * build all of its children symbols, then build it.
     */
    private Symbol buildSymbol(XmlElement eltXml) {
        if(eltXml.getType().equals(STROKE_TAG)) {
            String outlineStr = eltXml.getAttribute(OUTLINE_TAG);
            String fillStr = eltXml.getAttribute(FILL_TAG);
            String linewidthStr = eltXml.getAttribute(LINEWIDTH_TAG);
            String pointStr = eltXml.getAttribute(SSTrainingParser.POINTS_TAG);
            Color outline = parseColor(outlineStr);
            Color fill = parseColor(fillStr);
            float w = Float.valueOf(linewidthStr).floatValue();
            TimedStroke stroke = SSTrainingParser.parsePoints(pointStr);
            return new StrokeSymbol(stroke, outline, fill, w);
        }
        else {
            Symbol[] children = new Symbol[eltXml.elementCount()];
            int i = 0;
            for(Iterator cs = eltXml.elements(); cs.hasNext(); ) {
                children[i++] = buildSymbol((XmlElement)cs.next());
            }
            return new CompositeSymbol(children);
        }
    }
}


