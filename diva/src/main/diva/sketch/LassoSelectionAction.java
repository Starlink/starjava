/*
 * $Id: LassoSelectionAction.java,v 1.3 2001/07/22 22:01:41 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch;
import diva.sketch.recognition.TimedStroke;
import diva.canvas.interactor.SelectionModel;
import diva.util.java2d.Polygon2D;
import diva.canvas.Figure;
import diva.canvas.FigureDecorator;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

/**
 * This class encapsulates lasso-style selection operation
 * for pen interaction.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public class LassoSelectionAction extends AbstractAction {
     /**
      * Threshold used for testing whether a shape is open or closed.
      * If the distance between the start and the end of a stroke is
      * less than or equal to 1/6 of the entire length of the stroke,
      * this shape is considered to be close.
      */
    private static double CLOSE_PROPORTION = 1.0/6.0;

    /** The interpreter in which we are operating.
     */
    private BasicInterpreter _interp;

    /** Construct a lasso selection operator that
     * executes within the given interpreter.
     */
    public LassoSelectionAction(BasicInterpreter interp) {
        _interp = interp;
    }
    
    /** Treat the current stroke as a lasso selection if it is
     * mostly closed.
     */
    public void actionPerformed(ActionEvent evt) {
        _interp.removeCurrentSymbol();
        TimedStroke stroke = _interp.getCurrentStroke();
        int num = stroke.getVertexCount();
        /*
        double length = PathLengthFE.pathLength(stroke);
        double dist = FEUtilities.distance(stroke.getX(0), stroke.getY(0),
                stroke.getX(num-1), stroke.getY(num-1));
        double ratio = dist/length;
        if(ratio <= CLOSE_PROPORTION){
        */
        Polygon2D poly = new Polygon2D.Float();
        poly.moveTo(stroke.getX(0), stroke.getY(0));
        for(int i = 1; i < num; i++) {
            poly.lineTo(stroke.getX(i), stroke.getY(i));
        }
        poly.closePath();
        Iterator iter = _interp.getController().containedSketchFigures(poly);
        ArrayList hitFigures = new ArrayList();
        while(iter.hasNext()){
            Figure f = (Figure)iter.next();
            if (f instanceof FigureDecorator) {
                f = ((FigureDecorator)f).getDecoratedFigure();
            }
            hitFigures.add(f);
        }
        //        }
        toggleSelection(hitFigures);        
    }

    /** Toggle the selection state of the given figures.  Implemented
     * as a template method so that subclasses can override the behavior.
     */
    protected void toggleSelection(List hitFigures) {
        SelectionModel m = _interp.getController().getSelectionModel();
        for(Iterator i = hitFigures.iterator(); i.hasNext(); ) {
            Figure f = (Figure)i.next();
            if (!(m.containsSelection(f))) {
                m.addSelection(f);
            }
            else {
                m.removeSelection(f);
            }
        }
    }
}


