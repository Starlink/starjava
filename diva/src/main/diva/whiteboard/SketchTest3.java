/*
 * $Id: SketchTest3.java,v 1.4 2001/07/23 03:59:01 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.whiteboard;
import diva.canvas.*;
import diva.canvas.interactor.*;
import diva.canvas.event.*;
import diva.canvas.toolbox.BasicFigure;
import diva.sketch.SketchLayer;
import diva.sketch.recognition.TimedStroke;
//import java.awt.event.MouseEvent;
//import java.awt.event.MouseListener;
//import java.awt.event.MouseMotionListener;
//import java.awt.event.MouseAdapter;
//import java.awt.event.MouseMotionAdapter;
//import java.awt.Graphics2D;
//import java.awt.RenderingHints;
import javax.swing.JFrame;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.GeneralPath;

public class SketchTest3 extends JFrame {
    private TimedStroke _strokeBuffer = new TimedStroke(2000);
    private LocalSketchPane _sketchPane = null;
    
    public static void main(String argv[]){
        SketchTest3 st= new SketchTest3();
        st.setSize(600,400);
        st.setVisible(true);
    }
    
    public SketchTest3 () {
        _sketchPane = new LocalSketchPane();
        SketchInteractor interactor = new SketchInteractor();
        _sketchPane.getForegroundEventLayer().setEnabled(true);
        _sketchPane.getForegroundEventLayer().setConsuming(true);
        _sketchPane.getForegroundEventLayer().addInteractor(interactor);
        JCanvas _canvas = new JCanvas(_sketchPane);
        getContentPane().add(_canvas);
    }

    public class SketchInteractor extends AbstractInteractor {

        public SketchInteractor(){}
        
        public void mousePressed(LayerEvent e){
            double x = e.getLayerX();
            double y = e.getLayerY();
            long time = System.currentTimeMillis();
            _strokeBuffer.addVertex((float)x,(float)y,time);
            SketchLayer l = _sketchPane.getSketchLayer();
            l.startStroke(x,y);
	    e.consume();
        }
        
        public void mouseDragged(LayerEvent e){
            double x = e.getLayerX();
            double y = e.getLayerY();
            long time = System.currentTimeMillis();
            _strokeBuffer.addVertex((float)x,(float)y,time);
            SketchLayer l = _sketchPane.getSketchLayer();
            l.appendStroke(x,y);
	    e.consume();
        }

        public void mouseReleased(LayerEvent e){
            Shape s = buildCurve(_strokeBuffer);
            BasicFigure f = new BasicFigure(s);
            f.setStrokePaint(Color.black);
            _sketchPane.getForegroundLayer().add(f);
            _strokeBuffer.reset();
            SketchLayer l = _sketchPane.getSketchLayer();
            l.finishStroke();
	    e.consume();
        }

	/** Build a bezier curve that smoothly 
	 * interpolates the points of the given stroke.
	 */
	private Shape buildCurve(TimedStroke stroke) {
	    GeneralPath gp = new GeneralPath();
	    if(stroke.getVertexCount() <= 2) {
		return new TimedStroke(stroke);
	    }
	    float x0 = (float)stroke.getX(0);
	    float y0 = (float)stroke.getY(0);
	    float x1 = (float)stroke.getX(1);
	    float y1 = (float)stroke.getY(1);
	    float x2 = (float)stroke.getX(2);
	    float y2 = (float)stroke.getY(2);

	    float dxA,dyA,dxB,dyB,dxT,dyT,distA,distB,distT,
		cx1,cy1,cx0,cy0;
            //	    float K = .2f;
	    float K = .25f;            

	    //first segment special case
	    dxT = x2 - x0;
	    dyT = y2 - y0;
	    dxA = x1 - x0;
	    dyA = y1 - y0;
	    dxB = x2 - x1;
	    dyB = y2 - y1;
	    distT = (float)Math.sqrt(dxT*dxT + dyT*dyT);
	    distA = (float)Math.sqrt(dxA*dxA + dyA*dyA);
	    distB = (float)Math.sqrt(dxB*dxB + dyB*dyB);

	    gp.moveTo(x0, y0);
	    cx0 = x0 + (distA*dxT*K/distT);
	    cy0 = y0 + (distA*dyT*K/distT);

	    //middle segments common case
	    for(int i = 2; i < stroke.getVertexCount(); i++) {
		x2 = (float)stroke.getX(i);
		y2 = (float)stroke.getY(i);
		dxT = x2 - x0;
		dyT = y2 - y0;
		dxA = x1 - x0;
		dyA = y1 - y0;
		dxB = x2 - x1;
		dyB = y2 - y1;
		distT = (float)Math.sqrt(dxT*dxT + dyT*dyT);
		distA = (float)Math.sqrt(dxA*dxA + dyA*dyA);
		distB = (float)Math.sqrt(dxB*dxB + dyB*dyB);

		//		System.out.println("OFF1 = " + (distA*dxT*K/distT) + ", " +
		//				   (distA*dyT*K/distT));
		cx1 = x1 - (distA*dxT*K/distT);
		cy1 = y1 - (distA*dyT*K/distT);

		
		//		System.out.println("PS: " + cx0 + ", " + cy0 + ", " 
		//				   + cx1 + ", " + cy1 + ", " + x1 + ", " + y1);
				   
		gp.curveTo(cx0,cy0,cx1,cy1,x1,y1);

		cx0 = x1 + (distB*dxT*K/distT);
		cy0 = y1 + (distB*dyT*K/distT);
		//		System.out.println("OFF2 = " + (distB*dxT*K/distT) + ", " +
		//				   (distB*dyT*K/distT));
		x0 = x1;
		y0 = y1;
		x1 = x2;
		y1 = y2;
	    }

	    //last segment special case
	    cx1 = x2 - (dxT*distB*K/distT);
	    cy1 = y2 - (dyT*distB*K/distT);
	    gp.curveTo(cx0,cy0,cx1,cy1,x2,y2);
	    return gp;
	}
    }

    public class LocalSketchPane extends GraphicsPane {
        /**
         * A layer on which to draw strokes in progress
         */
        private SketchLayer _sketchLayer;
        
        /**
         * A very nasty hack that allows us to only paint one layer, rather
         * than the whole set of layers, so that sketching strokes is fast.
         */
        private boolean _isSketching = false;
        
        /**
         * Create a new sketch pane with the given controller
         * that controls the behavior of this pane.
         */
        public LocalSketchPane (){
            super();
            _sketchLayer = new SketchLayer();
            _initNewLayer(_sketchLayer);
            _rebuildLayerArray();
        }

        /**
         * Return the sketch layer for optimized drawing
         * of strokes.
         */
        SketchLayer getSketchLayer() {
            return _sketchLayer;
        }
    
        /** Rebuild the array of layers for use by iterators.
         * Override superclass to include sketch layer.
         */
        protected void _rebuildLayerArray () {
            _layers = new CanvasLayer[6];
            int cursor = 0;
            _layers[cursor++] = _foregroundEventLayer;
            _layers[cursor++] = _sketchLayer;
            _layers[cursor++] = _overlayLayer;
            _layers[cursor++] = _foregroundLayer;
            _layers[cursor++] = _backgroundLayer;
            _layers[cursor++] = _backgroundEventLayer;
        }
    }
}


