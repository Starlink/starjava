/*
 * $Id: MSTrainingParser.java,v 1.2 2001/10/31 00:46:35 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.sketch.recognition;

import diva.util.ModelParser;
import diva.util.aelfred.*;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;

/**
 * MSTrainingParser (Multi-Stroke Training Parser) reads in an XML
 * file and parses it into a MSTrainingModel.  The XML file should
 * conform to multiStrokeTrain.dtd format so that it can be parsed correctly.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating Red
 */
public class MSTrainingParser extends HandlerBase implements ModelParser {
    /**
     * The public identity of the sketch dtd file.
     */
    public static final String PUBLIC_ID = "-//UC Berkeley//DTD train 1//EN";

    /**
     * The URL where the DTD is stored.
     */
    public static final String DTD_URL = "http://www.gigascale.org/diva/dtd/multiStrokeTrain.dtd";

    /**
     * The DTD for sketch files.
     */
    public static final String DTD_1 =
    "<!ELEMENT MSTrainingModel (type+)> <!ELEMENT type (example+)> <!ATTLIST type name CDATA #REQUIRED> <!ELEMENT example (stroke+)> <!ATTLIST example label CDATA #REQUIRED numStrokes CDATA #REQUIRED> <!ELEMENT stroke EMPTY> <!ATTLIST stroke points CDATA #REQUIRED>";
    
    /**
     * Indicate that the file contains a training model.
     */
    public static final String MODEL_TAG = "MSTrainingModel";
    
    /**
     * Indicate the version of this training model.
     */
    public static final String VERSION_TAG = "version";

    /**
     * Indicate a type in the training model.
     */
    public static final String TYPE_TAG = "type";
    
    /**
     * Indicate the name of a type.
     */
    public static final String NAME_TAG = "name";

    /**
     * Indicate an example.
     */
    public static final String EXAMPLE_TAG = "example";

    /**
     * Indicate the label (positive or negative) for an example.
     */
    public static final String LABEL_TAG = "label";

    /**
     * Indicate the number of strokes in an example.
     */
    public static final String NUM_STROKE_TAG = "numStrokes";

    /**
     * Indicate a set of points in a stroke.
     */
    public static final String POINTS_TAG = "points";

    /**
     * Indicate a stroke path.
     */
    public static final String STROKE_TAG = "stroke";
    
    /**
     * The model that we are accumulating during the
     * parse.
     */
    private MSTrainingModel _model;

    /**
     * Collect the strokes of an example.  Set to null when an example
     * has finished.
     */
    private TimedStroke _strokes[];

    /**
     * Index into _strokes[].  This gets incremented when a stroke has
     * been set and is set to 0 at the beginning of an example.
     */
    private int _index;
    
    /**
     * The parser driver.
     */
    private XmlParser _parser;

    /**
     * Keeps the attributes and their values.
     */
    private HashMap _currentAttributes = new HashMap();
 
    /**
     * Handle an attribute value assignment.
     * @see com.microstar.xml.XmlHandler#attribute
     */
    public void attribute(String name, String value, boolean isSpecified) {
        _currentAttributes.put(name, value);
    }

    public void startElement(String name){
        if(name.equalsIgnoreCase(EXAMPLE_TAG)) {
            String numStrokes = (String)_currentAttributes.get(NUM_STROKE_TAG);
            int num = Integer.parseInt(numStrokes);
            _strokes = new TimedStroke[num];
            _index = 0;
        }
    }
    
    /**
     * Handle the end of an element.
     * @see com.microstar.xml.XmlHandler#endElement
     */
    public void endElement(String name) {
        if(name.equalsIgnoreCase(EXAMPLE_TAG)) {
            String labelStr = (String)_currentAttributes.get(LABEL_TAG);
            String type = (String)_currentAttributes.get(NAME_TAG);
            if(_strokes.length != 0){
                if(labelStr.equals("+")) {
                    _model.addPositiveExample(type, _strokes);
                }
                else {
                    _model.addNegativeExample(type, _strokes);
                }
            }
            _strokes = null;
            _index = 0;
        }
        else if(name.equalsIgnoreCase(STROKE_TAG)){
            String pointStr =
                (String)_currentAttributes.get(POINTS_TAG);
            TimedStroke stroke = SSTrainingParser.parsePoints(pointStr);
            _strokes[_index++] = stroke;
        }
    }
    
    /**
     * Create the full path string for the url and parses the file
     * into a MSTrainingModel object.
     */
    public Object parse(Reader reader) throws java.lang.Exception  {
        _model = new MSTrainingModel();

        // create the parser
        _parser = new XmlParser();
        _parser.setHandler(this);
        _parser.parse(null, null, reader);
        return _model;
    }

    /**
     * Resolve an external entity.  If the first argument is the name
     * of the MoML PUBLIC DTD ("-//UC Berkeley//DTD train 1//EN"),
     * then return a StringReader that will read the locally cached
     * version of this DTD (the public variable DTD_1). Otherwise,
     * return null, which has the effect of deferring to &AElig;lfred
     * for resolution of the URI.  Derived classes may return a a
     * modified URI (a string), an InputStream, or a Reader.  In the
     * latter two cases, the input character stream is provided.
     *
     * @param publicId The public identifier, or null if none was supplied.
     * @param systemId The system identifier.
     * @return Null, indicating to use the default system identifier.
     */
    public Object resolveEntity(String publicID, String systemID) {
        if (publicID != null &&
                publicID.equals(PUBLIC_ID)) {
            // This is the generic MoML DTD.
            return new StringReader(DTD_1);
        } else {
            return null;
        }
    }
}

