/*
 * $Id: TextAnnotations.java,v 1.2 2001/07/22 22:01:59 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.recognition.TypedData;
import diva.sketch.recognition.Type;
import diva.util.xml.XmlElement;
import diva.util.xml.AbstractXmlBuilder;
import java.util.Set;
import java.util.HashSet;

/**
 * An abstract typed data that holds a recognized text string and
 * annotations about the geometry of the text including the character
 * height and width.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating Red
 */
public abstract class TextAnnotations extends AbstractXmlBuilder
    implements TypedData {

    public static final String CHAR_HEIGHT = "charHeight";
    public static final String CHAR_WIDTH = "charWidth";
    private double _charHeight;
    private double _charWidth;
    public TextAnnotations() {
    }
    public TextAnnotations(double charHeight, double charWidth) {
        _charHeight = charHeight;
        _charWidth = charWidth;
    }
    public XmlElement generate(Object in) throws Exception {
        TextAnnotations ta = (TextAnnotations)in;
        XmlElement out = new XmlElement(in.getClass().getName());
        out.setAttribute(CHAR_WIDTH, Double.toString(ta.getCharWidth()));
        out.setAttribute(CHAR_HEIGHT, Double.toString(ta.getCharHeight()));
        return out;
    }
    public Object build(XmlElement elt, String type) throws Exception {
        Class c = this.getClass().forName(type);
        TextAnnotations ta = (TextAnnotations)c.newInstance();
        ta.setCharWidth(Double.parseDouble(elt.getAttribute(CHAR_WIDTH)));
        ta.setCharHeight(Double.parseDouble(elt.getAttribute(CHAR_HEIGHT)));
        return ta;
    }
    public double getCharHeight() {
        return _charHeight;
    }
    public double getCharWidth() {
        return _charWidth;
    }
    public void setCharHeight(double charHeight) {
        _charHeight = charHeight; 
    }
    public void setCharWidth(double charWidth) {
        _charWidth = charWidth;
    }
    public boolean equals(Object o) {
        if(o instanceof TextAnnotations) {
            TextAnnotations ta = (TextAnnotations)o;
            return (_charHeight == ta._charHeight &&
                    _charWidth == ta._charWidth);
        }
        return false;
    }
    public String toString() {
        return getClass().getName() + "[charHeight = " + _charHeight + 
		  ", charWidth = " + _charWidth + "]";
    }
}

