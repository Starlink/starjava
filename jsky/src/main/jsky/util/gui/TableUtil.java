/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TableUtil.java,v 1.7 2002/08/04 21:48:51 brighton Exp $
 */

package jsky.util.gui;

import java.awt.Component;
import java.lang.Math;
import java.lang.reflect.Method;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * Implements static utility methods for use with JTables.
 *
 * @version $Revision: 1.7 $
 * @author Allan Brighton
 */
public class TableUtil {

    /**
     * Return the default cell renderer for the given JTable column.
     */
    public static TableCellRenderer getDefaultRenderer(JTable table, TableColumn column) {
        try {
            return table.getTableHeader().getDefaultRenderer();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * This method picks good column sizes for the given JTable.
     * If all column heads are wider than the column's cells'
     * contents, then you can just use column.sizeWidthToFit().
     *
     * @param table the target JTable
     * @param show if not null, should be an array with a boolean entry for each column
     *             indicating whether the column should be shown or ignored.
     *
     * @return the sum of all the column widths
     */
    public static int initColumnSizes(JTable table, boolean[] show) {
        TableColumn column = null;
        Component comp = null;
        int cellWidth = 0;
        TableModel model = table.getModel();
        int numCols = model.getColumnCount();
        int numRows = model.getRowCount();

        if (show != null && show.length != numCols)
            show = null;

        int sumColWidths = 0;
        for (int col = 0; col < numCols; col++) {
            column = table.getColumnModel().getColumn(col);

            if (show == null || show[col]) {
                TableCellRenderer defaultRenderer = getDefaultRenderer(table, column);
                TableCellRenderer cellRenderer = column.getCellRenderer();
                if (cellRenderer == null)
                    cellRenderer = defaultRenderer;
                TableCellRenderer headerRenderer = column.getHeaderRenderer();
                if (headerRenderer == null)
                    headerRenderer = defaultRenderer;

                // check the header width
                comp = headerRenderer.getTableCellRendererComponent(table, column.getHeaderValue(), false, false, -1, col);
                cellWidth = comp.getPreferredSize().width;

                // check the rendered width of the widest row
                for (int row = 0; row < numRows; row++) {
                    Object o = model.getValueAt(row, col);
                    comp = cellRenderer.getTableCellRendererComponent(table, o, false, false, row, col);
                    cellWidth = Math.max(cellWidth, comp.getPreferredSize().width);
                }

                cellWidth += 10; // add padding
                sumColWidths += cellWidth;
                column.setPreferredWidth(cellWidth);
                column.setMinWidth(5);
                column.setMaxWidth(1000);
            }
            else {
                // hide column
                column.setMinWidth(0);
                column.setMaxWidth(0);
                column.setPreferredWidth(0);
            }
        }
        return sumColWidths;
    }

    /*
     * Return the index of the row containing the longest value in the given column.
     */
    public static int getWidestRow(TableModel model, int col) {
        int widestRow = 0;
        int maxLength = 0;
        int numRows = model.getRowCount();
        for (int row = 0; row < numRows; row++) {
            Object o = model.getValueAt(row, col);
            if (o != null) {
                String s = o.toString();
                int length = s.length();
                if (length > maxLength) {
                    maxLength = length;
                    widestRow = row;
                }
            }
        }
        return widestRow;
    }
}



