/*
 * $Id: BasicInterpreter.java,v 1.30 2001/01/27 00:27:19 neuendor Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch;
import diva.sketch.recognition.TimedStroke;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.toolbox.BasicFigure;
import diva.canvas.event.LayerEvent;
import diva.canvas.event.MouseFilter;
import diva.canvas.interactor.AbstractInteractor;
import diva.canvas.interactor.Interactor;

import java.util.ArrayList;
import java.util.Iterator;
import java.awt.event.*;
import javax.swing.Timer;

/**
 * A class that interprets changes to a stroke.  BasicInterpreter is a
 * sketch interpreter that gets called whenever a user sketches on the
 * screen.  It extends from diva.canvas.AbstractInteractor and should
 * be added to the pane layer in order to receive mouse events.  The
 * interpreter creats a figure and adds it to the pane whenever
 * there's a mouse pressed event (meaning the user has started drawing
 * a stroke).  And when mouse drag events occur, the interpreter would
 * update the figure.
 *
 * Since BasicInterpreter is a very simple implementation of a sketch
 * interpreter, it doesn't do any interpretation on user input.  In
 * general, a sketch interpreter would receive mouse events and use a
 * recognizer to process these events.  The recognizer comes back with
 * an answer, and depending on what the answer is, the interpreter
 * tells the sketch controller to perform the corresponding
 * application-specific actions.  The recognizer can be arbitrarily
 * complex (or simple).  It could be a low-level recognizer that
 * determines the geometric shape of a user stroke or a voting
 * recognizer that uses a bunch of different recognizers.  It is meant
 * to be highly configurable.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @author  Heloise (hwawen@eecs.berkeley.edu) 
 * @version $Revision: 1.30 $
 * @rating Red
 */
public class BasicInterpreter extends AbstractInteractor {
    private ArrayList _holdListeners = new ArrayList();
    private ArrayList _strokeListeners = new ArrayList();
    private ArrayList _clickListeners = new ArrayList();

    /**
     * The swing timer for timed operations.
     */
    private Timer _timer = null;
    
    /**
     * Current state.
     */
    private int _state = NO_STATE;

    /**
     * X coordinate of the last mouse press.
     */
    private int _x; 

    /**
     * Y coordinate of the last mouse press.
     */
    private int _y;

    /**
     * Current number of clicks.
     */
    private int _clickCount;
    
    /**
     * Event of the last mouse press.
     */
    private LayerEvent _pressedEvent;    

    /**
     * The distance beyond which a click becomes a
     * drag.
     */
    public static final int MIN_DRAG_DISTANCE = 3;
    
    /**
     * The timeout for the popup hold, in milliseconds.
     */
    public static final int HOLD_TIMEOUT = 500;

    /**
     * The timeout for single/double/triple/.. clicks,
     * in milliseconds.
     */
    public static final int CLICK_TIMEOUT = 200;
    
    /**
     * The empty initial state that the user is in before
     * he has pressed the mouse.
     */
    private static final int NO_STATE = 0;

    /**
     * A state when the mouse has been pressed
     * once, but has not yet been moved or released.
     */
    private static final int PRESSED_STATE = 1;

    /**
     * An state when the mouse has been
     * clicked and is being dragged.
     */
    private static final int DRAGGING_STATE = 2;

    /**
     * An state when the mouse has been
     * clicked and released, but may be pressed
     * again for a multi-click gesture.
     */
    private static final int RELEASED_STATE = 3;
    
    /**
     * The sketch controller that uses this interpreter to interpret
     * sketch input.
     */
    BasicSketchController _controller;

    /**
     * A buffer for the stroke that's currently being drawn.
     * This is to avoid allocating a TimedStroke object at
     * the beginning of each new stroke, thus avoiding the
     * overhead of memory allocation.
     */
    TimedStroke _strokeBuffer = new TimedStroke(2000);

    /**
     * The current stroke that's being drawn, gets its data from
     * _strokeBuffer and is reallocated at the end of a stroke.
     */
    TimedStroke _curStroke;
    
    /**
     * The current symbol which wraps the current stroke and keeps 
     * visual information about the stroke (color, line width).
     */
    StrokeSymbol _curSymbol;
    
    /**
     * Create a BasicInterpreter which is used by the
     * specified controller to interpret sketch input.
     */
    public BasicInterpreter (BasicSketchController c) {
        _controller = c;
        setEnabled(true);
        addStrokeListener(new StrokePainter());
    }

    /**
     * Add a listener of clicks.  This listener will get MOUSE_CLICKED
     * events when the mouse is clicked one or more times.  It will
     * only get the largest number of clicks in a sequence.  So, for
     * example, if the user triple-clicks, the listener will only get
     * a triple- click event and won't get a double-click event.
     */
    public void addClickListener(Interactor l) {
        _clickListeners.add(l);
    }

    /**
     * Add a listener of hold invokations.  This listener will get an
     * action event when the user presses the mouse button and holds
     * for the duration of HOLD_TIMEOUT.
     */
    public void addHoldListener(Interactor l) {
        _holdListeners.add(l);
    }

    /**
     * Add a stroke listener that gets mouse-down, mouse-moved, and
     * mouse-released events for drawing strokes.  The click-count of
     * the mouse events allows the client to do multiple modes based
     * on a single/double/etc. click before the stroke was drawn.
     */
    public void addStrokeListener(Interactor l) {
        _strokeListeners.add(l);
    }

    /**
     * Append the given new point/timestamp to the current
     * stroke.  Consume the event when done.
     */
    protected void appendStroke (LayerEvent e) {
        //FIXME?        if(_curStroke != null) {
        _strokeBuffer.addVertex((float)e.getLayerX(),
                (float)e.getLayerY(), e.getWhen());
        GraphicsPane gp = _controller.getSketchPane();
        if(gp instanceof SketchPane) {
            SketchLayer l = ((SketchPane)gp).getSketchLayer();
            l.appendStroke(e.getLayerX(), e.getLayerY());
        }
        else {
            _controller.getSketchModel().updateSymbol(_curSymbol);
        }
        e.consume();
    }

    /**
     * Called at the end of the mouseReleased method to
     * finish the drawing of a stroke.
     */
    protected final void finishStroke (LayerEvent e) {
        GraphicsPane gp = _controller.getSketchPane();
        _curStroke = new TimedStroke(_strokeBuffer);
        _curSymbol.setStroke(_curStroke);
        if(gp instanceof SketchPane) {
            _controller.getSketchModel().addSymbol(_curSymbol);
        }
    }
    
    /**
     * Return the current stroke being drawn.
     */
    public final TimedStroke getCurrentStroke () {
        // This is public for jdk1.2.2 compatability.
        return _curStroke;
    }

    /**
     * Return the current figure being drawn.
     * @deprecated Use getCurrentSymbol() instead, since there may be
     *             no figure associated with the drawing until the
     *             end of the stroke.
     */
    protected BasicFigure getCurrentFigure() {
        StrokeSymbol s = getCurrentSymbol();
        return (BasicFigure)(getController().figureForSymbol(s));
    }

    /**
     * Return the current symbol being drawn.
     */
    protected final StrokeSymbol getCurrentSymbol () {
        return _curSymbol;
    }

    
    /**
     * Return the controller that uses this interpreter.
     */
    public final BasicSketchController getController () {
        // public for jdk1.2.2 compatability
        return _controller;
    }

    /**
     * Remove the current figure from the figure layer.
     */
    public void removeCurrentSymbol () {
        // public for jdk1.2.2 compatability
        _controller.getSketchModel().removeSymbol(_curSymbol);
    }
    
    /**
     * This method is invoked upon mouse down.  A TimedStroke object,
     * a Symbol, and a BasicFigure object are created to represent the
     * stroke being drawn.  The figure is added to the pane, so that
     * the user can see what he's drawing.  The symbol wraps the stroke
     * and adds color and linewidth information to the stroke and is
     * add to the sketch model in the controller.
     */
    protected final void startStroke (LayerEvent e) {
        _strokeBuffer.reset();
        _curStroke = _strokeBuffer;
        _curSymbol = new StrokeSymbol(_curStroke, _controller.getPenColor(),
                _controller.getFillColor(),_controller.getLineWidth());

        GraphicsPane gp = _controller.getSketchPane();
        if(gp instanceof SketchPane) {
            SketchLayer l = ((SketchPane)gp).getSketchLayer();
            l.startStroke(e.getLayerX(), e.getLayerY());
        }
        else {
            _controller.getSketchModel().addSymbol(_curSymbol);
        }
    }

    /**
     * We are consuming motion events.
     */
    public boolean isMotionEnabled () {
        return true;
    }
    
    /**
     * Update the current stroke and its visual representation.
     */
    public void mouseDragged (LayerEvent e) {
        //appendStroke(e);
        switch(_state) {
        case NO_STATE:
            break;
        case PRESSED_STATE:
            int abs = Math.abs(e.getX()-_x) + Math.abs(e.getY()-_y);
            if(abs >= MIN_DRAG_DISTANCE) {
                for(Iterator i = _strokeListeners.iterator(); i.hasNext();) {
                    Interactor l = (Interactor)i.next();
                    l.mousePressed(_pressedEvent);
                    l.mouseDragged(e);
                }
                _state = DRAGGING_STATE;
            }
            break;
        case DRAGGING_STATE:
            for(Iterator i = _strokeListeners.iterator(); i.hasNext();) {
                Interactor l = (Interactor)i.next();
                l.mouseDragged(e);
            }
            break;
        default:
            throw new RuntimeException("Received dragged event in state: "
                    + printState(_state));
        }
    }
    
    /**
     * Instantiate a new stroke and add its visual representation
     * as a symbol in the pane.
     */
    public void mousePressed (LayerEvent e) {
        e.consume();

        //        startStroke(e);
        //        appendStroke(e);
        switch(_state) {
        case NO_STATE:
        case RELEASED_STATE:
            _x = e.getX();
            _y = e.getY();
            _clickCount++;
            _state = PRESSED_STATE;
            _pressedEvent = e;
            //initiate timeout
            if(_timer != null) {
                _timer.stop();
            }
            _timer = new Timer(HOLD_TIMEOUT, new Timeout(NO_STATE));
            _timer.setRepeats(false);
            _timer.start();
            break;
        default:
            reset();
            throw new RuntimeException("Received pressed event in state "
                    + printState(_state) + "; resetting");
        }
    }
    
    /**
     * Update the current stroke and its visual representation,
     * then reset the stroke to "null".
     */
    public void mouseReleased (LayerEvent e) {
        //        appendStroke(e);
        //        finishStroke(e);
        switch(_state) {
        case NO_STATE:
            //hold timeout?
            break;
        case PRESSED_STATE:
            _state = RELEASED_STATE;
            //initiate timeout
            if(_timer != null) {
                _timer.stop();
            }
            _timer = new Timer(CLICK_TIMEOUT, new Timeout(PRESSED_STATE));
            _timer.setRepeats(false);
            _timer.start();
            break;
        case DRAGGING_STATE:
            for(Iterator i = _strokeListeners.iterator(); i.hasNext();) {
                Interactor l = (Interactor)i.next();
                l.mouseReleased(e);
            }
            reset();
            break;
        default:
            throw new RuntimeException("Received released event in state: "
                    + printState(_state));
        }
    }

    public int getClickCount(){
        return _clickCount;
    }

    /**
     * Respond to the mouse released event.  This adjusts the state
     * based on the current state and outputs an event if the mouse
     * has been pressed and we are now dragging.
     */
    public synchronized void timeout(int sourceState) {
        switch(_state) {
        case PRESSED_STATE:
            for(Iterator i = _holdListeners.iterator(); i.hasNext();) {
                Interactor l = (Interactor)i.next();
                l.mousePressed(_pressedEvent);
            }
            reset();
            break;
        case RELEASED_STATE:
            for(Iterator i = _clickListeners.iterator(); i.hasNext();) {
                Interactor l = (Interactor)i.next();
                l.mouseClicked(_pressedEvent);
            }
            reset();
            break;
        default:
            //            throw new RuntimeException
            debug("Received timeout event in state: "
                    + printState(_state));
        }
    }

    /**
     * Return a string representation for the given state.
     */
    private String printState(int state) {
        switch(state) {
        case NO_STATE:
            return "NO_STATE";
        case PRESSED_STATE:
            return "PRESSED_STATE";
        case RELEASED_STATE:
            return "RELEASED_STATE";
        case DRAGGING_STATE:
            return "DRAGGING_STATE";
        default:
            throw new IllegalArgumentException("Illegal state: " + state);
        }
    }

    /**
     * Debugging to standard output.
     */
    private void debug(String s) {
        System.out.println(s);
    }

    /**
     * Reset the state machine.
     */
    private void reset() {
        if(_timer != null) {
            _timer.stop();
            _timer = null;
        }
        _clickCount = 0;
        _state = NO_STATE;
        _pressedEvent = null;
    }

    /**
     * Removes a click listener that was added with
     * addClickListener().
     * 
     * @see #addClickListener(MouseListener)
     */
    public void removeClickListener(Interactor l) {
        _clickListeners.remove(l);
    }

    /**
     * Removes a hold listener that was added with
     * addHoldListener().
     * 
     * @see #addHoldListener(ActionListener)
     */
    public void removeHoldListener(Interactor l) {
        _holdListeners.remove(l);
    }

    /**
     * Removes a stroke listener that was added with
     * addStrokeListener().
     * 
     * @see #addStrokeListener(MouseInputListener)
     */
    public void removeStrokeListener(Interactor l) {
        _strokeListeners.remove(l);
    }
    
    /**
     * A class that handles asynchronous timeouts
     * according to a timer thread.
     */
    private class Timeout implements ActionListener {
        private int _curState;
        public Timeout(int curState) {
            _curState = curState;
        }
        public void actionPerformed(ActionEvent e) {
            if(_timer != null) {
                timeout(_curState);
            }
        }
    }

    private class StrokePainter extends AbstractInteractor {
        public void mousePressed(LayerEvent e) {
            startStroke(e);
            appendStroke(e);
            e.consume();
        }

        public void mouseDragged(LayerEvent e) {
            appendStroke(e);
            e.consume();
        }
        
        public void mouseReleased(LayerEvent e) {
            appendStroke(e);
            finishStroke(e);
            e.consume();
        }
    }
}

