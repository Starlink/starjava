/*
 * $Id: TableTransducer.java,v 1.2 2001/07/22 22:01:59 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import java.awt.datatransfer.*;
import diva.sketch.SketchModel;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import java.io.IOException;

/**
 * Recognize the ink as a table and make the recognition
 * available as HTML. FIXME - implement recognition!
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating Red
 */
public class TableTransducer implements Transducer {
    private static final DataFlavor[] _flavors = {
        new DataFlavor("text/html", "HTML Format") };

    private static final DataFlavor HTML_FLAVOR = _flavors[0];

    private static final String _table = "<html><body><table><tr><td>hello</td><td><b>world</b></td></tr></table></body></html>";

    /**
     * This constructor should only be called once, to create
     * the prototype object.  
     */
    public TableTransducer() {
    }

    /**
     * This constructor is called by the newInstance() method,
     * which builds a new transducer for the given sketch
     * model.
     */
    public TableTransducer(SketchModel model) {
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
        if(!HTML_FLAVOR.equals(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        return new StringBufferInputStream(_table);
    }
        
    /** Call StringSelection's predicate.
     */
    public boolean isDataFlavorSupported(DataFlavor in) {
        return HTML_FLAVOR.equals(in);
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
        return new TableTransducer(in);
    }
}

