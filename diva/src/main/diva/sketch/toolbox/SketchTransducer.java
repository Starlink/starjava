/*
 * $Id: SketchTransducer.java,v 1.4 2001/10/11 21:35:41 michaels Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import java.awt.datatransfer.*;
import diva.sketch.SketchModel;
import diva.sketch.SketchWriter;
import java.io.*;

/**
 * Write a sketch model into an XML string.
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 * @rating Red
 */
public class SketchTransducer implements Transducer {
    private static final DataFlavor[] _flavors = {
        new DataFlavor("application/x-sketch", "Digital ink"),
    };

    /** The data flavor corresponding to sketch strokes
     * serialized in XML.
     */
    public static final DataFlavor SKETCH_FLAVOR = _flavors[0];
    
    /** The sketch we are trying to recognize.
     */
    private SketchModel _model = null;

    /**
     * This constructor should only be called once, to create
     * the prototype object.  
     */
    public SketchTransducer() {
    }

    /**
     * This constructor is called by the newInstance() method,
     * which builds a new transducer for the given sketch
     * model.
     */
    public SketchTransducer(SketchModel model) {
        _model = model;
    }

    /** Return StringSelection's data flavors.
     */
    public DataFlavor[] getTransferDataFlavors() {
        return _flavors;
    }

    /** Perform recognition on the sketch model and return the
     * transfer data as a string, or as plain text (based on
     * StringSelection's implementation of getTransferData();
     */
    public Object getTransferData(DataFlavor flavor)
        throws UnsupportedFlavorException, IOException {
        if(!SKETCH_FLAVOR.equals(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        StringWriter buffer = new StringWriter();
        SketchWriter writer = new SketchWriter();
        writer.writeModel(_model, buffer);
        System.out.println(buffer.toString());
        return new StringBufferInputStream(buffer.toString());
    }
        
    /** Call StringSelection's predicate.
     */
    public boolean isDataFlavorSupported(DataFlavor in) {
        return in.equals(SKETCH_FLAVOR);
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
        return new SketchTransducer(in);
    }
}

