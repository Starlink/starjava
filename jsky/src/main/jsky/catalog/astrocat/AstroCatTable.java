// Copyright 2002
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: AstroCatTable.java,v 1.2 2002/08/05 10:57:20 brighton Exp $

package jsky.catalog.astrocat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Vector;

import jsky.catalog.BasicQueryArgs;
import jsky.catalog.Catalog;
import jsky.catalog.FieldDesc;
import jsky.catalog.MemoryCatalog;
import jsky.catalog.QueryArgs;
import jsky.catalog.QueryResult;
import jsky.coords.CoordinateRadius;
import jsky.coords.WorldCoords;


/**
 * Used to read and write Skycat style tab separated
 * catalog data and manage the rows and columns in memory.
 * This class extends the  MemoryCatalog class, which supports
 * searching and working with
 * a JTable widget.
 *
 * @version $Revision: 1.2 $ $Date: 2002/08/05 10:57:20 $
 * @author Allan Brighton
 */
public class AstroCatTable extends MemoryCatalog {

    /**
     * Initialize the table from the given stream by reading up to
     * maxRows of the data.
     *
     * @param catalog the catalog where the data originated, if known
     * @param in the stream to read the catalog data from
     * @param maxRows the maximum number of data rows to read
     */
    public AstroCatTable(AstroCatalog catalog, InputStream in, int maxRows) throws IOException {
        super(catalog, in, maxRows);
    }

    /**
     * Initialize the table from the given stream by reading up to
     * maxRows of the data.
     *
     * @param catalog the catalog where the data originated, if known
     * @param in the stream to read the catalog data from
     * @param queryArgs represents the arguments to the query that resulted in this table
     */
    public AstroCatTable(AstroCatalog catalog, InputStream in, QueryArgs queryArgs) throws IOException {
        super(catalog, in, queryArgs);
    }


    /**
     * Initialize the table from the given stream.
     *
     * @param catalog the catalog where the data originated, if known
     * @param in the stream to read the catalog data from
     */
    public AstroCatTable(AstroCatalog catalog, InputStream in) throws IOException {
        super(catalog, in);
    }


    /**
     * Initialize the table from the given file.
     *
     * @param catalog the catalog where the data originated, if known
     * @param filename the name of the catalog file
     */
    public AstroCatTable(AstroCatalog catalog, String filename) throws IOException {
        super(catalog, filename);
    }


    /**
     * Initialize the table from the given file
     *
     * @param filename the name of the catalog file
     */
    public AstroCatTable(String filename) throws IOException {
        super((Catalog)null, filename);
    }


    /**
     * Construct a new AstroCatTable with the given column fields and data rows
     * (For internal use only).
     *
     * @param table the source catalog table
     * @param fields an array of objects describing the table columns
     * @param dataVector a vector of data rows, each of which is a vector of column values.
     */
    private AstroCatTable(AstroCatTable table, FieldDesc[] fields, Vector dataRows) {
        super(table, fields, dataRows);
    }

    /**
     * Construct a new AstroCatTable with no header or data
     * (For use only by derived classes).
     */
    protected AstroCatTable() {
    }

    /**
     * Return the catalog used to create this table,
     * or a dummy, generated catalog object, if not known.
     */
    public Catalog getCatalog() {
	Catalog catalog = super.getCatalog();
        if (catalog != null) 
	    return catalog;

	String filename = getFilename();
	if (filename == null)
	    filename = "unknown";
	File file = new File(filename);
	String name = file.getName();
	AstroCatalog cat = new AstroCatalog();
	cat.setType("local");
	cat.setName(name);
	cat.setId(name);
	cat.setProtocol("file");
	cat.setHost("localhost");
	cat.setURLPath(file.getPath());
	return cat;
    }

    /**
     * Return a new MemoryCatalog with the given column fields and data rows.
     *
     * @param fields an array of objects describing the table columns
     * @param dataVector a vector of data rows, each of which is a vector of column values.
     */
    protected MemoryCatalog makeQueryResult(FieldDesc[] fields, Vector dataRows) {
        AstroCatTable table = new AstroCatTable(this, fields, dataRows);
        table.setProperties(getProperties());
        return table;
    }

    /**
     * Test cases
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("usage: java -classpath ... AstroCatTable filename");
            System.exit(1);
        }
        AstroCatTable cat = null;
        try {
            cat = new AstroCatTable(args[0]);
            cat.saveAs(System.out);
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("");
        System.out.println("test row,col access:");
        System.out.println("table(0,0) = " + cat.getValueAt(0, 0));
        System.out.println("table(3,4) = " + cat.getValueAt(3, 4));
        System.out.println("table(3, ra) = " + cat.getValueAt(3, "ra"));
        System.out.println("table(3, RA) = " + cat.getValueAt(3, "RA"));
        System.out.println("table(3, dec) = " + cat.getValueAt(3, "dec"));
        System.out.println("table(3, Dec) = " + cat.getValueAt(3, "Dec"));

        try {
            System.out.println("");
            System.out.println("test query: of GSC0285601186");
            QueryArgs q = new BasicQueryArgs(cat);
            q.setId("GSC0285601186");
            QueryResult r = cat.query(q);
            if (r instanceof AstroCatTable) {
                AstroCatTable table = (AstroCatTable) r;
                System.out.println("Number of result rows: " + table.getRowCount());
                if (table.getRowCount() != 0)
                    System.out.println("result: " + ((AstroCatTable) r).toString());
            }
            else {
                System.out.println("Failed search by ID");
            }

            System.out.println("");
            System.out.println("test query: at center position/radius: ");
            q = new BasicQueryArgs(cat);
            q.setRegion(new CoordinateRadius(new WorldCoords("03:19:44.44", "+41:30:58.21"), 1.));
            r = cat.query(q);

            if (r instanceof AstroCatTable) {
                AstroCatTable table = (AstroCatTable) r;
                System.out.println("Number of result rows: " + table.getRowCount());
                if (table.getRowCount() != 0)
                    System.out.println("result: " + ((AstroCatTable) r).toString());
            }
            else {
                System.out.println("Failed search by position");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}


