/*
 * ESO Archive
 *
 * $Id: FITSKeywords.java,v 1.7 2002/07/09 13:30:37 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.image.fits.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import jsky.image.ImageChangeEvent;
import jsky.image.fits.codec.FITSImage;
import jsky.image.gui.MainImageDisplay;
import jsky.util.gui.SortedJTable;
import jsky.util.gui.TableUtil;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;


/**
 * Dialog to view the FITS keywords .
 *
 * @version $Revision: 1.7 $
 * @author Allan Brighton
 */
public class FITSKeywords extends JPanel {

    /** The top level parent frame (or internal frame) used to close the window */
    protected Component parent;

    /** The image being displayed */
    protected MainImageDisplay imageDisplay;

    /** The table used to display the keywords */
    protected SortedJTable table;

    /** Sum of column widths, used during resize */
    protected int sumColWidths = 0;

    /**
     * Make a window to display the FITS keyword values for the image being displayed
     * in the given image display window.
     *
     * @param parent The top level parent frame (or internal frame) used to close the window
     * @param imageDisplay The image display window
     */
    public FITSKeywords(Component parent, MainImageDisplay imageDisplay) {
        this.parent = parent;
        this.imageDisplay = imageDisplay;
        setBackground(Color.white);

        setLayout(new BorderLayout());

        add(makeTable(), BorderLayout.CENTER);
        add(makeButtonPanel(), BorderLayout.SOUTH);

        // register to receive notification when the image changes
        imageDisplay.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent ce) {
                ImageChangeEvent e = (ImageChangeEvent) ce;
                if (e.isNewImage() && !e.isBefore())
                    updateDisplay();
            }
        });

        // setPreferredSize(new Dimension(675, 450));

        // initialize the display
        updateDisplay();
    }


    /**
     * Make and return the handle for the table window
     */
    JScrollPane makeTable() {
        table = new SortedJTable();

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setIntercellSpacing(new Dimension(6, 3));
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        JTableHeader header = table.getTableHeader();
        header.setUpdateTableInRealTime(false);
        header.setFont(header.getFont().deriveFont(Font.BOLD));

        // handle resize events
        addComponentListener(new ComponentAdapter() {

            public void componentResized(ComponentEvent e) {
                resize();
            }
        });

        return new JScrollPane(table);
    }


    /** Called when the table is resized */
    public void resize() {
        if (sumColWidths < getWidth()) {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        }
        else {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        }
    }

    /**
     * Make the dialog button panel
     */
    JPanel makeButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        JButton closeButton = new JButton("Close");
        panel.add(closeButton);
        closeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                close();
            }
        });

        return panel;
    }

    /**
     * Update the display to show the values in the current FITS HDU.
     */
    public void updateDisplay() {
        FITSImage fitsImage = imageDisplay.getFitsImage();

        if (fitsImage == null) {
            table.setModel(new DefaultTableModel());
            return;
        }
        updateDisplay(fitsImage.getCurrentHDUIndex());
    }


    /**
     * Update the display to show the values in the current FITS HDU.
     */
    public void updateDisplay(int hduIndex) {
        FITSImage fitsImage = imageDisplay.getFitsImage();

        BasicHDU hdu = null;
        if (fitsImage == null || (hdu = fitsImage.getHDU(hduIndex)) == null) {
            table.setModel(new DefaultTableModel());
            return;
        }

        String[] columnNames = {"Keyword", "Value", "Comment"};
        Header header = hdu.getHeader();
        int numKeywords = header.getNumberOfCards();
        String[][] values = new String[numKeywords][3];
        Iterator it = header.iterator();
        int n = 0;
        while (it.hasNext()) {
            HeaderCard card = (HeaderCard) (it.next());
            String name = card.getKey();
            String value = card.getValue();
            String comment = card.getComment();
            values[n][0] = name;
            values[n][1] = value;
            values[n++][2] = comment;
        }

        table.setModel(new DefaultTableModel(values, columnNames));
        //table.sizeColumnsToFit(-1);  // XXX workaround for Java bugID# 4226181
        sumColWidths = TableUtil.initColumnSizes(table, null);
        resize();
        updateTitle(header.getStringValue("EXTNAME"));
    }

    /** Set the frame's title. */
    protected void updateTitle(String title) {
        String s = "FITS Keywords";
        if (title != null)
            s += ": " + title;

        if (parent != null) {
            if (parent instanceof JFrame)
                ((JFrame) parent).setTitle(s);
            else
                ((JInternalFrame) parent).setTitle(s);
        }
    }

    /**
     * Close the window
     */
    void close() {
        if (parent != null)
            parent.setVisible(false);
    }
}

