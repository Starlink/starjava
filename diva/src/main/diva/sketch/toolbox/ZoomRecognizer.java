/*
 * $Id: ZoomRecognizer.java,v 1.8 2001/08/28 06:37:13 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;

import diva.util.xml.AbstractXmlBuilder;
import diva.util.xml.XmlElement;

import diva.sketch.recognition.Type;
import diva.sketch.recognition.TypedData;
import diva.sketch.recognition.StrokeRecognizer;
import diva.sketch.recognition.Recognition;
import diva.sketch.recognition.RecognitionSet;
import diva.sketch.recognition.TimedStroke;

/**
 * A recognizer that recognizes "zoom" gestures ("Z") and
 * then zooms in if the the events are "down" and out if the
 * events are "up".
 *
 * ZoomRecognizer calls upon a ClassifyingRecognizer to recognize "Z".
 * Since ClassifyingRecognizer reports results by dispatching events
 * to its listeners, we use a local classification listener in the
 * ZoomRecognizer to receive the events and report back to the
 * ZoomRecognizer.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @author  Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.8 $
 * @rating Red
 */
public class ZoomRecognizer extends ModedIncrRecognizer {
    public final static String LETTER_Z = "z";
    private static int NUM_PTS_THRESH = 10;
    private static double _recognitionRate = 90;
    private StrokeRecognizer _recognizer;

    /**
     * The index of the coordinate at which the recognition
     * occurred, so that scaling can be relative to *that* point.
     */
    private int _recognizedIndex = 0;
    
    /**
     * A scaling factor for zoom events.
     */
    private double _zoomFactor;

    /**
     * Construct a zoom recognizer with a default scaling factor of
     * 1.0 and a classifying recognizer which has been trained on "Z"
     * shapes.
     */
    public ZoomRecognizer(StrokeRecognizer r) {
        this(1.0, r);
    }

    /**
     * Construct a zoom recognizer with the given scaling factor and a
     * classifying recognizer which has been trained on "Z" shapes..
     *
     * @see #setZoomFactor(double)
     */
    public ZoomRecognizer(double zoomFactor, StrokeRecognizer r) {
        setZoomFactor(zoomFactor);
        _recognizer = r;
    }

    /**
     * Return the zoom scaling factor for this recognizer.
     *
     * @see #setZoomFactor(double)
     */
    public double getZoomFactor() {
        return _zoomFactor;
    }

    /**
     * Emit zoom events based on whether the mouse has
     * gone up or down.
     */
    public RecognitionSet processActionStroke(TimedStroke s) {
        int numPts = s.getVertexCount();
        if(numPts > 1) {
            double dy = s.getY(numPts-1) - s.getY(_recognizedIndex);
            double zoom = getZoomFactor()*Math.pow(2, -dy/100.0);
            ZoomData type = new ZoomData(s.getX(0), s.getY(0), zoom);
            Recognition []r = { new Recognition(type, 100), };
            RecognitionSet rset = new RecognitionSet(r);
            return rset;
        }
        return RecognitionSet.NO_RECOGNITION;
    }

    /**
     * Perform the recognition of the "Z".
     */
    public int recognizeActionSignal(TimedStroke s){
        int numPts = s.getVertexCount();
        if(numPts > NUM_PTS_THRESH){
            RecognitionSet rset = _recognizer.strokeModified(s);
            Recognition r = rset.getBestRecognition();
            if(r != null) {
                if(r.getType().getID().equals(LETTER_Z) &&
                        (r.getConfidence() > _recognitionRate)){
                    _recognizedIndex = numPts-1;                    
                    return ACTION;
                }
            }
        }
        return UNKNOWN;
    }

    /**
     * Set the zoom scaling factor for this recognizer. XXX
     */
    public void setZoomFactor(double zoomFactor) {
        _zoomFactor = zoomFactor;
    }

    /**
     * A classification type for zooming.
     */
    public static class ZoomData extends AbstractXmlBuilder implements TypedData {
        public static final String ZOOM_AMOUNT = "zoomAmount";
        public static final String CENTER_X = "centerX";
        public static final String CENTER_Y = "centerY";

        /**
         * The string id for this classification type.
         */
        public static final Type type = Type.makeType(ZoomData.class);

        /**
         * How much to zoom.
         */
        private double _zoom;

        /**
         * The X coordinate center of the zoom.
         */
        private double _cx;

        /**
         * The Y coordinate center of the zoom.
         */
        private double _cy;

        /**
         * Contruct a zoom type object with the zoom amount
         * given.
         */
        public ZoomData(double cx, double cy, double zoom) {
            _zoom = zoom;
            _cx = cx;
            _cy = cy;
        }

	public Type getType() {
            return ZoomData.type;
	}
	
        /**
         * Return the absolute scale factor relative to the
         * zoom center point.
         */
        public double getZoomAmount() {
            return _zoom;
        }

        /**
         * Return the X coordinate of the center point.
         */
        public double getCenterX() {
            return _cx;
        }

        /**
         * Return the Y coordinate of the center point.
         */
        public double getCenterY() {
            return _cy;
        }

        public Object build(XmlElement in, String type) {
            //FIXME
            return this;
        }

        public XmlElement generate(Object in) {
            ZoomData dat = (ZoomData)in;
            XmlElement out = new XmlElement(in.getClass().getName());
            out.setAttribute(ZOOM_AMOUNT, Double.toString(dat.getZoomAmount()));
            out.setAttribute(CENTER_X, Double.toString(dat.getCenterX()));
            out.setAttribute(CENTER_Y, Double.toString(dat.getCenterY()));
            return out;
        }
    }
}

