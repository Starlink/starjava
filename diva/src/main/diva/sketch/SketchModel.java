/*
 * $Id: SketchModel.java,v 1.11 2001/07/22 22:01:41 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * A SketchModel stores sketched symbols.  Symbols can either be
 * individual strokes or groups of strokes, represented by
 * StrokeSymbol and CompositeSymbol, respectively.  When the model
 * changes (symbols have been added, removed, or modified), it
 * generates SketchEvents to notify its listeners that a change has
 * occured.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.11 $
 * @rating Red
 */
public class SketchModel {
    /**
     * A list of SketchEvent listeners.
     */
    private ArrayList _listeners;

    /**
     * A list of symbols that reside in the model.
     */
    private ArrayList _symbols;

    /**
     * Create a SketchModel with no symbols.
     */
    public SketchModel() {
        _symbols = new ArrayList();
        _listeners = new ArrayList();
    }
     
    /**
     * Add the given SketchListener to the set of listeners.
     */
    public void addSketchListener(SketchListener l) {
        _listeners.add(l);
    }

    /**
     * Add the given symbol to the model.
     */
    public void addSymbol(Symbol s) {
        _symbols.add(0, s);
        dispatch(new SketchEvent(SketchEvent.SYMBOL_ADDED, s));
    }

    /**
     * Insert a symbol into the model at the given position (Z order).
     * To insert the symbol just in front of some other symbol, use
     * getSymbol() to get the other symbol's index, and pass
     * <i>index</i> as the first argument. To insert the symbol just
     * behind some other symbol, pass <i>index+1</i> as the first
     * argument. To insert so the symbol displays over the top of
     * other symbols, insert at zero.
     *
     * <p>Clients should assume that an implementation of this method
     * does <i>not</i> check if the symbol is already contained --
     * clients are therefore responsible for being bug-free.
     */
    public void addSymbol(int index, Symbol s) {
        _symbols.add(index, s);
        dispatch(new SketchEvent(SketchEvent.SYMBOL_ADDED, s));
    }

    /**
     * Send the event to the listeners.
     */
    private void dispatch(SketchEvent e) {
        switch(e.getID()) {
            case SketchEvent.SYMBOL_ADDED:
                for(Iterator i = _listeners.iterator(); i.hasNext(); ) {
                    SketchListener sl = (SketchListener)i.next();
                    sl.symbolAdded(e);
                }
                break;
            case SketchEvent.SYMBOL_REMOVED:
                for(Iterator i = _listeners.iterator(); i.hasNext(); ) {
                    SketchListener sl = (SketchListener)i.next();
                    sl.symbolRemoved(e);
                }
                break;
            case SketchEvent.SYMBOL_MODIFIED:
                for(Iterator i = _listeners.iterator(); i.hasNext(); ) {
                    SketchListener sl = (SketchListener)i.next();
                    sl.symbolModified(e);
                }
                break;
        }
    }

    /**
     * Return the number of symbols in this model.
     */
    public int getSymbolCount() {
        return _symbols.size();
    }

    /**
     * Return the index of the given symbol, or -1 if
     * the symbol is not contained by the model.
     */
    public int indexOf(Symbol s) {
        return _symbols.indexOf(s);
    }
    
    /**
     * Remove the specified SketchListener from the set
     * of listeners.
     */
    public void removeSketchListener(SketchListener l) {
        _listeners.remove(l);
    }

    /**
     * Remove the specified symbol from the model.
     */
    public void removeSymbol(Symbol s) {
        _symbols.remove(s);
        dispatch(new SketchEvent(SketchEvent.SYMBOL_REMOVED, s));
    }

    /**
     * Return an iterator over the symbols in the model.
     */
    public Iterator symbols() {
        return new Iterator() {
                Iterator _target = _symbols.iterator();
                Object _prev = null;
                public boolean hasNext() {
                    return _target.hasNext();
                }
                public Object next() {
                    _prev = _target.next();
                    return _prev;
                }
                public void remove() {
                    _target.remove();
                    dispatch(new SketchEvent(SketchEvent.SYMBOL_REMOVED, (Symbol)_prev));
                }
            };
    }

    
    /**
     * Provide the means for a client to notify the listeners that the
     * specified symbol has been updated.
     */
    public void updateSymbol(Symbol s) {
        dispatch(new SketchEvent(SketchEvent.SYMBOL_MODIFIED, s));
    }
}


