//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class PrintableJTable
//
//--- Description -------------------------------------------------------------
//	A JTable that implements the java.awt.print.Printable interface.
//	The table knows how to print its contents.  Also includes print()
//	convenience methods which contain all the code necessary to perform the print,
//	including prompting user with page setup dialogs, etc.
//
//--- Notes -------------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	10/27/99	J. Jones / 588
//
//		Original implementation.
//
//	02/03/01	A. Brighton
//		Fixed problems with multi-page tables.
//	11/20/01
//		Added support for zero length (Hidden) columns
//
//
//--- DISCLAIMER---------------------------------------------------------------
//
//	This software is provided "as is" without any warranty of any kind, either
//	express, implied, or statutory, including, but not limited to, any
//	warranty that the software will conform to specification, any implied
//	warranties of merchantability, fitness for a particular purpose, and
//	freedom from infringement, and any warranty that the documentation will
//	conform to the program, or any warranty that the software will be error
//	free.
//
//	In no event shall NASA be liable for any damages, including, but not
//	limited to direct, indirect, special or consequential damages, arising out
//	of, resulting from, or in any way connected with this software, whether or
//	not based upon warranty, contract, tort or otherwise, whether or not
//	injury was sustained by persons or property or otherwise, and whether or
//	not loss was sustained from or arose out of the results of, or use of,
//	their software or services provided hereunder.
//
//=== End File Prolog =========================================================

//package gov.nasa.gsfc.util.gui;

package jsky.util.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.print.*;
import java.util.Vector;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RepaintManager;
import javax.swing.table.*;


/**
 * A JTable that implements the java.awt.print.Printable interface.
 * The table knows how to print its contents.  Also includes print()
 * convenience methods which contain all the code necessary to perform the print,
 * including prompting user with page setup dialogs, etc.
 *
 * <P>PrintableJTable adds a Title property that can be set via setTitle()
 * or specified as an argument to print().  The Title is optional.  It will be printed
 * above the table if it is specified.
 *
 * <P>Original print code taken from "Swing" online book by
 * Matthew Robinson and Pavel Vorobiev, Ph.D:
 * <A HREF="http://manning.spindoczine.com/sbe/">http://manning.spindoczine.com/sbe/</A>.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		10/27/99
 * @author		J. Jones / 588
 **/
public class PrintableJTable extends JTable implements Printable {

    /**
     * The maximum number of pages for a given print job.
     * This number is computed when a print is started,
     * but defaults to 1.
     **/
    private int fMaxNumPage = 1;

    /**
     * The Title as a JLabel to preserve the font.
     * Title may be null.
     **/
    private JLabel fTitle = null;

    public PrintableJTable() {
        super();
    }

    public PrintableJTable(int numRows, int numColumns) {
        super(numRows, numColumns);
    }

    public PrintableJTable(Object[][] rowData, Object[] columnNames) {
        super(rowData, columnNames);
    }

    public PrintableJTable(TableModel dm) {
        super(dm);
    }

    public PrintableJTable(TableModel dm, TableColumnModel cm) {
        super(dm, cm);
    }

    public PrintableJTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);
    }

    public PrintableJTable(Vector rowData, Vector columnNames) {
        super(rowData, columnNames);
    }

    /**
     * Gets the title to be printed before the table contents.
     *
     * @return title to print before the table
     **/
    public String getTitle() {
        if (fTitle != null)
            return fTitle.getText();
        return "";
    }

    /**
     * Sets the title to be printed before the table contents.
     * Title will be drawn in same font as the table.
     *
     * @param	title	title to print before the table
     **/
    public void setTitle(String title) {
        fTitle = new JLabel(title);
    }

    /**
     * Sets the title to be printed before the table contents.
     * Title will be drawn using the specified font.
     *
     * @param	title		title to print before the table
     * @param	titleFont	font used to print the title
     **/
    public void setTitle(String title, Font titleFont) {
        fTitle = new JLabel(title);
        fTitle.setFont(titleFont);
    }

    /**
     * Prints the contents of the table, prompting the user with page setup dialogs and such.
     * Prints title string above the table.
     *
     * @param		title				title to print above the table
     * @exception	PrinterException	thrown if any print-related errors occur
     **/
    public void print(String title) throws PrinterException {
        setTitle(title);

        print();
    }

    /**
     * Prints the contents of the table, prompting the user with page setup dialogs and such.
     *
     * @exception	PrinterException	thrown if any print-related errors occur
     ** XXX allan: this version didn't print multiple pages: see below.

     public void print() throws PrinterException {
     // Get a PrinterJob
     final PrinterJob job = PrinterJob.getPrinterJob();
     job.setJobName(fTitle != null ? fTitle.getText() : "Table Contents");

     // Get the page format from the user
     PageFormat format = job.pageDialog(job.defaultPage());

     // Create a Book to contain all the page info
     // Pass the canvas to the print job, since canvas is a Printable
     Book document = new Book();
     document.append(this, format);
     job.setPageable(document);

     // Put up the dialog box
     if (job.printDialog()) {
     // And finally print the table contents
     job.print();
     }
     }
     */

    /**
     * Prints the contents of the table, prompting the user with page setup dialogs and such.
     *
     * @exception	PrinterException	thrown if any print-related errors occur
     **/
    public boolean print() throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName(fTitle != null ? fTitle.getText() : "Table Contents");

        PageFormat format = job.pageDialog(job.defaultPage());
        job.setPrintable(this, format);

        try {
            if (job.printDialog())
                job.print();
        }
        catch (Exception e) {
            DialogUtil.error(e);
            return false;
        }
        return true;
    }

    /**
     * Implements the Printable interface for the JTable, printing the contents
     * of the table.
     *
     * @exception	PrinterException	thrown if any print-related errors occur
     */
    public int print(Graphics pg, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex == 0) {
            fMaxNumPage = 1;
        }

        if (pageIndex >= fMaxNumPage) {
            return NO_SUCH_PAGE;
        }

        // make sure we can see the text on a white background
        //pg.setColor(Color.black);

        pg.translate((int) pageFormat.getImageableX(), (int) pageFormat.getImageableY());

        int wPage = (int) pageFormat.getImageableWidth();
        int hPage = (int) pageFormat.getImageableHeight();

        // If the table is wider than the page width, determine an
        // appropriate scale factor to make it fit.
        //double scaleFactor = (wPage >= getWidth()) ? 1.0 : ((double)wPage / (double)getWidth());

        // XXX Allan: use a smaller font (and save trees ;-)
        double scaleFactor = (wPage >= getWidth()) ? 0.5 : ((double) wPage / (double) getWidth());

        int y = 0;
        RepaintManager rm = RepaintManager.currentManager(getRootPane());
        rm.setDoubleBufferingEnabled(false);
        FontMetrics fm = null;

        // Draw the title, and scale it if necessary
        if (fTitle != null) {
            // Get current font and scale it by the current scale factor
            // If the title fits, then OK, otherwize scale it further to fit
            double titleScale = scaleFactor;
            Font titleFont = fTitle.getFont();
            float fontSize = (float) (titleFont.getSize2D() * titleScale);

            titleFont = titleFont.deriveFont(fontSize);
            pg.setFont(titleFont);
            fm = pg.getFontMetrics();

            // How big is our title in the current font?
            int titleSize = fm.stringWidth(fTitle.getText());
            if (titleSize > wPage) {   // Need to further scale to fit the title all in
                // We use an int here because we want a whole number for
                // the font size. These seem to work better with printing.
                // We don't want to round up because that
                // may make the font slightly too big.
                int scaledFontSize = (int) (fontSize * (float) wPage / (float) titleSize);
                titleFont = fTitle.getFont().deriveFont((float) scaledFontSize);
                pg.setFont(titleFont);
                fm = pg.getFontMetrics();
            }

            Font fn = pg.getFont();
            y += fm.getAscent();
            pg.drawString(fTitle.getText(), 0, y);
            y += 20; // space between title and table headers
        }

        // Get a Bold font for the column headers and scale if necessary
        Font headerFont = getFont().deriveFont(Font.BOLD);

        // Bolding add some size, so lets scale that down
        double boldScaleFactor = scaleFactor * ((double) (getFont().getSize2D() / headerFont.getSize2D()));

        if (boldScaleFactor != 1.0) {
            int headerFontSize = (int) (headerFont.getSize2D() * boldScaleFactor);
            headerFont = headerFont.deriveFont((float) headerFontSize);
        }

        pg.setFont(headerFont);
        fm = pg.getFontMetrics();
        TableColumnModel colModel = getColumnModel();
        int nColumns = colModel.getColumnCount();
        int[] widths = new int[nColumns];
        int[] x = new int[nColumns];
        x[0] = 0;
        int h = fm.getAscent();
        y += h;	// add ascent of header font because of baseline

        int nRow, nCol;
        // Draw the headers
        for (nCol = 0; nCol < nColumns; nCol++) {
            TableColumn tk = colModel.getColumn(nCol);
            widths[nCol] = tk.getWidth();

            if (nCol + 1 < nColumns) {
                x[nCol + 1] = x[nCol] + (int) (widths[nCol] * scaleFactor);
            }

            if (widths[nCol] > 0) {
                // columns with a width of 0 are "hidden"
                String title = (String) tk.getIdentifier();
                pg.drawString(title, x[nCol], y);
            }
        }

        // Column headers are done, now print the columns. Scale the font if necessary
        Font columnFont = getFont();

        if (scaleFactor != 1.0) {
            float columnFontSize = (float) (getFont().getSize2D() * scaleFactor);
            columnFont = columnFont.deriveFont(columnFontSize);
        }

        pg.setFont(columnFont);
        fm = pg.getFontMetrics();
        int header = y;
        h = fm.getHeight();
        int rowH = Math.max((int) (h * 1.5), 10);
        int rowPerPage = (hPage - header) / rowH;
        fMaxNumPage = Math.max((int) Math.ceil(getRowCount() / (double) rowPerPage), 1);
        TableModel tblModel = getModel();
        int iniRow = pageIndex * rowPerPage;
        int endRow = Math.min(getRowCount(), iniRow + rowPerPage);
        y += 5; // allan: add a few pixels space

        // Draw the cell contents
        for (nRow = iniRow; nRow < endRow; nRow++) {
            y += h + 3; // allan: add a few pixels space
            for (nCol = 0; nCol < nColumns; nCol++) {
                if (widths[nCol] > 0) {
                    int col = getColumnModel().getColumn(nCol).getModelIndex();
                    Object obj = getModel().getValueAt(nRow, col);
                    TableCellRenderer cellRend = getCellRenderer(nRow, col);
                    Component c = cellRend.getTableCellRendererComponent(this, obj, false, false, nRow, col);
                    String str;
                    if (c instanceof JLabel) {
                        str = ((JLabel) c).getText();
                    }
                    else {
                        str = obj.toString();
                    }

                    pg.drawString(str, x[nCol], y);
                }
            }
        }

        rm.setDoubleBufferingEnabled(true);

        return PAGE_EXISTS;
    }
}

