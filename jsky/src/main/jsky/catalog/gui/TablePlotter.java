/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TablePlotter.java,v 1.13 2002/08/04 21:48:50 brighton Exp $
 */

package jsky.catalog.gui;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

import jsky.catalog.TablePlotSymbol;
import jsky.catalog.TableQueryResult;
import jsky.coords.CoordinateConverter;
import jsky.coords.NamedCoordinates;
import jsky.graphics.CanvasGraphics;

/**
 * This defines the interface for plotting the contents of a catalog table.
 *
 * @version $Revision: 1.13 $
 * @author Allan Brighton
 */
public abstract interface TablePlotter {

    /** Plot the given table data */
    public void plot(TableQueryResult table);

    /** Erase the plot of the given table data */
    public void unplot(TableQueryResult table);

    /** Erase all plot symbols */
    public void unplotAll();

    /** Recalculate the coordinates and replot all symbols after a change in the coordinate system. */
    public void replotAll();

    /** Return an array containing the tables managed by this object. */
    public TableQueryResult[] getTables();

    /** Select the symbol corresponding to the given table row */
    public void selectSymbol(TableQueryResult table, int tableRow);

    /** Deselect the symbol corresponding to the given table row */
    public void deselectSymbol(TableQueryResult table, int tableRow);


    /** Set the plot symbol info for the given table */
    public void setPlotSymbolInfo(TableQueryResult table, TablePlotSymbol[] symbols);

    /**
     * Return the plot symbol info for the given table. 
     *
     * @param table object representing the catalog table
     * @return an array of PlotSymbol objects, one for each plot symbol defined.
     */
    public TablePlotSymbol[] getPlotSymbolInfo(TableQueryResult table);


    /**
     * If the given argument is false, hide all plot symbols managed by this object,
     * otherwise show them again.
     */
    public void setVisible(boolean isVisible);

    /** Set the object to use to draw catalog symbols */
    public void setCanvasGraphics(CanvasGraphics canvasGraphics);

    /** Return the object to use to draw catalog symbols */
    public CanvasGraphics getCanvasGraphics();

    /** Set the object used to convert to screen coordinates for drawing */
    public void setCoordinateConverter(CoordinateConverter c);

    /**
     * If the given screen coordinates point is within a displayed catalog symbol, set it to
     * point to the center of the symbol and return the name and coordinates
     * from the catalog table row. Otherwise, return null and do nothing.
     */
    public NamedCoordinates getCatalogPosition(Point2D.Double p);

    /** Return the object used to convert to screen coordinates for drawing */
    public CoordinateConverter getCoordinateConverter();

    /** Add a listener for selection events on symbols */
    public void addSymbolSelectionListener(SymbolSelectionListener listener);

    /** Remove a listener for selection events on symbols */
    public void removeSymbolSelectionListener(SymbolSelectionListener listener);

    /** Add a listener for selection events on tables */
    public void addTableSelectionListener(TableSelectionListener listener);

    /** Remove a listener for selection events on tables */
    public void removeTableSelectionListener(TableSelectionListener listener);

    /**
     * Return a panel to use to configure the plot symbols for the given table.
     *
     * @param table the result of a query
     */
    public JPanel getConfigPanel(TableQueryResult table);

    /**
     * Paint the catalog symbols using the given graphics object.
     *
     * @param g2D the graphics context
     * @param region if not null, the region to paint
     */
    public void paintSymbols(Graphics2D g, Rectangle2D region);

    /**
     * Transform the plot symbols using the given AffineTransform
     * (called when the image is transformed, to keep the plot symbols up to date).
     */
    public void transformGraphics(AffineTransform trans);
}
