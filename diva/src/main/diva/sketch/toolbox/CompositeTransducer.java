/*
 * $Id: CompositeTransducer.java,v 1.3 2001/10/11 21:35:41 michaels Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import java.awt.datatransfer.*;
import diva.sketch.SketchModel;
import java.io.OutputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * A composite class that can be used to multiplex between different
 * transducers.  This transducer supports the union of all the data
 * flavors of the children.
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public class CompositeTransducer implements Transducer, Cloneable {
    /** The sketch we are trying to recognize.
     */
    private HashMap _map;

    /** Our child delegates.
     */
    private Transducer[] _children;

    /** A temporary array that is pre-allocated for our
     * utility.
     */
    private DataFlavor[][] _tmp;
    
    /**
     * This constructor should only be called once, to create
     * the prototype object.  From then on, the newInstance()
     * method should be used to construct 
     */
    public CompositeTransducer(Transducer[] children) {
        _children = children;
        _tmp = new DataFlavor[_children.length][];
    }

    /** Return the union of all the children's data flavors.
     */
    public DataFlavor[] getTransferDataFlavors() {
        System.out.println("Composite's Data Flavors: ");
        DataFlavor[] out = new DataFlavor[_map.keySet().size()];
        _map.keySet().toArray(out);
        for(int i = 0; i < out.length; i++) {
            System.out.println("   " + out[i]);
        }
        return out;
    }

    /** Perform recognition on the sketch model and return the
     * transfer data as a string, or as plain text (based on
     * StringSelection's implementation of getTransferData();
     */
    public Object getTransferData(DataFlavor flavor)
        throws UnsupportedFlavorException, IOException {
        Transducer t = (Transducer)_map.get(flavor);
        if(t == null) {
            throw new UnsupportedFlavorException(flavor);
        }
        return t.getTransferData(flavor);
    }
        
    /** Return whether any of the child transducers support the given
     * flavor.
     */
    public boolean isDataFlavorSupported(DataFlavor in) {
        return (_map.get(in) != null);
    }

    /** Do nothing.
     */
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }
    
    /**
     * Apply the transducer to the given set of strokes by
     * performing sketch recognition on the given strokes.
     */
    public Transducer newInstance(SketchModel in) {
        try {
            CompositeTransducer out = (CompositeTransducer)clone();
            out.setSketchModel(in);
            return out;
        }
        catch(CloneNotSupportedException ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    /** Set the sketch model for a new instance of the transducer.
     * Called by newInstance().  Sets up the mapping of flavors to
     * the transducers that support those transducers.
     *
     * @throw RuntimeException if there are multiple transducers
     *        that support the same type.
     */
    protected void setSketchModel(SketchModel in) throws RuntimeException {
        _map = new HashMap();
        for(int i = 0; i < _children.length; i++) {
            Transducer t = _children[i].newInstance(in);
            DataFlavor[] supported = t.getTransferDataFlavors();
            for(int j = 0; j < supported.length; j++) {
                if(_map.get(supported[j]) != null) {
                    String err = "Duplicate transducers for the same flavor: "
                        + supported[j];
                    throw new RuntimeException(err);
                }
                _map.put(supported[j], t);
            }
        }
    }
}

