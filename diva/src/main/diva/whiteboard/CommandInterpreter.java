package diva.whiteboard;
import diva.sketch.BasicInterpreter;
import diva.sketch.BasicSketchController;
import diva.sketch.SketchModel;
import diva.sketch.StrokeSymbol;
import diva.sketch.Symbol;
import diva.sketch.features.FEUtilities;
import diva.sketch.features.StrokePathLengthFE;
import diva.sketch.recognition.BasicStrokeRecognizer;
import diva.sketch.recognition.StrokeRecognition;
import diva.sketch.recognition.StrokeRecognitionSet;
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

import java.awt.Color;
import java.awt.Component;
import java.io.FileReader;
import java.util.Iterator;
import java.awt.geom.Rectangle2D;

public class CommandInterpreter extends BasicInterpreter {
    public final static String DELETION_TYPE_ID = "scribble";
    public final static String SELECTION_TYPE_ID = "check";

    /**
     * If the number of points in a stroke is less than
     * CLICK_VERTEX_COUNT points, then it's probably a click.  We also
     * have to check the distance between its start and end points.
     */
    //    private final static int CLICK_VERTEX_COUNT = 10;

    /**
     * If the distance between the start and the end points of a
     * stroke is less than CLICK_VERTEX_THRESHOLD, this could be a
     * click (or a closed shape.
     */
    //    private final static int CLICK_VERTEX_THRESHOLD = 15;
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

    /**
     * The feature extraction used to compute a stroke's length.
     */
    private StrokePathLengthFE _pathLengthFE = new StrokePathLengthFE();

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

    public CommandInterpreter(BasicSketchController c, WhiteboardState state, MultipageDocument d){
        super(c);
        _whiteboardState = state;
        _document = d;
        addStrokeListener(new CommandStrokeListener());
        addClickListener(new ClickListener());
        addHoldListener(new HoldListener());
        try{
            _recognizer =
                new BasicStrokeRecognizer(new FileReader("commands.tc"));
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
            double length = _pathLengthFE.apply(stroke);
            //int num = stroke.getVertexCount();
            //double dist = FEUtilities.distance(stroke.getX(0), stroke.getY(0),
            //       stroke.getX(num-1), stroke.getY(num-1));
            //if((num < CLICK_VERTEX_COUNT)&&(dist < CLICK_VERTEX_THRESHOLD)){
            if(length < DOT_THRESHOLD){
                removeCurrentSymbol();
                if(getController().getSelectionModel().getSelectionCount()>0){
                    Rectangle2D bounds = stroke.getBounds();
                    Iterator iter = getController().hitSketchFigures(bounds);
                    if(!iter.hasNext()){//clicked on the background
                        getController().getSelectionModel().clearSelection();
                    }
                }
            }
            else{
                removeCurrentSymbol();
                processCommand(stroke);
            }
        }
    }
 
    public class ClickListener extends AbstractInteractor {
        public ClickListener() {}

        public void mouseClicked(LayerEvent e){
            //if clicked, deselect figure
            //should I check if the click occurs on the background?
            //This would need to create a Rectangle2D bound.
            getController().getSelectionModel().clearSelection();
        }
    }

    public class HoldListener extends AbstractInteractor {
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

    public void processCommand(TimedStroke stroke){
        _gestureModel.addSymbol(new StrokeSymbol(stroke, Color.black, null, 2.0f));
        BasicSketchController controller =
            (BasicSketchController)getController();
        FigureLayer layer =
            controller.getSketchPane().getForegroundLayer();
        StrokeRecognitionSet rset =_recognizer.strokeCompleted(stroke);
        StrokeRecognition r = rset.getBestRecognition();
        SelectionModel m = controller.getSelectionModel();
        boolean isProcessed = false;

        if(r != null) {
            double confidence = r.getConfidence();
            if((r.getType().getID().equals(DELETION_TYPE_ID))
                    && (confidence > DELETE_THRESHOLD)){
                Rectangle2D bounds = stroke.getBounds();
                // We first remove the objects that are hit by the
                // deletion gesture from the selection model if
                // they are currently selected.  This is because
                // when objects are selected they have
                // FigureDecorators wrapped around them, so if an
                // object is a node, it will not be detected.
                Iterator iter = controller.hitSketchFigures(bounds);
                while(iter.hasNext()){
                    Figure f = (Figure)iter.next();
                    if (f instanceof FigureDecorator) {
                        f = ((FigureDecorator)f).getDecoratedFigure();
                        m.removeSelection(f);
                    }
                }
                // Remove random unrecognized scribbles
                iter = controller.hitSketchFigures(bounds);
                WhiteboardEdits.DeleteGroupedStrokeEdit compoundEdit =
                    new WhiteboardEdits.DeleteGroupedStrokeEdit();
                int numEdits = 0;
                while(iter.hasNext()) {
                    Figure f = (Figure)iter.next();
                    Object obj = f.getUserObject();
                    if(obj instanceof Symbol) {
                        Symbol s = (Symbol)obj;
                        SketchModel sm = controller.getSketchModel();
                        sm.removeSymbol(s);
                        //create and post edits
                        WhiteboardEdits.DeleteStrokeEdit edit =
                            new WhiteboardEdits.DeleteStrokeEdit(sm, s, _document.getMultipageModel());
                        compoundEdit.addEdit(edit);
                        numEdits++;
                    }
                }
                compoundEdit.end();
                if(numEdits>0){
                    _document.getEditSupport().postEdit(compoundEdit);
                }
                isProcessed = true;
            }
            else if((r.getType().getID().equals(SELECTION_TYPE_ID))&&
                    (confidence > SELECT_THRESHOLD)){
                int ct=0;
                Rectangle2D bounds = stroke.getBounds();
                // Toggle the hit figures into or out of the
                // selection.  For each, make sure that we have
                // the figure itself, and not the manipulator, by
                // testing its class and calling
                // FigureDecorator.getDecoratedFigure if
                // necessary. Then see if the figure is in or not
                // in the selection, and add it or take it out.
                Iterator i = controller.hitSketchFigures(bounds);
                while(i.hasNext()) {
                    ct++;
                    Figure f = (Figure)i.next();
                    if (f instanceof FigureDecorator) {
                        f = ((FigureDecorator) f).getDecoratedFigure();
                    }
                    if (!(m.containsSelection(f))) {
                        m.addSelection(f);
                    } else {
                        m.removeSelection(f);
                    }
                }
                if(ct==0){
                    // a selection gesture is made on the empty part of
                    // the canvas, therefore deselect everything that
                    // is currently selected.
                    m.clearSelection();
                }
                isProcessed = true;
            }
            else {
                // confidence lower than threshold
                // try to do selection
                int num = stroke.getVertexCount();
                double dist = FEUtilities.distance(stroke.getX(0), stroke.getY(0), stroke.getX(num-1), stroke.getY(num-1));
                double length = _pathLengthFE.apply(stroke);
                double ratio = dist/length;
                if(ratio <= CLOSE_PROPORTION){
                    Polygon2D poly = new Polygon2D.Float();
                    poly.moveTo(stroke.getX(0), stroke.getY(0));
                    for(int i = 1; i < num; i++) {
                        poly.lineTo(stroke.getX(i), stroke.getY(i));
                    }
                    poly.closePath();
                    Iterator iter = controller.containedSketchFigures(poly);
                    while(iter.hasNext()){
                        Figure f = (Figure)iter.next();
                        if (f instanceof FigureDecorator) {
                            f = ((FigureDecorator)f).getDecoratedFigure();
                        }
                        if (!(m.containsSelection(f))) {
                            m.addSelection(f);
                        }
                        else {
                            m.removeSelection(f);
                        }
                    }
                    isProcessed = true;
                }
            }
        }
        if(!isProcessed) {
            System.out.println("NO RECOGNITION");
            // no recognition at all
        }
    }
}
