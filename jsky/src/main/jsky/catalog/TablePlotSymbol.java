/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TablePlotSymbol.java,v 1.2 2002/08/05 10:57:20 brighton Exp $
 */

package jsky.catalog;

import java.awt.Color;
import java.awt.Shape;
import java.util.Vector;

import jsky.catalog.RowCoordinates;
import jsky.catalog.TableQueryResult;
import jsky.util.JavaExpr;
import jsky.util.TclUtil;

import gnu.jel.DVResolver;
import jsky.util.StringUtil;


/**
 * Represents the contents of a catalog table plot symbol definition.
 * Any number of plot symbols may be defined. For each row in a table query result, 
 * each symbol may be displayed in an image window at a position given by the row's 
 * coordinate columns. Each symbol's size, rotation angle, x/y ratio, and label
 * may be calculated from table column values.
 *
 * @version $Revision: 1.2 $
 * @author Allan Brighton
 */
public class TablePlotSymbol implements DVResolver {

    // symbol shape constants
    public static final int CIRCLE = 0;
    public static final int SQUARE = 1;
    public static final int PLUS = 2;
    public static final int CROSS = 3;
    public static final int TRIANGLE = 4;
    public static final int DIAMOND = 5;
    public static final int ELLIPSE = 6;
    public static final int COMPASS = 7;
    public static final int LINE = 8;
    public static final int ARROW = 9;

    /** valid symbols (keep in the same order as the constants above */
    public static final String[] SYMBOLS = {
        "circle",
        "square",
        "plus",
        "cross",
        "triangle",
        "diamond",
        "ellipse",
        "compass",
        "line",
        "arrow"
    };

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
        "yellow"
    };

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
        Color.yellow
    };

    /** The default table column index containing the RA world coordinate value */
    public static final int DEFAULT_RA_COL = 1;

    /** The default table column index containing the Dec world coordinate value */
    public static final int DEFAULT_DEC_COL = 2;

    /** The default equinox for RA and Dec */
    public static final double DEFAULT_EQUINOX = 2000.;


    /** The symbol's name */
    private String _name;

    /** The symbol's description */
    private String _description;

    /** Contains the table data and description */
    private TableQueryResult _table;

    // optional index of the center position RA column (default 1)
    private int _raCol = DEFAULT_RA_COL;

    // optional index of the center position Dec column (default 2)
    private int _decCol = DEFAULT_DEC_COL;

    // optional equinox of RA and DEC (default 2000)
    private double _equinox = DEFAULT_EQUINOX;

    /** Array of column names used in the symbol expressions */
    private String[] _colNames;

    /** Array of column indexes for column names */
    private int[] _colIndexes;

    /** A Vector of column values from the table for the current row, used to get variable values */
    private Vector _rowVec;


    /** The symbol's shape (one of the constants defined in this class) */
    private int _shape = SQUARE;

    /** The symbol's shape name */
    private String _shapeName = SYMBOLS[SQUARE];

    /** Set to true if the shape has an internal, fillable area, otherwise false. */
    private boolean _hasArea;

    /** The symbol's foreground color (if no color is specified, use 2 colors: black and white) */
    private Color _fg = Color.white;

    /** The symbol's background color */
    private Color _bg = Color.black;


    /** X/Y size ratio (stretching) (may be expression using column names) */
    private String _ratio = "1";

    /** Used to evaluate the ratio expression */
    private JavaExpr _ratioExpr;


    /** The symbol's rotation angle (may be an expression using column names) */
    private String _angle = "0";

    /** Used to evaluate the angle expression */
    private JavaExpr _angleExpr;


    /** The symbol's label  (may be an expression using column names) */
    private String _label = "";

    /** Used to evaluate the label expression */
    private JavaExpr _labelExpr;


    /** The condition under which the symbol is plotted  (may be an expression using column names) */
    private String _cond = "1";

    /** Used to evaluate the symbol condition expression */
    private JavaExpr _condExpr;


    /** The symbol's size (may be an expression using column names) */
    private String _size = "4";

    /** Used to evaluate the symbol size expression */
    private JavaExpr _sizeExpr;

    /** Holds the value of the size expression, if it is a constant. */
    private double _sizeVal = 4.;

    /** The units of the symbol size ("image" for image pixels, "deg equinox" for world coordinate degrees */
    private String _units = "image";

    /** Return an object storing the column indexes where RA and Dec are found */
    private RowCoordinates _rowCoordinates;


    /**
     * Initialize an TablePlotSymbol with the default values.
     */
    public TablePlotSymbol() {
    }

    /**
     * Parses the given fields from a plot symbol definition in Tcl list format
     * and makes the values available via methods.
     * Default values are filled in where needed.
     *
     * @param table contains the table data and information
     * @param cols a Tcl list of column names that may be used in symbol expressions
     * @param symbol a Tcl list of the form {shape color ratio angle label condition}
     * @param expr a Tcl list of the form {sizeExpr units}
     */
    public TablePlotSymbol(TableQueryResult table, String cols, String symbol, String expr) {
        setTable(table);

        // check that plot columns are valid and also save the column indexes
        // for accessing row values as variables later
        setColNames(TclUtil.splitList(cols));

        // parse symbol info, a variable length list of
        _parseSymbol(TclUtil.splitList(symbol));

        // parse the size expr list: {size units}
        String[] exprList = TclUtil.splitList(expr);
        if (exprList.length == 0)
            throw new RuntimeException("invalid symbol expression: " + expr);
        setSize(exprList[0]);
        if (exprList.length > 1)
            setUnits(exprList[1]);
    }


    /**
     * Initialize a TablePlotSymbol from the given values.
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
    public TablePlotSymbol(TableQueryResult table, String[] colNames, String shapeName,
                            String fg, String bg, String ratio, String angle, String label,
                            String cond, String size, String units) {
        setTable(table);
        setColNames(colNames);
        setShapeName(shapeName);
        setFg(fg);
        setBg(bg);
        setRatio(ratio);
        setAngle(angle);
        setLabel(label);
        setCond(cond);
        setSize(size);
        setUnits(units);
    }


    /**
     * Parsed the plot symbol information in Tcl list format and return
     * an array of objects describing it.
     * Each symbol description needs to be in Tcl list format {colList shapeInfo sizeInfo}, 
     * with each symbol description separated by a colon ":".
     *
     * @param table object representing the catalog table
     * @return an array of TablePlotSymbol objects, one for each plot symbol defined.
     */
    public static TablePlotSymbol[] parsePlotSymbolInfo(String symbolInfo) {
        String[] ar = null;
        if (symbolInfo == null) {
            // default symbol settings
            ar = new String[]{"", "square yellow", "4"};
        }
        else {
            // Some config entries may not be separated with spaces ("{...}:{...}").
            // Insert spaces to avoid Tcl list errors
            symbolInfo = StringUtil.replace(symbolInfo, ":", " : ");

            // The format of symbolInfo is: list : list : ...,
            // where each list has 3 items: colInfo plotInfo sizeInfo,
            // where each item may have some more details specified (see the
            // skycat docs for details).
            // In the array below, every 4th element (if there is one) should
            // be a ":", since we are treating the entire string as one Tcl list.
            ar = TclUtil.splitList(symbolInfo);
            if (ar.length < 3)
                throw new RuntimeException("Bad plot symbol entry: " + symbolInfo);
        }

        // number of plot symbols (each entry has three elements, plus ":" between)
        TablePlotSymbol[] symbols = new TablePlotSymbol[(ar.length + 1) / 4];

        int n = 0;
        for (int i = 0; i < ar.length; i += 4) {
            if ((ar.length > i + 3 && !ar[i + 3].equals(":")) || ar.length < i + 3)
                throw new RuntimeException("Bad plot symbol entry: " + symbolInfo);
            symbols[n++] = new TablePlotSymbol(null, ar[i], ar[i + 1], ar[i + 2]);
        }
        return symbols;
    }


    /**
     * Return a String in Tcl list format describing the given array of plot symbol
     * objects.
     *
     * @param symbols an array of objects describing table plot symbols
     * @return a string where each symbol description is in Tcl list format 
     *         {colList shapeInfo sizeInfo}, with each symbol description separated 
     *         by a colon ":".
     */
    public static String getPlotSymbolInfo(TablePlotSymbol[] symbols) {
	if (symbols == null || symbols.length == 0)
	    return null;

	StringBuffer symbolStr = new StringBuffer();

	for(int i = 0; i < symbols.length; i++) {
	    String[] ar = new String[3];
	    ar[0] = symbols[i].getColNamesList();

	    String[] shapeInfo = new String[6];
	    shapeInfo[0] = symbols[i].getShapeName();
	    shapeInfo[1] = symbols[i].getColorName(symbols[i].getFg());
	    shapeInfo[2] = symbols[i].getRatio();
	    shapeInfo[3] = symbols[i].getAngle();
	    shapeInfo[4] = symbols[i].getLabel();
	    shapeInfo[5] = symbols[i].getCond();
	    ar[1] = TclUtil.makeList(shapeInfo);

	    String[] sizeInfo = new String[2];
	    sizeInfo[0] = symbols[i].getSize();
	    sizeInfo[1] = symbols[i].getUnits();
	    ar[2] = TclUtil.makeList(sizeInfo);

	    symbolStr.append(TclUtil.makeList(ar));
	    if (i < symbols.length-1)
		symbolStr.append(" : ");
	}

	return symbolStr.toString();
    }

    /**
     * Parse the fields of the symbol entry and set the relevant member variables.
     *
     * @param symb an array with strings describing the symbol's shape,
     *             color, ratio, angle, label, and condition, where only
     *             the shape is required. All other fields have default values.
     */
    private void _parseSymbol(String[] symb) {
        if (symb.length < 1)
            throw new RuntimeException("Bad plot symbol entry");

        // symbol shape
        setShapeName(symb[0]);

        // color
        if (symb.length >= 2) {
            if (symb[1].length() > 0) {
                setFg(symb[1]);
                setBg(symb[1]);
            }
        }

        // ratio
        if (symb.length >= 3) {
            if (symb[2].length() > 0) {
                setRatio(symb[2]);
            }
        }

        // angle
        if (symb.length >= 4) {
            if (symb[3].length() > 0) {
                setAngle(symb[3]);
            }
        }

        // label
        if (symb.length >= 5) {
            if (symb[4].length() > 0) {
                setLabel(symb[4]);
            }
        }

        // cond
        if (symb.length >= 6) {
            if (symb[5].length() > 0) {
                setCond(symb[5]);
            }
        }
    }


    /* Calculate the indexs of the column names being used */
    private void _calculateColumnIndexes() {
        if (_colNames != null && _table != null) {
            _colIndexes = new int[_colNames.length];
            for (int i = 0; i < _colNames.length; i++) {
                _colIndexes[i] = _table.getColumnIndex(_colNames[i]);
            }
        }
    }


    /** Return an object storing the column indexes where RA and Dec are found */
    public RowCoordinates getRowCoordinates() {
	if (_rowCoordinates != null)
	    return _rowCoordinates;

	_rowCoordinates = new RowCoordinates(_raCol, _decCol, _equinox);
	return _rowCoordinates;
    }


    /** Set the display name of the symbol */
    public void setName(String name) {
        _name = name;
    }

    /** Return the display name of the symbol */
    public String getName() {
        return _name;
    }

    /** Set the symbol description */
    public void setDescription(String description) {
        _description = description;
    }

    /** Return the symbol description, or null if not available */
    public String getDescription() {
        return _description;
    }


    /** Return the index for the given shape name, or a default, if not found. */
    private int _getSymbolIndex(String shapeName) {
        for (int i = 0; i < SYMBOLS.length; i++) {
            if (shapeName.equals(SYMBOLS[i])) {
                return i;
            }
        }
        return 0;
    }

    /** Return true if the given symbol shape has an internal area */
    private boolean _checkHasArea(int s) {
        return (s == CIRCLE || s == SQUARE || s == TRIANGLE || s == DIAMOND || s == ELLIPSE);
    }


    /**
     * Return the Color object, given the name, or a default color (yellow), if not known
     */
    private Color _getColor(String name) {
        for (int i = 0; i < COLOR_NAMES.length; i++) {
            if (name.equals(COLOR_NAMES[i]))
                return COLORS[i];
        }
        return Color.yellow;
    }

    /**
     * Return the Color name, given a Color object, if known, otherwise just "yellow", if not known.
     */
    public String getColorName(Color c) {
        for (int i = 0; i < COLORS.length; i++) {
            if (c.equals(COLORS[i]))
                return COLOR_NAMES[i];
        }
        return "yellow";
    }

    /** Set the catalog table containing the data to be plotted */
    public void setTable(TableQueryResult table) {
        _table = table;
	if (_table != null) {
	    _calculateColumnIndexes();
	    _compileExpressions();
	}
    }

    /** Return the catalog table containing the data to be plotted */
    public TableQueryResult getTable() {
        return _table;
    }


    /** Return the index of the center position RA column */
    public int getRaCol() {return _raCol;}

    /** Set the index of the center position RA column (default 1) */
    public void setRaCol(int raCol) {
	_raCol = raCol;
	_rowCoordinates = null;
    }

    /** Return the index of the center position Dec column */
    public int getDecCol() {return _decCol;}

    /** Set the index of the center position Dec column (default 2) */
    public void setDecCol(int decCol) {
	_decCol = decCol;
	_rowCoordinates = null;
    }

    /**  Return the equinox of the RA and DEC columns */
    public double getEquinox() {return _equinox;}

    /** Set the equinox of the RA and DEC columns (default 2000) */
    public void setEquinox(double equinox) {
	_equinox = equinox;
	_rowCoordinates = null;
    }

    /** Set the array of column names used in the symbol expressions */
    public void setColNames(String[] colNames) {
        _colNames = colNames;
        _calculateColumnIndexes();
    }

    /** Return the array of column names used in the symbol expressions */
    public String[] getColNames() {
        return _colNames;
    }

    /** Return the array of column indexes for column names */
    public int[] getColIndexes() {
        return _colIndexes;
    }

    /** Return a list of column names used in the symbol expressions in Tcl list format */
    public String getColNamesList() {
        return TclUtil.makeList(_colNames);
    }


    /**
     * Set the symbol's shape as a constant (CIRCLE, ELLIPSE, etc...).
     */
    public void setShape(int shape) {
        _shape = shape;
        _hasArea = _checkHasArea(_shape);
        _shapeName = SYMBOLS[shape];
    }

    /**
     * Return the symbol's shape as a constant (CIRCLE, ELLIPSE, etc...).
     * (Note: this method could return a Shape object, but the coordinates
     *  are different for each row).
     */
    public int getShape() {
        return _shape;
    }

    /** Set the symbol shape name  */
    public void setShapeName(String shapeName) {
        _shapeName = shapeName;
        _shape = _getSymbolIndex(shapeName);
        _hasArea = _checkHasArea(_shape);
    }

    /** Return the symbol shape name  */
    public String getShapeName() {
        return _shapeName;
    }

    /**
     * If this symbol type has an internal area, return the given shape, otherwise return
     * the bounding box of the shape.
     */
    public Shape getBoundingShape(Shape shape) {
        if (_hasArea)
            return shape;
        return shape.getBounds2D();
    }

    /** Set the symbol's foreground color */
    public void setFg(String colorName) {
        _fg = _getColor(colorName);
    }

    /** Set the symbol's foreground color */
    public void setFg(Color color) {
        _fg = color;
    }

    /** Return the symbol's foreground color (if no color is specified, use 2 colors: black and white) */
    public Color getFg() {
        return _fg;
    }

    /** Set the symbol's background color */
    public void setBg(String colorName) {
        _bg = _getColor(colorName);
    }

    /** Set the symbol's background color */
    public void setBg(Color color) {
        _bg = color;
    }

    /** Return the symbol's background color */
    public Color getBg() {
        return _bg;
    };


    /** Return the X/Y size ratio (stretching) (may be expression using column names) */
    public void setRatio(String ratio) {
        _ratio= ratio;
    }

    /** Return the X/Y size ratio (stretching) (may be expression using column names) */
    public String getRatio() {
        return _ratio;
    }

    /** Return the symbol's ratio, evaluating the expression for the given row. */
    public double getRatio(Vector rowVec) {
        if (_ratioExpr != null) {
            _rowVec = rowVec;
            try {
                return _ratioExpr.eval();
            }
            catch (Throwable t) {
            }
        }
        return 1.;
    }


    /** Return the symbol's rotation angle (may be an expression using column names) */
    public void setAngle(String angle) {
        _angle = angle;
    }

    /** Return the symbol's rotation angle (may be an expression using column names) */
    public String getAngle() {
        return _angle;
    }

    /** Return the symbol's rotation angle, evaluating the expression for the given row. */
    public double getAngle(Vector rowVec) {
        if (_angleExpr != null) {
            _rowVec = rowVec;
            try {
                return _angleExpr.eval();
            }
            catch (Throwable t) {
            }
        }
        return 0.;
    }


    /** Set the symbol's label (may be an expression using column names) */
    public void setLabel(String label) {
        _label = label;
    }

    /** Return the symbol's label (may be an expression using column names) */
    public String getLabel() {
        return _label;
    }

    /** Return the symbol's label,  evaluating the expression for the given row. */
    public String getLabel(Vector rowVec) {
        if (_labelExpr != null) {
            _rowVec = rowVec;
            try {
                return _labelExpr.evalObject().toString();
            }
            catch (Throwable t) {
            }
        }
        return null;
    }


    /** Set the condition under which the symbol will be plotted  (may be an expression using column names) */
    public void setCond(String cond) {
        _cond = cond;
    }

    /** Return the condition under which the symbol will be plotted  (may be an expression using column names) */
    public String getCond() {
        return _cond;
    }

    /** Return the condition under which the symbol will be plotted,  evaluating the expression for the given row. */
    public boolean getCond(Vector rowVec) {
        if (_condExpr != null) {
            _rowVec = rowVec;
            try {
                return _condExpr.evalBoolean();
            }
            catch (Throwable t) {
            }
        }
        return true;
    }


    /** Set the symbol's size (may be an expression using column names) */
    public void setSize(String size) {
        _size = size;
    }

    /** Return the symbol's size (may be an expression using column names) */
    public String getSize() {
        return _size;
    }

    /** Return the symbol's size, evaluating the size expression for the given row. */
    public double getSize(Vector rowVec) {
        if (_sizeExpr != null) {
            _rowVec = rowVec;
            try {
                return _sizeExpr.eval();
            }
            catch (Throwable t) {
            }
        }
        return _sizeVal;
    }


    /** Set the units of the symbol size ("image" for image pixels, "deg equinox" for world coordinate degrees */
    public void setUnits(String units) {
        _units = units;
    }

    /** Return the units of the symbol size ("image" for image pixels, "deg equinox" for world coordinate degrees */
    public String getUnits() {
        return _units;
    }


    /** Implements the DVResolver interface */
    public String getTypeName(String name) {
        if (name.startsWith("$"))
            name = name.substring(1);

        for (int i = 0; i < _colNames.length; i++) {
            if (_colNames[i].equals(name)) {
                Class c = _table.getColumnClass(_colIndexes[i]);
                if (c.equals(Double.class))
                    return "Double";
            }
        }
        return "String";
    }


    /** Called by reflection for the DVResolver interface to get the value of the named variable of type Double */
    public double getDoubleProperty(String name) {
        if (name.startsWith("$"))
            name = name.substring(1);

        for (int i = 0; i < _colNames.length; i++) {
            if (_colNames[i].equals(name)) {
                Object value = _rowVec.get(_colIndexes[i]);
                if (value instanceof Double)
                    return ((Double) value).doubleValue();
            }
        }
        return 0.0;
    }

    /** Called by reflection for the DVResolver interface to get the value of the named variable of type String */
    public String getStringProperty(String name) {
        if (name.startsWith("$"))
            name = name.substring(1);

        for (int i = 0; i < _colNames.length; i++) {
            if (_colNames[i].equals(name)) {
                String value = (String) _rowVec.get(_colIndexes[i]);
                if (value != null)
                    return value;
            }
        }
        return null;
    }


    /**
     * Compile any expressions that are based on column values. If the expression is constant,
     * it is not compiled.
     */
    private void _compileExpressions() {
        try {
            _sizeVal = Double.parseDouble(_size);
        }
        catch (Exception e) {
            try {
                // cast to make sure the result of the expr is a double
                _sizeExpr = new JavaExpr("(double)" + _size, this);
            }
            catch (Throwable t) {
                _sizeVal = 5;
                //Logger.debug(this, "Error in symbol size expression: " + _size, t);
            }
        }

        if (_cond.length() != 0 && !_cond.equals("1")) {
            try {
                _condExpr = new JavaExpr(_cond, this);
            }
            catch (Throwable t) {
            }
        }

        if (_angle.length() != 0 && !_angle.equals("0")) {
            try {
                _angleExpr = new JavaExpr("(double)" + _angle, this);
            }
            catch (Throwable t) {
            }
        }

        if (_ratio.length() != 0 && !_ratio.equals("1")) {
            try {
                _ratioExpr = new JavaExpr("(double)" + _ratio, this);
            }
            catch (Throwable t) {
            }
        }

        if (_label.length() != 0) {
            try {
                _labelExpr = new JavaExpr(_label, this);
            }
            catch (Throwable t) {
            }
        }
    }
}
