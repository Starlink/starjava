/*
 * $Id: SketchController.java,v 1.16 2000/10/30 00:15:16 michaels Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch;

import diva.canvas.Figure;
import diva.canvas.FigureDecorator;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.interactor.SelectionModel;
import diva.canvas.interactor.BasicSelectionModel;
import diva.sketch.recognition.TimedStroke;
import diva.util.java2d.Polygon2D;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * SketchController controls the behavior of a SketchPane.  It is
 * meant to be subclassed by application specific controllers to do
 * "intelligent" interpretation on sketched input.  The interpretation
 * is performed by using a sketch interpreter.
 *
 * @see BasicInterpreter
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.16 $
 * @rating Red
 */
public abstract class SketchController  {
    /**
     * The graphics pane that this is controlling.
     */
    private GraphicsPane _pane = null;
    
    /**
     * The default selection model.  Selected sketched figures are
     * added to this selection model.
     */
    private SelectionModel _selectionModel = new BasicSelectionModel();
    
    /**
     * Create a SketchController without a parent pane.
     * The parent pane is specified by the <i>setSketchPane()</i>
     * method.
     */
    public SketchController () {}

    /**
     * Get the default selection model.
     */
    public SelectionModel getSelectionModel () {
        return _selectionModel;
    }
    
    /**
     * Return the parent pane of this controller.
     */
    public GraphicsPane getSketchPane () {
        return _pane;
    }

    /**
     * Return an iterator over the figures that are completely contained
     * by the given polygonal region.
     */
    public Iterator containedSketchFigures(Polygon2D region) {
        FigureLayer layer = getSketchPane().getForegroundLayer();
        Iterator i =
            layer.getFigures().getIntersectedFigures(region.getBounds2D()).figures();
        ArrayList containedFigures = new ArrayList();
        while(i.hasNext()){
            Figure f = (Figure) i.next();
            if(f instanceof FigureDecorator) {
                //this figure is currently selected
                f = ((FigureDecorator)f).getChild();
            }
            Object userObj = f.getUserObject();
            if (userObj instanceof Symbol &&
                    symbolContained(region, (Symbol)userObj)) {
                containedFigures.add(f);
            }
        }
        return containedFigures.iterator();
    }

    private boolean symbolContained(Polygon2D region, Symbol symbol) {
        if(symbol instanceof StrokeSymbol) {
            TimedStroke ts = (TimedStroke)((StrokeSymbol)symbol).getStroke();
            boolean miss = false;
            for(int i = 0; i < ts.getVertexCount(); i++) {
                if(!region.contains(ts.getX(i), ts.getY(i))) {
                    miss = true;
                    break;
                }
            }
            return !miss;
        }
        else {
            CompositeSymbol comp = (CompositeSymbol)symbol;
            Symbol[] children = comp.getChildren();
            boolean miss = false;
            for(int i = 0; i < children.length; i++) {
                if(!symbolContained(region, children[i])) {
                    miss = true;
                    break;
                }
            }
            return !miss;
        }
    }

    /**
     * Return an iterator over the figures that intersect the given
     * rectangular region.
     */
    public Iterator hitSketchFigures(Rectangle2D region) {
        FigureLayer layer = getSketchPane().getForegroundLayer();
        Iterator i =
            layer.getFigures().getIntersectedFigures(region).figures();
        ArrayList hitFigures = new ArrayList();
        while(i.hasNext()){
            Figure f = (Figure) i.next();
            if (f.hit(region)) {
                hitFigures.add(f);
            }
        }
        return hitFigures.iterator();
    }

    /**
     * Initialize all interaction on the sketch pane. This method is
     * called by the setSketchPane() method, and must be overridden by
     * subclasses. This initialization cannot be done in the
     * constructor because the controller does not yet have a
     * reference to its pane at that time.
     */
    protected abstract void initializeInteraction ();

    /**
     * Set the default selection model. The caller is expected to ensure
     * that the old model is empty before calling this.
     */
    public void setSelectionModel (SelectionModel m){
        _selectionModel = m;
    }

    /**
     * Set the graphics pane.  This is done once by the SketchPane
     * immediately after construction of the controller.
     */
    public void setSketchPane (GraphicsPane p){
        _pane = p;
        initializeInteraction();
    }
}

