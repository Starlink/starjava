/*
 * $Id: CheckSelectionAction.java,v 1.3 2001/07/22 22:01:40 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch;
import diva.sketch.recognition.TimedStroke;
import diva.canvas.interactor.SelectionModel;
import diva.canvas.Figure;
import diva.canvas.FigureDecorator;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.awt.geom.Rectangle2D;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

/**
 * This class encapsulates a check selection operation
 * for pen interaction.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public class CheckSelectionAction extends AbstractAction {
    /** The interpreter in which we are operating.
     */
    private BasicInterpreter _interp;

    /** Construct a deletion operator that
     * executes within the given interpreter.
     */
    public CheckSelectionAction(BasicInterpreter interp) {
        _interp = interp;
    }
    
    /** Treat the current stroke as a 'check' selection.
     */
    public void actionPerformed(ActionEvent evt) {
        _interp.removeCurrentSymbol();
        TimedStroke stroke = _interp.getCurrentStroke();
        Rectangle2D bounds = stroke.getBounds();
        // Toggle the hit figures into or out of the
        // selection.  For each, make sure that we have
        // the figure itself, and not the manipulator, by
        // testing its class and calling
        // FigureDecorator.getDecoratedFigure if
        // necessary. Then see if the figure is in or not
        // in the selection, and add it or take it out.
        Iterator i = _interp.getController().hitSketchFigures(bounds);
        ArrayList hitFigures = new ArrayList();
        while(i.hasNext()) {
            Figure f = (Figure)i.next();
            if (f instanceof FigureDecorator) {
                f = ((FigureDecorator) f).getDecoratedFigure();
            }
            hitFigures.add(f);
        }
        toggleSelection(hitFigures);
    }
    
    /** Toggle the selection state of the given figures, or clear the
     * selection if no figures were hit.  Implemented as a template
     * method so that subclasses can override the behavior.
     */
    protected void toggleSelection(List hitFigures) {
        SelectionModel m = _interp.getController().getSelectionModel();
        if(hitFigures.size()==0){
            // a selection gesture is made on the empty part of
            // the canvas, therefore deselect everything that
            // is currently selected.
            m.clearSelection();
            return;
        }
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


