/*
 * $Id: WhiteboardState.java,v 1.5 2001/07/22 22:02:27 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.whiteboard;
import diva.sketch.SketchController;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A model which maintains Whiteboard's UI properties such as pen
 * color, pen width, and the mode (sketch or command).  It sents an
 * event to its listeners when a property has been changed.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.5 $
 */
public class WhiteboardState {
    public final static String PEN_COLOR = "PEN_COLOR";
    public final static String FILL_COLOR = "FILL_COLOR";
    public final static String PEN_WIDTH = "PEN_WIDTH";
    public final static String MODE = "MODE";    
    public final static String SKETCH_MODE = "SKETCH";
    public final static String COMMAND_MODE = "COMMAND";
    public final static String HIGHLIGHT_MODE = "HIGHLIGHT";
    
    ArrayList _listeners = new ArrayList();
    Float _penWidth = Float.valueOf(SketchController.DEFAULT_LINE_WIDTH);
    Color _penColor = SketchController.DEFAULT_PEN_COLOR;
    Color _fillColor = SketchController.DEFAULT_FILL_COLOR;
    String _mode = SKETCH_MODE;

    public WhiteboardState(){}
    
    public void addStateListener(PropertyChangeListener l){
        _listeners.add(l);
    }

    public void removeStateListener(PropertyChangeListener l){
        _listeners.remove(l);
    }

    public Color getPenColor(){
        return _penColor;
    }

    public Color getFillColor(){
        return _fillColor;
    }
    
    public float getPenWidth(){
        return _penWidth.floatValue();
    }

    public String getMode(){
        return _mode;
    }

    public void setPenColor(Color c){
        PropertyChangeEvent evt = new PropertyChangeEvent(this, PEN_COLOR, _penColor, c);
        dispatch(evt);
        _penColor = c;
    }

    public void setFillColor(Color c){
        PropertyChangeEvent evt = new PropertyChangeEvent(this, FILL_COLOR, _fillColor, c);
        dispatch(evt);
        _fillColor = c;
    }
    
    public void setPenWidth(float w){
        Float newWidth = Float.valueOf(w);
        PropertyChangeEvent evt = new PropertyChangeEvent(this, PEN_WIDTH, _penWidth, newWidth);
        dispatch(evt);
        _penWidth = newWidth;
    }

    public void setMode(String m){
        PropertyChangeEvent evt = new PropertyChangeEvent(this, MODE, _mode, m);
        dispatch(evt);
        _mode = m;
    }

    private void dispatch(PropertyChangeEvent evt){
        for(Iterator i = _listeners.iterator(); i.hasNext();){
            PropertyChangeListener l = (PropertyChangeListener)i.next();
            l.propertyChange(evt);
        }
    }
}

