/*
 * $Id: DashedPathData.java,v 1.3 2001/07/22 22:01:57 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.recognition.Type;
import diva.sketch.recognition.TypedData;
import diva.util.xml.AbstractXmlBuilder;
import diva.util.xml.XmlElement;
import diva.util.java2d.Polyline2D;

/**
 * A typed data that holds a recognized dashed path
 * which consists of a Polyline2D path, an average
 * dash length, and an average gap length.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public class DashedPathData extends AbstractXmlBuilder {
    public static final String DASHED_PATH_DATA = "DashedPathData";
    public static final String SEGMENT_LENGTH = "segmentLegth";
    public static final String GAP_LENGTH = "gapLegth";
    public static final String PATH = "path";

    /**
     * The static type associated with this typed data.
     */
    public static final Type type = Type.makeType(DashedPathData.class);
    
    /**
     * The approximate path that the dashed line follows.
     */
    private Polyline2D _path;

    /** The average segment length.
     */
    private double _segLen;

    /** The average gap length.
     */
    private double _gapLen;

    public DashedPathData() {
    }
    
    /**
     * Construct a text data that contains an empty string.
     */
    public DashedPathData(Polyline2D path, double segLen, double gapLen) {
        _path = path;
        _segLen = segLen;
        _gapLen = gapLen;
    }
    
    /**
     * Return the type of this data, implementing the TypedData
     * interface.  Returns the static type <i>DashedPathData.type</i>.
     */
    public Type getType() {
        return DashedPathData.type;
    }
	
    /**
     * Return the polyline path that the dashed path follows.
     */
    public Polyline2D getPath() {
        return _path;
    }

    /**
     * Set the polyline path that the dashed path follows.
     */
    public void setPath(Polyline2D path) {
        _path = path;
    }

    /**
     * Return the average segment length of the path.
     */
    public double getSegmentLength() {
        return _segLen;
    }

    /**
     * Set the average segment length of the path.
     */
    public void setSegmentLength(double segLen) {
        _segLen = segLen;
    }

    /**
     * Return the average gap length of the path.
     */
    public double getGapLength() {
        return _gapLen;
    }

    /**
     * Set the average gap length of the path.
     */
    public void setGapLength(double gapLen) {
        _gapLen = gapLen;
    }
    
    /**
     * Equality test: are the paths, segment lengths, and
     * gap lengths identical?
     */
    public boolean equals(Object o) {
        if(o instanceof DashedPathData) {
            DashedPathData dpd = (DashedPathData)o;
            return (_segLen == dpd._segLen &&
                    _gapLen == dpd._gapLen &&
                    _path.equals(dpd._path));
        }
        return false;
    }

    public Object build(XmlElement elt, String type) {
        _segLen = Double.parseDouble(elt.getAttribute(SEGMENT_LENGTH));
        _gapLen = Double.parseDouble(elt.getAttribute(GAP_LENGTH));
        //FIXME        _path = Double.parsePath(elt.getAttribute(PATH));
        return this;
    }

    public XmlElement generate(Object in) {
        DashedPathData dpd = (DashedPathData)in;
        XmlElement out = new XmlElement(DASHED_PATH_DATA);
        out.setAttribute(SEGMENT_LENGTH, Double.toString(dpd.getSegmentLength()));
        out.setAttribute(GAP_LENGTH, Double.toString(dpd.getGapLength()));
        //FIXME  out.setAttribute(PATH, Double.toString(dpd.getPath()));
        return out;
    }
	
    /**
     * Return a string representation of this data for debugging.
     */
    public String toString() {
        return "DashedPathData[" + _path + ", " + _segLen + ", "
            + _gapLen + "]";
    }
}

