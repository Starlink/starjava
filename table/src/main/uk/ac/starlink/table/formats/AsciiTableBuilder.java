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
 * A table builder which reads tables in simple ASCII format.
 * The detailed format of input file which is understood is documented
 * fully in the {@link AsciiStarTable} class.
 *
 * @author   Mark Taylor (Starlink)
 */
public class AsciiTableBuilder extends DocumentedTableBuilder {

    private int maxSample_;

    public AsciiTableBuilder() {
        super( new String[] { "txt" } );
    }

    public String getFormatName() {
        return "ASCII";
    }

    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy policy )
            throws TableFormatException, IOException {
        return new AsciiStarTable( datsrc, maxSample_ );
    }

    public void streamStarTable( InputStream in, TableSink sink, String pos )
            throws TableFormatException {
        throw new TableFormatException( "Can't stream ASCII format tables" ); 
    }

    public boolean canStream() {
        return false;
    }

    public boolean docIncludesExample() {
        return false;
    }

    public String getXmlDescription() {
        return readText( "AsciiTableBuilder.xml" );
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
            + "When reading ASCII files, since no type information is present\n"
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
        example = "5000"
    )
    public void setMaxSample( int maxSample ) {
        maxSample_ = maxSample;
    }

    /**
     * Returns the maximum number of rows that will be sampled to determine
     * column data types.
     *
     * @return  maximum number of rows sampled;
     *          if &lt;=0, all rows are sampled
     */
    public int getMaxSample() {
        return maxSample_;
    }
}
