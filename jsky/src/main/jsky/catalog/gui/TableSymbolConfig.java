/*
 * Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TableSymbolConfig.java,v 1.1 2002/08/04 21:48:50 brighton Exp $
 */

package jsky.catalog.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import jsky.catalog.Catalog;
import jsky.catalog.CatalogDirectory;
import jsky.catalog.PlotableCatalog;
import jsky.catalog.TablePlotSymbol;
import jsky.catalog.TableQueryResult;
import jsky.util.I18N;
import jsky.util.IApplyCancel;
import jsky.util.Resources;
import jsky.util.TclUtil;

/**
 * A user interface for setting plot symbol preferences.
 *
 * @version $Revision: 1.1 $
 * @author Allan Brighton
 */
public class TableSymbolConfig extends TableSymbolConfigGUI implements IApplyCancel {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(TableSymbolConfig.class);

    /** Object to use to plot catalog symbols */
    private TablePlotter _plotter;

    /** The table being configured */
    private TableQueryResult _table;

    /** The currently selected row in the symbol JTable */
    private int _selectedSymbolRow = 0;

    /** Plot symbol descriptions */
    private TablePlotSymbol[] _symbols;

    /** The icons in the symbol menu */
    private Icon[] _symbolIcons;

    /** Columns in the symbol description table */
    public static final String[] COL_NAMES = {
        _I18N.getString("columns"),
        _I18N.getString("symbol"),
        _I18N.getString("color"),
        _I18N.getString("ratio"),
        _I18N.getString("angle"),
        _I18N.getString("label"),
        _I18N.getString("condition"),
        _I18N.getString("size"),
        _I18N.getString("units")
    };

    /** Index of the column of the same name in the symbol JTable. */
    public static final int COLUMNS = 0;
    public static final int SYMBOL = 1;
    public static final int COLOR = 2;
    public static final int RATIO = 3;
    public static final int ANGLE = 4;
    public static final int LABEL = 5;
    public static final int CONDITION = 6;
    public static final int SIZE = 7;
    public static final int UNITS = 8;

    /** Display names for units */
    public static final String[] UNIT_NAMES = {
        _I18N.getString("imagePixels"),
        _I18N.getString("wcsDeg")
    };

    /** Internal names for units */
    public static final String[] UNIT_STRINGS = {
        "image", "deg"
    };
    public static final int IMAGE_PIXEL_UNITS = 0;
    public static final int WCS_DEG_UNITS = 1;

    /**
     * Constructor.
     *
     * @param plotter object to use to plot catalog symbols.
     * @param table the table being configured
     */
    public TableSymbolConfig(TablePlotter plotter, TableQueryResult table) {
        super();
        _plotter = plotter;

        symbolTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        symbolTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;
                int first = e.getFirstIndex();
                int last = e.getLastIndex();
                ListSelectionModel model = symbolTable.getSelectionModel();
                for (int i = first; i <= last; i++) {
                    if (model.isSelectedIndex(i) && i >= 0 && i < _symbols.length) {
                        _selectedSymbolRow = i;
                        editSymbol(_symbols[i]);
                        break;
                    }
                }
            }
        });
        //symbolTable.setCellSelectionEnabled(false);
	symbolTable.setRowSelectionAllowed(true);
        symbolTable.setColumnSelectionAllowed(false);
        symbolTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        useList.setModel(new DefaultListModel());
        ignoreList.setModel(new DefaultListModel());

        // setup the symbol combobox
        _symbolIcons = new Icon[TablePlotSymbol.SYMBOLS.length];
        for (int i = 0; i < TablePlotSymbol.SYMBOLS.length; i++) {
            _symbolIcons[i] = Resources.getIcon("symb_" + TablePlotSymbol.SYMBOLS[i] + ".gif");
            symbolComboBox.addItem(_symbolIcons[i]);
        }
        symbolComboBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                // enable/disable the ratio and angle text fields for some symbols
                symbolSelected();
            }
        });

        // setup the color combobox
        for (int i = 0; i < TablePlotSymbol.COLOR_NAMES.length; i++) {
            colorComboBox.addItem(TablePlotSymbol.COLOR_NAMES[i]);
        }

        // setup the units combobox
        for (int i = 0; i < UNIT_NAMES.length; i++) {
            unitsComboBox.addItem(UNIT_NAMES[i]);
        }

        setTable(table);
    }


    /** Called when a symbol shape is selected from the combo box */
    protected void symbolSelected() {
        boolean enabled = true;
        Object o = symbolComboBox.getSelectedItem();
        for (int i = 0; i < _symbolIcons.length; i++) {
            if (o.equals(_symbolIcons[i])) {
                switch (i) {
                case TablePlotSymbol.CIRCLE:
                case TablePlotSymbol.SQUARE:
                case TablePlotSymbol.CROSS:
                case TablePlotSymbol.TRIANGLE:
                case TablePlotSymbol.DIAMOND:
                    enabled = false;
                    break;
                }
                break;
            }
        }
        ratioTextField.setEnabled(enabled);
        angleTextField.setEnabled(enabled);
    }


    /**
     * Set the table to configure. If the table is the result of a catalog
     * query, this configures the plot symbols for the source catalog.
     *
     * @param table the table being configured.
     */
    public void setTable(TableQueryResult table) {
        _table = table;
        _symbols = _plotter.getPlotSymbolInfo(_table);
        if (_symbols == null) {
            removeButton.setEnabled(false);
            return;
        }
        removeButton.setEnabled(_symbols.length > 1);

        // set the contents of the table listing the symbol info
        String[][] data = new String[_symbols.length][COL_NAMES.length];
        for (int i = 0; i < _symbols.length; i++) {
            TablePlotSymbol symb = _symbols[i];
            for (int j = 0; j < COL_NAMES.length; j++) {
                String s = "";
                switch (j) {
                case COLUMNS:
                    s = symb.getColNamesList();
                    break;
                case SYMBOL:
                    s = symb.getShapeName();
                    break;
                case COLOR:
                    s = symb.getColorName(symb.getFg());
                    break;
                case RATIO:
                    s = symb.getRatio();
                    break;
                case ANGLE:
                    s = symb.getAngle();
                    break;
                case LABEL:
                    s = symb.getLabel();
                    break;
                case CONDITION:
                    s = symb.getCond();
                    break;
                case SIZE:
                    s = symb.getSize();
                    break;
                case UNITS:
                    s = symb.getUnits();
                    break;
                }
                data[i][j] = s;
            }
        }
        symbolTable.setModel(new DefaultTableModel(data, COL_NAMES) {

            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        symbolTable.getSelectionModel().setSelectionInterval(0, 0);
    }


    /** Set the contents of the given list to the given array. */
    protected void setListData(JList list, String[] ar) {
        DefaultListModel model = (DefaultListModel) list.getModel();
        model.removeAllElements();
        if (ar != null) {
            for (int i = 0; i < ar.length; i++)
                model.addElement(ar[i]);
        }
    }

    /** Set the contents of the given list to the given vector. */
    protected void setListData(JList list, Vector v) {
        DefaultListModel model = (DefaultListModel) list.getModel();
        model.removeAllElements();
        if (v != null) {
            int n = v.size();
            for (int i = 0; i < n; i++)
                model.addElement(v.get(i));
        }
    }


    /** Edit the given symbol definition */
    protected void editSymbol(TablePlotSymbol symb) {
        // List of column variables used
        String[] used = symb.getColNames();
        setListData(useList, used);

        // List of column variables not used
        Vector v = _table.getColumnIdentifiers();
        int size = v.size();
        Vector ignore = new Vector(size);
        for (int i = 0; i < size; i++) {
            String s = (String)v.get(i);
            if (used == null) {
                ignore.add(s);
            }
            else {
                for (int j = 0; j < used.length; j++) {
                    if (s.equals(used[j]))
                        continue;
                    ignore.add(s);
                }
            }
        }
        setListData(ignoreList, ignore);

        // Select the correct plot symbol
        String shapeName = symb.getShapeName();
        String[] ar = TablePlotSymbol.SYMBOLS;
        for (int i = 0; i < ar.length; i++) {
            if (shapeName.equals(ar[i])) {
                symbolComboBox.getModel().setSelectedItem(_symbolIcons[i]);
                break;
            }
        }

        // Select the correct color
        String colorName = symb.getColorName(symb.getFg());
        ar = TablePlotSymbol.COLOR_NAMES;
        for (int i = 0; i < ar.length; i++) {
            if (colorName.equals(ar[i])) {
                colorComboBox.getModel().setSelectedItem(ar[i]);
                break;
            }
        }

        // Select the correct units
        String units = symb.getUnits();
        if (units.startsWith("deg"))
            unitsComboBox.getModel().setSelectedItem(UNIT_NAMES[WCS_DEG_UNITS]);
        else
            unitsComboBox.getModel().setSelectedItem(UNIT_NAMES[IMAGE_PIXEL_UNITS]);

        // update the text fields
        ratioTextField.setText(symb.getRatio());
        angleTextField.setText(symb.getAngle());
        labelTextField.setText(symb.getLabel());
        conditionTextField.setText(symb.getCond());
        sizeTextField.setText(symb.getSize());
    }

    /** Update the selected symbolTable row with the currently displayed plot information. */
    protected void updateSymbolTable() {
        DefaultTableModel tableModel = (DefaultTableModel) symbolTable.getModel();

        // columns
        Object[] ar = ((DefaultListModel) useList.getModel()).toArray();
        String value = TclUtil.makeList(ar);
        tableModel.setValueAt(value, _selectedSymbolRow, COLUMNS);

        // symbol
        Object o = symbolComboBox.getSelectedItem();
        for (int i = 0; i < _symbolIcons.length; i++) {
            if (o.equals(_symbolIcons[i])) {
                tableModel.setValueAt(TablePlotSymbol.SYMBOLS[i], _selectedSymbolRow, SYMBOL);
                break;
            }
        }

        // color
        String color = (String)colorComboBox.getSelectedItem();
        tableModel.setValueAt(color, _selectedSymbolRow, COLOR);

        // ratio
        tableModel.setValueAt(ratioTextField.getText(), _selectedSymbolRow, RATIO);

        // angle
        tableModel.setValueAt(angleTextField.getText(), _selectedSymbolRow, ANGLE);

        // label
        tableModel.setValueAt(labelTextField.getText(), _selectedSymbolRow, LABEL);

        // condition
        tableModel.setValueAt(conditionTextField.getText(), _selectedSymbolRow, CONDITION);

        // size
        tableModel.setValueAt(sizeTextField.getText(), _selectedSymbolRow, SIZE);

        // units
        String units = UNIT_STRINGS[unitsComboBox.getSelectedIndex()];
        tableModel.setValueAt(units, _selectedSymbolRow, UNITS);
    }


    /**
     * Return an array of TablePlotSymbol objects based on the user's selections
     * (one object for each row in the table).
     */
    protected TablePlotSymbol[] getPlotSymbolInfo() {
        // make sure the current info is in the table
        updateSymbolTable();

        // initialize the symbol info from the table
        DefaultTableModel tableModel = (DefaultTableModel) symbolTable.getModel();
        int numRows = tableModel.getRowCount();
        TablePlotSymbol[] symbols = new TablePlotSymbol[numRows];
        for (int i = 0; i < numRows; i++) {
	    symbols[i] = new TablePlotSymbol();
            symbols[i].setColNames(TclUtil.splitList((String)tableModel.getValueAt(i, COLUMNS)));
            symbols[i].setShapeName((String)tableModel.getValueAt(i, SYMBOL));
            symbols[i].setFg((String)tableModel.getValueAt(i, COLOR));
            symbols[i].setBg(symbols[i].getFg());
            symbols[i].setRatio((String)tableModel.getValueAt(i, RATIO));
            symbols[i].setAngle((String)tableModel.getValueAt(i, ANGLE));
            symbols[i].setLabel((String)tableModel.getValueAt(i, LABEL));
            symbols[i].setCond((String)tableModel.getValueAt(i, CONDITION));
            symbols[i].setSize((String)tableModel.getValueAt(i, SIZE));
            symbols[i].setUnits((String)tableModel.getValueAt(i, UNITS));
        }
        return symbols;
    }


    /** Move a column name to the left listbox (usedList) */
    void leftArrowButton_actionPerformed(ActionEvent e) {
        Object o = ignoreList.getSelectedValue();
        if (o instanceof String) {
            ((DefaultListModel) ignoreList.getModel()).remove(ignoreList.getSelectedIndex());
            ((DefaultListModel) useList.getModel()).addElement(o);
        }
    }

    /** Move a column name to the right listbox (ignoreList) */
    void rightArrowButton_actionPerformed(ActionEvent e) {
        Object o = useList.getSelectedValue();
        if (o instanceof String) {
            ((DefaultListModel) useList.getModel()).remove(useList.getSelectedIndex());
            ((DefaultListModel) ignoreList.getModel()).addElement(o);
        }
    }

    /** Add a new default symbol entry to the table. */
    void addButton_actionPerformed(ActionEvent e) {
        Object[] ar = new Object[COL_NAMES.length];
        ar[COLUMNS] = "";
        ar[SYMBOL] = "square";
        ar[COLOR] = "yellow";
        ar[RATIO] = "";
        ar[ANGLE] = "";
        ar[LABEL] = "";
        ar[CONDITION] = "";
        ar[SIZE] = "4";
        ar[UNITS] = "";
        DefaultTableModel tableModel = (DefaultTableModel) symbolTable.getModel();
        int numRows = tableModel.getRowCount();
        tableModel.addRow(ar);
        removeButton.setEnabled(true);

        // update the symbols array
        TablePlotSymbol[] oldSymbols = _symbols;
        int n = 0;
        if (oldSymbols != null) {
            n = oldSymbols.length;
            _symbols = new TablePlotSymbol[n + 1];
            for (int i = 0; i < n; i++)
                _symbols[i] = oldSymbols[i];
        }
	else {
	    _symbols = new TablePlotSymbol[1];
	}
        _symbols[n] = new TablePlotSymbol();
	_symbols[n].setTable(_table);
        symbolTable.getSelectionModel().setSelectionInterval(numRows, numRows);
    }

    /** Remove the selected symbol entry from the table. */
    void removeButton_actionPerformed(ActionEvent e) {
        DefaultTableModel tableModel = (DefaultTableModel) symbolTable.getModel();
        int numRows = tableModel.getRowCount();

        if (_symbols.length > 1 && numRows > 1 && _selectedSymbolRow >= 0 && _selectedSymbolRow < numRows) {
            tableModel.removeRow(_selectedSymbolRow);
            TablePlotSymbol[] oldSymbols = _symbols;
            _symbols = new TablePlotSymbol[oldSymbols.length - 1];
            int n = 0;
            for (int i = 0; i < numRows; i++) {
                if (i != _selectedSymbolRow)
                    _symbols[n++] = oldSymbols[i];
            }
            if (--_selectedSymbolRow < 0)
                _selectedSymbolRow = 0;
            symbolTable.getSelectionModel().setSelectionInterval(_selectedSymbolRow, _selectedSymbolRow);
        }
        removeButton.setEnabled(_symbols.length > 1);
    }

    /** Apply changes and replot.  */
    public void apply() {
        // update the plotter info
        _symbols = getPlotSymbolInfo();
        _plotter.setPlotSymbolInfo(_table, _symbols);

        // replot the table with the new settings
        _plotter.unplot(_table);
        _plotter.plot(_table);

        // update the catalog and config file with the new information
        PlotableCatalog cat = (PlotableCatalog)_table.getCatalog();
        if (cat != null) {
	    // save changes
	    cat.setSymbols(_symbols);
	    cat.setSymbolsEdited(true);
	    
	    // Add the catalog to the top level catalog directory and then call save on it
	    CatalogDirectory rootDir = null, catDir = cat.getParent();
	    if (catDir != null)
		rootDir = (CatalogDirectory)catDir.getRoot();
	    if (rootDir != null) {
		Catalog existingCat = rootDir.getCatalog(cat.getName());
		if (existingCat != null && existingCat != cat)
		    rootDir.removeCatalog(existingCat);
		rootDir.addCatalog(cat);
		rootDir.save();
	    }
	}
    }

    /** Cancel changes */
    public void cancel() {
        if (_symbols != null)
            editSymbol(_symbols[_selectedSymbolRow]);
    }
}

