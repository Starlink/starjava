/*
 * $Id: CompositeSymbol.java,v 1.4 2001/07/22 22:01:41 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch;
import diva.sketch.recognition.TimedStroke;
import java.awt.Color;
import java.awt.Shape;
import java.util.Iterator;

/**
 * A composite symbol that is made up of one or more
 * symbols.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 * @rating Red
 */
public class CompositeSymbol implements Symbol {
    /**
     * Storage for the children.
     */
    private Symbol[] _children;

    /**
     * Create a symbol for the given stroke with the color and line
     * width information.
     */
    public CompositeSymbol(Symbol[] children){
        _children = children;
    }

    /**
     * Return the stroke that's wrapped by this symbol.
     */
    public Symbol[] getChildren(){
        return _children;
    }

    /** Return the outline color of the children if it is
     * the same for all children, or Symbol.MIXED_COLOR if
     * not.
     */
    public Color getOutline() {
	Color c = _children[0].getOutline();
	for(int i = 1; i < _children.length; i++) {
	    if(c != _children[i].getOutline()) {
		return Symbol.MIXED_COLOR;
	    }
	}
	return c;
    }

    /** Return the fill color of the children if it is
     * the same for all children, or Symbol.MIXED_COLOR if
     * not.
     */
    public Color getFill() {
	Color c = _children[0].getFill();
	for(int i = 1; i < _children.length; i++) {
	    if(c != _children[i].getFill()) {
		return Symbol.MIXED_COLOR;
	    }
	}
	return c;
    }

    /** Return the linewidth of the children if it is
     * the same for all children, or Symbol.MIXED_LINEWIDTH if
     * not.
     */
    public float getLineWidth() {
	float f = _children[0].getLineWidth();
	for(int i = 1; i < _children.length; i++) {
	    if(f != _children[i].getLineWidth()) {
		return Symbol.MIXED_LINEWIDTH;
	    }
	}
	return f;
    }

    public void setLineWidth(float lineWidth) {
        for(int i = 0; i < _children.length; i++) {
            _children[i].setLineWidth(lineWidth);
        }
    }

    public void setOutline(Color c) {
        for(int i = 0; i < _children.length; i++) {
            _children[i].setOutline(c);
        }
    }
        
    public void setFill(Color c) {
        for(int i = 0; i < _children.length; i++) {
            _children[i].setFill(c);
        }
    }
}


