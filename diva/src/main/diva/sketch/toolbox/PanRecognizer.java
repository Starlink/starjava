/*
 * $Id: PanRecognizer.java,v 1.8 2001/08/28 06:37:13 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.recognition.StrokeRecognizer;
import diva.sketch.recognition.Type;
import diva.sketch.recognition.TypedData;
import diva.util.xml.AbstractXmlBuilder;
import diva.util.xml.XmlElement;
import diva.sketch.recognition.Recognition;
import diva.sketch.recognition.RecognitionSet;
import diva.sketch.recognition.TimedStroke;
/**
 * A recognizer that recognizes "pan" gestures ("P") and
 * then pans left if the events are "left", etc.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @author  Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.8 $
 * @rating Red
 */
public class PanRecognizer extends ModedIncrRecognizer {
    public final static String LETTER_P = "p";
    private static int NUM_PTS_THRESH = 10;
    private static double _recognitionRate = 90;
    private StrokeRecognizer _recognizer;

    /**
     * Construct a pan recognizer with a classifying recognizer which
     * has been trained on "P" shapes.
     */
    public PanRecognizer(StrokeRecognizer r) {
        super();
        _recognizer = r;
    }
    
    /**
     * Emit pan events based on whether the mouse has
     * gone up or down.
     */
    public RecognitionSet processActionStroke(TimedStroke s){
        int numPts = s.getVertexCount();
        if(numPts > 1) {
            double dx = s.getX(numPts-1) - s.getX(numPts-2);
            double dy = s.getY(numPts-1) - s.getY(numPts-2);
            PanData type = new PanData(dx, dy);
            Recognition []r = { new Recognition(type, 100), };
            RecognitionSet rset = new RecognitionSet(r);
            return rset;
        }
        return RecognitionSet.NO_RECOGNITION;
    }

    /**
     * Perform the recognition of the pan signal gesture, which
     * is a segment that closes on itself.
     */
    public int recognizeActionSignal(TimedStroke s){
        int numPts = s.getVertexCount();
        if(numPts > NUM_PTS_THRESH){
            RecognitionSet rset = _recognizer.strokeModified(s);
            Recognition r = rset.getBestRecognition();
            if(r != null) {
                if(r.getType().getID().equals(LETTER_P) &&
                        (r.getConfidence() > _recognitionRate)){
                    return ACTION;
                }
            }
        }
        return UNKNOWN;
    }

    /**
     * A classification type for paning.
     */
    public static class PanData extends AbstractXmlBuilder implements TypedData {
        public static final String PAN_X = "panX";
        public static final String PAN_Y = "panY";
        
        /**
         * The static type associated with this typed data.
         */
        public static final Type type = Type.makeType(PanData.class);

        /**
         * How much to pan in X.
         */
        private double _panX;

        /**
         * How much to pan in Y.
         */
        private double _panY;

        /**
         * For XmlBuilder interface; do not call directly!
         */
        public PanData() {
        }
        
        /**
         * Contruct a pan type object with the pan amount
         * given.
         */
        public PanData(double panX, double panY) {
            _panX = panX;
            _panY = panY;
        }

	public Type getType() {
            return PanData.type;
	}
	
        public double getPanX() {
            return _panX;
        }

        public double getPanY() {
            return _panY;
        }

        public Object build(XmlElement elt, String type) {
            _panX = Double.parseDouble(elt.getAttribute(PAN_X));
            _panY = Double.parseDouble(elt.getAttribute(PAN_Y));
            return this;
        }

        public XmlElement generate(Object in) {
            PanData dat = (PanData)in;
            XmlElement out = new XmlElement(in.getClass().getName());
            out.setAttribute(PAN_X, Double.toString(dat.getPanX()));
            out.setAttribute(PAN_Y, Double.toString(dat.getPanY()));
            return out;
        }
    }
}

