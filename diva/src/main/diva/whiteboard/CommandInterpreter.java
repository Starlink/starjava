/*
 * $Id: CommandInterpreter.java,v 1.39 2001/10/27 00:29:24 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.whiteboard;
import diva.sketch.SketchController;
import diva.sketch.CheckSelectionAction;
import diva.sketch.DeletionAction;
import diva.sketch.LassoSelectionAction;
import diva.sketch.MultiStateInterpreter;
import diva.sketch.SketchModel;
import diva.sketch.StrokeSymbol;
import diva.sketch.Symbol;
import diva.sketch.features.FEUtilities;
import diva.sketch.features.PathLengthFE;
import diva.sketch.recognition.BasicStrokeRecognizer;
import diva.sketch.recognition.Recognition;
import diva.sketch.recognition.RecognitionSet;
import diva.sketch.recognition.StrokeRecognizer;
import diva.sketch.recognition.TimedStroke;
import diva.canvas.Figure;
import diva.canvas.FigureDecorator;
import diva.canvas.FigureLayer;
import diva.canvas.event.LayerEvent;
import diva.canvas.interactor.AbstractInteractor;
import diva.canvas.interactor.SelectionModel;
import diva.gui.MultipageDocument;
import diva.util.java2d.Polygon2D;
import diva.resource.DefaultBundle;

import java.awt.Color;
import java.awt.event.ActionEvent;
//import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.awt.geom.Rectangle2D;

/**
 * This interpreter handles selection and deletion of a stroke or a
 * group of strokes.
 *
 * @author  Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.39 $
 * @rating Red
 */
public class CommandInterpreter extends MultiStateInterpreter {
    public final static String DELETION_TYPE_ID = "scribble";
    public final static String SELECTION_TYPE_ID = "check";

    private final static int DOT_THRESHOLD = 15;

    /**
     * Selection accuracy.
     */
    private static double SELECT_THRESHOLD = 70;

    /**
     * Deletion accuracy.
     */
    private static double DELETE_THRESHOLD = 90;

    /**
     * Threshold used for testing whether a shape is open or closed.
     * If the distance between the start and the end of a stroke is
     * less than or equal to 1/6 of the entire length of the stroke,
     * this shape is considered to be close.
     */
    private static double CLOSE_PROPORTION = 1.0/6.0;

    private WhiteboardState _whiteboardState = null;

    private MultipageDocument _document = null;
    
    /**
     * Save all of the gestures we've drawn in this interpreter
     * for debugging use.
     */
    private SketchModel _gestureModel = new SketchModel();

    /**
     * Command recognizer that knows about checks for selection and
     * scribble for deletion.
     */
    private static StrokeRecognizer _recognizer;

    LassoSelectionAction _lassoSelAction;
    CheckSelectionAction _checkSelAction;
    DeletionAction _delAction;

    public CommandInterpreter(SketchController c, WhiteboardState state, MultipageDocument d){
        super(c);
        _whiteboardState = state;
        _document = d;
        addStrokeListener(new CommandStrokeListener());
        addClickListener(new ClickListener());
        addHoldListener(new HoldListener());
        _lassoSelAction = new LassoSelectionAction(this);
        _checkSelAction = new CheckSelectionAction(this);
        _delAction = new DeletionAction(this);
        try{
            DefaultBundle bundle = new DefaultBundle();
            Reader reader = new InputStreamReader(bundle.getResourceAsStream("CommandGestures"));
            _recognizer = new BasicStrokeRecognizer(reader);
        }
        catch(Exception e){
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Return a sketch model containing all of the gestures that this
     * interpreter has processed, for debugging.
     */
    public SketchModel getGestureModel() {
        return _gestureModel;
    }

    /**
     * CommandStrokeListener receives stroke events from the
     * interpreter and processes these strokes as gesture commands.
     */
    public class CommandStrokeListener extends AbstractInteractor {
        public CommandStrokeListener(){}
        public void mouseReleased(LayerEvent e){
            //if the movement is very small and is on the background,
            //it is likely to be a click in which case, deselect
            //figures
            TimedStroke stroke = getCurrentStroke();
            double length = PathLengthFE.pathLength(stroke);
            removeCurrentSymbol();            
            if(length < DOT_THRESHOLD){
                if(getController().getSelectionModel().getSelectionCount()>0){
                    Rectangle2D bounds = stroke.getBounds();
                    Iterator iter = getController().hitSketchFigures(bounds);
                    if(!iter.hasNext()){//clicked on the background
                        getController().getSelectionModel().clearSelection();
                    }
                }
            }
            else{
                _gestureModel.addSymbol(new StrokeSymbol(stroke,Color.black,null,2.0f));
                FigureLayer layer =
                    getController().getSketchPane().getForegroundLayer();
                RecognitionSet rset =_recognizer.strokeCompleted(stroke);
                Recognition r = rset.getBestRecognition();
                SelectionModel m = getController().getSelectionModel();
                boolean isProcessed = false;

                if(r != null) {
                    String id = r.getType().getID();                    
                    double confidence = r.getConfidence();
                    if((id.equals(DELETION_TYPE_ID))&&
                            (confidence>DELETE_THRESHOLD)){
                        _delAction.actionPerformed(new ActionEvent(this,0,DELETION_TYPE_ID));
                        isProcessed = true;
                    }
                    else if((id.equals(SELECTION_TYPE_ID))&&
                            (confidence>SELECT_THRESHOLD)){
                        _checkSelAction.actionPerformed(new ActionEvent(this,0,SELECTION_TYPE_ID));
                        isProcessed = true;
                    }
                    else {
                        // confidence lower than threshold,
                        //check for lasso selection
                        int num = stroke.getVertexCount();
                        double dist = FEUtilities.distance(stroke.getX(0), stroke.getY(0), stroke.getX(num-1), stroke.getY(num-1));
                        double ratio = dist/length;
                        if(ratio <= CLOSE_PROPORTION){
                            _lassoSelAction.actionPerformed(new ActionEvent(this, 0, SELECTION_TYPE_ID));
                            isProcessed = true;
                        }
                    }
                }
                if(!isProcessed) {
                    System.out.println("NOT RECOGNIZED");
                }
            }
        }
    }

    private class ClickListener extends AbstractInteractor {
        public ClickListener() {}

        public void mouseClicked(LayerEvent e){
            //if clicked, deselect figure
            //should I check if the click occurs on the background?
            //This would need to create a Rectangle2D bound.
            getController().getSelectionModel().clearSelection();
        }
    }

    private class HoldListener extends AbstractInteractor {
        public HoldListener() {}
        public void mousePressed(LayerEvent e){
            //if the pen is on a figure, select that figure
            Rectangle2D region = new Rectangle2D.Double(e.getLayerX()-2,
                    e.getLayerY()-2, e.getLayerX()+2, e.getLayerY()+2);
            Iterator iter = getController().hitSketchFigures(region);
            if(iter.hasNext()){//hold on a figure
                Figure f = (Figure)iter.next();
                getController().getSelectionModel().addSelection(f);
            }
        }
    }
}

