/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: NavigatorFITSTable.java,v 1.14 2002/08/05 10:57:21 brighton Exp $
 */

package jsky.navigator;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Properties;
import java.util.Vector;

import jsky.catalog.FieldDesc;
import jsky.catalog.FieldDescAdapter;
import jsky.catalog.TableQueryResult;
import jsky.catalog.skycat.SkycatConfigEntry;
import jsky.catalog.skycat.SkycatTable;
import jsky.util.gui.DialogUtil;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTable;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.FitsFactory;
import nom.tam.fits.Header;
import nom.tam.fits.TableHDU;


/**
 * Used to read and write FITS binary tables and store catalog query results as
 * FITS tables.
 * This class is the bridge between the catalog classes and the FITS I/O library.
 *
 * @version $Revision: 1.14 $
 * @author Allan Brighton
 */
public class NavigatorFITSTable extends SkycatTable {

    /** Name of a FITS binary table containing skycat style catalog configuration information. */
    protected static final String CATINFO = "CATINFO";

    /** Table column headings for the CATINFO table (corresponds to the fields in a skycat catalog config file) */
    protected static final String[] CATINFO_COLUMNS = {
        "SHORT_NAME", "ID_COL", "RA_COL", "DEC_COL", "X_COL", "Y_COL", "EQUINOX",
        "SYMBOL", "SEARCH_COLS", "SORT_COLS", "SORT_ORDER", "SHOW_COLS", "HELP", "COPYRIGHT"
    };

    /** Number of columns in the CATINFO table */
    protected static final int NUM_CATINFO_COLUMNS = CATINFO_COLUMNS.length;


    /**
     * Initialize from the given FITS HDU
     *
     * @param filename The name of the FITS file
     * @param fits object to use for FITS I/O
     * @param hdu the HDU containing the FITS table
     */
    public NavigatorFITSTable(String filename, Fits fits, TableHDU hdu) throws IOException, FitsException {
        setFilename(filename);
        int ncols = hdu.getNCols();
        int nrows = hdu.getNRows();
        dataVector = new Vector(nrows);
        columnIdentifiers = new Vector(ncols);
        Vector columnClasses = new Vector(ncols, 1);
        FieldDesc[] fields = new FieldDescAdapter[ncols];
        Header header = hdu.getHeader();
        String name = header.getStringValue("EXTNAME");
        if (name == null)
            name = "FITS Table";
	setName(name);
        setTitle(name);
	setId(name);

        // table header
        for (int i = 0; i < ncols; i++) {
            String colName = hdu.getColumnName(i);
            columnIdentifiers.add(colName);
            fields[i] = new FieldDescAdapter(colName);
            columnClasses.add(null);
        }
	setColumnClasses(columnClasses);
	setFields(fields);

        // table data
        if (nrows > 0) {
            for (int i = 0; i < nrows; i++) {
                Object[] row = hdu.getRow(i);
                Vector v = new Vector(ncols);
                for (int j = 0; j < ncols; j++) {
                    Object o = row[j];
                    if (o instanceof String)
                        o = _parseItem((String) o);
                    // can't display array values...
                    if (o.getClass().isArray())
                        o = Array.get(o, 0);
                    _checkColumnClass(j, o);
                    v.add(o);
                }
                dataVector.add(v);
            }
        }
	

        // look for a skycat catalog configuration entry for this table in another
        // FITS table with the name "CATINFO".
        try {
            SkycatConfigEntry entry = findConfigEntry(fits);
            if (entry != null)
                setConfigEntry(entry);
        }
        catch (Exception e) {
            DialogUtil.error(e);
        }
    }


    /**
     * Initialize from the given FITS HDU
     *
     * @param filename The name of the FITS file
     * @param fits object to use for FITS I/O
     * @param hduIndex the index of the HDU containing the FITS table
     */
    public NavigatorFITSTable(String filename, Fits fits, int hduIndex) throws IOException, FitsException {
        this(filename, fits, NavigatorFITSTable.getTableHDU(fits, hduIndex));
    }

    /**
     * Return the object for the given HDU index.
     * @param fits object to use for FITS I/O
     * @param hduIndex the index of the HDU containing the FITS table
     */
    protected static TableHDU getTableHDU(Fits fits, int hduIndex) throws IOException, FitsException {
        BasicHDU basicHDU = fits.getHDU(hduIndex);
        if (!(basicHDU instanceof TableHDU))
            throw new RuntimeException("HDU type not supported: " + basicHDU.getClass());

        return (TableHDU) basicHDU;
    }


    /**
     * Look for a skycat catalog configuration entry for this table in another
     * FITS table in this file with the name "CATINFO".
     *
     * @param fits object to use for FITS I/O
     * @return a skycat config entry for this table, or null if not found
     */
    protected SkycatConfigEntry findConfigEntry(Fits fits) throws FitsException, IOException {
	String name = getName();
        if (name == null)
            return null;

        // find the "CATINFO" table, if it exists
        BinaryTableHDU hdu = findBinaryTableHDU(fits, CATINFO);
        if (hdu == null)
            return null;

        int ncols = hdu.getNCols();

        // find the entry for this table
        int rowIndex = findConfigEntryRow(hdu, name);
        if (rowIndex == -1)
            return null;

        // if we get here, we found the entry
        Object[] row = hdu.getRow(rowIndex);
	Properties properties = new Properties();
        properties.setProperty("serv_type", "local");
        properties.setProperty("long_name", name);
        properties.setProperty("url", getFilename());
        for (int colIndex = 0; colIndex < ncols; colIndex++) {
            String value = (String) row[colIndex];
            if (value.length() != 0)
                properties.setProperty(hdu.getColumnName(colIndex).toLowerCase(), value);
        }
        return new SkycatConfigEntry(properties);
    }

    /**
     * Return index of the row in the given binary table who's first element contains
     * the given string, or -1 if not found.
     */
    protected static int findConfigEntryRow(BinaryTableHDU hdu, String name) throws FitsException, IOException {
        int nrows = hdu.getNRows();
        for (int rowIndex = 0; rowIndex < nrows; rowIndex++) {
            String extName = (String) hdu.getElement(rowIndex, 0);
            if (extName.equals(name))
                return rowIndex;
        }
        return -1;
    }


    /**
     * Look for a binary table with the given name and return it if found,
     * otherwise null.
     *
     * @param fits object to use for FITS I/O
     * @return a FITS binary table, or null if not found
     */
    protected static BinaryTableHDU findBinaryTableHDU(Fits fits, String name) throws FitsException, IOException {
        if (name == null)
            return null;

        int n = fits.getNumberOfHDUs();
        for (int hduIndex = 0; hduIndex < n; hduIndex++) {
            BasicHDU basicHDU = fits.getHDU(hduIndex);
            if (!(basicHDU instanceof BinaryTableHDU))
                continue;

            BinaryTableHDU hdu = (BinaryTableHDU) basicHDU;
            Header header = hdu.getHeader();
            String extName = header.getStringValue("EXTNAME");
            if (extName != null && extName.equals(name))
                return hdu;
        }

        return null;
    }


    /**
     * Save the given table as a binary FITS table in the given FITS image file and
     * return a NavigatorFITSTable object for the new table.
     * <p>
     * If the table is an instance of {@link SkycatTable}, the catalog configuration information
     * is also saved in a separate table named CATINFO.
     *
     * @param filename The name of the FITS file
     * @param fits object to use for FITS I/O
     * @param table contains the table data
     *
     * @return a NavigatorFITSTable object for the new table
     */
    public static NavigatorFITSTable saveWithImage(String filename, Fits fits, TableQueryResult table) throws FitsException, IOException {
        // Note: we have to use the table's id as the table name, since the long name might be
        // too long for the 80 char FITS line size
        String name = table.getId();
        if (name == null)
            name = "";

        // make it easier to recognize that this is a FITS table by appending this to the name
        String suffix = " (FITS Table)";
        if (!name.endsWith(suffix))
            name = name + suffix;

        int nrows = table.getRowCount();
        int ncols = table.getColumnCount();
        if (nrows == 0 || ncols == 0) {
            //DialogUtil.error("The table is empty.");
            return null;
        }

        // make the new table
        BinaryTable binTable = new BinaryTable();
        FitsFactory.setUseAsciiTables(false);
        for (int col = 0; col < ncols; col++) {
            String[] data = new String[nrows];
            for (int row = 0; row < nrows; row++) {
                Object o = table.getValueAt(row, col);
                String s;
                if (o == null)
                    s = "";
                else
                    s = o.toString();
                data[row] = s;
            }
            binTable.addColumn(data);
        }

        BinaryTableHDU hdu = (BinaryTableHDU) Fits.makeHDU(binTable);
        hdu.getHeader().addValue("EXTNAME", name, "Contains saved query results");

        for (int col = 0; col < ncols; col++) {
            hdu.setColumnName(col, table.getColumnName(col), null);
        }

        // add the new table (delete existing one, if found)
        deleteTable(fits, name);
        fits.addHDU(hdu);
        updateCatInfo(fits, table, name);

        return new NavigatorFITSTable(filename, fits, hdu);
    }

    /**
     * Delete the named FITS table from the given FITS file.
     *
     * @param fits object to use for FITS I/O
     * @param name the name of the table
     */
    protected static void deleteTable(Fits fits, String name) throws FitsException, IOException {
        int numHDUs = fits.getNumberOfHDUs();
        for (int hduIndex = 0; hduIndex < numHDUs; hduIndex++) {
            BasicHDU basicHDU = fits.getHDU(hduIndex);
            if (!(basicHDU instanceof BinaryTableHDU))
                continue;

            BinaryTableHDU hdu = (BinaryTableHDU) basicHDU;
            Header header = hdu.getHeader();
            String extName = header.getStringValue("EXTNAME");
            if (extName != null && extName.equals(name)) {
                // delete the old table
                fits.deleteHDU(hduIndex);
                return;
            }
        }
    }

    /**
     * Add (or update) a FITS table named CATINFO with catalog configuration information
     * for the given catalog.
     * Note: This method only supports tables derived from SkycatTable.
     *
     * @param fits object to use for FITS I/O
     * @param table contains the table data
     * @param name the name to give the FITS table
     */
    protected static void updateCatInfo(Fits fits, TableQueryResult table, String name)
            throws FitsException, IOException {

        if (!(table instanceof SkycatTable))
            return;

        SkycatConfigEntry entry = ((SkycatTable) table).getConfigEntry();
        if (entry == null)
            return;

        // find the CATINFO table, remove it and recreate it with the new data
        // (since the column widths may have changed)
        //
        // XXX Note: the Fits class doesn't currently handle empty columns correctly,
        //           so we have to add one row with dummy values ("-").
        int ncols = NUM_CATINFO_COLUMNS;
        Object[][] data;
        BinaryTableHDU hdu = findBinaryTableHDU(fits, CATINFO);
        int newRowIndex = 0;
        if (hdu != null) {
            // merge data from old table to new table
            int nrows = hdu.getNRows();
            int entryIndex = findConfigEntryRow(hdu, name);
            int dummyIndex = findConfigEntryRow(hdu, "-");
            int n = nrows;
            if (entryIndex == -1)
                n++;
            if (dummyIndex == -1)
                n++;
            data = new Object[n][];
            for (int rowIndex = 0; rowIndex < nrows; rowIndex++) {
                if (rowIndex == entryIndex)
                    continue;
                if (rowIndex == dummyIndex)
                    continue;
                data[newRowIndex++] = hdu.getRow(rowIndex);
            }
            deleteTable(fits, CATINFO);
        }
        else {
            data = new Object[2][];
        }

        // add a new row for the given table
        Properties properties = entry.getProperties();
        Object[] row = new Object[ncols];
        row[0] = name;  // use the given name rather than the original one
        for (int colIndex = 1; colIndex < ncols; colIndex++) {
            String value = properties.getProperty(CATINFO_COLUMNS[colIndex].toLowerCase());
            if (value != null) {
                row[colIndex] = value;
            }
            else {
                row[colIndex] = "";
            }
        }
        data[newRowIndex++] = row;

        // add a dummy row as a workaround to a Fits lib bug with empty columns
        row = new Object[ncols];
        for (int colIndex = 0; colIndex < ncols; colIndex++) {
            row[colIndex] = "-";
        }
        data[newRowIndex++] = row;

        BinaryTable binTable = new BinaryTable(data);
        FitsFactory.setUseAsciiTables(false);
        hdu = (BinaryTableHDU) Fits.makeHDU(binTable);
        hdu.getHeader().addValue("EXTNAME", CATINFO, "Contains catalog config info");
        for (int i = 0; i < ncols; i++) {
            hdu.setColumnName(i, CATINFO_COLUMNS[i], null);
        }
        fits.addHDU(hdu);
    }


    /**
     * Check for any catalog tables saved as FITS binary tables and
     * plot the ones found on the image. Each catalog table should
     * have an entry in the CATINFO FITS table, which describes how to
     * plot it.
     *
     * @param filename The name of the FITS file
     * @param fits object to use for FITS I/O
     * @param navigator window object managing the table display and plotting
     */
    public static void plotTables(String filename, Fits fits, Navigator navigator) throws FitsException, IOException {
        BinaryTableHDU hdu = findBinaryTableHDU(fits, CATINFO);
        if (hdu == null)
            return;
        int nrows = hdu.getNRows();
        for (int rowIndex = 0; rowIndex < nrows; rowIndex++) {
            Object[] row = hdu.getRow(rowIndex);
            String name = (String) row[0];
            plotTable(filename, fits, navigator, name);
        }
    }

    /**
     * Plot the named binary table on the image.
     *
     * @param filename The name of the FITS file
     * @param fits object to use for FITS I/O
     * @param navigator window object managing the table display and plotting
     * @param name the name of the table to plot (FITS keyword EXTNAME)
     */
    protected static void plotTable(String filename, Fits fits, Navigator navigator, String name) throws FitsException, IOException {
        NavigatorFITSTable table = NavigatorFITSTable.findTable(filename, fits, name);
        if (table != null)
            navigator.setQueryResult(table.getCatalog());
    }

    /**
     * Find the named FITS binary table, make a NavigatorFITSTable out of it and return it.
     *
     * @param filename The name of the FITS file
     * @param fits object to use for FITS I/O
     * @param name the name of the table to plot (FITS keyword EXTNAME)
     */
    protected static NavigatorFITSTable findTable(String filename, Fits fits, String name) throws FitsException, IOException {
        BinaryTableHDU hdu = findBinaryTableHDU(fits, name);
        if (hdu == null) {
            return null;
        }
        return new NavigatorFITSTable(filename, fits, hdu);
    }
}
