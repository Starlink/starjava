/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: DivaImageGraphics.java,v 1.20 2002/08/20 09:57:58 brighton Exp $
 */

package jsky.image.graphics;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.Double;
import java.lang.Runnable;
import java.util.Iterator;
import javax.media.jai.JAI;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import diva.canvas.AbstractFigure;
import diva.canvas.Figure;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.event.LayerAdapter;
import diva.canvas.event.LayerEvent;
import diva.canvas.interactor.BoundsManipulator;
import diva.canvas.interactor.DragInteractor;
import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.PathManipulator;
import diva.canvas.interactor.SelectionEvent;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.SelectionListener;
import diva.canvas.interactor.SelectionModel;
import diva.canvas.toolbox.BasicHighlighter;
import jsky.coords.CoordinateConverter;
import jsky.graphics.CanvasFigure;
import jsky.graphics.CanvasFigureGroup;
import jsky.graphics.CanvasGraphics;
import jsky.graphics.SelectedAreaListener;
import jsky.image.gui.DivaGraphicsImageDisplay;
import jsky.image.gui.GraphicsImageDisplay;
import jsky.util.gui.BasicWindowMonitor;


/**
 * Implements drawing for image overlays. It is based on the Diva package.
 *
 * See <a href="http://www.gigascale.org/diva/">The Diva package</a>
 *
 * @version $Revision: 1.20 $
 * @author Allan Brighton
 */
public class DivaImageGraphics implements CanvasGraphics {

    /** The image display that we are drawing on */
    private GraphicsImageDisplay _imageDisplay;

    /** The Diva pane (with selection support) to draw on */
    private GraphicsPane _graphicsPane;

    /** The Diva layer to draw on */
    private FigureLayer _figureLayer;

    /** The Diva layer containing the image */
    private ImageLayer _imageLayer;

    /** An interactor that allows selection, dragging and resizing figures */
    private SelectionInteractor _selectionInteractor;

    /** An interactor for lines. */
    private SelectionInteractor _lineInteractor;

    /** An interactor that allows dragging figures */
    private DragInteractor _dragInteractor;

    /** An interactor that allows selecting figures, but not dragging or resizing */
    private SelectionInteractor _fixedSelectionInteractor;

    /** An object that listens for changes in the figure selection */
    private SelectionListener _selectionListener;

    /** Used for selecting all objects in a rectangular region */
    private SelectionDragger _selectionDragger;

    /**
     * Construct an object for drawing on the given image display.
     */
    public DivaImageGraphics(DivaGraphicsImageDisplay imageDisplay) {
        _imageDisplay = imageDisplay;
        _graphicsPane = (GraphicsPane) imageDisplay.getCanvasPane();
        _graphicsPane.setAntialiasing(false);
        _imageLayer = _makeImageLayer();
        _graphicsPane.setBackgroundLayer(_imageLayer);
        _figureLayer = _graphicsPane.getForegroundLayer();
        _graphicsPane.getBackgroundEventLayer().addLayerListener(new LayerAdapter() {

            public void mousePressed(LayerEvent e) {
                clearSelection();
            }
        });

        // make the interactors, which determine how the user can interact with a figure
        _makeSelectionListener();
        _makeSelectionInteractor();
        _makeLineInteractor();
        _makeDragInteractor();
        _makeFixedSelectionInteractor();
        _makeSelectionDragger();
    }

    /** Return the Diva graphics pane. */
    public GraphicsPane getGraphicsPane() {
        return _graphicsPane;
    }

    /**
     * Make and return the image layer. This is the background layer used
     * to display the image, but can also be used by derived classes for
     * drawing directly on the image.
     */
    private ImageLayer _makeImageLayer() {
        return new ImageLayer((DivaGraphicsImageDisplay) _imageDisplay);
    }

    /** Return the Diva layer containing the image */
    public ImageLayer getImageLayer() {
        return _imageLayer;
    }

    /**
     * Make an Interactor for figures that may be selected, resized and moved.
     */
    private void _makeSelectionInteractor() {
        BoundsManipulator boundsManipulator = new BoundsManipulator();
        boundsManipulator.getHandleInteractor().addLayerListener(new LayerAdapter() {

            public void mouseReleased(LayerEvent e) {
                Figure fig = e.getFigureSource();
                if (fig instanceof CanvasFigure) {
                    ((CanvasFigure) fig).fireCanvasFigureEvent(CanvasFigure.RESIZED);
                }
            }
        });

        // Create a movable, resizable selection interactor
        _selectionInteractor = new SelectionInteractor();
        _selectionInteractor.setPrototypeDecorator(boundsManipulator);
        _selectionInteractor.getSelectionModel().addSelectionListener(_selectionListener);
    }

    /**
     * Make an Interactor for lines.
     */
    private void _makeLineInteractor() {
        PathManipulator pathManipulator = new PathManipulator();
        pathManipulator.getHandleInteractor().addLayerListener(new LayerAdapter() {

            public void mouseReleased(LayerEvent e) {
                Figure fig = e.getFigureSource();
                if (fig instanceof CanvasFigure) {
                    ((CanvasFigure) fig).fireCanvasFigureEvent(CanvasFigure.RESIZED);
                }
            }
        });

        // Create a movable, resizable selection interactor
        _lineInteractor = new SelectionInteractor(_selectionInteractor.getSelectionModel());
        _lineInteractor.setPrototypeDecorator(pathManipulator);
        _lineInteractor.getSelectionModel().addSelectionListener(_selectionListener);
    }


    /**
     * Make the object that listens for changes in the figure selection and
     * notifies the target figure listeners.
     */
    private void _makeSelectionListener() {
        _selectionListener = new SelectionListener() {

            public void selectionChanged(SelectionEvent e) {
                try {
                    Iterator it = e.getSelectionAdditions();
                    while (it.hasNext()) {
                        Object o = it.next();
                        if (o instanceof CanvasFigure) {
                            ((CanvasFigure) o).fireCanvasFigureEvent(CanvasFigure.SELECTED);
                        }
                    }
                }
                catch (Exception e2) {
                    // XXX got a null reference in Diva during testing...
                }
                try {
                    Iterator it = e.getSelectionRemovals();
                    while (it.hasNext()) {
                        Object o = it.next();
                        if (o instanceof CanvasFigure) {
                            ((CanvasFigure) o).fireCanvasFigureEvent(CanvasFigure.DESELECTED);
                        }
                    }
                }
                catch (Exception e3) {
                }
            }
        };
    }


    /**
     * Make the drag interactor, for figures that may be dragged.
     */
    private void _makeDragInteractor() {
        _dragInteractor = new DragInteractor();
        _selectionInteractor.addInteractor(_dragInteractor);
        _dragInteractor.addLayerListener(new LayerAdapter() {

            public void mouseReleased(LayerEvent e) {
                Figure fig = e.getFigureSource();
                if (fig instanceof CanvasFigure) {
                    ((CanvasFigure) fig).fireCanvasFigureEvent(CanvasFigure.MOVED);
                }
            }
        });
    }


    /**
     * Make the drag interactor, for figures that may be dragged.
     */
    private void _makeFixedSelectionInteractor() {
        // create an fixed position, unresizable interactor
        SelectionModel model = _selectionInteractor.getSelectionModel();
        _fixedSelectionInteractor = new SelectionInteractor(model);
        // highlighting will draw a partially see-through rectangle over the figure
        Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75F);
        BasicHighlighter h = new BasicHighlighter(new Color(204, 204, 255), 1.0F, composite);
        _fixedSelectionInteractor.setPrototypeDecorator(h);
    }


    /**
     * Make the selection drag-selector (for selecting all objects in a rectangular region)
     */
    private void _makeSelectionDragger() {
        _selectionDragger = new SelectionDragger(_graphicsPane) {

            public void mouseReleased(LayerEvent e) {
                super.mouseReleased(e);
                setEnabled(false);
            }
        };
        _selectionDragger.addSelectionInteractor(_selectionInteractor);
        _selectionDragger.addSelectionInteractor(_lineInteractor);
        _selectionDragger.addSelectionInteractor(_fixedSelectionInteractor);
        _selectionDragger.setEnabled(false);
    }

    /** Used for selecting all objects in a rectangular region */
    public SelectionDragger getSelectionDragger() {
        return _selectionDragger;
    }

    /** Return an interactor that allows dragging figures */
    public DragInteractor getDragInteractor() {
        return _dragInteractor;
    }

    /** Return an interactor that allows selection, dragging and resizing figures */
    public SelectionInteractor getSelectionInteractor() {
        return _selectionInteractor;
    }

    /** Return an interactor for lines */
    public SelectionInteractor getLineInteractor() {
        return _lineInteractor;
    }

    /** Return an interactor that allows selecting figures, but not dragging or resizing */
    public SelectionInteractor getFixedSelectionInteractor() {
        return _fixedSelectionInteractor;
    }


    /**
     * Make and return a labeled figure with the given shape, fill, outline and
     * line width. The shape is expected to be in screen coordinates.
     * If a label is specified, it will be attached to the figure using
     * the given anchor argument for the relative position.
     * <p>
     * The CoordinateConverter object in the image display class may be
     * used while constructing the shape to convert to screen coordinates.
     * <p>
     * Event handling for figures is done through Diva Interactors.
     * A number of common interactors are defined in this class
     * for dragging and selecting figures. When selecting figures,
     * the Diva SelectionModel class can also be used to get notification
     * whenever the selection changes. When dragging figures, the DragIterator
     * class has listener methods that can notify you whenever a figure is
     * dragged.
     *
     * @see CoordinateConverter
     * @see GraphicsImageDisplay
     *
     * @param shape the shape to draw
     * @param fill the paint to use to fill the shape
     * @param outline the paint to use for the outline
     * @param lineWidth the width of the shape lines in pixels
     * @param label an optional label text to be displayed with the figure (may be null)
     * @param anchor SwingConstants value for label position
     * @param labelColor color of the label
     * @param font the label's font
     * @param interactor determines the behavior of the figure (may be null)
     *
     * @return the handle for  the figure
     */
    public CanvasFigure makeLabeledFigure(Shape shape, Paint fill, Paint outline, float lineWidth,
                                          String label, int anchor, Paint labelColor, Font font,
                                          Interactor interactor) {
        if (label == null || label.length() == 0) {
            return new ImageFigure(shape, fill, outline, lineWidth, interactor);
        }
        else {
            return new LabeledImageFigure(new ImageFigure(shape, fill, outline, lineWidth, null),
                    label, anchor, labelColor, font, interactor);
        }
    }


    /**
     * Make and return a figure with the given shape, fill, outline and
     * line width. The shape is expected to be in screen coordinates.
     * If a label is specified, it will be attached to the figure using
     * the given anchor argument for the relative position.
     *
     * @param shape the shape to draw
     * @param fill the paint to use to fill the shape
     * @param outline the paint to use for the outline
     * @param lineWidth the width of the shape lines in pixels
     * @param interactor determines the behavior of the figure (may be null)
     *
     * @return the handle for  the figure
     */
    public CanvasFigure makeFigure(Shape shape, Paint fill, Paint outline, float lineWidth,
                                   Interactor interactor) {
        return new ImageFigure(shape, fill, outline, lineWidth, interactor);
    }


    /**
     * Make and return a labeled figure with the given shape, fill, outline and line width.
     * The shape is expected to be in screen coordinates.
     *
     * @param shape the shape to draw
     * @param fill the paint to use to fill the shape
     * @param outline the paint to use for the outline
     * @param lineWidth the width of the shape lines in pixels
     * @param label an optional label text to be displayed with the figure (may be null)
     * @param anchor SwingConstants value for label position
     * @param labelColor color of the label
     * @param font the label's font
     *
     * @return an object representing the figure
     */
    public CanvasFigure makeLabeledFigure(Shape shape, Paint fill, Paint outline, float lineWidth,
                                          String label, int anchor, Paint labelColor, Font font) {
        return makeLabeledFigure(shape, fill, outline, lineWidth, label, anchor, labelColor, font,
                _fixedSelectionInteractor);
    }

    /**
     * Make and return a figure with the given shape, fill, outline and line width.
     * The shape is expected to be in screen coordinates.
     *
     * @param shape the shape to draw
     * @param fill the paint to use to fill the shape
     * @param outline the paint to use for the outline
     * @param lineWidth the width of the shape lines in pixels
     *
     * @return an object representing the figure
     */
    public CanvasFigure makeFigure(Shape shape, Paint fill, Paint outline, float lineWidth) {
        return makeFigure(shape, fill, outline, lineWidth, _fixedSelectionInteractor);
    }

    /**
     * This is a convenience method for making a labeled rectangle that allows you to
     * specify the type of the coordinates given by the rect argument.
     * The rect argument is converted to screen coordinates before creating the figure as
     * in add(Shape shape, Paint fill, Paint outline, float lineWidth, Interactor interactor).
     */
    public CanvasFigure makeLabeledRectangle(Rectangle2D.Double rect, int coordType, Paint fill,
                                             Paint outline, float lineWidth,
                                             String label, int anchor, Paint labelColor, Font font,
                                             Interactor interactor) {
        CoordinateConverter coordinateConverter = _imageDisplay.getCoordinateConverter();
        Point2D.Double p = new Point2D.Double(rect.x, rect.y);
        coordinateConverter.convertCoords(p, coordType, CoordinateConverter.SCREEN, false);
        Point2D.Double size = new Point2D.Double(rect.width, rect.height);
        coordinateConverter.convertCoords(size, coordType, CoordinateConverter.SCREEN, true);
        Rectangle2D.Double r = new Rectangle2D.Double(p.x, p.y, size.x, size.y);
        return makeLabeledFigure(r, fill, outline, lineWidth, label, anchor, labelColor, font, interactor);
    }

    /**
     * This is a convenience method for making rectangles that allows you to
     * specify the type of the coordinates given by the rect argument.
     * The rect argument is converted to screen coordinates before creating the figure as
     * in add(Shape shape, Paint fill, Paint outline, float lineWidth, Interactor interactor).
     */
    public CanvasFigure makeRectangle(Rectangle2D.Double rect, int coordType, Paint fill,
                                      Paint outline, float lineWidth, Interactor interactor) {
        CoordinateConverter coordinateConverter = _imageDisplay.getCoordinateConverter();
        Point2D.Double p = new Point2D.Double(rect.x, rect.y);
        coordinateConverter.convertCoords(p, coordType, CoordinateConverter.SCREEN, false);
        Point2D.Double size = new Point2D.Double(rect.width, rect.height);
        coordinateConverter.convertCoords(size, coordType, CoordinateConverter.SCREEN, true);
        Rectangle2D.Double r = new Rectangle2D.Double(p.x, p.y, size.x, size.y);
        return makeFigure(r, fill, outline, lineWidth, interactor);
    }

    /**
     * This is a convenience method for drawing a labeled ellipse that allows you to
     * specify the type of the coordinates given by the ellipse argument.
     * The ellipse argument is converted to screen coordinates before creating the figure as
     * in makeFigure(Shape shape, Paint fill, Paint outline, float lineWidth, Interactor interactor).
     */
    public CanvasFigure makeLabeledEllipse(Ellipse2D.Double ellipse, int coordType, Paint fill,
                                           Paint outline, float lineWidth,
                                           String label, int anchor, Paint labelColor, Font font,
                                           Interactor interactor) {
        CoordinateConverter coordinateConverter = _imageDisplay.getCoordinateConverter();
        Point2D.Double p = new Point2D.Double(ellipse.x, ellipse.y);
        coordinateConverter.convertCoords(p, coordType, CoordinateConverter.SCREEN, false);
        Point2D.Double size = new Point2D.Double(ellipse.width, ellipse.height);
        coordinateConverter.convertCoords(size, coordType, CoordinateConverter.SCREEN, true);
        Ellipse2D.Double r = new Ellipse2D.Double(p.x, p.y, size.x, size.y);
        return makeLabeledFigure(r, fill, outline, lineWidth, label, anchor, labelColor, font, interactor);
    }

    /**
     * This is a convenience method for drawing ellipses that allows you to
     * specify the type of the coordinates given by the ellipse argument.
     * The ellipse argument is converted to screen coordinates before creating the figure as
     * in makeFigure(Shape shape, Paint fill, Paint outline, float lineWidth, Interactor interactor).
     */
    public CanvasFigure makeEllipse(Ellipse2D.Double ellipse, int coordType, Paint fill,
                                    Paint outline, float lineWidth, Interactor interactor) {
        CoordinateConverter coordinateConverter = _imageDisplay.getCoordinateConverter();
        Point2D.Double p = new Point2D.Double(ellipse.x, ellipse.y);
        coordinateConverter.convertCoords(p, coordType, CoordinateConverter.SCREEN, false);
        Point2D.Double size = new Point2D.Double(ellipse.width, ellipse.height);
        coordinateConverter.convertCoords(size, coordType, CoordinateConverter.SCREEN, true);
        Ellipse2D.Double r = new Ellipse2D.Double(p.x, p.y, size.x, size.y);
        return makeFigure(r, fill, outline, lineWidth, interactor);
    }


    /**
     * Make and return a canvas label.
     *
     * @param pos the label position
     * @param text the text of the label to draw
     * @param color the paint to use to draw the text
     * @param font the font to use for the label
     * @param interactor determines the behavior of the figure (may be null)
     */
    public CanvasFigure makeLabel(Point2D.Double pos, String text, Paint color,
                                  Font font, Interactor interactor) {
        ImageLabel imageLabel = new ImageLabel(text, pos, color, font, interactor);
        return imageLabel;
    }

    /**
     * Make and return a canvas label.
     *
     * @param text the text of the label to draw
     * @param pos the label position
     * @param fill the paint to use to draw the text
     * @param font the font to use for the label
     */
    public CanvasFigure makeLabel(Point2D.Double pos, String text, Paint color, Font font) {
        ImageLabel imageLabel = new ImageLabel(text, pos, color, font, null);
        return imageLabel;
    }


    /**
     * Make and return a new CanvasFigureGroup object that can be used as a
     * figure container to hold other figures.
     *
     * @param interactor determines the selection behavior of the figure group (may be null)
     */
    public CanvasFigureGroup makeFigureGroup(Interactor interactor) {
        return new ImageFigureGroup(interactor);
    }


    /**
     * Make and return a new CanvasFigureGroup object that can be used as a
     * figure container to hold other figures.
     */
    public CanvasFigureGroup makeFigureGroup() {
        return new ImageFigureGroup(_fixedSelectionInteractor);
    }


    /**
     * Add the given figure to the canvas.
     */
    public void add(CanvasFigure fig) {
        _figureLayer.add((Figure) fig);
    }

    /**
     * Remove the given figure from the display.
     */
    public void remove(CanvasFigure fig) {
        Figure f = (Figure) fig;
        Interactor interactor = f.getInteractor();
        if (interactor instanceof SelectionInteractor) {
            // remove any selection handles, etc.
            SelectionModel model = ((SelectionInteractor) interactor).getSelectionModel();
            if (model.containsSelection(f))
                model.removeSelection(f);
        }
        _figureLayer.remove(f);
    }

    /**
     * Select the given figure.
     */
    public void select(CanvasFigure fig) {
        Interactor i = ((AbstractFigure) fig).getInteractor();
        if (i instanceof SelectionInteractor)
            ((SelectionInteractor) i).getSelectionModel().addSelection(fig);
    }

    /**
     * Deselect the given figure.
     */
    public void deselect(CanvasFigure fig) {
        Interactor i = ((AbstractFigure) fig).getInteractor();
        if (i instanceof SelectionInteractor)
            ((SelectionInteractor) i).getSelectionModel().removeSelection(fig);
    }

    /**
     * Clear the selection.
     */
    public void clearSelection() {
        _selectionInteractor.getSelectionModel().clearSelection();
    }

    /**
     * Schedule the removal of the given figure from the display at a later time.
     * This version may be used to avoid problems with iterators working on a
     * a list of figures that should not be modified inside the loop.
     */
    public void scheduleRemoval(CanvasFigure fig) {
        SwingUtilities.invokeLater(new FigureRemover(fig));
    }


    /**
     * Return the number of figures.
     */
    public int getFigureCount() {
        return _figureLayer.getFigureCount();
    }


    /**
     * Transform all graphics according to the given AffineTransform object.
     */
    public void transform(AffineTransform trans) {
        // iterate over all figures in the foreground layer
        Iterator it = _figureLayer.figures();
        while (it.hasNext()) {
            Figure fig = (Figure) (it.next());
            fig.transform(trans);
        }
    }


    /**
     * Set the interaction mode for the given figure to an OR'ed combination of
     * the following constants: SELECT, MOVE, RESIZE, ROTATE.
     * For example, if mode is (SELECT | MOVE | RESIZE), the figure can be selected,
     * moved, and resized. (Note that MOVE, RESIZE and ROTATE automatically assume
     * SELECT).
     */
    public void setInteractionMode(CanvasFigure fig, int mode) {
        Figure figure = (Figure) fig;
        Interactor interactor = null;
        if ((mode & (ROTATE | MOVE | RESIZE)) != 0)
            mode |= SELECT;

        if (mode == SELECT) {
            interactor = _fixedSelectionInteractor;
        }
        else if (mode == (SELECT | MOVE)) {
            interactor = _dragInteractor;
        }
        else if (mode == (SELECT | MOVE | RESIZE)) {
            interactor = _selectionInteractor;
        }

        // XXX else: implement other combinations

        figure.setInteractor(interactor);
    }


    /** Local class used to remove a figure at a later time */
    private class FigureRemover implements Runnable {

        CanvasFigure fig;

        public FigureRemover(CanvasFigure fig) {
            this.fig = fig;
        }

        public void run() {
            remove(fig);
        }
    }

    /**
     * Wait for the user to drag out an area on the image canvas and then
     * notify the listener with the coordinates of the box.
     */
    public void selectArea(final SelectedAreaListener l) {
        SelectionDragger sd = new SelectionDragger(_graphicsPane) {

            public void mouseReleased(LayerEvent e) {
                super.mouseReleased(e);
                setEnabled(false);
                Rectangle2D r = getSelectedArea();
                if (r != null)
                    l.setSelectedArea(r);
            }
        };
        sd.setEnabled(true);
    }

    /**
     * Schedule a repaint of the window containing the graphics.
     */
    public void repaint() {
        _figureLayer.repaint();
    }

    /**
     * Schedule a repaint of the given area of the window containing the graphics.
     */
    public void repaint(Rectangle2D region) {
        _figureLayer.repaint(region);
    }


    /**
     * test main: usage: java GraphicsImageDisplay <filename>.
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("GraphicsImageDisplay");
        DivaGraphicsImageDisplay imageDisplay = new DivaGraphicsImageDisplay();
        if (args.length > 0) {
            try {
                imageDisplay.setImage(JAI.create("fileload", args[0]));
            }
            catch (Exception e) {
                System.out.println("error: " + e.toString());
                System.exit(1);
            }
        }

        // Add some test objects
        DivaImageGraphics g = new DivaImageGraphics(imageDisplay);
        CoordinateConverter coordinateConverter = imageDisplay.getCoordinateConverter();
        SelectionInteractor si = g.getSelectionInteractor();
        SelectionInteractor fsi = g.getFixedSelectionInteractor();
        DragInteractor di = g.getDragInteractor();
        int anchor = SwingConstants.CENTER;
        Font font = new Font("Dialog", Font.PLAIN, 10);

        Rectangle2D.Double r1 = new Rectangle2D.Double(50., 50., 50., 50.);
        g.add(g.makeLabeledRectangle(r1, CoordinateConverter.USER, null, Color.blue, 2.0F,
                "Test1", anchor, Color.blue, font, fsi));

        Rectangle2D.Double r2 = new Rectangle2D.Double(70., 70., 50., 50.);
        g.add(g.makeLabeledRectangle(r2, CoordinateConverter.USER, null, Color.white, 2.0F,
                "Test2", anchor, Color.blue, font, fsi));

        Ellipse2D.Double e1 = new Ellipse2D.Double(150., 150., 50., 50.);
        g.add(g.makeEllipse(e1, CoordinateConverter.USER, Color.red, Color.white, 2.0F, si));

        Ellipse2D.Double e2 = new Ellipse2D.Double(120., 120., 20., 60.);
        g.add(g.makeEllipse(e2, CoordinateConverter.USER, Color.green, Color.yellow, 2.0F, si));

        Ellipse2D.Double e3 = new Ellipse2D.Double(20., 220., 50., 20.);
        g.add(g.makeEllipse(e3, CoordinateConverter.USER, null, Color.white, 2.0F, si));

        Ellipse2D.Double e4 = new Ellipse2D.Double(55., 200., 10., 40.);
        g.add(g.makeEllipse(e4, CoordinateConverter.USER, Color.white, Color.yellow, 2.0F, di));

        // test labels
        Point2D.Double pos = new Point2D.Double(10., 50.);
        g.add(g.makeLabel(pos, "Test Label", Color.yellow, font));

        // test grouping
        Rectangle2D.Double r3 = new Rectangle2D.Double(150., 50., 50., 50.);
        CanvasFigure f1 = g.makeRectangle(r3, CoordinateConverter.USER, null, Color.yellow, 2.0F, fsi);
        pos = new Point2D.Double(r3.x + 20, r3.y + 20);
        CanvasFigure f2 = g.makeLabel(pos, "Group", Color.yellow, new JLabel().getFont());
        CanvasFigureGroup group = g.makeFigureGroup();
        group.add(f1);
        group.add(f2);
        g.add(group);

        // test transformation
        //double angle = 90.;
        //g.transform(AffineTransform.getRotateInstance(angle * Math.PI/180., 150, 150));

        // test the area selection once
        g.selectArea(new SelectedAreaListener() {

            public void setSelectedArea(Rectangle2D r) {
                System.out.println("Selected area: " + r);
            }
        });

        frame.getContentPane().add(imageDisplay, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }

}

