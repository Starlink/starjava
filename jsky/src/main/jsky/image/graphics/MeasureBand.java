/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: MeasureBand.java,v 1.9 2002/08/20 09:57:58 brighton Exp $
 */

package jsky.image.graphics;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.Locale;
import javax.media.jai.JAI;
import javax.swing.JFrame;
import javax.swing.SwingConstants;

import diva.canvas.CompositeFigure;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.event.EventLayer;
import diva.canvas.event.LayerEvent;
import diva.canvas.event.MouseFilter;
import diva.canvas.interactor.DragInteractor;
import diva.canvas.toolbox.BasicFigure;
import diva.canvas.toolbox.LabelFigure;
import diva.util.java2d.Polyline2D;
import jsky.coords.CoordinateConverter;
import jsky.coords.WorldCoords;
import jsky.image.gui.DivaGraphicsImageDisplay;
import jsky.image.gui.GraphicsImageDisplay;
import jsky.util.gui.BasicWindowMonitor;

/**
 * Class to display a "measure band" showing the distance between
 * 2 points in world coordinates.
 *
 * @version $Revision: 1.9 $
 * @author Allan Brighton
 */
public class MeasureBand extends DragInteractor {

    /** The image display that we are drawing on */
    protected DivaGraphicsImageDisplay imageDisplay;

    /** The object used to convert image coordinates. */
    CoordinateConverter coordinateConverter;

    /* The figure layer */
    protected FigureLayer figureLayer;

    /* The event layer */
    protected EventLayer eventLayer;

    /* The measure-band figure (made up of sub figures below) */
    protected CompositeFigure mband = null;

    /** Diagonal line with arrows */
    protected BasicFigure mbandLine;

    /** Angle lines (horizontal and vertical) */
    protected BasicFigure mbandAngle;

    /** box around width label */
    protected BasicFigure mbandWidthRect;

    /** box around height label */
    protected BasicFigure mbandHeightRect;

    /** box around diagonal label */
    protected BasicFigure mbandDiagRect;

    /** The width label */
    protected LabelFigure mbandWidthText;

    /** The height label */
    protected LabelFigure mbandHeightText;

    /** The diagonal label */
    protected LabelFigure mbandDiagText;


    /* The origin X coordinate */
    protected double originX;

    /* The origin Y coordinate */
    protected double originY;

    /** Used to filter mouse events */
    protected MouseFilter dragFilter;

    /** Used to format values as strings. */
    protected NumberFormat nf;

    /**
     * Create a new MeasureBand attached to the given graphics
     * pane.
     */
    public MeasureBand(DivaGraphicsImageDisplay imageDisplay) {
        this.imageDisplay = imageDisplay;
        coordinateConverter = imageDisplay.getCoordinateConverter();

        nf = NumberFormat.getInstance(Locale.US);
        nf.setMinimumIntegerDigits(2);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        // The measure-band figure (made up of sub figures below)
        mband = new CompositeFigure();

        // Diagonal line
        mbandLine = new BasicFigure(new GeneralPath(), 1.0f);
        mbandLine.setStrokePaint(Color.white);
        mband.add(mbandLine);

        // Angle lines (horizontal and vertical)
        mbandAngle = new BasicFigure(new Polyline2D.Double(), 1.0f);
        mbandAngle.setStrokePaint(Color.white);
        mbandAngle.setStroke(new BasicStroke(1,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL,
                0,
                new float[]{4, 4},
                0));
        mband.add(mbandAngle);

        // box around width label
        mbandWidthRect = new BasicFigure(new Rectangle2D.Double(), Color.yellow);
        mband.add(mbandWidthRect);

        // box around height label
        mbandHeightRect = new BasicFigure(new Rectangle2D.Double(), Color.yellow);
        mband.add(mbandHeightRect);

        // box around diagonal label
        mbandDiagRect = new BasicFigure(new Rectangle2D.Double(), Color.yellow);
        mband.add(mbandDiagRect);

        // The width label
        mbandWidthText = new LabelFigure(" "); // note: diva-0.3 bug with empty string
        mbandWidthText.setFillPaint(Color.blue);
        Font font = new Font("Dialog", Font.BOLD, 12);
        mbandWidthText.setFont(font);
        mband.add(mbandWidthText);

        // The height label
        mbandHeightText = new LabelFigure(" ");
        mbandHeightText.setFillPaint(Color.blue);
        mbandHeightText.setFont(font);
        mband.add(mbandHeightText);

        // The diagonal label
        mbandDiagText = new LabelFigure(" ");
        mbandDiagText.setFillPaint(Color.blue);
        mbandDiagText.setFont(font);
        mband.add(mbandDiagText);

        GraphicsPane gpane = (GraphicsPane) imageDisplay.getCanvasPane();
        setFigureLayer(gpane.getForegroundLayer());
        setEventLayer(gpane.getBackgroundEventLayer());

        // Bind to mouse button 3 (and 2, in case there are only 2 buttons)
        dragFilter = new MouseFilter(InputEvent.BUTTON3_MASK | InputEvent.BUTTON2_MASK, 0, 0);
    }

    /**
     * Get the layer that figures are selected on
     */
    public FigureLayer getFigureLayer() {
        return figureLayer;
    }

    /**
     * Get the layer that drag events are listened on
     */
    public EventLayer getEventLayer() {
        return eventLayer;
    }

    /**
     * Set the layer that figures are selected on
     */
    public void setFigureLayer(FigureLayer l) {
        figureLayer = l;
    }

    /**
     * Set the layer that drag events are listened on
     */
    public void setEventLayer(EventLayer l) {
        if (eventLayer != null) {
            eventLayer.removeLayerListener(this);
        }
        eventLayer = l;
        eventLayer.addLayerListener(this);
    }

    /** Set the enabled state. */
    public void setEnabled(boolean enabled) {
        if (eventLayer != null)
            eventLayer.removeLayerListener(this);
        if (enabled)
            eventLayer.addLayerListener(this);
    }

    /** Clear the selection, and create the rubber-band
     */
    public void mousePressed(LayerEvent event) {
        if (!isEnabled() || !dragFilter.accept(event)) {
            return;
        }

        // Do it
        originX = event.getLayerX();
        originY = event.getLayerY();
        boolean showAngle = !(event.isShiftDown() || event.isControlDown());
        updateMBand(originX, originY, originX, originY, showAngle);

        figureLayer.add(mband);
        figureLayer.repaint(mband.getBounds());
    }

    /** Reshape the rubber-band, swapping coordinates if necessary.
     */
    public void mouseDragged(LayerEvent event) {
        if (!isEnabled() || !dragFilter.accept(event)) {
            return;
        }
        double x = event.getLayerX();
        double y = event.getLayerY();
        double w;
        double h;

        // Figure out the coordinates of the rubber band
        figureLayer.repaint(mband.getBounds());

        boolean showAngle = !(event.isShiftDown() || event.isControlDown());
        updateMBand(originX, originY, event.getLayerX(), event.getLayerY(), showAngle);

        figureLayer.repaint(mband.getBounds());
    }

    /** Delete the rubber-band
     */
    public void mouseReleased(LayerEvent event) {
        if (!isEnabled() || !dragFilter.accept(event)) {
            return;
        }
        figureLayer.repaint(mband.getBounds());
        figureLayer.remove(mband);
    }

    /*
     * Utility method to format a floting point value in arcsec as minutes and seconds and
     * return the new String.
     */
    public String formatHM(double val) {
        int sign = 1;
        if (val < 0.0) {
            sign = -1;
            val = -val;
        }
        double dd = val + 0.0000000001;
        double md = dd / 60;
        int min = (int) md;
        double sec = (md - min) * 60;
        if (min != 0.0) {
            return Integer.toString(min * sign) + ":" + nf.format(sec);
        }
        else {
            return nf.format(sec * sign);
        }
    }


    /**
     * Update the measure band.
     *
     * @param x0 the X screen coordinate of the starting point of the drag
     * @param y0 the Y screen coordinate of the starting point of the drag
     * @param x1 the X screen coordinate of the mouse pointer
     * @param y1 the Y screen coordinate of the mouse pointer
     * @param showAngle if true, show the mbandAngle item, otherwise only the diagonal line.
     */
    protected void updateMBand(double x0, double y0, double x1, double y1, boolean showAngle) {
        if (!imageDisplay.isWCS())
            return;

        // note: wcs coords are not linear, so we need all 3 points in wcs
        Point2D.Double p0 = new Point2D.Double(x0, y0);
        Point2D.Double p1 = new Point2D.Double(x1, y1);
        Point2D.Double p2 = new Point2D.Double(x1, y0);

        try {
            coordinateConverter.screenToWorldCoords(p0, false);
            coordinateConverter.screenToWorldCoords(p1, false);
            coordinateConverter.screenToWorldCoords(p2, false);
        }
        catch (Exception e) {
            // ignore if outside image bounds
            return;
        }

        // get distances in arcmin
        double dist = WorldCoords.dist(p0.x, p0.y, p1.x, p1.y) * 60.;
        String distStr = formatHM(dist);
        String widthStr = null, heightStr = null;
        if (showAngle) {
            double width = WorldCoords.dist(p0.x, p0.y, p2.x, p2.y) * 60.;
            widthStr = formatHM(width);
            double height = WorldCoords.dist(p2.x, p2.y, p1.x, p1.y) * 60;
            heightStr = formatHM(height);
        }

        // calculate screen coords for lines and labels and
        // try to keep the labels out of the way so they don't block anything
        double mx = (x0 + x1) / 2;
        double my = (y0 + y1) / 2;
        int offset = 10;		// offset of labels from lines

        // label anchors
        int diagAnchor = SwingConstants.CENTER;
        int widthAnchor = SwingConstants.CENTER;
        int heightAnchor = SwingConstants.CENTER;

        int diagXOffset = 0,	// x,y offsets for labels
                diagYOffset = 0,
                widthYOffset = 0,
                heightXOffset = 0;

        if (Math.abs(y0 - y1) < 5) {
            diagAnchor = SwingConstants.SOUTH;
            diagYOffset = offset;
            showAngle = false;
        }
        else if (y0 < y1) {
            widthAnchor = SwingConstants.SOUTH;
            widthYOffset = -offset;
        }
        else {
            widthAnchor = SwingConstants.NORTH;
            widthYOffset = offset;
        }

        if (Math.abs(x0 - x1) < 5) {
            diagAnchor = SwingConstants.WEST;
            diagXOffset = offset;
            diagYOffset = 0;
            showAngle = false;
        }
        else if (x0 < x1) {
            diagAnchor = SwingConstants.SOUTH_EAST;
            diagXOffset = -offset;
            diagYOffset = offset;
            heightAnchor = SwingConstants.WEST;
            heightXOffset = offset;
        }
        else {
            diagAnchor = SwingConstants.NORTH_WEST;
            diagXOffset = offset;
            diagYOffset = -offset;
            heightAnchor = SwingConstants.EAST;
            heightXOffset = -offset;
        }

        // set diagonal line coords and arrows
        GeneralPath path = new GeneralPath();
        ShapeUtil.addArrowLine(path, new Point2D.Double(x0, y0), new Point2D.Double(x1, y1));
        mbandLine.setShape(path);

        // adjust labels
        mbandDiagText.translateTo(mx + diagXOffset, my + diagYOffset);
        mbandDiagText.setString(distStr);
        mbandDiagText.setAnchor(diagAnchor);
        mbandDiagRect.setShape(getBoundsWithPadding(mbandDiagText));

        if (showAngle) {
            // show the width and height labels and lines
            mbandAngle.setVisible(true);
            mbandWidthText.setVisible(true);
            mbandWidthRect.setVisible(true);
            mbandHeightText.setVisible(true);
            mbandHeightRect.setVisible(true);

            // set angle line coords
            Polyline2D.Double pl = new Polyline2D.Double(x0, y0, x1, y0);
            pl.lineTo(x1, y1);
            mbandAngle.setShape(pl);

            mbandWidthText.translateTo(mx, y0 + widthYOffset);
            mbandWidthText.setString(widthStr);
            mbandWidthText.setAnchor(widthAnchor);
            mbandWidthRect.setShape(getBoundsWithPadding(mbandWidthText));

            mbandHeightText.translateTo(x1 + heightXOffset, my);
            mbandHeightText.setString(heightStr);
            mbandHeightText.setAnchor(heightAnchor);
            mbandHeightRect.setShape(getBoundsWithPadding(mbandHeightText));
        }
        else {
            // hide the width and height labels and lines
            mbandAngle.setVisible(false);

            mbandWidthText.setString(" ");
            mbandWidthText.setVisible(false);

            mbandWidthRect.setVisible(false);

            mbandHeightText.setString(" ");
            mbandHeightText.setVisible(false);

            mbandHeightRect.setVisible(false);
        }
    }


    /** Return the bounds of the given label with padding */
    protected Rectangle2D getBoundsWithPadding(LabelFigure fig) {
        Rectangle2D bounds = fig.getBounds();
        bounds.setFrame(bounds.getX() - 1,
                bounds.getY() - 1,
                bounds.getWidth() + 2,
                bounds.getHeight() + 2);
        return bounds;
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

        new MeasureBand(imageDisplay);

        frame.getContentPane().add(imageDisplay, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}
