/*
 * ESO Archive
 *
 * $Id: FITSHDUChooser.java,v 1.12 2002/08/04 19:50:39 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/12/10  Created
 */

package jsky.image.fits.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.media.jai.PlanarImage;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import jsky.image.fits.codec.FITSImage;
import jsky.image.gui.MainImageDisplay;
import jsky.util.gui.BusyWin;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.SwingUtil;

import nom.tam.fits.AsciiTableHDU;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.ImageHDU;


/**
 * Displays a window listing the HDU extensions in the current
 * FITS image and allows the user to select the one to display.
 *
 * @version $Revision: 1.12 $
 * @author Allan Brighton
 */
public class FITSHDUChooser extends JPanel {

    // The top level parent frame (or internal frame) used to close the window
    private Component _parent;

    // main image display window 
    private MainImageDisplay _imageDisplay;

    // Table displaying the HDU info 
    private JTable _table;

    // Handle to FITSImage object 
    private FITSImage _fitsImage;

    // Table column headings 
    private static final String[] COLUMN_NAMES = {
        "HDU", "Type", "EXTNAME", "NAXIS", "NAXIS1", "NAXIS2", "NAXIS3", "CRPIX1", "CRPIX2"
    };

    // Table column classes 
    private static final Class[] COLUMN_CLASSES = {
        Integer.class, String.class, String.class, Integer.class, Integer.class, Integer.class,
        Integer.class, Double.class, Double.class
    };


    // Table column widths 
    private static final int[] COLUMN_WIDTHS = {
        50, 70, 200, 60, 60, 60, 60, 70, 70
    };

    // Number of rows in the table (number FITS HDUS in the current image) 
    private int _numRows;

    // Number of columns in the table 
    private static final int NUM_COLS = COLUMN_NAMES.length;

    // The data to display in the table 
    private Object[][] _tableData;

    // Button to delete a FITS extension 
    private JButton _deleteButton;


    /**
     * Pop up a window displaying FITS HDU information and let the user choose the
     * HDU/extension to display.
     *
     * @param parent The top level parent frame (or internal frame) used to close the window
     * @param imageDisplay The image display window
     * @param fitsImage The FITSImage object returned by the "#fits_image" property for FITS images
     */
    public FITSHDUChooser(Component parent, MainImageDisplay imageDisplay, FITSImage fitsImage) {
        this._parent = parent;
        this._imageDisplay = imageDisplay;

        setLayout(new BorderLayout());
        add(_makeTablePane(), BorderLayout.CENTER);
        add(_makeButtonPanel(), BorderLayout.SOUTH);

        updateDisplay(fitsImage);
    }


    /** Make and return the table panel displaying the HDU information */
    private JScrollPane _makeTablePane() {
        _table = new JTable();
        //_table.getTableHeader().setUpdateTableInRealTime(false);
        _table.setCellSelectionEnabled(false);
        //_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        _table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        _table.setRowSelectionAllowed(true);
        _table.setColumnSelectionAllowed(false);

        // track double-clicks in table to select HDU for viewing
        _table.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent evt) {
                Point pt = evt.getPoint();
                int row = _table.rowAtPoint(pt);
                _deleteButton.setEnabled(row > 0);
                if (evt.getClickCount() == 2) {
                    _selectTableRow(row);
                }
            }
        });

        // set preferred table size
        _table.setPreferredScrollableViewportSize(new Dimension(450, 90));

        return new JScrollPane(_table);
    }


    /**
     * Update the display with the latest information (after a new image was loaded).
     */
    public void updateDisplay(FITSImage fitsImage) {
        this._fitsImage = fitsImage;
        _numRows = _fitsImage.getNumHDUs();
        _tableData = new Object[_numRows][NUM_COLS];
        for (int row = 0; row < _numRows; row++) {
            BasicHDU hdu = _fitsImage.getHDU(row);
            Header header = hdu.getHeader();
            int col = 0;
            // keep in sync with columnHeadings
            _tableData[row][col++] = new Integer(row);
            _tableData[row][col++] = _getHDUType(hdu);
            _tableData[row][col++] = header.getStringValue("EXTNAME");
            _tableData[row][col++] = new Integer(header.getIntValue("NAXIS"));
            _tableData[row][col++] = new Integer(header.getIntValue("NAXIS1"));
            _tableData[row][col++] = new Integer(header.getIntValue("NAXIS2"));
            _tableData[row][col++] = new Integer(header.getIntValue("NAXIS3"));
            _tableData[row][col++] = new Double(header.getDoubleValue("CRPIX1"));
            _tableData[row][col++] = new Double(header.getDoubleValue("CRPIX2"));
        }

        _table.setModel(new AbstractTableModel() {

            public int getColumnCount() {
                return NUM_COLS;
            }

            public String getColumnName(int i) {
                return COLUMN_NAMES[i];
            }

            public Class getColumnClass(int i) {
                return COLUMN_CLASSES[i];
            }

            public int getRowCount() {
                return _numRows;
            }

            public Object getValueAt(int row, int col) {
                return _tableData[row][col];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        });

        // set column widths
        for (int i = 0; i < NUM_COLS; i++) {
            _table.getColumn(COLUMN_NAMES[i]).setPreferredWidth(COLUMN_WIDTHS[i]);
        }
    }


    /**
     * Return a String describing the given HDU
     */
    private String _getHDUType(BasicHDU hdu) {
        if (hdu instanceof ImageHDU)
            return "image";
        if (hdu instanceof BinaryTableHDU)
            return "binary";
        if (hdu instanceof AsciiTableHDU)
            return "ascii";
        return "unknown";
    }


    /**
     * Called when the given row of the table is selected.
     */
    private void _selectTableRow(int rowIndex) {
        if (rowIndex != -1) {
            BusyWin.setBusy(true);
            try {
                TableModel model = _table.getModel();
                int hdu = ((Integer) model.getValueAt(rowIndex, 0)).intValue();
                String type = (String) model.getValueAt(rowIndex, 1);
                if (type.equals("image")) {
                    selectImage(hdu);
                }
                else if (type.equals("binary") || type.equals("ascii")) {
                    _imageDisplay.displayFITSTable(hdu);
                }
            }
            finally {
                BusyWin.setBusy(false);
            }
        }
    }


    /**
     * Select the given image HDU and display it.
     */
    public void selectImage(int hdu) {
        try {
            _fitsImage.setHDU(hdu);
        }
        catch (Exception e) {
            throw new RuntimeException("Can't select FIT HDU# " + hdu + ": " + e.getMessage());
        }
        _imageDisplay.setImage(PlanarImage.wrapRenderedImage(_fitsImage));
    }


    /**
     * Return the JTable used to display the HDU information for the FITS file.
     */
    public JTable getTable() {
        return _table;
    }


    /**
     * Make the dialog button panel
     */
    private JPanel _makeButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        JButton openButton = new JButton("Open");
        openButton.setToolTipText("Open and display the selected FITS HDU (header/data unit)");
        panel.add(openButton);
        openButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                _open();
            }
        });

        _deleteButton = new JButton("Delete");
        _deleteButton.setToolTipText("Delete the selected FITS HDU");
        panel.add(_deleteButton);
        _deleteButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                _delete();
            }
        });

        JButton closeButton = new JButton("Close");
        closeButton.setToolTipText("Hide this window");
        panel.add(closeButton);
        closeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                _close();
            }
        });

        return panel;
    }


    /**
     * Open and display the selected extension.
     */
    private void _open() {
        _selectTableRow(_table.getSelectedRow());
    }


    /**
     * Delete the selected extension.
     */
    private void _delete() {
        int hdu = _table.getSelectedRow();
        if (hdu > 0 && hdu < _fitsImage.getNumHDUs()) {
            try {
                _fitsImage.getFits().deleteHDU(hdu);
                _imageDisplay.setSaveNeeded(true);
            }
            catch (FitsException e) {
                DialogUtil.error(e);
                return;
            }
            updateDisplay(_fitsImage);
        }
    }


    /**
     * Close the window
     */
    private void _close() {
        if (_parent != null)
            _parent.setVisible(false);
    }


    /** Clear the table and remove any referenes to the image data */
    public void clear() {
        _table.setModel(new DefaultTableModel());
        _fitsImage = null;
    }


    /** Show or hide the top level window */
    public void setShow(boolean show) {
        _parent.setVisible(show);
        if (show) {
	    int hdu = _fitsImage.getCurrentHDUIndex();
	    _table.getSelectionModel().setSelectionInterval(hdu, hdu);
            SwingUtil.showFrame(_parent);
	}
    }
}


