/*
 * ESO Archive
 *
 * $Id: TableDisplay.java,v 1.25 2002/08/04 21:48:50 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/12  Created
 */

package jsky.catalog.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.print.PrinterException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Hashtable;
import javax.swing.AbstractButton;
import javax.swing.BoundedRangeModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import jsky.catalog.FieldDesc;
import jsky.catalog.QueryResult;
import jsky.catalog.TableQueryResult;
import jsky.catalog.TestTableQueryResult;
import jsky.util.Preferences;
import jsky.util.PrintableWithDialog;
import jsky.util.SaveableAsHTML;
import jsky.util.gui.ArrayTableCellRenderer;
import jsky.util.gui.BasicWindowMonitor;
import jsky.util.gui.SexagesimalTableCellEditor;
import jsky.util.gui.SexagesimalTableCellRenderer;
import jsky.util.gui.SortedJTable;
import jsky.util.gui.TableUtil;


/**
 * This widget displays the contents of a TableQueryResult in a JTable and
 * implements scrolling, editing, searching and sorting methods.
 */
public class TableDisplay extends JPanel
        implements QueryResultDisplay, PrintableWithDialog, SaveableAsHTML {

    /** The table object containing the data to be displayed */
    private TableQueryResult _tableQueryResult;

    /** Used to display any query results found by following links */
    private QueryResultDisplay _queryResultDisplay;

    /** Used to scroll the table */
    private JScrollPane _scrollPane;

    /** widget used to display the table */
    private SortedJTable _table;

    /** Sum of column widths, used during resize */
    private int _sumColWidths = 0;

    /** If not null, an array specifying which columns to show or hide */
    private boolean[] _show;

    /** Used to remember _show settings */
    private static Hashtable _showTab = new Hashtable();

    /** Name of file used to remember the _showTab settings between sessions */
    private static String SHOW_TAB_FILE_NAME = "tableDisplayShowTab";

    /** Restore _showTab from the previous session, if the file exists */
    static {
        try {
            _showTab = (Hashtable) Preferences.getPreferences().deserialize(SHOW_TAB_FILE_NAME);
        }
        catch (Exception e) {
        }
    }


    /**
     * Create a TableDisplay for viewing the given table data.
     *
     * @param tableQueryResult the table to use.
     * @param queryResultDisplay used to display any query results (resulting from following links)
     */
    public TableDisplay(TableQueryResult tableQueryResult, QueryResultDisplay queryResultDisplay) {
        _queryResultDisplay = queryResultDisplay;
        _table = new SortedJTable();
        setBackground(Color.white);
        //_table.setIntercellSpacing(new Dimension(6, 3));
        if (tableQueryResult != null)
            setModel(tableQueryResult);
        _table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        _table.setRowSelectionAllowed(true);
        _table.setColumnSelectionAllowed(false);
        JTableHeader header = _table.getTableHeader();
        header.setUpdateTableInRealTime(false);
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        setLayout(new BorderLayout());
        _scrollPane = new JScrollPane(_table);
        add(_scrollPane, BorderLayout.CENTER);

        // handle resize events
        addComponentListener(new ComponentAdapter() {

            public void componentResized(ComponentEvent e) {
                resize();
            }
        });
    }


    /**
     * Create an empty TableDisplay (Call setModel to set the data to display).
     *
     * @param tableQueryResult the table to use.
     */
    public TableDisplay(TableQueryResult tableQueryResult) {
        this(tableQueryResult, null);
    }


    /**
     * Create an empty TableDisplay (Call setModel to set the data to display).
     *
     * @param queryResultDisplay used to display any query results (resulting from following links)
     */
    public TableDisplay(QueryResultDisplay queryResultDisplay) {
        this(null, queryResultDisplay);
    }


    /**
     * Initialize an empty table. Call setModel() to set the data to display,
     * and setQueryResultDisplay to set the display class to use when following links.
     */
    public TableDisplay() {
        this(null, null);
    }

    /** Return the widget used to display the table. */
    public SortedJTable getTable() {
        return _table;
    }

    /** Return the JScrollPane used to scroll the table */
    public JScrollPane getScrollPane() {
        return _scrollPane;
    }

    /** Set the object used to display query results (when following links) */
    public void setQueryResultDisplay(QueryResultDisplay q) {
        _queryResultDisplay = q;
    }

    /** Return the object used to display query results (when following links) */
    public QueryResultDisplay getQueryResultDisplay() {
        return _queryResultDisplay;
    }


    /**
     * If the given query result is a table, display it,
     * otherwise do nothing.
     */
    public void setQueryResult(QueryResult queryResult) {
        if (queryResult instanceof TableQueryResult)
            setModel((TableQueryResult) queryResult);
    }

    /**
     * Return the current table query result (same as the table model).
     */
    public TableQueryResult getTableQueryResult() {
        return _tableQueryResult;
    }


    /**
     * Set the data to display in the table.
     */
    public void setModel(TableQueryResult tableQueryResult) {
        _tableQueryResult = tableQueryResult;

        Object o = _showTab.get(_tableQueryResult.getName());
        if (o != null) {
            _show = (boolean[]) o;
            if (_show.length != _tableQueryResult.getColumnCount())
                _show = null;
        }

        _table.setModel(tableQueryResult);
        setColumnRenderers();
        _sumColWidths = TableUtil.initColumnSizes(_table, _show);
    }

    /**
     * Update the table after the model has changed.
     */
    public void update() {
        _table.setModel(_tableQueryResult);
        setColumnRenderers();
        _sumColWidths = TableUtil.initColumnSizes(_table, _show);
    }


    /**
     * Set up any required JTable renderers, so that the values in the query result are displayed
     * correctly by the JTable. This is needed, for example, if a certain column should
     * contain a button or other widget instead of the default text item, or if a value needs
     * special formatting.
     */
    protected void setColumnRenderers() {
        int numCols = _tableQueryResult.getColumnCount();
        for (int col = 0; col < numCols; col++) {
            FieldDesc field = _tableQueryResult.getColumnDesc(col);
            TableColumn column = _table.getColumn(_tableQueryResult.getColumnName(col));

            // linked fields are displayed as buttons
            if (field.hasLink()) {
                column.setCellRenderer(new HyperlinkTableCellRenderer(field, _tableQueryResult));
                column.setCellEditor(new HyperlinkTableCellEditor(field, _tableQueryResult, _queryResultDisplay));
            }
            else if (field.isRA()) {
                // RA,DEC coordinates are displayed in sexagesimal (hh:mm:ss.sss) notation
                column.setCellRenderer(new SexagesimalTableCellRenderer(true));
                column.setCellEditor(new SexagesimalTableCellEditor(true));
            }
            else if (field.isDec()) {
                column.setCellRenderer(new SexagesimalTableCellRenderer(false));
                column.setCellEditor(new SexagesimalTableCellEditor(false));
            }
            else {
                Class c = _tableQueryResult.getColumnClass(col);
                if (c != null && c.isArray()) {
                    column.setCellRenderer(new ArrayTableCellRenderer(c));
                }
            }
        }
    }

    /**
     * Display a print dialog to print the contents of this object
     * with the specified table title.
     */
    public void print(String title) throws PrinterException {
        _table.setTitle(title);
        _table.print();
    }

    /**
     * Display a print dialog to print the contents of this object.
     */
    public void print() throws PrinterException {
        _table.print();
    }


    /** Called when the table is resized */
    public void resize() {
        if (_tableQueryResult == null)
            return;
        int numCols = _tableQueryResult.getColumnCount();
        if (numCols == 1) {
            _table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            _table.sizeColumnsToFit(0);
        }
        else {
            if (_sumColWidths < getWidth()) {
                _table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
            }
            else {
                _table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            }
        }
    }


    /**
     * Select the given table row.
     *
     * @param row the index of the row (0 is the first row)
     */
    public void selectRow(int row) {
        // table may be sorted ...
        row = _table.getUnsortedRowIndex(row);

        // select the row
        ListSelectionModel model = _table.getSelectionModel();
        model.addSelectionInterval(row, row);

        // scroll to make the row visible
        BoundedRangeModel brm = _scrollPane.getVerticalScrollBar().getModel();
        int min = brm.getMinimum();
        int max = brm.getMaximum();
        int value = brm.getValue();
        int extent = brm.getExtent();
        int newvalue = row * (max - min) / _tableQueryResult.getRowCount();

        if (newvalue < value || newvalue > value + extent)
            brm.setValue(newvalue - extent / 2);
    }

    /**
     * Deselect the given table row.
     *
     * @param row the index of the row (0 is the first row)
     */
    public void deselectRow(int row) {
        row = _table.getUnsortedRowIndex(row);
        _table.getSelectionModel().removeSelectionInterval(row, row);
    }

    /** Return an array specifying which columns to show, if defined, otherwise null. */
    public boolean[] getShow() {
        return _show;
    }

    /** Set an array specifying which columns to show (or null, for default) */
    public void setShow(boolean[] show) {
        _show = show;
        _sumColWidths = TableUtil.initColumnSizes(_table, _show);

        if (_show != null)
            _showTab.put(_tableQueryResult.getName(), show);
        else
            _showTab.remove(_tableQueryResult.getName());

        update();

        // make the change permanent
        try {
            Preferences.getPreferences().serialize(SHOW_TAB_FILE_NAME, _showTab);
        }
        catch (Exception e) {
            // ignore
        }
    }


    /** Save the table to the given filename in HTML format */
    public void saveAsHTML(String filename) throws IOException {
        FileOutputStream os = new FileOutputStream(filename);

        int numCols = _table.getColumnCount(),
                numRows = _table.getRowCount(),
                n = numCols - 1;
        if (numCols == 0)
            return;

        PrintStream out = new PrintStream(os);

        // table title
        out.println("<html>");
        out.println("<body>");
        out.println("<table BORDER COLS=" + numCols + " WIDTH=\"100%\" NOSAVE>");
        out.println("<caption>" + _tableQueryResult.getTitle() + "</caption>");

        // column headings
        out.println("<tr>");
        for (int col = 0; col < numCols; col++) {
            if (_show == null || _show[col])
                out.println("<th>" + _table.getColumnName(col) + "</th>");
        }
        out.println("</tr>");

        // data rows
        for (int row = 0; row < numRows; row++) {
            out.println("<tr>");
            for (int col = 0; col < numCols; col++) {
                if (_show == null || _show[col]) {
                    // The renderer might display a different string, so use it to get the string
                    TableCellRenderer r = _table.getCellRenderer(row, col);
                    Component c = r.getTableCellRendererComponent(_table,
                            _table.getValueAt(row, col),
                            false, false, row, col);
                    String s = null;
                    if (c instanceof JLabel) {
                        s = ((JLabel) r).getText();
                    }
                    else if (c instanceof AbstractButton) {
                        s = ((AbstractButton) r).getText();
                    }
                    else {
                        Object o = _table.getValueAt(row, col);
                        if (o != null)
                            s = o.toString();
                        else
                            s = " ";
                    }
                    out.println("<td>" + s + "</td>");
                }
            }
            out.println("</tr>");
        }
        out.println("</table>");
        out.println("</body>");
        out.println("</html>");
    }


    /**
     * test main
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("TableDisplay");
        TableDisplay tableDisplay = new TableDisplay(new TestTableQueryResult());

        frame.getContentPane().add(tableDisplay, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}

