/*
 * $Id: SSTrainingParser.java,v 1.1 2001/08/27 22:16:40 hwawen Exp $
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
 * SSTrainingParser (Single Stroke Training Parser) reads in an XML
 * file and parses it into a SSTrainingModel.  The XML file should
 * conform to singleStrokeTrain.dtd format so that it can be parsed correctly.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 * @rating Red
 */
public class SSTrainingParser extends HandlerBase implements ModelParser {
    /**
     * The public identity of the sketch dtd file.
     */
    public static final String PUBLIC_ID = "-//UC Berkeley//DTD train 1//EN";

    /**
     * The URL where the DTD is stored.
     */
    public static final String DTD_URL = "http://www.gigascale.org/diva/dtd/singleStrokeTrain.dtd";

    /**
     * The DTD for sketch files.
     */
    public static final String DTD_1 =
    "<!ELEMENT SSTrainingModel (type+)> <!ELEMENT type (example+)> <!ATTLIST type name CDATA #REQUIRED> <!ELEMENT example EMPTY> <!ATTLIST example label CDATA #REQUIRED points CDATA #REQUIRED>";

    /**
     * Indicate that the file contains a training model.
     */
    public static final String MODEL_TAG = "SSTrainingModel";
    
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
     * Indicate a set of points in a stroke.
     */
    public static final String POINTS_TAG = "points";

    /**
     * The model that we are accumulating during the
     * parse.
     */
    private SSTrainingModel _model;

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
       
    /**
     * Handle the end of an element.
     * @see com.microstar.xml.XmlHandler#endElement
     */
    public void endElement(String name) {
        if(name.equalsIgnoreCase(EXAMPLE_TAG)) {        
            String labelStr = (String)_currentAttributes.get(LABEL_TAG);
            String type = (String)_currentAttributes.get(NAME_TAG);
            String pointStr =
                (String)_currentAttributes.get(POINTS_TAG);
            TimedStroke stroke = parsePoints(pointStr);
            if(labelStr.equals("+")) {
                _model.addPositiveExample(type, stroke);
            }
            else {
                _model.addNegativeExample(type, stroke);
            }
        }
    }
    
    /**
     * Parses the training files into one training model and return
     * the model.  The classes in the training files are combined.
     * For exmple, if the "circle" class appears in several files, the
     * examples for the "circle" class from these files are combined.
     */
    public SSTrainingModel parse(Reader[] readers) throws java.lang.Exception  {
        SSTrainingModel models[] = new SSTrainingModel[readers.length];
        for(int i=0; i< readers.length; i++){
            models[i] = (SSTrainingModel)parse(readers[i]);
        }
        SSTrainingModel combinedModel = new SSTrainingModel();
        for(int i=0; i< models.length; i++){
            for(Iterator types = models[i].types(); types.hasNext();){
                String type = (String)types.next();
                for(Iterator examples = models[i].positiveExamples(type);
                    examples.hasNext();){
                    TimedStroke stroke = (TimedStroke)examples.next();
                    combinedModel.addPositiveExample(type, stroke);
                }
                for(Iterator examples = models[i].negativeExamples(type);
                    examples.hasNext();){
                    TimedStroke stroke = (TimedStroke)examples.next();
                    combinedModel.addNegativeExample(type, stroke);
                }
            }
        }
        return combinedModel;
    }
    
    /**
     * Create the full path string for the url and parses the file
     * into a SSTrainingModel object.
     */
    public Object parse(Reader reader) throws java.lang.Exception  {
        _model = new SSTrainingModel();

        // create the parser
        _parser = new XmlParser();
        _parser.setHandler(this);
        _parser.parse(null, null, reader);
        return _model;
    }

    /**
     * val is a stream of numbers representing the points in a pen
     * stroke.  The format of each point is <x, y, timestamp>.
     */
    public static TimedStroke parsePoints(String val){
        int beginIndex = 0;
        int endIndex = 0;
        int length = val.length();
        String space = " ";
        String sub, err;
        double x, y;
        long timestamp;
        TimedStroke stroke = new TimedStroke();
        val = val.concat(space);
        while(beginIndex < (length-1)){
            endIndex = val.indexOf(space, beginIndex);
            if(endIndex == -1){
                err = "Error: expecting x coordinate value.";
                throw new RuntimeException(err);
            }
            sub = val.substring(beginIndex, endIndex);
            x = Double.valueOf(sub).doubleValue();
            beginIndex = endIndex+1;

            endIndex = val.indexOf(space, beginIndex);
            if(endIndex == -1){
                err = "Error: expecting y coordinate value.";
                throw new RuntimeException(err);
            }
            sub = val.substring(beginIndex, endIndex);
            y = Double.valueOf(sub).doubleValue();
            beginIndex = endIndex+1;

            endIndex = val.indexOf(space, beginIndex);
            if(endIndex == -1){
                err = "Error: expecting timestamp value.";
                throw new RuntimeException(err);
            }
            sub = val.substring(beginIndex, endIndex);
            timestamp = Long.valueOf(sub).longValue();
            beginIndex = endIndex+1;
            stroke.addVertex((float)x, (float)y, timestamp);
        }
        return stroke;
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

