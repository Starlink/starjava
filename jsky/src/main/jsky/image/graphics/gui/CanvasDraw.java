/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: CanvasDraw.java,v 1.11 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.image.graphics.gui;


import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.swing.AbstractAction;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.MouseInputListener;

import jsky.graphics.CanvasFigure;
import jsky.image.graphics.DivaImageGraphics;
import jsky.image.gui.DivaMainImageDisplay;
import jsky.util.I18N;
import jsky.util.Resources;
import jsky.util.gui.DialogUtil;

import diva.canvas.AbstractFigure;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.SelectionModel;
import diva.canvas.toolbox.BasicFigure;
import diva.canvas.toolbox.LabelFigure;
import diva.util.java2d.Polygon2D;
import diva.util.java2d.Polyline2D;


/**
 * This class defines a set of AbstractAction objects for drawing on the image.
 * These can be used for menu items or buttons in the user interface.
 *
 * @version $Revision: 1.11 $
 * @author Allan Brighton
 */
public class CanvasDraw implements MouseInputListener {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(CanvasDraw.class);

    /** The target image display. */
    protected DivaMainImageDisplay imageDisplay;

    /** Object managing image graphics */
    protected DivaImageGraphics graphics;

    /** list of listeners for change events */
    protected EventListenerList listenerList = new EventListenerList();

    /** Event fired for changes */
    protected ChangeEvent changeEvent = new ChangeEvent(this);

    /** True if mouse was clicked */
    protected boolean mouseClicked = false;

    /** Starting point of drag */
    protected int startX;

    /** Starting point of drag */
    protected int startY;

    /** Used while drawing a polyline */
    protected Polyline2D.Double polyline;

    /** Used while drawing a polygon */
    protected Polygon2D.Double polygon;

    /** Used while drawing freehand */
    protected Polyline2D.Double freehand;

    /** Current figure (during figure creation) */
    protected AbstractFigure figure;

    /** List of figures created by this instance */
    protected LinkedList figureList = new LinkedList();


    // Drawing modes

    /** Mode to select an object */
    public static final int SELECT = 0;

    /** Mode to select objects in a rectangular region */
    public static final int REGION = 1;

    /** Mode to draw a line */
    public static final int LINE = 2;

    /** Mode to draw a rectangle */
    public static final int RECTANGLE = 3;

    /** Mode to draw an ellipse */
    public static final int ELLIPSE = 4;

    /** Mode to draw a polyline */
    public static final int POLYLINE = 5;

    /** Mode to draw a polygon */
    public static final int POLYGON = 6;

    /** Mode to draw a free-hand figure */
    public static final int FREEHAND = 7;

    /** Mode to insert a text label */
    public static final int TEXT = 8;

    /** Drawing mode action names */
    public static final String[] DRAWING_MODES = new String[]{
        "select", // keep in sync with above definitions
        "region",
        "line",
        "rectangle",
        "ellipse",
        "polyline",
        "polygon",
        "freehand",
        "text"
    };

    /** The number of drawing modes. */
    public static final int NUM_DRAWING_MODES = DRAWING_MODES.length;

    /** Drawing mode actions */
    protected AbstractAction[] drawingModeActions = new AbstractAction[NUM_DRAWING_MODES];

    /** Current drawing mode */
    protected int drawingMode = SELECT;


    /** Used to toggle the visibility of all figures */
    protected boolean visible = true;


    // Colors

    /** Colors for color change actions */
    public static final Color[] COLORS = new Color[]{
        Color.black,
        Color.blue,
        Color.cyan,
        Color.darkGray,
        Color.gray,
        Color.green,
        Color.lightGray,
        Color.magenta,
        Color.orange,
        Color.pink,
        Color.red,
        Color.white,
        Color.yellow,
        null
    };

    /** Colors names corresponding to the above array */
    public static final String[] COLOR_NAMES = new String[]{
        "black",
        "blue",
        "cyan",
        "darkGray",
        "gray",
        "green",
        "lightGray",
        "magenta",
        "orange",
        "pink",
        "red",
        "white",
        "yellow",
        null
    };

    /** The number of colors for which fill and outline actions are defined. */
    public static final int NUM_COLORS = COLORS.length;

    /** Current fill paint */
    protected Paint fill = null;

    /** Current outline paint */
    protected Paint outline = Color.white;

    /** Actions to use to set the outline color */
    protected AbstractAction[] outlineActions = new AbstractAction[NUM_COLORS];

    /** Actions to use to set the fill color */
    protected AbstractAction[] fillActions = new AbstractAction[NUM_COLORS];


    // Composites

    /** Composites */
    public static final Composite[] COMPOSITES = new Composite[]{
        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.0F),
        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1F),
        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2F),
        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3F),
        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4F),
        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5F),
        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6F),
        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7F),
        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8F),
        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9F),
        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0F)
    };

    /** Display names for Composites */
    public static final String[] COMPOSITE_NAMES = new String[]{
        "0%", "10%", "20%", "30%", "40%", "50%", "60%", "70%", "80%", "90%", "100%"
    };

    /** The number of composites defined above */
    public static final int NUM_COMPOSITES = COMPOSITES.length;

    /** Actions to use to set the composite */
    protected AbstractAction[] compositeActions = new AbstractAction[NUM_COMPOSITES];

    /** Current composite */
    protected Composite composite = null;


    // Fonts

    /** Fonts for font change actions */
    public static final Font[] FONTS = new Font[]{
        new Font("Dialog", Font.PLAIN, 12),
        new Font("Dialog", Font.ITALIC, 12),
        new Font("Dialog", Font.BOLD, 12),
        new Font("Dialog", Font.PLAIN, 14),
        new Font("Dialog", Font.ITALIC, 14),
        new Font("Dialog", Font.BOLD, 14),
        new Font("Dialog", Font.PLAIN, 18),
        new Font("Dialog", Font.ITALIC, 18),
        new Font("Dialog", Font.BOLD, 18),
        new Font("Dialog", Font.PLAIN, 24),
        new Font("Dialog", Font.ITALIC, 24),
        new Font("Dialog", Font.BOLD, 24)
    };

    /** The number of fonts for which actions are defined. */
    public static final int NUM_FONTS = FONTS.length;

    /** Actions to use to set the font */
    protected AbstractAction[] fontActions = new AbstractAction[NUM_FONTS];

    /** Default font for text items (labels can be resized afterwards) */
    protected Font font = FONTS[3];



    // Line widths

    /** Supported line widths */
    public static final int[] LINE_WIDTHS = {1, 2, 3, 4};

    /** Number of Supported line widths */
    public static final int NUM_LINE_WIDTHS = LINE_WIDTHS.length;

    /** Current line width */
    protected int lineWidth = 1;

    /** Actions to use to set the line width */
    protected AbstractAction[] lineWidthActions = new AbstractAction[NUM_LINE_WIDTHS];


    /** Action to use to delete the selected figure. */
    protected AbstractAction deleteSelectedAction = new AbstractAction(_I18N.getString("deleteSelected")) {

        public void actionPerformed(ActionEvent evt) {
            deleteSelected();
        }
    };

    /** Action to use to remove all figures. */
    protected AbstractAction clearAction = new AbstractAction(_I18N.getString("clear")) {

        public void actionPerformed(ActionEvent evt) {
            clear();
        }
    };

    /** Action to use to toggle the visibility of all figures. */
    protected AbstractAction hideGraphicsAction = new AbstractAction(_I18N.getString("hideGraphics")) {

        public void actionPerformed(ActionEvent evt) {
            hideGraphics();
        }
    };


    /**
     * Create a menu with graphics related items.
     *
     * @param imageDisplay used to access the JCanvas and DivaImageGraphics objects
     */
    public CanvasDraw(DivaMainImageDisplay imageDisplay) {
        this.imageDisplay = imageDisplay;
        graphics = (DivaImageGraphics) imageDisplay.getCanvasGraphics();

        imageDisplay.addMouseListener(this);
        imageDisplay.addMouseMotionListener(this);

        for (int i = 0; i < NUM_DRAWING_MODES; i++)
            drawingModeActions[i] = new DrawingModeAction(i);

        for (int i = 0; i < NUM_LINE_WIDTHS; i++)
            lineWidthActions[i] = new LineWidthAction(i);

        for (int i = 0; i < NUM_COLORS; i++)
            outlineActions[i] = new OutlineAction(COLORS[i]);

        for (int i = 0; i < NUM_COLORS; i++)
            fillActions[i] = new FillAction(COLORS[i]);

        for (int i = 0; i < NUM_COMPOSITES; i++)
            compositeActions[i] = new CompositeAction(COMPOSITE_NAMES[i], COMPOSITES[i]);

        for (int i = 0; i < NUM_FONTS; i++)
            fontActions[i] = new FontAction(FONTS[i]);
    }

    /** Return the target image display. */
    public DivaMainImageDisplay getImageDisplay() {
        return imageDisplay;
    }


    /**
     * Set the drawing mode.
     *
     * @param mode one of the mode constants defined in this class
     */
    public void setDrawingMode(int drawingMode) {
        // XXX set cursor?
        this.drawingMode = drawingMode;

        if (drawingMode == REGION)
            graphics.getSelectionDragger().setEnabled(true);

        fireChange();
    }

    /** Return the current drawing mode */
    public int getDrawingMode() {
        return drawingMode;
    }

    /** Return the action for the given mode */
    public AbstractAction getDrawingModeAction(int drawingMode) {
        return drawingModeActions[drawingMode];
    }


    /**
     * Set the line width.
     */
    public void setLineWidth(int lineWidth) {
        this.lineWidth = lineWidth;

        // apply change to any selected figures
        SelectionModel sm = graphics.getSelectionInteractor().getSelectionModel();
        ListIterator it = figureList.listIterator(0);
        while (it.hasNext()) {
            CanvasFigure fig = (CanvasFigure) it.next();
            if (sm.containsSelection(fig)) {
                if (fig instanceof BasicFigure)
                    ((BasicFigure) fig).setLineWidth(lineWidth);
            }
        }

        fireChange();
    }


    /** Return the current line width for drawing. */
    public int getLineWidth() {
        return lineWidth;
    }

    /** Return the action for the given line width */
    public AbstractAction getLineWidthAction(int i) {
        return lineWidthActions[i];
    }


    /**
     * Set the outline color.
     */
    public void setOutline(Paint outline) {
        this.outline = outline;

        // apply change to any selected figures
        SelectionModel sm = graphics.getSelectionInteractor().getSelectionModel();
        ListIterator it = figureList.listIterator(0);
        while (it.hasNext()) {
            CanvasFigure fig = (CanvasFigure) it.next();
            if (sm.containsSelection(fig)) {
                if (fig instanceof BasicFigure)
                    ((BasicFigure) fig).setStrokePaint(outline);
            }
        }

        fireChange();
    }


    /** Return the current outline color for drawing. */
    public Paint getOutline() {
        return outline;
    }

    /** Return the action for the given outline color */
    public AbstractAction getOutlineAction(int i) {
        return outlineActions[i];
    }


    /**
     * Set the fill color.
     */
    public void setFill(Paint fill) {
        this.fill = fill;

        // apply change to any selected figures
        SelectionModel sm = graphics.getSelectionInteractor().getSelectionModel();
        ListIterator it = figureList.listIterator(0);
        while (it.hasNext()) {
            CanvasFigure fig = (CanvasFigure) it.next();
            if (sm.containsSelection(fig)) {
                if (fig instanceof BasicFigure)
                    ((BasicFigure) fig).setFillPaint(fill);
                else if (fig instanceof LabelFigure)
                    ((LabelFigure) fig).setFillPaint(fill);
            }
        }

        fireChange();
    }


    /** Return the current fill color for drawing. */
    public Paint getFill() {
        return fill;
    }

    /** Return the action for the given fill color */
    public AbstractAction getFillAction(int i) {
        return fillActions[i];
    }


    /**
     * Set the composite (transparency).
     */
    public void setComposite(Composite composite) {
        this.composite = composite;

        // apply change to any selected figures
        SelectionModel sm = graphics.getSelectionInteractor().getSelectionModel();
        ListIterator it = figureList.listIterator(0);
        while (it.hasNext()) {
            CanvasFigure fig = (CanvasFigure) it.next();
            if (sm.containsSelection(fig)) {
                if (fig instanceof BasicFigure)
                    ((BasicFigure) fig).setComposite((AlphaComposite) composite); // XXX cast for diva-28Jan02
            }
        }

        fireChange();
    }


    /** Return the current composite composite for drawing. */
    public Composite getComposite() {
        return composite;
    }

    /** Return the action for the given composite composite */
    public AbstractAction getCompositeAction(int i) {
        return compositeActions[i];
    }


    /**
     * Set the font to use for labels.
     */
    public void setFont(Font font) {
        this.font = font;

        // apply change to any selected figures
        SelectionModel sm = graphics.getSelectionInteractor().getSelectionModel();
        ListIterator it = figureList.listIterator(0);
        while (it.hasNext()) {
            CanvasFigure fig = (CanvasFigure) it.next();
            if (sm.containsSelection(fig)) {
                if (fig instanceof LabelFigure)
                    ((LabelFigure) fig).setFont(font);
            }
        }

        fireChange();
    }


    /** Return the current font color for drawing. */
    public Font getFont() {
        return font;
    }

    /** Return the action for the given font color */
    public AbstractAction getFontAction(int i) {
        return fontActions[i];
    }


    /**
     * register to receive change events from this object whenever the
     * drawing settings are changed.
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    /**
     * Stop receiving change events from this object.
     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    /**
     * Notify any listeners of a change in the image or cut levels.
     */
    protected void fireChange() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
    }


    // For the MouseInputListener interface

    public void mouseClicked(MouseEvent e) {
        int count = e.getClickCount();
        SelectionInteractor si = graphics.getSelectionInteractor();

        if (mouseClicked) {  // clicked previously?
            if (drawingMode == POLYLINE) {
                if (count == 1) {
                    polyline = (Polyline2D.Double) figure.getShape();
                    return;
                }
                else if (count > 1) {
                    figure.setInteractor(si);
                }
            }
            else if (drawingMode == POLYGON) {
                if (count == 1) {
                    polygon = (Polygon2D.Double) figure.getShape();
                    return;
                }
                else if (count > 1) {
                    figure.setInteractor(si);
                }
            }
            else if (drawingMode == FREEHAND) {
                if (count == 1) {
                    freehand = (Polyline2D.Double) figure.getShape();
                    return;
                }
                else if (count > 1) {
                    figure.setInteractor(si);
                }
            }

            // finish drawing figure
            finishFigure();
        }
        else {
            mouseClicked = true;
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        if ((drawingMode == POLYLINE && polyline != null)
                || (drawingMode == POLYGON && polygon != null)
                || (drawingMode == FREEHAND && freehand != null))
            return;

        startX = e.getX();
        startY = e.getY();
        SelectionInteractor interactor = graphics.getSelectionInteractor();

        Shape shape = null;
        figure = null;
        switch (drawingMode) {
        case LINE:
            shape = new Polyline2D.Double(startX, startY, startX + 1, startY + 1);
            //interactor = graphics.getLineInteractor();
            break;
        case RECTANGLE:
            shape = new Rectangle2D.Double(startX, startY, 1, 1);
            break;
        case ELLIPSE:
            shape = new Ellipse2D.Double(startX, startY, 1, 1);
            break;
        case POLYLINE:
            polyline = new Polyline2D.Double();
            polyline.moveTo(startX, startY);
            shape = polyline;
            interactor = null;
            break;
        case POLYGON:
            polygon = new Polygon2D.Double();
            polygon.moveTo(startX, startY);
            shape = polygon;
            interactor = null;
            break;
        case FREEHAND:
            freehand = new Polyline2D.Double();
            freehand.moveTo(startX, startY);
            shape = freehand;
            break;
        case TEXT:
            return;
        }

        if (shape != null) {
            figure = (AbstractFigure) graphics.makeFigure(shape, fill, outline, lineWidth, interactor);
            graphics.add((CanvasFigure) figure);
            figureList.add(figure);
        }
    }

    public void mouseReleased(MouseEvent e) {
        switch (drawingMode) {
        case POLYLINE:
            if (polyline != null)
                polyline = (Polyline2D.Double) figure.getShape();
            else if (startX != e.getX() || startY != e.getY())
                finishFigure();
            break;

        case POLYGON:
            if (polygon != null)
                polygon = (Polygon2D.Double) figure.getShape();
            else if (startX != e.getX() || startY != e.getY())
                finishFigure();
            break;

        case TEXT:
            Point2D.Double pos = new Point2D.Double(startX, startY);
            // XXX eventually implement labels that you can edit
            String s = DialogUtil.input(_I18N.getString("pleaseEnterLabelText"));
            if (s != null && s.length() != 0) {
                figure = (AbstractFigure) graphics.makeLabel(pos, s, outline, font, graphics.getSelectionInteractor());
                addFigure((CanvasFigure) figure);
                finishFigure();
                return;
            }
            break;

        default:
            if (startX != e.getX() || startY != e.getY())
                finishFigure();
        }
    }

    public void mouseDragged(MouseEvent e) {
        if (figure != null) {
            int endX = e.getX();
            int endY = e.getY();
            int n;

            Shape shape = null;
            switch (drawingMode) {
            case LINE:
                shape = new Polyline2D.Double(startX, startY, endX, endY);
                break;
            case RECTANGLE:
                shape = new Rectangle2D.Double(startX, startY, endX - startX, endY - startY);
                break;
            case ELLIPSE:
                shape = new Ellipse2D.Double(startX, startY, endX - startX, endY - startY);
                break;
            case POLYLINE:
                n = polyline.getVertexCount();
                Polyline2D.Double pl = new Polyline2D.Double();
                pl.moveTo(polyline.getX(0), polyline.getY(0));
                for (int i = 1; i < n; i++) {
                    pl.lineTo(polyline.getX(i), polyline.getY(i));
                }
                pl.lineTo(endX, endY);
                shape = pl;
                break;
            case POLYGON:
                n = polygon.getVertexCount();
                Polygon2D.Double pg = new Polygon2D.Double();
                pg.moveTo(polygon.getX(0), polygon.getY(0));
                for (int i = 1; i < n; i++) {
                    pg.lineTo(polygon.getX(i), polygon.getY(i));
                }
                pg.lineTo(endX, endY);
                shape = pg;
                break;
            case FREEHAND:
                freehand.lineTo(endX, endY);
                shape = freehand;
                break;
            case TEXT:
                return;
            }

            if (shape != null) {
                ((BasicFigure) figure).setShape(shape);
            }
        }
    }

    public void mouseMoved(MouseEvent e) {
        mouseDragged(e);
    }

    /** Finish off the current figure and select it */
    protected void finishFigure() {
        if (figure != null) {
            graphics.clearSelection();
            graphics.select((CanvasFigure) figure);
            figure = null;
        }
        polyline = null;
        polygon = null;
        freehand = null;
        setDrawingMode(SELECT);
        mouseClicked = false;
    }

    /** Remove all figures created by this instance. */
    public void clear() {
        ListIterator it = figureList.listIterator(0);
        while (it.hasNext()) {
            CanvasFigure fig = (CanvasFigure) it.next();
            graphics.remove((CanvasFigure) fig);
            it.remove();
        }
    }

    /** Delete the selected figures. */
    public void deleteSelected() {
        SelectionModel sm = graphics.getSelectionInteractor().getSelectionModel();
        ListIterator it = figureList.listIterator(0);
        while (it.hasNext()) {
            CanvasFigure fig = (CanvasFigure) it.next();
            if (sm.containsSelection(fig)) {
                graphics.remove((CanvasFigure) fig);
                it.remove();
            }
        }
    }

    /** Toggle the visibility all figures created by this instance. */
    public void hideGraphics() {
        visible = !visible;
        ListIterator it = figureList.listIterator(0);
        while (it.hasNext()) {
            CanvasFigure fig = (CanvasFigure) it.next();
            fig.setVisible(visible);
        }
    }

    /** Return a list of figures managed by this instance. */
    public LinkedList getFigureList() {
        return figureList;
    }

    /** Add the given figure to the list of managed figures. */
    public void addFigure(CanvasFigure fig) {
        figureList.add(fig);
        graphics.add(fig);
    }


    // -- Local Action classes --


    /** Local base class for creating menu/toolbar actions. */
    abstract class GraphicsAction extends AbstractAction {

        String name;

        public GraphicsAction(String name) {
            super(null, Resources.getIcon(name + ".gif"));
            this.name = name;
        }
    }

    /** Local class used to set the drawing mode. */
    class DrawingModeAction extends GraphicsAction {

        int drawingMode;

        public DrawingModeAction(int drawingMode) {
            super(DRAWING_MODES[drawingMode]);
            this.drawingMode = drawingMode;
        }

        public void actionPerformed(ActionEvent evt) {
            setDrawingMode(drawingMode);
        }
    }

    /** Local class used to set the line width. */
    class LineWidthAction extends GraphicsAction {

        int width;

        public LineWidthAction(int i) {
            super("width" + (++i));
            width = i;
        }

        public void actionPerformed(ActionEvent evt) {
            setLineWidth(width);
        }
    }

    /** Local class used to set the outline color for figures. */
    class OutlineAction extends AbstractAction {

        Paint color;

        public OutlineAction(Paint color) {
            super(color != null ? "    " : _I18N.getString("none"));
            this.color = color;
        }

        public void actionPerformed(ActionEvent evt) {
            setOutline(color);
        }
    }

    /** Local class used to set the fill color for figures. */
    class FillAction extends AbstractAction {

        Paint color;

        public FillAction(Paint color) {
            super(color != null ? "    " : _I18N.getString("none"));
            this.color = color;
        }

        public void actionPerformed(ActionEvent evt) {
            setFill(color);
        }
    }

    /** Local class used to set the transparency for figures. */
    class CompositeAction extends AbstractAction {

        Composite composite;

        public CompositeAction(String label, Composite composite) {
            super(label);
            this.composite = composite;
        }

        public void actionPerformed(ActionEvent evt) {
            setComposite(composite);
        }
    }

    /** Local class used to set the label font. */
    class FontAction extends AbstractAction {

        Font font;

        public FontAction(Font font) {
            super("abc");
            this.font = font;
        }

        public void actionPerformed(ActionEvent evt) {
            setFont(font);
        }
    }

}
