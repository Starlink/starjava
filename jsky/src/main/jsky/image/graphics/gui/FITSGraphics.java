/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: FITSGraphics.java,v 1.8 2002/08/20 09:57:58 brighton Exp $
 */

package jsky.image.graphics.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.util.LinkedList;
import java.util.ListIterator;

import jsky.coords.CoordinateConverter;
import jsky.graphics.CanvasFigure;
import jsky.image.fits.codec.FITSImage;
import jsky.image.graphics.DivaImageGraphics;
import jsky.image.graphics.ImageFigure;
import jsky.image.graphics.ImageLabel;
import jsky.image.gui.DivaMainImageDisplay;
import jsky.util.TclUtil;
import jsky.util.gui.DialogUtil;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTable;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.FitsFactory;
import nom.tam.fits.Header;
import nom.tam.fits.TableHDU;

import diva.canvas.interactor.SelectionInteractor;
import diva.util.java2d.Polygon2D;
import diva.util.java2d.Polyline2D;


/**
 * This class allows you to save the current image graphics to a FITS binary table and
 * reload it again later.
 *
 * @version $Revision: 1.8 $
 * @author Allan Brighton
 */
public class FITSGraphics {

    /** The target image display. */
    protected DivaMainImageDisplay imageDisplay;

    /** Object managing image graphics */
    protected DivaImageGraphics graphics;

    /** User interface object managing a list of graphics objects. */
    protected CanvasDraw canvasDraw;

    /** Handles selections for graphics objects. */
    protected SelectionInteractor interactor;

    /** Maps a skycat pattern (pat0..15) index to CanvasDraw composite index */
    protected static final int[] PATTERNS = {
        10, 9, 8, 6, 4, 2, 1, 0, 9, 8, 7, 6, 5, 4, 3, 1
    };


    /**
     * Initialize with the image display object.
     *
     * @param imageDisplay used to access the JCanvas and DivaImageGraphics objects
     */
    public FITSGraphics(DivaMainImageDisplay imageDisplay) {
        this.imageDisplay = imageDisplay;
        graphics = (DivaImageGraphics) imageDisplay.getCanvasGraphics();
        canvasDraw = (CanvasDraw) imageDisplay.getCanvasDraw();
        interactor = graphics.getSelectionInteractor();
    }


    /**
     * Save the current image graphics to a binary FITS table with the given name
     * in the current image.
     */
    public void saveGraphicsWithImage(String extName) throws FitsException {
        FITSImage fitsImage = imageDisplay.getFitsImage();
        if (fitsImage == null) {
            DialogUtil.error("Graphics can only be saved in a FITS image");
            return;
        }

        LinkedList figureList = canvasDraw.getFigureList();
        if (figureList.size() == 0) {
            return;
        }

        int n = figureList.size();
        String[] type = new String[n];
        String[] coords = new String[n];
        String[] config = new String[n];

        ListIterator it = figureList.listIterator(0);
        int i = 0;
        while (it.hasNext()) {
            CanvasFigure cfig = (CanvasFigure) it.next();
            if (cfig instanceof ImageFigure) {
                ImageFigure fig = (ImageFigure) cfig;
                Shape shape = fig.getShape();
                Paint fill = fig.getFillPaint();
                Paint outline = fig.getStrokePaint();
                float lineWidth = fig.getLineWidth();
                Composite composite = fig.getComposite();
                type[i] = getType(shape);
                coords[i] = getCoords(shape);
                config[i] = getConfig(fill, outline, (int) lineWidth, composite);
            }
            else if (cfig instanceof ImageLabel) {
                ImageLabel fig = (ImageLabel) cfig;
                Font font = fig.getFont();
                String text = fig.getString();
                Paint fill = fig.getFillPaint();
                type[i] = "text";
                coords[i] = getCoords((Point2D.Double) fig.getAnchorPoint());
                config[i] = getConfig(text, font, fill);
            }
            i++;
        }

        Fits fits = fitsImage.getFits();
        BinaryTable table = new BinaryTable();
        FitsFactory.setUseAsciiTables(false);
        table.addColumn(type);
        table.addColumn(coords);
        table.addColumn(config);
        BinaryTableHDU hdu = (BinaryTableHDU) Fits.makeHDU(table);
        hdu.getHeader().addValue("EXTNAME", extName, "Contains saved JSkyCat graphics");
        hdu.setColumnName(0, "type", null);
        hdu.setColumnName(1, "coords", null);
        hdu.setColumnName(2, "config", null);
        deleteHDU(extName);
        fits.addHDU(hdu);
        imageDisplay.checkExtensions(true);
    }


    /**
     * If a binary table with the given name is found in the current image,
     * load the previously saved image graphics from it.
     */
    public void loadGraphicsFromImage(String extName) {
        FITSImage fitsImage = imageDisplay.getFitsImage();
        if (fitsImage == null)
            return;
        int n = fitsImage.getNumHDUs();
        if (n <= 1)
            return;

        for (int i = 0; i < n; i++) {
            BasicHDU hdu = fitsImage.getHDU(i);
            if (hdu instanceof TableHDU) {
                Header header = hdu.getHeader();
                String name = header.getStringValue("EXTNAME");
                if (name != null && name.equals(extName)) {
                    try {
                        loadGraphicsFromImage((TableHDU) hdu);
                    }
                    catch (Exception e) {
                        DialogUtil.error(e);
                    }
                    return;
                }
            }
        }
    }


    /**
     * Load previously saved graphics from the given FITS binary table.
     */
    public void loadGraphicsFromImage(TableHDU hdu) throws FitsException {
        int nrows = hdu.getNRows();
        int ncols = hdu.getNCols();

        if (nrows <= 0)
            return;

        if (ncols != 3)
            return;

        // The table items are in Tcl list format, the graphics syntax is based on Tk
        for (int rowNum = 0; rowNum < nrows; rowNum++) {
            Object[] row = hdu.getRow(rowNum);

            // Tk canvas shape name
            String type = (String) row[0];

            // coordinates
            double[] c = getCoords(TclUtil.splitList((String) row[1]));

            // Tk canvas item config
            String[] config = TclUtil.splitList((String) row[2]);

            Shape shape = null;
            Paint fill = null;
            Paint outline = Color.white;
            float lineWidth = 1;
            Composite composite = null;
            Font font = CanvasDraw.FONTS[3];
            String text = null;

            // parse the configuration options: {-opt arg} {-opt arg} ...
            for (int i = 0; i < config.length; i++) {
                String[] optArg = TclUtil.splitList(config[i]);
                if (optArg.length != 2)
                    continue;
                if (optArg[0].equals("-fill")) {
                    fill = getColor(optArg[1]);
                }
                else if (optArg[0].equals("-outline")) {
                    outline = getColor(optArg[1]);
                }
                else if (optArg[0].equals("-width")) {
                    lineWidth = Float.parseFloat(optArg[1]);
                }
                else if (optArg[0].equals("-font")) {
                    font = getFont(optArg[1]);
                }
                else if (optArg[0].equals("-text")) {
                    text = optArg[1];
                }
                else if (optArg[0].equals("-stipple")) {
                    composite = getStipple(optArg[1]);
                }
                else if (optArg[0].equals("-composite")) {
                    composite = getComposite(optArg[1]);
                }
            }

            // parse the shape
            if (type.equals("rectangle")) {
                // rectangle x1 y1 x2 y2
                shape = new Rectangle2D.Double(c[0], c[1], c[2] - c[0], c[3] - c[1]);
            }
            else if (type.equals("oval")) {
                // oval x1 y1 x2 y2
                shape = new Ellipse2D.Double(c[0], c[1], c[2] - c[0], c[3] - c[1]);
            }
            else if (type.equals("line")) {
                if (outline == null)
                    outline = fill;
                fill = null;
                // line x1 y1... xn yn
                if (c.length == 4) {
                    shape = new Polyline2D.Double(c[0], c[1], c[2], c[3]);
                }
                else if (c.length > 4) {
                    Polyline2D p = new Polyline2D.Double(c.length);
                    p.moveTo(c[0], c[1]);
                    for (int i = 2; i < c.length; i += 2)
                        p.lineTo(c[i], c[i + 1]);
                    shape = p;
                }
            }
            else if (type.equals("polygon")) {
                // polygon x1 y1 ... xn yn
                shape = new Polygon2D.Double(c);
            }
            else if (type.equals("text") && text != null) {
                // text x y
                Point2D.Double pos = new Point2D.Double(c[0], c[1]);
                ImageLabel fig = new ImageLabel(text, pos, fill, font, interactor);
                canvasDraw.addFigure(fig);
            }

            if (shape != null) {
                ImageFigure fig = new ImageFigure(shape, fill, outline, lineWidth, interactor);
                if (composite != null)
                    fig.setComposite((AlphaComposite) composite);  // XXX cast for diva-28Jan02
                canvasDraw.addFigure(fig);
            }
        }
    }


    /**
     * Delete the table HDU with the given name, if found.
     */
    public void deleteHDU(String extName) {
        FITSImage fitsImage = imageDisplay.getFitsImage();
        int n = fitsImage.getNumHDUs();
        for (int i = 0; i < n; i++) {
            BasicHDU hdu = fitsImage.getHDU(i);
            if (hdu instanceof TableHDU) {
                Header header = hdu.getHeader();
                String name = header.getStringValue("EXTNAME");
                if (name != null && name.equals(extName)) {
                    try {
                        fitsImage.getFits().deleteHDU(i);
                    }
                    catch (Exception e) {
                        DialogUtil.error(e);
                    }
                    return;
                }
            }
        }
    }


    /**
     * Convert the given String formatted image coords to doubles in screen coords and
     * return the new array.
     */
    protected double[] getCoords(String[] coords) {
        double[] c = new double[coords.length];
        CoordinateConverter coordinateConverter = imageDisplay.getCoordinateConverter();
        Point2D.Double p = new Point2D.Double();

        for (int i = 0; i < c.length; i += 2) {
            p.x = Double.parseDouble(coords[i]);
            p.y = Double.parseDouble(coords[i + 1]);
            coordinateConverter.imageToScreenCoords(p, false);
            c[i] = p.x;
            c[i + 1] = p.y;
        }
        return c;
    }


    /** Return a color for the given name */
    protected Color getColor(String s) {
        String[] ar = CanvasDraw.COLOR_NAMES;
        int n = ar.length - 1; // ignore null color at end
        for (int i = 0; i < n; i++) {
            if (ar[i].equals(s))
                return CanvasDraw.COLORS[i];
        }
        if (s.startsWith("grey")) {
            // skycat used a number of gray levels, just use light and dark gray here
            try {
                int i = Integer.parseInt(s.substring(4));
                if (i > 50)
                    return Color.lightGray;
                return Color.darkGray;
            }
            catch (Exception e) {
            }
        }
        return Color.white;  // default to white
    }

    /** Return the name of the given color */
    protected String getColorName(Color c) {
        Color[] ar = CanvasDraw.COLORS;
        int n = ar.length - 1; // ignore null color at end
        for (int i = 0; i < n; i++) {
            if (ar[i] == c)
                return CanvasDraw.COLOR_NAMES[i];
        }
        return "white";
    }


    /** Return a font for the given name */
    protected Font getFont(String s) {
        return Font.decode(s);
    }

    /** Return a composite for the given skycat stipple name (pat0..pat15) */
    protected Composite getStipple(String s) {
        if (s.startsWith("pat")) {
            // try one of the skycat patterns: pat0..pat15
            try {
                int i = Integer.parseInt(s.substring(3));
                return CanvasDraw.COMPOSITES[PATTERNS[i]];
            }
            catch (Exception e) {
            }
        }
        return null;
    }

    /** Return a composite for the given composite string as defined in CanvasDraw (0%,100%). */
    protected Composite getComposite(String s) {
        if (s.endsWith("%")) {
            for (int i = 0; i < CanvasDraw.COMPOSITE_NAMES.length; i++) {
                if (s.equals(CanvasDraw.COMPOSITE_NAMES[i]))
                    return CanvasDraw.COMPOSITES[i];
            }
        }
        return null;
    }

    /** Return the name corresponding to the given composite. */
    protected String getCompositeName(Composite composite) {
        Composite[] ar = CanvasDraw.COMPOSITES;
        int n = ar.length;
        for (int i = 0; i < n; i++) {
            if (ar[i] == composite)
                return CanvasDraw.COMPOSITE_NAMES[i];
        }
        return CanvasDraw.COMPOSITE_NAMES[0];
    }


    /**
     * Return the Tk canvas item type name corresponding to the given shape.
     */
    protected String getType(Shape shape) {
        if (shape instanceof Rectangle2D)
            return "rectangle";
        if (shape instanceof Polyline2D)
            return "line";
        if (shape instanceof Ellipse2D)
            return "oval";
        if (shape instanceof Polygon2D)
            return "polygon";
        throw new RuntimeException("Unsupported shape for saved graphics: " + shape);
    }

    /**
     * Return a Tcl formatted list of image coordinate values
     * for the given screen coordinate shape.
     */
    protected String getCoords(Shape shape) {
        double[] c = null;
        if (shape instanceof RectangularShape) {
            RectangularShape r = (RectangularShape) shape;
            c = new double[4];
            c[0] = r.getX();
            c[1] = r.getY();
            c[2] = c[0] + r.getWidth();
            c[3] = c[1] + r.getHeight();
        }
        else if (shape instanceof Polyline2D) {
            Polyline2D p = (Polyline2D) shape;
            int n = p.getVertexCount();
            c = new double[n * 2];
            for (int i = 0; i < n; i++) {
                c[i * 2] = p.getX(i);
                c[i * 2 + 1] = p.getY(i);
            }
        }
        else if (shape instanceof Polygon2D.Double) {
            Polygon2D p = (Polygon2D) shape;
            int n = p.getVertexCount();
            c = new double[n * 2];
            for (int i = 0; i < n; i++) {
                c[i * 2] = p.getX(i);
                c[i * 2 + 1] = p.getY(i);
            }
        }
        else
            throw new RuntimeException("Unsupported shape for saved graphics: " + shape);

        // convert screen to image coords
        CoordinateConverter coordinateConverter = imageDisplay.getCoordinateConverter();
        Point2D.Double p = new Point2D.Double();
        for (int i = 0; i < c.length; i += 2) {
            p.x = c[i];
            p.y = c[i + 1];
            coordinateConverter.screenToImageCoords(p, false);
            c[i] = p.x;
            c[i + 1] = p.y;
        }

        // convert to Tcl list format
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < c.length; i++) {
            buf.append(Double.toString(c[i]));
            buf.append(' ');
        }
        return buf.toString();
    }

    /**
     * Return a Tcl formatted list of two image coordinate values
     * for the given screen coordinate point.
     */
    protected String getCoords(Point2D.Double p) {
        CoordinateConverter coordinateConverter = imageDisplay.getCoordinateConverter();
        coordinateConverter.screenToImageCoords(p, false);
        return Double.toString(p.getX()) + " " + Double.toString(p.getY());
    }

    /**
     * Return a Tcl formatted list of Tk canvas item style configuration options
     * and values for the given arguments.
     * <p>
     * Example "{-fill red} {-outline black} {-width 2} {-composite 20%}"
     * <p>
     * Note: the return value is Tk canvas "style", but may contain other options,
     * such as -composite.
     */
    protected String getConfig(Paint fill, Paint outline, int lineWidth, Composite composite) {
        StringBuffer buf = new StringBuffer();
        if (fill != null) {
            buf.append("{-fill ");
            buf.append(getColorName((Color) fill));
            buf.append("} ");
        }
        if (outline != null) {
            buf.append("{-outline ");
            buf.append(getColorName((Color) outline));
            buf.append("} ");
        }
        if (lineWidth != 1) {
            buf.append("{-width ");
            buf.append(Integer.toString(lineWidth));
            buf.append("} ");
        }
        if (composite != null) {
            buf.append("{-composite ");
            buf.append(getCompositeName(composite));
            buf.append("} ");
        }
        return buf.toString();
    }

    /**
     * Return a Tcl formatted list of Tk canvas item style configuration options
     * and values for the given arguments.
     * <p>
     * Example "{-text {some text}} {-font Dialog-italic-14} {-fill white}"
     */
    protected String getConfig(String text, Font font, Paint fill) {
        StringBuffer buf = new StringBuffer();
        if (text != null) {
            buf.append("{-text {");
            buf.append(text);
            buf.append("}} ");
        }
        if (font != null) {
            buf.append("{-font {");
            String style = font.isItalic() ? "italic" : (font.isBold() ? "bold" : "plain");
            buf.append(font.getFontName() + "-" + style + "-" + font.getSize());
            buf.append("}} ");
        }
        if (fill != null) {
            buf.append("{-fill ");
            buf.append(getColorName((Color) fill));
            buf.append("} ");
        }
        return buf.toString();
    }

}

