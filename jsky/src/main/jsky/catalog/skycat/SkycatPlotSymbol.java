/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SkycatPlotSymbol.java,v 1.2 2002/08/05 10:57:21 brighton Exp $
 */

package jsky.catalog.skycat;

import jsky.catalog.RowCoordinates;
import jsky.catalog.TablePlotSymbol;
import jsky.catalog.TableQueryResult;
import jsky.util.TclUtil;



/**
 * Represents the contents of a plot symbol definition,
 * as defined in a skycat catalog config file.
 *
 * @version $Revision: 1.2 $
 * @author Allan Brighton
 */
public class SkycatPlotSymbol extends TablePlotSymbol {

    /**
     * Parses the given fields from the plot symbol definition in the
     * skycat catalog config file and makes the values available via methods.
     * Default values are filled in where needed.
     *
     * @param table contains the table data and information
     * @param cols a Tcl list of column names that may be used in symbol expressions
     * @param symbol a Tcl list of the form {shape color ratio angle label condition}
     * @param expr a Tcl list of the form {sizeExpr units}
     */
    public SkycatPlotSymbol(SkycatTable table, String cols, String symbol, String expr) {
        super(table, cols, symbol, expr);
    }


    /**
     * Initialize a SkycatPlotSymbol from the given values.
     *
     * @param table contains the table data and information
     * @param colNames an array of column headings used as variables
     * @param shapeName the name of the plot symbol shape
     * @param fg the name of the foreground color of the plot symbol
     * @param bg the name of the background color of the plot symbol
     * @param ratio the x/y ratio expression (stretch)
     * @param angle the angle expression
     * @param label the label expression
     * @param cond the condition expression
     * @param size the symbol size expression
     * @param units the units of the symbol size
     */
    public SkycatPlotSymbol(SkycatTable table, String[] colNames, String shapeName,
                            String fg, String bg, String ratio, String angle, String label,
                            String cond, String size, String units) {
        super(table, colNames, shapeName, fg, bg, ratio, angle, label, cond, size, units);
    }

    /** Return an object storing the column indexes where RA and Dec are found */
    public RowCoordinates getRowCoordinates() {
	TableQueryResult table = getTable();
	if (table instanceof SkycatTable) {
	    SkycatTable t = (SkycatTable)table;
	    return t.getConfigEntry().getRowCoordinates();
	}
	return super.getRowCoordinates();
    }

    /** Return the index of the center position RA column */
    public int getRaCol() {
	return getRowCoordinates().getRaCol();
    }

    /** Return the index of the center position Dec column */
    public int getDecCol() {
	return getRowCoordinates().getDecCol();
    }
}
