/*
 * $Id: SketchTest2.java,v 1.3 2001/07/23 03:59:01 johnr Exp $
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

public class SketchTest2 extends JFrame {
    private TimedStroke _strokeBuffer = new TimedStroke(2000);
    private LocalSketchPane _sketchPane = null;
    
    public static void main(String argv[]){
        SketchTest2 st= new SketchTest2();
        st.setSize(600,400);
        st.setVisible(true);
    }
    
    public SketchTest2 () {
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
        }
        
        public void mouseDragged(LayerEvent e){
            double x = e.getLayerX();
            double y = e.getLayerY();
            long time = System.currentTimeMillis();
            _strokeBuffer.addVertex((float)x,(float)y,time);
            SketchLayer l = _sketchPane.getSketchLayer();
            l.appendStroke(x,y);
        }

        public void mouseReleased(LayerEvent e){
            TimedStroke s = new TimedStroke(_strokeBuffer);
            BasicFigure f = new BasicFigure(s);
            f.setStrokePaint(Color.black);
            _sketchPane.getForegroundLayer().add(f);
            _strokeBuffer.reset();
            SketchLayer l = _sketchPane.getSketchLayer();
            l.finishStroke();
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


