/*
 * $Id: SketchController.java,v 1.21 2002/01/04 04:16:24 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch;

import diva.canvas.BasicZList;
import diva.canvas.CompositeFigure;
import diva.canvas.Figure;
import diva.canvas.FigureDecorator;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.ZList;
import diva.canvas.event.LayerEvent;
import diva.canvas.event.MouseFilter;
import diva.canvas.interactor.AbstractInteractor;
import diva.canvas.interactor.BasicSelectionModel;
import diva.canvas.interactor.BoundsManipulator;
import diva.canvas.interactor.DragInteractor;
import diva.canvas.interactor.GrabHandle;
import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.SelectionDragger;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.SelectionModel;
import diva.canvas.toolbox.BasicFigure;
import diva.sketch.recognition.TimedStroke;
import diva.util.java2d.Polygon2D;
import diva.util.java2d.Polyline2D;
import diva.util.java2d.ShapeUtilities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.awt.event.InputEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

/**
 * SketchController controls the behavior of a SketchPane.  It is
 * meant to be subclassed by application specific controllers to do
 * "intelligent" interpretation on sketched input.  The interpretation
 * is performed by using a sketch interpreter.  This basic
 * implementation allows selection of symbols.  It also adds strokes
 * to a sketch model which can be written out to a sketch file.
 *
 * @see BasicInterpreter
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.21 $
 * @rating Red
 */
public class SketchController  {
    /**
     * The default pen color used for sketching.
     */
    public static Color DEFAULT_PEN_COLOR = Color.black;

    /**
     * The default fill color used for sketching.
     */
    public static Color DEFAULT_FILL_COLOR = null;
    
    /**
     * The default pen line width used for sketching.
     */
    public static float DEFAULT_LINE_WIDTH = 2f;

    /**
     * If SHOW_POINTS is true, each stroke will be displayed with the
     * points showing on its path.
     */
    //    private static boolean SHOW_POINTS = true;
    
    /**
     * The graphics pane that this is controlling.
     */
    private GraphicsPane _pane = null;
    
    /**
     * The model that the pane renders.
     */
    private SketchModel _model;

    /**
     * The default selection model.  Selected sketched figures are
     * added to this selection model.
     */
    private SelectionModel _selectionModel = new BasicSelectionModel();
    
    /**
     * The interactor that all symbols take. It enables symbols to be
     * selected and moved around.
     */
    private SelectionInteractor _selectionInteractor;

    /** The selection dragger that all figures take.
     */
    private SelectionDragger _selectionDragger;

    /**
     * Interprets mouse events on the foreground event layer as
     * sketching, calls a recognition engine, and processes the
     * results accordingly.  It also calls the controller to add a
     * symbol to the sketch model.
     */
    private Interactor _foregroundInterpreter;

    /**
     * Interprets mouse events on the background event layer.  This is
     * usually set enabled when sketching need to be interpreted as
     * commands.
     */
    private Interactor _backgroundInterpreter;    

    /**
     * The color used for sketching.  Default to black.
     */
    private Color _penColor = DEFAULT_PEN_COLOR;

    /**
     * The color used for filling sketches.  Default to null.
     */
    private Color _fillColor = DEFAULT_FILL_COLOR;
    
    /**
     * The line width used to draw strokes.
     */
    private float _lineWidth = DEFAULT_LINE_WIDTH;

    /**
     * Listen for changes in the sketch model and act accordingly.
     */
    private RepaintListener _repaintListener;

    /**
     * Map from symbols to figures.
     */
    private HashMap _map;

    /**
     * The filter for selection interactor.  The point of setting the
     * selection filter is to overwrite the default filter because
     * the default filter would be confused with sketching.
     */
    MouseFilter _selectionFilter = new MouseFilter (
            InputEvent.BUTTON1_MASK,
            InputEvent.CTRL_MASK);

    /**
     * The filter for toggling selection.
     */
    MouseFilter _altSelectionFilter = new MouseFilter (
            InputEvent.BUTTON1_MASK,
            InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK );

    /**
     * The filter for the drag interactor that moves objects. 
     */
    MouseFilter _dragFilter = new MouseFilter (
            InputEvent.BUTTON1_MASK,
            InputEvent.CTRL_MASK,
            InputEvent.CTRL_MASK);

    /**
     * The filter for sketching.
     */
    MouseFilter _sketchFilter = new MouseFilter (
            InputEvent.BUTTON1_MASK );
    
    /**
     * Create a SketchController and initialize its foreground
     * interpreter to a BasicInterpreter.  The foreground interpreter
     * will recieve mouse events as the user sketches and forward
     * these events to its listeners.  Also add a RepaintListener to
     * the sketch model.  The repaint listener manages painting of
     * stroke symbols as they are being added/updated/removed from the
     * model.  Most of the real work is done in the
     * <i>initializeInteraction()</i> method which is not called until
     * parent pane is set.  The pane is specified by the
     * <i>setSketchPane()</i> method.
     */
    public SketchController () {
        super();
        _model = new SketchModel();
        _repaintListener = new RepaintListener();
        _map = new HashMap();
        setForegroundInterpreter(new BasicInterpreter(this));
        _model.addSketchListener(_repaintListener);
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
     * Debugging output.
     */
    private void debug (String s) {
        System.err.println(s);
    }

    /**
     * Return a figure for the given symbol, or null if
     * the symbol does not have a figure representation.
     */
    public Figure figureForSymbol(Symbol symbol) {
        return (Figure)_map.get(symbol);
    }

    /**
     * Return the interpreter that processes strokes on the
     * backgound event layer.
     */
    public Interactor getBackgroundInterpreter(){
        return _backgroundInterpreter;
    }
    
    /**
     * Return the mouse filter used for dragging.
     */
    public MouseFilter getDragFilter(){
        return _dragFilter;
    }
    
    /**
     * Return the color used to fill symbols.
     */
    public Color getFillColor () {
        return _fillColor;
    }
    
    /**
     * Return the interpreter that processes strokes on the
     * foreground event layer.
     */
    public Interactor getForegroundInterpreter(){
        return _foregroundInterpreter;
    }

    /**
     * Return the line width used to draw symbols.
     */
    public float getLineWidth () {
        return _lineWidth;
    }
 
    /**
     * Return the color used to draw symbols.
     */
    public Color getPenColor () {
        return _penColor;
    }

    /**
     * Return the mouse filter used for the selection interactor.
     */
    public MouseFilter getSelectionFilter(){
        return _selectionFilter;
    }

    /**
     * Return the selection interactor on all symbols.
     */
    public SelectionInteractor getSelectionInteractor(){
        return _selectionInteractor;
    }

    /**
     * Get the default selection model.
     */
    public SelectionModel getSelectionModel () {
        return _selectionModel;
    }
    
    /**
     * Return the sketch model this pane views.
     */
    public SketchModel getSketchModel () {
        return _model;
    }

    /**
     * Return the parent pane of this controller.
     */
    public GraphicsPane getSketchPane () {
        return _pane;
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
     * Initialize all interaction on the sketch pane.  This method is
     * called by the setSketchPane() method.  The initialization
     * cannot be done in the constructor because the controller does
     * not yet have a reference to its pane at that time.
     */
    protected void initializeInteraction () {
        SelectionModel sm = getSelectionModel();
        GraphicsPane pane = getSketchPane();
	pane.getForegroundLayer().setPickHalo(5);
        
        pane.getForegroundEventLayer().setEnabled(true);
        if(_foregroundInterpreter != null) {
            pane.getForegroundEventLayer().addInteractor(_foregroundInterpreter);
        }
        if(_backgroundInterpreter != null) {
            pane.getBackgroundEventLayer().addInteractor(_backgroundInterpreter);
        }
        
        // Selection interactor for selecting objects
        DragInteractor dragger = new DragInteractor();
        dragger.setMouseFilter(new MouseFilter(1,0,0));
        dragger.setSelectiveEnabled(true);

        /** The selection interactor enabled sketch figures to be selected
         * using conventional click-selection and drag-selection, if the
         * control key is pressed at the same time.
         */
        _selectionInteractor = new SelectionInteractor(sm);
        _selectionInteractor.setSelectionFilter(_selectionFilter);
        _selectionInteractor.setToggleFilter(_altSelectionFilter);
        _selectionInteractor.setConsuming(false);
        /*
          _selectionDragger = new SelectionDragger(pane);
          _selectionDragger.setSelectionFilter(_selectionFilter);
          _selectionDragger.setToggleFilter(_altSelectionFilter);
          _selectionDragger.addSelectionInteractor(_selectionInteractor);
        */
        /** When an item is selected, give it a bounds manipulator,
         * which responds to button 1, and ignores modifier keys.
         */
        BoundsManipulator manipulator = new BoundsManipulator();
        manipulator.getHandleInteractor().setMouseFilter(
                new MouseFilter(1, 0, 0));

        _selectionInteractor.setPrototypeDecorator(manipulator);
        _selectionInteractor.addInteractor(dragger);

        /** The bounds manipulator can be used to move the figures
         * as well as using a move handle. This handle responds to button
         * 1, and ignores modifier keys.
         */
        DragInteractor mover = new DragInteractor() {
            public void mouseReleased(LayerEvent e) {
                Object[] targets = getTargetArray();
                if(targets != null) {
                    for(int i = 0; i < targets.length; i++) {
                        Figure f = (Figure)targets[i];
                        if(f instanceof GrabHandle) {
                            f = ((GrabHandle)f).getSite().getFigure();
                        }
                        Object userObj = f.getUserObject();
                        if(userObj instanceof StrokeSymbol) {
                            _model.updateSymbol((StrokeSymbol)userObj);
                        }
                    }
                    super.mouseReleased(e);
                }
            }
        };
        mover.setMouseFilter(new MouseFilter(1,0,0));
        manipulator.setDragInteractor(mover);
    }    

    /**
     * Set the interpreter that processes strokes in the background
     * event layer.  This would be useful for processing events that
     * you'd like to go to selected objects.  Remove the previous
     * background interpreter from the background event layer before
     * adding the new one.
     */
    public void setBackgroundInterpreter (Interactor interactor) {
        GraphicsPane pane = getSketchPane();
        if(pane != null) {
            if(_backgroundInterpreter != null) {
                pane.getBackgroundEventLayer().removeInteractor(_backgroundInterpreter);
            }
            pane.getBackgroundEventLayer().addInteractor(interactor);
        }
        _backgroundInterpreter = interactor;
    }

    /**
     * Set the color used to fill symbols.
     */
    public void setFillColor (Color c) {
        _fillColor = c;
    }
   
    /**
     * Set the interpreter that processes strokes in the foreground
     * event layer.  If these events are consumed, they don't get
     * passed to the layers below, therefore _backgroundInterpreter
     * will not process these strokes.  Remove the previous foreground
     * interpreter from the foreground event layer before adding this
     * one.
     */
    public void setForegroundInterpreter (Interactor interactor) {
        GraphicsPane pane = getSketchPane();
        if(pane != null) {
            if(_foregroundInterpreter != null) {
                pane.getForegroundEventLayer().removeInteractor(_foregroundInterpreter);
            }
            pane.getForegroundEventLayer().addInteractor(interactor);
        }
        _foregroundInterpreter = interactor;

    }
    
    /**
     * Set the line width used to draw symbols.  This also sets the
     * line width in SketchLayer if a SketchPane is used.
     */
    public void setLineWidth (float w) {
        _lineWidth = w;
        GraphicsPane gp = getSketchPane();
        if(gp instanceof SketchPane) {
            ((SketchPane)gp).getSketchLayer().setLineWidth(w);
        }
    }

    /**
     * Set the color used to draw symbols.  This also sets the
     * pen color in SketchLayer if a SketchPane is used.
     */
    public void setPenColor (Color c) {
        _penColor = c;
        GraphicsPane gp = getSketchPane();
        if(gp instanceof SketchPane) {
            ((SketchPane)gp).getSketchLayer().setPenColor(c);
        }
    }
    
    /**
     * Set the default selection model. The caller is expected to ensure
     * that the old model is empty before calling this.
     */
    public void setSelectionModel (SelectionModel m){
        _selectionModel = m;
    }

    /**
     * Set the sketch model in this controller.  It replaces the
     * existing model and updates the pane with figures according to
     * the new model.
     *
     * Remove the repaint listener from the previous sketch model,
     * then remove all existing figures from the pane.  Add repaint
     * listener to the new sketch model and update the pane with new
     * figures (add symbols from the new model).
     */
    public void setSketchModel(SketchModel m){
        if(_model != null) {
            _model.removeSketchListener(_repaintListener);
        }
        removeAllSymbols();
        
        // set new sketch model
        _model = m;
        _model.addSketchListener(_repaintListener);
        
        // for each symbol in the model, create a figure and add it to
        // the layer
        for(Iterator iter = _model.symbols(); iter.hasNext();){
            Symbol symbol = (Symbol)iter.next();
            addSymbol(0, symbol);
        }
    }

    /**
     * Set the graphics pane.  This is done once by the SketchPane
     * immediately after construction of the controller.
     */
    public void setSketchPane (GraphicsPane p){
        _pane = p;
        initializeInteraction();
    }

    /**
     * Remove all existing figures from the foreground layer, and
     * clear _map to get rid of existing symbols.
     */
    private void removeAllSymbols() {
        FigureLayer layer = getSketchPane().getForegroundLayer();
        getSelectionModel().clearSelection();
        // remove all existing figures from the pane
        layer.clear();
        _map.clear();
    }

    /**
     * Update the selected symbols using the current drawing settings
     * and call the model to pass this along to its listeners.
     *
     public void updateSelectedSymbols (){
     SelectionModel sm = _selectionInteractor.getSelectionModel();
     for(Iterator i = sm.getSelection(); i.hasNext();){
     Figure f = (Figure)i.next();
     Symbol s = (Symbol)f.getUserObject();
     s.setLineWidth(_lineWidth);
     s.setOutline(_penColor);
     s.setFill(_fillColor);
     getSketchModel().updateSymbol(s);
     }
     }
    */

    /**
     * Remove the given symbol by calling removeStrokeSymbol or
     * removeCompositeSymbol depending on the type of 'symbol'.
     */
    private void removeSymbol(Symbol symbol) {
        if(symbol instanceof StrokeSymbol) {
            removeStrokeSymbol((StrokeSymbol)symbol);
        }
        else if(symbol instanceof CompositeSymbol) {
            removeCompositeSymbol((CompositeSymbol)symbol);
        }
        else {
            throw new RuntimeException("Unknown symbol type: " + symbol);
        }
    }

    /**
     * Remove the composite symbol.
     * <ul>
     * <li>get the figure for the symbol from _map</li>
     * <li>if the figure is selected, remove it from the selection model</li>
     * <li>remove the figure from the figure layer</li>
     * <li>remove the symbol from _map</li>
     * <li>call removeSymbol on each of the children of the composite symbol</li>
     * </ul>
     */
    private void removeCompositeSymbol(CompositeSymbol symbol) {
        FigureLayer layer = getSketchPane().getForegroundLayer();
        Figure f = (Figure)_map.get(symbol);
        if(getSelectionModel().containsSelection(f)){
            getSelectionModel().removeSelection(f);
        }
        layer.remove(f);
        /*
          if(f.getParent() instanceof FigureDecorator) {
          FigureDecorator fd = (FigureDecorator)f.getParent();
          layer.remove(fd);
          }
          else {
          layer.remove(f);
          }
        */
        _map.remove(symbol);
        Symbol[] children = symbol.getChildren();
        for(int i = 0; i < children.length; i++) {
            removeSymbol(children[i]);
        }
    }

    /**
     * Remove the stroke symbol.
     * <ul>
     * <li>get the figure for the symbol from _map</li>
     * <li>if the figure is not null, remove it from the selection model if it's selected, then remove the figure from the figure layer</li>
     * <li>remove the symbol from _map</li>
     * </ul>
     */
    private void removeStrokeSymbol(StrokeSymbol symbol) {
        FigureLayer layer = getSketchPane().getForegroundLayer();
        Figure f = (Figure)_map.get(symbol);
        //figure might be null if this stroke is the child of a
        //composite
        if(f != null) {
            if(getSelectionModel().containsSelection(f)){
                getSelectionModel().removeSelection(f);
            }
            layer.remove(f);
            /*
              if(f.getParent() instanceof FigureDecorator) {
              FigureDecorator fd = (FigureDecorator)f.getParent();
              layer.remove(fd);
              }
              else {
              layer.remove(f);
              }
            */
        }
        _map.remove(symbol);
    }
    
    private Figure renderSymbol(Symbol symbol) {
        Figure out;
        if(symbol instanceof CompositeSymbol) {
            CompositeSymbol cs = (CompositeSymbol)symbol;
            Symbol[] children = cs.getChildren();
            Figure[] childFigures = new Figure[children.length];
            BasicZList zlist = new BasicZList();
            for(int i = 0; i < children.length; i++) {
                Symbol child = children[i];
                childFigures[i] = renderSymbol(child);
                zlist.add(childFigures[i]);
            }
            out = new GroupFigure(zlist);
        }
        else {
            /*
              if(SHOW_POINTS){
              StrokeSymbol ss = (StrokeSymbol)symbol;
              TimedStroke stroke = ss.getStroke();
              BasicFigure bf = new BasicFigure(stroke);
              bf.setUserObject(ss);
              bf.setStrokePaint(ss.getOutline());
              bf.setFillPaint(ss.getFill());
              CompositeFigure cf = new CompositeFigure(bf);
              for(int i=0; i<stroke.getVertexCount(); i++){
              BasicFigure dotFigure = new BasicFigure(new Rectangle(2,2));
              dotFigure.setFillPaint(Color.red);
              dotFigure.translate(stroke.getX(i), stroke.getY(i));
              cf.add(dotFigure);
              }
              out = cf;
              }
              else {
            */
            StrokeSymbol ss = (StrokeSymbol)symbol;
            Shape s = makeBezier(ss.getStroke());
            out = new StrokeFigure(s);
            //((BasicFigure)out).setUserObject(ss);
            ((BasicFigure)out).setStrokePaint(ss.getOutline());
            ((BasicFigure)out).setFillPaint(ss.getFill());
            //      f.setLineWidth(ss.getLineWidth());
            ((BasicFigure)out).setStroke(
                    new BasicStroke(ss.getLineWidth(),
                            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            //            }
        }
        _map.put(symbol, out);
        out.setUserObject(symbol);
        out.setInteractor(_selectionInteractor);
        return out;
    }

    private Shape makeBezier(TimedStroke stroke) {
        GeneralPath gp = new GeneralPath();
        if(stroke.getVertexCount() <= 2) {
            return stroke;
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

    private void addSymbol(int index, Symbol symbol) {
        FigureLayer layer = getSketchPane().getForegroundLayer();
        Figure f = renderSymbol(symbol);
        layer.add(index, f);
    }

    private void updateSymbol(Symbol symbol) {
        if(symbol instanceof StrokeSymbol) {
            updateStrokeSymbol((StrokeSymbol)symbol);
        }
        else if(symbol instanceof CompositeSymbol) {
            updateCompositeSymbol((CompositeSymbol)symbol);
        }
        else {
            throw new RuntimeException("Unknown symbol type: " + symbol);
        }
    }

    private void updateCompositeSymbol(CompositeSymbol symbol) {
        //FIXME
        throw new UnsupportedOperationException("Update composite symbol not supported");
    }
    
    private void updateStrokeSymbol(StrokeSymbol symbol) {
        BasicFigure f = (BasicFigure)_map.get(symbol);
        if(f != null){
            f.setShape(makeBezier(symbol.getStroke()));
            f.setStrokePaint(symbol.getOutline());
            f.setFillPaint(symbol.getFill());
            f.setLineWidth(symbol.getLineWidth());        
            f.repaint();
        }
    }

    /**
     * Listens to the sketch model and receives events whenever
     * changes occur in the model.  This listener handle the rendering
     * of strokes.  It tell the canvas when to repaint.
     */
    private class RepaintListener implements SketchListener {
        public void symbolAdded(SketchEvent e) {
            Symbol s = e.getSymbol();
            addSymbol(_model.indexOf(s), s);
        }
        public void symbolRemoved(SketchEvent e) {
            removeSymbol(e.getSymbol());
        }
        public void symbolModified(SketchEvent e) {
            updateSymbol(e.getSymbol());
        }
    }

    private static class GroupFigure extends CompositeFigure {
        public GroupFigure() {
            super();
        }
        public GroupFigure(ZList zlist) {
            super(zlist);
        }

        /** Translate this figure by the given distance.
         * This default implementation simply forwards the translate
         * call to each child.
         */
        public void translate (double x, double y) {
            repaint();
            invalidateCachedBounds();
            Iterator i = figures();
            while (i.hasNext()) {
                Figure f = (Figure) i.next();
                f.translate(x,y);
            }
            repaint();
        }
    
        /** Transform this figure with the supplied transform.
         * This default implementation simply forwards the transform
         * call to each child.
         */
        public void transform (AffineTransform at) {
            repaint();
            invalidateCachedBounds();
            Iterator i = figures();
            while (i.hasNext()) {
                Figure f = (Figure) i.next();
                f.transform(at);
            }
            repaint();
        }
    }

    private static class StrokeFigure extends BasicFigure {
        public StrokeFigure(Shape s) {
            super(s);
        }
        
        /**
         * Translate this figure by the given distance.  Get the user
         * object from the figure.  The user object should be a
         * StrokeSymbol.  Then translate the actual stroke (get it
         * from the StrokeSymbol) by x and y amount.
         */
        public void translate (double x, double y) {
            super.translate(x,y);
            StrokeSymbol sym = (StrokeSymbol)getUserObject();
            TimedStroke s = (TimedStroke)ShapeUtilities.translateModify(sym.getStroke(), x, y);
            sym.setStroke(s);
        }

        /**
         * Transform this figure with the supplied transform.  Get the
         * user object from the figure.  The user object should be a
         * StrokeSymbol.  Then transfrom the actual stroke (get it
         * from the StrokeSymbol).
         */
        public void transform (AffineTransform at) {
            super.transform(at);
            StrokeSymbol sym = (StrokeSymbol)getUserObject();
            TimedStroke s = (TimedStroke)ShapeUtilities.transformModify(sym.getStroke(), at);
            sym.setStroke(s);
        }
    }
}


