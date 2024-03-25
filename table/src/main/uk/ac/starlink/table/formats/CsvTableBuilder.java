package uk.ac.starlink.table.formats;

import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.io.InputStream;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.ConfigMethod;
import uk.ac.starlink.util.DataSource;

/**
 * A table builder which reads tables in Comma-Separated Values format.
 * The detailed format of input file which is understood is documented
 * fully in the {@link CsvStarTable} class.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Sep 2004
 */
public class CsvTableBuilder extends DocumentedTableBuilder {

    private Boolean hasHeader_;
    private int maxSample_;

    public CsvTableBuilder() {
        super( new String[] { "csv" } );
    }

    public String getFormatName() {
        return "CSV";
    }

    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy policy )
            throws TableFormatException, IOException {
        return new CsvStarTable( datsrc, hasHeader_, maxSample_ );
    }

    public void streamStarTable( InputStream in, TableSink sink, String pos )
            throws TableFormatException {
        throw new TableFormatException( "Can't stream ASCII format tables" ); 
    }

    public boolean canStream() {
        return false;
    }

    public boolean docIncludesExample() {
        return true;
    }

    public String getXmlDescription() {
        return readText( "CsvTableBuilder.xml" );
    }

    /**
     * Sets whether input CSV files are known to include the optional
     * header line or not.
     *
     * @param  hasHeader  true if input files are known to contain column
     *                    names as the first line; false if they are
     *                    known not to; null to auto-detect
     */
    @ConfigMethod(
        property = "header",
        doc = "<p>Indicates whether the input CSV file contains the\n"
            + "optional one-line header giving column names.\n"
            + "Options are:\n"
            + "<ul>\n"
            + "<li><code>true</code>: the first line is a header line "
            +     "containing column names</li>\n"
            + "<li><code>false</code>: all lines are data lines, "
            +     "and column names will be assigned automatically</li>\n"
            + "<li><code>null</code>: a guess will be made about whether "
            +     "the first line is a header or not depending on "
            +     "what it looks like</li>\n"
            + "</ul>\n"
            + "The default value is <code>null</code> (auto-determination).\n"
            + "This usually works OK, but can get into trouble if\n"
            + "all the columns look like string values.\n"
 
            + "</p>",
        usage = "true|false|null",
        example = "true",
        sequence = 1
    )
    public void setHasHeader( Boolean hasHeader ) {
        hasHeader_ = hasHeader;
    }

    /**
     * Returns header interpretation policy.
     *
     * @return  true if input files are known to contain column names as
     *          the first line; false if they are known not to;
     *          null to auto-detect
     */
    public Boolean getHasHeader() {
        return hasHeader_;
    }

    /**
     * Sets the maximum number of rows that will be sampled to determine
     * column data types.
     *
     * @param   maxSample  maximum number of rows sampled;
     *                     if &lt;=0, all rows are sampled
     */
    @ConfigMethod(
        property = "maxSample",
        doc = "<p>Controls how many rows of the input file are sampled\n"
            + "to determine column datatypes.\n"
            + "When reading CSV files, since no type information is present\n"
            + "in the input file, the handler has to look at the column data\n"
            + "to see what type of value appears to be present\n"
            + "in each column, before even starting to read the data in.\n"
            + "By default it goes through the whole table when doing this,\n"
            + "which can be time-consuming for large tables.\n"
            + "If this value is set, it limits the number of rows\n"
            + "that are sampled in this data characterisation pass,\n"
            + "which can reduce read time substantially.\n"
            + "However, if values near the end of the table differ\n"
            + "in apparent type from those near the start,\n"
            + "it can also result in getting the datatypes wrong.\n"
            + "</p>",
        usage = "<int>",
        example = "100000",
        sequence = 2
    )
    public void setMaxSample( int maxSample ) {
        maxSample_ = maxSample;
    }

    /**
     * Returns the maximum number of rows that will be sampled to determine
     * column data types.
     *
     * @return   maximum number of rows sampled;
     *           if &lt;=0, all rows are sampled
     */
    public int getMaxSample() {
        return maxSample_;
    }
}
