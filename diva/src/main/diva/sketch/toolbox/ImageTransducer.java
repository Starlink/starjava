/*
 * $Id: ImageTransducer.java,v 1.4 2001/07/22 22:01:57 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.SketchModel;
import diva.sketch.JSketch;
import diva.sketch.SketchController;
import diva.sketch.StrokeSymbol;
import java.awt.datatransfer.*;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import java.io.IOException;

/**
 * Draw the ink into a bitmap image for cut-and-paste.
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 * @rating Red
 */
public class ImageTransducer implements Transducer, Cloneable {
    /** The amount of padding on each edge of the image, in pixels.
     */
    private static final int PADDING = 5;
    
    private static final DataFlavor[] _flavors = {
        new DataFlavor("image/x-java-image; class=java.awt.Image", "Image") };
    private static final DataFlavor IMAGE_FLAVOR = _flavors[0];
    private SketchModel _model;
    private Image _image;
    private Component _comp;
    
    /**
     * This constructor should only be called once, to create
     * the prototype object.  The given component must be a
     * visible component that is used to create graphics.
     */
    public ImageTransducer(Component comp) {
        _comp = comp;
    }

    /**
     * This is called by the newInstance() method, which builds a new
     * transducer for the given sketch model.
     */
    private void setSketchModel(SketchModel model) {
        _model = model;
    }

    /** Return StringSelection's data flavors.
     */
    public DataFlavor[] getTransferDataFlavors() {
        return _flavors;
    }

    /** Render the selection into an image and return
     * the image.
     */
    public Object getTransferData(DataFlavor flavor)
        throws UnsupportedFlavorException, IOException {
        if(!IMAGE_FLAVOR.equals(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        if(_image == null) {
            //first ungroup all the strokes
            _model = flattenModel(_model);
            //next get the union of the stroke bounding boxes
            Rectangle2D bbox = bboxAll(_model);
            bbox.setFrame(bbox.getX()-PADDING, bbox.getY()-PADDING,
                    bbox.getWidth()+2*PADDING, bbox.getHeight()+2*PADDING);
            
            //next translate the strokes back to the origin.
            translateModel(_model, -bbox.getX(), -bbox.getY());

            //next instantiate a JSketch that will be used to
            //render the strokes, and set the sketch model
            JSketch sketch = new JSketch();
            int width = (int)Math.ceil(bbox.getWidth());
            int height = (int)Math.ceil(bbox.getHeight());
            sketch.setSize(width, height);
            (sketch.getSketchPane().getSketchController()).setSketchModel(_model);

            //finally, render the sketch into an appropriate
            //image buffer.
            _image = (BufferedImage)_comp.createImage(width, height);
            Graphics2D g = (Graphics2D)_image.getGraphics();
            sketch.getSketchPane().paint(g);
        }
        return _image;
    }

    /** Flatten the given model by removing all of its CompositeSymbols.
     * Doesn't modify the given model; returns a new model.
     */
    private static SketchModel flattenModel(SketchModel in) {
        return in; //FIXME
    }

    /** Translate the given model by the given amount.  Assumes a flat
     * model (no CompositeSymbols).  Modifies the given model.
     */
    private static void translateModel(SketchModel in, double dx, double dy) {
        for(Iterator i = in.symbols(); i.hasNext(); ) {
            StrokeSymbol s = (StrokeSymbol)i.next();
            s.getStroke().translate(dx,dy);
            in.updateSymbol(s);
        }
    }

    /** Return the bounding of the given sketch model.  Assumes a flat
     * model (no CompositeSymbols).
     */
    private static Rectangle2D bboxAll(SketchModel in) {
        if(in.getSymbolCount() == 0) {
            return null;
        }
        Iterator i = in.symbols();
        Rectangle2D b = ((StrokeSymbol)i.next()).getStroke().getBounds();
        Rectangle2D bounds = (Rectangle2D)b.clone();
        while(i.hasNext()) {
            StrokeSymbol s = (StrokeSymbol)i.next();
            Rectangle2D.union(bounds, s.getStroke().getBounds(), bounds);
        }
        return bounds;
    }
     
    /** Call StringSelection's predicate.
     */
    public boolean isDataFlavorSupported(DataFlavor in) {
        return IMAGE_FLAVOR.equals(in);
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
            ImageTransducer out = (ImageTransducer)clone();
            out.setSketchModel(in);
            return out;
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Clone failed!");
        }
    }
}

