/*
 * Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: BasicTablePlotter.java,v 1.1 2002/08/04 21:48:50 brighton Exp $
 */

package jsky.catalog.gui;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;

import javax.swing.JDesktopPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import jsky.catalog.BasicQueryArgs;
import jsky.catalog.Catalog;
import jsky.catalog.PlotableCatalog;
import jsky.catalog.QueryArgs;
import jsky.catalog.RowCoordinates;
import jsky.catalog.TablePlotSymbol;
import jsky.catalog.TableQueryResult;
import jsky.coords.CoordinateConverter;
import jsky.coords.CoordinateRadius;
import jsky.coords.Coordinates;
import jsky.coords.ImageCoords;
import jsky.coords.NamedCoordinates;
import jsky.coords.WorldCoordinates;
import jsky.coords.WorldCoords;
import jsky.graphics.CanvasGraphics;
import jsky.image.graphics.DivaImageGraphics;
import jsky.image.graphics.ShapeUtil;
import jsky.image.gui.GraphicsImageDisplay;
import jsky.image.gui.ImageCoordinateConverter;
import jsky.navigator.NavigatorPane;
import jsky.util.java2d.ShapeUtilities;

import diva.canvas.CanvasLayer;
import diva.canvas.DamageRegion;
import diva.canvas.TransformContext;
import diva.canvas.event.LayerEvent;
import diva.canvas.event.LayerListener;


/**
 * Implements basic plotting of catalog tables on in an image window.
 * For each row in a table query result, one or more symbols may be displayed 
 * in an image window at a position given by the row's coordinate columns. 
 * <p>
 * Note: This class was previously implemented using Diva figures for catalog symbols,
 * however this turned out to be slow for large numbers of figures, so this version
 * handles the drawing and selection of catalog symbols directly.
 *
 * @version $Revision: 1.1 $
 * @author Allan Brighton
 */
public class BasicTablePlotter
        implements TablePlotter, LayerListener, ChangeListener {

    /** If using internal frames, the JDesktopPane to use for popup windows. */
    private JDesktopPane _desktop;

    /** The object to use to draw catalog symbols */
    private DivaImageGraphics _imageGraphics;

    /** The Diva layer to use to draw catalog symbols */
    private CanvasLayer _layer;

    /** The object used to convert to screen coordinates for drawing */
    private ImageCoordinateConverter _coordinateConverter;

    /** The equinox of the image (assumed by the ImageCoordinateConverter methods) */
    private double _imageEquinox = 2000.;

    /** List of (table, symbolList) pairs, used to keep track of symbols for tables. */
    private LinkedList _tableList = new LinkedList();

    /** Array of (symbol, figureList) pairs (for each table, which may have multiple plot symbols) */
    private SymbolListItem[] _symbolAr;

    /** List of (shape, rowNum) pairs (for each table/symbol entry). */
    private LinkedList _figureList;

    /** If true, catalog symbols are visible, otherwise hidden */
    private boolean _visible = true;

    /** list of listeners for selection events on symbols */
    private EventListenerList _listenerList = new EventListenerList();

    /** list of listeners for selection events on symbols */
    private EventListenerList _tableListenerList = new EventListenerList();

    /** Hash table for caching parsed plot symbol info. */
    private Hashtable _plotSymbolMap = new Hashtable(32);

    /** Used to draw selected symbols */
    private Stroke _selectedStroke = new BasicStroke(3.0F);


    /**
     * Default Constructor
     * (Note: you need to call setCanvasGraphics() and setCoordinateConverter()
     * before using this object).
     */
    public BasicTablePlotter() {
    }

    /**
     * Constructor.
     * (Note: you need to call setCanvasGraphics() and setCoordinateConverter()
     * before using this object).
     *
     * @param desktop The JDesktopPane to use for popup windows.
     */
    public BasicTablePlotter(JDesktopPane desktop) {
        _desktop = desktop;
    }

    /**
     * Construct an object to plot the contents of a table (query result or local
     * catalog file).
     *
     * @param canvasGraphics the object to use to draw the plot symbols
     * @param coordinateConverter the object to use to convert between world and image coordinates
     */
    public BasicTablePlotter(CanvasGraphics canvasGraphics,
                              CoordinateConverter coordinateConverter) {
        setCanvasGraphics(canvasGraphics);
        setCoordinateConverter(_coordinateConverter);
    }


    /** Set the object to use to draw catalog symbols */
    public void setCanvasGraphics(CanvasGraphics canvasGraphics) {
        _imageGraphics = (DivaImageGraphics) canvasGraphics;
        NavigatorPane pane = (NavigatorPane) _imageGraphics.getGraphicsPane();
        _layer = pane.getSymbolLayer();
        pane.getBackgroundEventLayer().addLayerListener(this);
    }

    /** Return the object to use to draw catalog symbols */
    public CanvasGraphics getCanvasGraphics() {
        return _imageGraphics;
    }


    /** Return the object used to convert to screen coordinates for drwing */
    public CoordinateConverter getCoordinateConverter() {
        return _coordinateConverter;
    }

    /** Set the object used to convert to screen coordinates for drwing */
    public void setCoordinateConverter(CoordinateConverter c) {
        _coordinateConverter = (ImageCoordinateConverter) c;
    }


    /**
     * Check if there is an image loaded and if so, if it supports world coordinates.
     * If no image is loaded, try to generate a blank image for plotting.
     *
     * @param table describes the table data 
     * @return false if no suitable image could be used or generated, otherwise true
     */
    public boolean check(TableQueryResult table) {
        // If no image is being displayed, try to generate a blank WCS image
        GraphicsImageDisplay imageDisplay = (GraphicsImageDisplay)_coordinateConverter.getImageDisplay();
        if (imageDisplay.isClear()) {
            WorldCoordinates pos = table.getWCSCenter();
            if (pos != null) {
                imageDisplay.blankImage(pos.getRaDeg(), pos.getDecDeg());
            }
        }

        if (!_coordinateConverter.isWCS()) {
            return false;
        }
        return true;
    }


    /**
     * Plot the given table data.
     *
     * @param table describes the table data
     */
    public void plot(TableQueryResult table) {
        if (_layer == null || _coordinateConverter == null) {
            throw new RuntimeException("Can't plot the given table data");	// shouldn't happen
	}

        if (!check(table)) {
            return;
	}

        TablePlotSymbol[] symbols = getPlotSymbolInfo(table);
        if (symbols == null) {
            return;
	}
	
	// The symbol objects need a reference to the table being plotted
	// to evaluate expressions based on column values
	for(int i = 0; i < symbols.length; i++) 
	    symbols[i].setTable(table);

        // holds the plot symbols for this table
        _symbolAr = new SymbolListItem[symbols.length];
        for (int i = 0; i < symbols.length; i++) {
            // holds the figure info for this table/symbol entry
            _symbolAr[i] = new SymbolListItem(symbols[i]);
        }

        // plot the symbols
        plotSymbols(table, symbols);

        // add this table to the list of plotted tables
        // (but remove any previous table from the same source)
        ListIterator it = _tableList.listIterator(0);
        while (it.hasNext()) {
            TableListItem item = (TableListItem) it.next();
            if (item.table == table || item.table.getName().equals(table.getName())) {
                if (item.table.getCatalog() == table.getCatalog()) {
                    it.remove();
                    break;
                }
            }
        }
        _tableList.add(new TableListItem(table, _symbolAr));

        // track changes in the WCS coords
        _coordinateConverter.removeChangeListener(this);
        _coordinateConverter.addChangeListener(this);

        _layer.repaint();
    }


    /** Called when the WCS info changes */
    public void stateChanged(ChangeEvent e) {
        replotAll();
    }


    /**
     * Return the plot symbol info for the given table. 
     *
     * @param table object representing the catalog table
     * @return an array of TablePlotSymbol objects, one for each plot symbol defined.
     */
    public TablePlotSymbol[] getPlotSymbolInfo(TableQueryResult table) {
        // first see if we have the plot information cached
        Object o = _plotSymbolMap.get(table);
        Catalog catalog = table.getCatalog();
        if (o == null) {
            // also check the catalog where the query originated
            if (catalog != null)
                o = _plotSymbolMap.get(catalog);
        }
        if (o instanceof TablePlotSymbol[])
            return (TablePlotSymbol[]) o;

	if (catalog instanceof PlotableCatalog) {
	    TablePlotSymbol[] symbols = ((PlotableCatalog)catalog).getSymbols();
	    if (symbols != null) {
		_plotSymbolMap.put(table, symbols);
		_plotSymbolMap.put(catalog, symbols);
	    }
	    return symbols;
	}
	return null;
    }

    /** Set the plot symbol info for the given table */
    public void setPlotSymbolInfo(TableQueryResult table, TablePlotSymbol[] symbols) {
        Catalog catalog = table.getCatalog();
        _plotSymbolMap.put(table, symbols);
        if (catalog != null)
            _plotSymbolMap.put(catalog, symbols);
    }

    /**
     * Plot the table data using the given symbol descriptions.
     *
     * @param table describes the table data
     * @param symbols an array of objects describing the symbols to plot
     */
    protected void plotSymbols(TableQueryResult table, TablePlotSymbol[] symbols) {
        // for each row in the catalog, evaluate the expressions and plot the symbols
        int nrows = table.getRowCount();
        RowCoordinates rowCoords = table.getRowCoordinates();
        double tableEquinox = rowCoords.getEquinox();  // XXX might be specified in a column?
        Vector dataVec = table.getDataVector();

        boolean isWCS = rowCoords.isWCS();
        boolean isPix = rowCoords.isPix();
        int cooSys;
        if (isPix) {
            cooSys = CoordinateConverter.IMAGE;
        }
        else if (isWCS) {
            cooSys = CoordinateConverter.WORLD;
            _imageEquinox = _coordinateConverter.getEquinox();
        }
        else
            throw new RuntimeException("no wcs or image coordinates to plot");

        for (int row = 0; row < nrows; row++) {
            Vector rowVec = (Vector) dataVec.get(row);
            Coordinates pos = rowCoords.getCoordinates(rowVec);
            if (pos == null)
                continue;	// coordinates might be missing - just ignore

            double x, y;
            if (isPix) {
                x = pos.getX();
                y = pos.getY();
            }
            else {
                // need to keep table values in the image equinox, since the WCS conversion
                // methods all assume the image equinox
                double[] radec = ((WorldCoords) pos).getRaDec(_imageEquinox);
                x = radec[0];
                y = radec[1];
            }

            for (int i = 0; i < symbols.length; i++) {
                _figureList = _symbolAr[i].figureList;
                try {
                    plotRow(table, row, rowVec, x, y, cooSys, symbols[i]);
                }
                catch (Exception e) {
                    // ignore: may be WCS out of range...
                }
            }
        }
    }


    /*
     * Plot the symbol for the given row.
     * The row data is taken from the given row vector.
     *
     * @param table describes the table data
     * @param row the row number (first row is 0)
     * @param rowVec a vector containing the row elements
     * @param x the X position coordinate
     * @param y the Y position coordinate
     * @param cooSys the coordinate system of X and Y (CoordinateConverter constant)
     * @param symbol an object describing the symbol
     */
    protected void plotRow(TableQueryResult table, int row, Vector rowVec, double x, double y,
                           int cooSys, TablePlotSymbol symbol) {
        // eval expr to get condition
        boolean cond = symbol.getCond(rowVec);
        if (!cond)
            return;

        // eval expr to get radius
        double radius = symbol.getSize(rowVec);
        if (radius <= 0. || Double.isNaN(radius)) {
            // don't want a neg or 0 radius
            radius = 1.;
        }

        // ratio may be an expression with column name variables
        double ratio = symbol.getRatio(rowVec);

        // angle may be an expression with column name variables
        double angle = symbol.getAngle(rowVec);

        // label may also contain col name vars, but might not be numeric
        String label = symbol.getLabel(rowVec);

        plotSymbol(table, row, symbol, x, y, cooSys, radius, ratio, angle, label);
    }


    /*
     * Plot the given symbol.
     *
     * @param table describes the table data
     * @param row the row number (starting with 0)
     * @param symbol an object describing the symbol
     * @param x the X position coordinate
     * @param y the Y position coordinate
     * @param cooSys the coordinate system of X and Y (CoordinateConverter constant)
     * @param radius the radius (size) of the symbol (the symbol object contains the size units)
     * @param ratio the x/y ratio (ellipticity ratio) of the symbol
     * @param angle the rotation angle
     * @param label the label to display next to the symbol
     */
    protected void plotSymbol(TableQueryResult table, int row, TablePlotSymbol symbol, double x, double y,
                              int cooSys, double radius, double ratio, double angle, String label) {

        // convert to screen coordinates
        Point2D.Double pos = new Point2D.Double(x, y);
        _coordinateConverter.convertCoords(pos, cooSys, CoordinateConverter.USER, false);

        // clip to image bounds
        double w = _coordinateConverter.getWidth();
        double h = _coordinateConverter.getHeight();
        if (pos.x < 0. || pos.y < 0. || pos.x >= w || pos.y >= h)
            return;
        _coordinateConverter.convertCoords(pos, CoordinateConverter.USER, CoordinateConverter.SCREEN, false);

        Point2D.Double size = new Point2D.Double(radius, radius);
        int sizeType = getCoordType(symbol.getUnits());
        _coordinateConverter.convertCoords(size, sizeType, CoordinateConverter.SCREEN, true);

        // get the Shape object for the symbol
        Shape shape = makeShape(symbol, pos.x, pos.y, Math.max(size.x, size.y), ratio, angle);

        // Add an item for this symbol to the figure list, and store it as client data also
        FigureListItem item = new FigureListItem(shape, label, row);
        _figureList.add(item);
    }

    /**
     * Return the CoordinateConverter type code for the given name.
     */
    protected int getCoordType(String name) {
        if (name != null && name.length() != 0) {
            if (name.startsWith("deg"))
                return CoordinateConverter.WORLD;
            if (name.equals("image"))
                return CoordinateConverter.IMAGE;
            if (name.equals("screen"))
                return CoordinateConverter.SCREEN;
            if (name.equals("canvas"))
                return CoordinateConverter.CANVAS;
            if (name.equals("user"))
                return CoordinateConverter.USER;
        }
        return _coordinateConverter.IMAGE;
    }


    /**
     * Return the Shape object for the given symbol.
     *
     * @param symbol an object describing the symbol
     * @param x the X position screen coordinate
     * @param y the Y position screen coordinate
     * @param size the radius of the symbol in screen coordinates
     * @param ratio the x/y ratio (ellipticity ratio) of the symbol
     * @param angle the rotation angle
     */
    protected Shape makeShape(TablePlotSymbol symbol, double x, double y, double size,
                              double ratio, double angle) {

        int shape = symbol.getShape();

        // do the simple ones first
        switch (shape) {
        case TablePlotSymbol.CIRCLE:
            return new Ellipse2D.Double(x - size, y - size, size * 2, size * 2);

        case TablePlotSymbol.SQUARE:
            return new Rectangle2D.Double(x - size, y - size, size * 2, size * 2);

        case TablePlotSymbol.CROSS:
            return ShapeUtil.makeCross(x, y, size);

        case TablePlotSymbol.TRIANGLE:
            return ShapeUtil.makeTriangle(x, y, size);

        case TablePlotSymbol.DIAMOND:
            return ShapeUtil.makeDiamond(x, y, size);
        }

        // get center, north and east in screen coords
        Point2D.Double center = new Point2D.Double(x, y);
        Point2D.Double north = new Point2D.Double(x, y - size);
        Point2D.Double east = new Point2D.Double(x - size, y);

        // Get WCS NORTH and EAST, converted to screen coords
        getNorthAndEast(center, size, ratio, angle, north, east);

        switch (shape) {
        case TablePlotSymbol.COMPASS:
            return ShapeUtil.makeCompass(center, north, east);

        case TablePlotSymbol.LINE:
            return ShapeUtil.makeLine(center, north, east);

        case TablePlotSymbol.ARROW:
            return ShapeUtil.makeArrow(center, north);

        case TablePlotSymbol.ELLIPSE:
            return ShapeUtil.makeEllipse(center, north, east);

        case TablePlotSymbol.PLUS:
            return ShapeUtil.makePlus(center, north, east);
        }

        throw new RuntimeException("Unknown plot symbol shape: " + symbol.getShapeName());
    }


    /*
     * Set x and y in the north and east parameters in screen
     * coordinates, given the center point and radius in screen
     * coordinates, an optional rotation angle, and an x/y ellipticity
     * ratio.  If the image supports world coordinates, that is taken
     * into account (the calculations are done in RA,DEC before
     * converting to screen coords).  The conversion to screen coords
     * automatically takes the current zoom and rotate settings into
     * account.
     *
     * @param center the center position screen coordinate
     * @param size the radius of the symbol in screen coordinates
     * @param ratio the x/y ratio (ellipticity ratio) of the symbol
     * @param angle the rotation angle
     * @param north on return, contains the screen coordinates of WCS north
     * @param east on return, contains the screen coordinates of WCS east
     */
    protected void getNorthAndEast(Point2D.Double center,
                                   double size,
                                   double ratio,
                                   double angle,
                                   Point2D.Double north,
                                   Point2D.Double east) {

        if (_coordinateConverter.isWCS()) {
            // get center and radius in deg 2000
            Point2D.Double wcsCenter = new Point2D.Double(center.x, center.y);
            _coordinateConverter.screenToWorldCoords(wcsCenter, false);
            Point2D.Double wcsRadius = new Point2D.Double(size, size);
            _coordinateConverter.screenToWorldCoords(wcsRadius, true);

            // adjust the radius by the ratio
            if (ratio < 1.)
                wcsRadius.y *= 1.0 / ratio;
            else if (ratio > 1.)
                wcsRadius.x *= ratio;

            // set east
            east.x = Math.IEEEremainder(wcsCenter.x + Math.abs(wcsRadius.x) / Math.cos((wcsCenter.y / 180.) * Math.PI), 360.);
            if (east.x < 0.)
                east.x += 360.;

            east.y = wcsCenter.y;

            // set north
            north.x = wcsCenter.x;

            north.y = wcsCenter.y + Math.abs(wcsRadius.y);
            if (north.y >= 90.)
                north.y = 180. - north.y;
            else if (north.y <= -90.)
                north.y = -180. - north.y;

            // convert back to screen coords
            _coordinateConverter.worldToScreenCoords(north, false);
            _coordinateConverter.worldToScreenCoords(east, false);
        }
        else {
            // not using world coords: adjust the radius by the ratio
            double rx = size, ry = size;
            if (ratio < 1.)
                ry *= 1.0 / ratio;
            else if (ratio > 1.)
                rx *= ratio;

            east.x = center.x - rx;
            east.y = center.y;

            north.x = center.x;
            north.y = center.y - ry;
        }

        // rotate by angle
        if (angle != 0.) {
            rotatePoint(north, center, angle);
            rotatePoint(east, center, angle);
        }
    }


    /*
     * Rotate the point p around the center point by the given
     * angle in deg.
     */
    protected void rotatePoint(Point2D.Double p, Point2D.Double center, double angle) {
        p.x -= center.x;
        p.y -= center.y;
        double tmp = p.x;
        double rad = angle * Math.PI / 180.;
        double cosa = Math.cos(rad);
        double sina = Math.sin(rad);
        p.x = p.x * cosa + p.y * sina + center.x;
        p.y = -tmp * sina + p.y * cosa + center.y;
    }


    /** Erase the plot of the given table data */
    public void unplot(TableQueryResult table) {
        ListIterator it = _tableList.listIterator(0);
        while (it.hasNext()) {
            TableListItem item = (TableListItem) it.next();
            if (item.table == table) {
                it.remove();
                _layer.repaint();
                break;
            }
        }
    }

    /** Erase all plot symbols */
    public void unplotAll() {
        _tableList = new LinkedList();
        _layer.repaint();
    }


    /** Recalculate the coordinates and replot all symbols after a change in the coordinate system. */
    public void replotAll() {
        LinkedList list = (LinkedList) _tableList.clone();
        _tableList = new LinkedList();
        ListIterator it = list.listIterator(0);
        while (it.hasNext()) {
            TableListItem tli = (TableListItem) it.next();
            tli.inRange = tableInRange(tli.table);
            if (tli.inRange)
                plot(tli.table);
            else
                _tableList.add(tli);
        }
        _layer.repaint();
    }

    /** Return an array containing the tables managed by this object. */
    public TableQueryResult[] getTables() {
        int n = _tableList.size();
        if (n == 0)
            return null;
        ListIterator it = _tableList.listIterator(0);
        Vector tableVec = new Vector(n);
        while (it.hasNext()) {
            TableListItem item = (TableListItem) it.next();
            if (item.inRange) {
                tableVec.add(item.table);
            }
        }
        n = tableVec.size();
        if (n == 0)
            return null;
        TableQueryResult[] tables = new TableQueryResult[n];
        for (int i = 0; i < n; i++)
            tables[i] = (TableQueryResult) tableVec.get(i);
        return tables;
    }

    /** Schedule a repaint of the area given by the given shape */
    protected void repaint(Shape shape) {
        _layer.repaint(DamageRegion.createDamageRegion(new TransformContext(_layer), shape.getBounds2D()));
    }

    /** Set the selection state of the symbol corresponding to the given table row */
    public void selectSymbol(TableQueryResult table, int tableRow, boolean selected) {
        // Find the plot symbol for the given row in the given table
        ListIterator tableIt = _tableList.listIterator(0);
        while (tableIt.hasNext()) {
            TableListItem tli = (TableListItem) tableIt.next();
            if (tli.table == table) {
                for (int i = 0; i < tli.symbolAr.length; i++) {
                    SymbolListItem sli = tli.symbolAr[i];
                    ListIterator figureListIt = sli.figureList.listIterator(0);
                    while (figureListIt.hasNext()) {
                        FigureListItem fli = (FigureListItem) figureListIt.next();
                        if (fli.row == tableRow) {
                            if (fli.selected != selected) {
                                fli.selected = selected;
                                repaint(fli.shape);
                            }
                            continue;  // may be more than one symbol per row
                        }
                    }
                }
            }
        }
        fireTableSelectionEvent(table, tableRow, selected);

    }

    /** Select the symbol corresponding to the given table row */
    public void selectSymbol(TableQueryResult table, int tableRow) {
        selectSymbol(table, tableRow, true);
    }

    /** Deselect the symbol corresponding to the given table row */
    public void deselectSymbol(TableQueryResult table, int tableRow) {
        selectSymbol(table, tableRow, false);
    }

    /** Add a listener for selection events on symbols */
    public void addSymbolSelectionListener(SymbolSelectionListener listener) {
        _listenerList.add(SymbolSelectionListener.class, listener);
    }

    /** Remove a listener for selection events on symbols */
    public void removeSymbolSelectionListener(SymbolSelectionListener listener) {
        _listenerList.remove(SymbolSelectionListener.class, listener);
    }

    /**
     * Notify any listeners that a symbol was selected or deselected.
     *
     * @param table the table containing the symbol
     * @param row the row index of the selected symbol
     * @param isSelected set to true if the symbol was selected, false if deselected
     */
    protected void fireSymbolSelectionEvent(TableQueryResult table, int row, boolean isSelected) {
        SymbolSelectionEvent event = new SymbolSelectionEvent(row, table);
        Object[] listeners = _listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == SymbolSelectionListener.class) {
                SymbolSelectionListener listener = (SymbolSelectionListener) listeners[i + 1];
                if (isSelected)
                    listener.symbolSelected(event);
                else
                    listener.symbolDeselected(event);
            }
        }
    }

    /** Add a listener for selection events on tables */
    public void addTableSelectionListener(TableSelectionListener listener) {
        _tableListenerList.add(TableSelectionListener.class, listener);
    }

    /** Remove a listener for selection events on tables */
    public void removeTableSelectionListener(TableSelectionListener listener) {
        _tableListenerList.remove(TableSelectionListener.class, listener);
    }

    /**
     * Notify any listeners that a table row was selected
     *
     * @param table the table containing the selected row
     * @param row the selected row index
     * @param isSelected set to true if the row was selected, false if deselected
     */
    protected void fireTableSelectionEvent(TableQueryResult table, int row, boolean selected) {
        TableSelectionEvent event = new TableSelectionEvent(row, table);
        Object[] listeners = _tableListenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TableSelectionListener.class) {
                TableSelectionListener listener = (TableSelectionListener) listeners[i + 1];
                if (selected) {
                    listener.tableSelected(event);
                }
            }
        }
    }


    /**
     * If the given argument is false, hide all plot symbols managed by this object,
     * otherwise show them again.
     */
    public void setVisible(boolean isVisible) {
        _visible = isVisible;
    }


    /** Return true if catalog symbols are visible, false if they are hidden. */
    public boolean isVisible() {
        return _visible;
    }


    /**
     * Return a panel to use to configure the plot symbols for the given table.
     *
     * @param table the result of a query
     */
    public JPanel getConfigPanel(TableQueryResult table) {
	return new TableSymbolConfig(this, table);
    }

    /**
     * Paint the catalog symbols using the given graphics object.
     *
     * @param g2D the graphics context
     * @param region if not null, the region to paint
     */
    public void paintSymbols(Graphics2D g2d, Rectangle2D region) {
        if (!_visible)
            return;

        // plot each table
        g2d.setPaintMode();
        ListIterator tableIt = _tableList.listIterator(0);
        while (tableIt.hasNext()) {
            TableListItem tli = (TableListItem) tableIt.next();
            if (!tli.inRange) // ignore tables not in image range
                continue;
            // plot each symbol type in the table
            for (int i = 0; i < tli.symbolAr.length; i++) {
                SymbolListItem sli = tli.symbolAr[i];
                g2d.setColor(sli.symbol.getFg());
                // plot each figure
                ListIterator figureListIt = sli.figureList.listIterator(0);
                while (figureListIt.hasNext()) {
                    FigureListItem fli = (FigureListItem) figureListIt.next();
                    if (region == null || fli.shape.intersects(region)) {
                        if (fli.selected) {
                            // draw selected symbols with a thicker stroke
                            Stroke stroke = g2d.getStroke();
                            g2d.setStroke(_selectedStroke);
                            g2d.draw(fli.shape);
                            g2d.setStroke(stroke);
                        }
                        else {
                            g2d.draw(fli.shape);
                        }
                        // If there is a label for the symbol, draw it too
                        if (fli.label != null) {
                            Rectangle2D r = fli.shape.getBounds();
                            g2d.drawString(fli.label, (float) r.getCenterX(), (float) r.getCenterY());
                        }
                    }
                }
            }
        }
    }


    /**
     * Transform the plot symbols using the given AffineTransform
     * (called when the image is transformed, to keep the plot symbols up to date).
     */
    public void transformGraphics(AffineTransform trans) {
        ListIterator tableIt = _tableList.listIterator(0);
        while (tableIt.hasNext()) {
            TableListItem tli = (TableListItem) tableIt.next();
            for (int i = 0; i < tli.symbolAr.length; i++) {
                SymbolListItem sli = tli.symbolAr[i];
                ListIterator figureListIt = sli.figureList.listIterator(0);
                while (figureListIt.hasNext()) {
                    FigureListItem fli = (FigureListItem) figureListIt.next();
                    fli.shape = ShapeUtilities.transformModify(fli.shape, trans);
                }
            }
        }
        _layer.repaint();
    }


    /**
     * Return true if the coordinates of the objects in the given table may be in a
     * range where they can be plotted in the current image.
     */
    protected boolean tableInRange(TableQueryResult table) {
        // get the coordinates of the region that the table covers from the query arguments, if known
        QueryArgs queryArgs = table.getQueryArgs();
        CoordinateRadius region = null;
        if (queryArgs == null) {
            // scan table here to get the range that it covers
            region = getTableRegion(table);
            if (region != null) {
                queryArgs = new BasicQueryArgs(table);
                queryArgs.setRegion(region);
                table.setQueryArgs(queryArgs);
            }
        }
        else {
            region = queryArgs.getRegion();
        }
        if (region == null)
            return false;
        Coordinates centerPosition = region.getCenterPosition();
        if (!(centerPosition instanceof WorldCoords)) {
            return true;
        }
        if (!_coordinateConverter.isWCS())
            return false;

        WorldCoords pos = (WorldCoords) centerPosition;
        double ra = pos.getRaDeg();
        double dec = pos.getDecDeg();
        double w = region.getWidth();     // in arcmin
        double h = region.getHeight();
        Rectangle2D.Double tableRect = new Rectangle2D.Double(ra, dec, w, h);

        // get the image coords
        Point2D.Double p = _coordinateConverter.getWCSCenter();
        pos = new WorldCoords(p.x, p.y, _imageEquinox);
        ra = pos.getRaDeg();
        dec = pos.getDecDeg();
        w = _coordinateConverter.getWidthInDeg() * 60;  // in arcmin
        h = _coordinateConverter.getHeightInDeg() * 60;
        Rectangle2D.Double imageRect = new Rectangle2D.Double(ra, dec, w, h);

        return tableRect.intersects(imageRect);
    }

    /**
     * Scan the given table and return an object describing the area of the sky that
     * it covers, or null if not known.
     */
    protected CoordinateRadius getTableRegion(TableQueryResult table) {
        int nrows = table.getRowCount();
        if (nrows == 0)
            return null;

        RowCoordinates rowCoords = table.getRowCoordinates();
        double tableEquinox = rowCoords.getEquinox();
        Vector dataVec = table.getDataVector();

        if (rowCoords.isPix()) {
            // no WCS, just use image center and size
            Point2D.Double p = _coordinateConverter.getImageCenter();
            ImageCoords pos = new ImageCoords(p.x, p.y);
            double w = _coordinateConverter.getWidth();
            double h = _coordinateConverter.getHeight();
            double r = Math.sqrt(w * w + h * h);
            return new CoordinateRadius(pos, r, w, h);
        }

        if (!rowCoords.isWCS())
            return null;

        // we have world coordinages: find the bounding box of the objects in the table
        int cooSys = CoordinateConverter.WORLD;
        double ra0 = 0., ra1 = 0., dec0 = 0., dec1 = 0.;
        boolean firstTime = true;
        for (int row = 1; row < nrows; row++) {
            Vector rowVec = (Vector) dataVec.get(row);
            Coordinates pos = rowCoords.getCoordinates(rowVec);
            if (pos == null)
                continue;
            if (firstTime) {
                firstTime = false;
                ra0 = pos.getX();
                ra1 = ra0;
                dec0 = pos.getY();
                dec1 = dec0;
            }
            else {
                double ra = pos.getX(), dec = pos.getY();
                ra0 = Math.min(ra0, ra);
                ra1 = Math.max(ra1, ra);
                dec0 = Math.min(dec0, dec);
                dec1 = Math.max(dec1, dec);
            }
        }

        // get the center point and radius
        WorldCoords centerPos = new WorldCoords((ra0 + ra1) / 2., (dec0 + dec1) / 2., tableEquinox);
        double d = WorldCoords.dist(ra0, dec0, ra1, dec1);
        CoordinateRadius region = new CoordinateRadius(centerPos, d / 2.);
        return region;
    }


    // -- Implement the LayerListener interface --

    /** Invoked when the mouse moves while the button is still held down. */
    public void mouseDragged(LayerEvent e) {
    }

    /** Invoked when the mouse is pressed on a layer or figure. */
    public void mousePressed(LayerEvent e) {
    }

    /** Invoked when the mouse is released on a layer or figure. */
    public void mouseReleased(LayerEvent e) {
    }

    /**
     * Invoked when the mouse is clicked on a layer or figure.
     * <p>
     * Note that if a catalog symbol is selected, the event is modified,
     * so that any other listeners will get the modified location, which is
     * set to the center of the selected catalog symbol. This implements
     * a kind of "snap to catalog symbol" feature for any layer listeners that
     * are added after this instance.
     */
    public void mouseClicked(LayerEvent e) {
        if (!_visible)
            return;

        double x = e.getLayerX(), y = e.getLayerY();
        boolean toggleSel = (e.isShiftDown() || e.isControlDown());

        // Find the plot symbol under the mouse pointer
        ListIterator tableIt = _tableList.listIterator(0);
        while (tableIt.hasNext()) {
            TableListItem tli = (TableListItem) tableIt.next();
            if (!tli.inRange)
                continue;
            for (int i = 0; i < tli.symbolAr.length; i++) {
                SymbolListItem sli = tli.symbolAr[i];
                ListIterator figureListIt = sli.figureList.listIterator(0);
                while (figureListIt.hasNext()) {
                    FigureListItem fli = (FigureListItem) figureListIt.next();
                    if (sli.symbol.getBoundingShape(fli.shape).contains(x, y)) {
                        if (toggleSel) {
                            fli.selected = !fli.selected;
                            repaint(fli.shape);
                            fireSymbolSelectionEvent(tli.table, fli.row, fli.selected);
                        }
                        else {
                            if (!fli.selected) {
                                fli.selected = true;
                                repaint(fli.shape);
                                fireSymbolSelectionEvent(tli.table, fli.row, fli.selected);
                            }
                        }
                    }
                    else if (!toggleSel) {
                        if (fli.selected) {
                            fli.selected = false;
                            repaint(fli.shape);
                            fireSymbolSelectionEvent(tli.table, fli.row, fli.selected);
                        }
                    }
                }
            }
        }
    }


    /**
     * If the given screen coordinates point is within a displayed catalog symbol, set it to
     * point to the center of the symbol and return the name and coordinates (and brightness,
     * if known) from the catalog table row. Otherwise, return null and do nothing.
     */
    public NamedCoordinates getCatalogPosition(Point2D.Double p) {
        // Find the plot symbol under the mouse pointer
        ListIterator tableIt = _tableList.listIterator(0);
        while (tableIt.hasNext()) {
            TableListItem tli = (TableListItem) tableIt.next();
            if (!tli.inRange)
                continue;
            for (int i = 0; i < tli.symbolAr.length; i++) {
                SymbolListItem sli = tli.symbolAr[i];
                ListIterator figureListIt = sli.figureList.listIterator(0);
                while (figureListIt.hasNext()) {
                    FigureListItem fli = (FigureListItem) figureListIt.next();
                    // assume symbol has already been selected
                    if (fli.selected && sli.symbol.getBoundingShape(fli.shape).contains(p)) {
                        RowCoordinates rowCoords = tli.table.getRowCoordinates();
                        if (!rowCoords.isWCS())
                            return null;
                        double tableEquinox = rowCoords.getEquinox();
                        Vector dataVec = tli.table.getDataVector();
                        _imageEquinox = _coordinateConverter.getEquinox();
                        Vector rowVec = (Vector) dataVec.get(fli.row);

                        // get the world coordinates
                        Coordinates cpos = rowCoords.getCoordinates(rowVec);
                        if (cpos == null)
                            return null;
                        WorldCoords pos = (WorldCoords) cpos;

                        // modify the parameter to point to the center of the symbol
                        double[] radec = pos.getRaDec(_imageEquinox);
                        p.x = radec[0];
                        p.y = radec[1];
                        _coordinateConverter.convertCoords(p, CoordinateConverter.WORLD, CoordinateConverter.SCREEN, false);

                        // get the id of the catalog symbol
                        int idCol = rowCoords.getIdCol();
                        String id = null;
                        if (idCol != -1)
                            id = (String) rowVec.get(idCol);

                        // get the brightness (mag) if known
                        Vector columnIdentifiers = tli.table.getColumnIdentifiers();
                        int numCols = columnIdentifiers.size();
                        String brightness = "";
                        for (int col = 0; col < numCols; col++) {
                            String s = (String) columnIdentifiers.get(col);
                            String sl = s.toLowerCase();
                            if (sl.equals("mag")) {
                                Object o = rowVec.get(col);
                                if (o != null) {
                                    brightness = o + " mag";
                                    break;
                                }
                            }
                            else if (sl.endsWith("mag") && !sl.startsWith("e")) {
                                Object o = rowVec.get(col);
                                if (o != null) {
                                    if (brightness.length() != 0)
                                        brightness += ", ";
                                    brightness = brightness + o + s.charAt(0);
                                }
                            }
                        }

                        return new NamedCoordinates(id, pos, brightness);
                    }
                }
            }
        }
        return null;
    }


    /**
     * Local class used for tableList elements (one for each table).
     */
    protected class TableListItem {

        public TableQueryResult table;      // a reference to the table to plot
        public SymbolListItem[] symbolAr;   // array mapping symbol desc to figure list
        public boolean inRange = true;      // set to true if table coords are in image range

        public TableListItem(TableQueryResult t, SymbolListItem[] ar) {
            table = t;
            symbolAr = ar;
        }
    }

    /**
     * Local class used for TableListItem.symbolList elements (one for each plot
     * symbol entry, for each table)
     */
    protected class SymbolListItem {

        public TablePlotSymbol symbol;                  // plot symbol description
        public LinkedList figureList = new LinkedList();   // list of figures to draw using the above symbol

        public SymbolListItem(TablePlotSymbol s) {
            symbol = s;
        }
    }

    /**
     * Local class used for SymbolListItem.figureList elements (one for each plot symbol).
     */
    protected class FigureListItem {

        public Shape shape;     // shape of the symbol
        public String label;    // optional label
        public int row;         // row index in table
        public boolean selected = false;  // true if selected

        public FigureListItem(Shape s, String lab, int r) {
            shape = s;
            label = lab;
            row = r;
        }
    }
}
