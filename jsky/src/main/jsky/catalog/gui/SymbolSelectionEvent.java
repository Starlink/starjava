/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SymbolSelectionEvent.java,v 1.3 2002/07/09 13:30:36 brighton Exp $
 */

package jsky.catalog.gui;

import java.util.EventObject;

import jsky.graphics.CanvasFigure;
import jsky.catalog.TableQueryResult;


/**
 * This event is generated when a catalog symbol is seletced or deselected.
 *
 * @version $Revision: 1.3 $
 * @author Allan Brighton
 */
public class SymbolSelectionEvent extends EventObject {

    /** The catalog table data row corresponding to the symbol. */
    protected int row;

    /** The table containing the data */
    TableQueryResult table;


    /**
     * Create a SymbolSelectionEvent for the given figure, row and table data.
     *
     * @param fig the figure that was selected or deselected
     * @param row the catalog table data row corresponding to the symbol
     * @param table the table containing the data
     */
    public SymbolSelectionEvent(CanvasFigure fig, int row, TableQueryResult table) {
        super(fig);
        this.row = row;
        this.table = table;
    }

    /**
     * Create a SymbolSelectionEvent for the given row and table data.
     *
     * @param row the catalog table data row corresponding to the symbol
     * @param table the table containing the data
     */
    public SymbolSelectionEvent(int row, TableQueryResult table) {
        super(table);
        this.row = row;
        this.table = table;
    }

    /** Return the figure for the event. */
    public CanvasFigure getFigure() {
        return (CanvasFigure) getSource();
    }

    /** Return the catalog table data row corresponding to the symbol. */
    public int getRow() {
        return row;
    }

    /** Return the table containing the data. */
    public TableQueryResult getTable() {
        return table;
    }
}
