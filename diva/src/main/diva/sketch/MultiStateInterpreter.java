/*
 * $Id: MultiStateInterpreter.java,v 1.3 2001/07/22 22:01:41 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch;
import diva.canvas.event.LayerEvent;
import diva.canvas.interactor.AbstractInteractor;
import diva.canvas.interactor.Interactor;

import java.util.ArrayList;
import java.util.Iterator;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.Timer;

/**
 * A MultiStateInterpreter inherits from BasicInterpreter.  It
 * implements a FSM (finite state machine) in order to keep track of
 * mouse states, for example, whether or not the mouse has been hold
 * down in place for a while (hold listeners), how many clicks have
 * been registered so far (click listeners), or it's simply a drawing
 * event (stroke listeners).  Different listeners can be added to
 * receive different notification from the interpreter.  For instance,
 * a click listener will be notified only when mouse clicks happen
 * whereas a hold listener will be notified only when the mouse has
 * been hold for a while.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @author  Heloise (hwawen@eecs.berkeley.edu) 
 * @version $Revision: 1.3 $
 * @rating Red
 */
public class MultiStateInterpreter extends BasicInterpreter {
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
     * Create a BasicInterpreter which is used by the
     * specified controller to interpret sketch input.
     */
    public MultiStateInterpreter (SketchController c) {
        super(c);
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

    public int getClickCount(){
        return _clickCount;
    }
    
    /**
     * Consume the event so it doesn't get passed down to the layer
     * below.
     *
     * If the previous event was a mouse press, check to see how far
     * the cursor has moved.  If the distance is >= MIN_DRAG_DISTANCE,
     * send each stroke listener both a pressed event(_pressedEvent)
     * and a dragged event (e).
     *
     * If the previous event was a mouse drag, send the event (e) to
     * the stroke listeners.
     */
    public void mouseDragged (LayerEvent e) {
        e.consume();
        
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
     * Consume the event so it doesn't get passed down to the * layer
     * below.  Start a Timer for HOLD_TIMEOUT amount of time.
     * 
     */
    public void mousePressed (LayerEvent e) {
        e.consume();
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
            _timer = new Timer(HOLD_TIMEOUT, new Timeout());
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
     * Consume the event.  If the previous event was a mouse press,
     * start a Timer for CLICK_TIMEOUT amount of time.  If the
     * previous event was a mouse drag, notify stroke listeners of
     * this mouse released event (e).
     */
    public void mouseReleased (LayerEvent e) {
        e.consume();
      
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
            _timer = new Timer(CLICK_TIMEOUT, new Timeout());
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
    
    /**
     * Respond to the mouse released event.  This adjusts the state
     * based on the current state and outputs an event if the mouse
     * has been pressed and we are now dragging.
     */
    public synchronized void timeout() {
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
     * A class that handles asynchronous timeouts
     * according to a timer thread.
     */
    private class Timeout implements ActionListener {

        public Timeout(){}

        public void actionPerformed(ActionEvent e) {
            if(_timer != null) {
                timeout();
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

