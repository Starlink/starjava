/*
 * $Id: DeletionAction.java,v 1.4 2001/07/22 22:01:41 johnr Exp $
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
import java.awt.geom.Rectangle2D;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

/**
 * This class encapsulates a deletion operation
 * for pen interaction.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 * @rating Red
 */
public class DeletionAction extends AbstractAction {
    /** The interpreter in which we are operating.
     */
    private BasicInterpreter _interp;

    /** Construct a deletion operator that
     * executes within the given interpreter.
     */
    public DeletionAction(BasicInterpreter interp) {
        _interp = interp;
    }
    
    /** Treat the current stroke as a lasso selection if it is
     * mostly closed.
     */
    public void actionPerformed(ActionEvent evt) {
        _interp.removeCurrentSymbol();
        TimedStroke stroke = _interp.getCurrentStroke();
        Rectangle2D bounds = stroke.getBounds();

        // We first remove the objects that are hit by the deletion
        // gesture from the selection model if they are currently
        // selected (because selected objects are wrapped by
        // FigureDecorators).
        SketchController controller = _interp.getController();
        SelectionModel m = controller.getSelectionModel();
        Iterator iter = controller.hitSketchFigures(bounds);
        while(iter.hasNext()){
            Figure f = (Figure)iter.next();
            if (f instanceof FigureDecorator) {
                f = ((FigureDecorator)f).getDecoratedFigure();
                m.removeSelection(f);
            }
        }
        // Remove random unrecognized scribbles
        deleteFigures(controller.hitSketchFigures(bounds));
    }

    /** Remove all of the figures in the given iterator.<p>
     *
     *  Get the sketch model from the controller, iterate over the
     *  'figures', retrieve the symbol from each figure's user object,
     *  and remove the symbol from the sketch model.
     */
    protected void deleteFigures(Iterator figures) {
        SketchModel sm = _interp.getController().getSketchModel();
        while(figures.hasNext()) {
            Figure f = (Figure)figures.next();
            Object obj = f.getUserObject();
            if(obj instanceof Symbol) {
                Symbol s = (Symbol)obj;
                sm.removeSymbol(s);
            }
        }
    }
}


