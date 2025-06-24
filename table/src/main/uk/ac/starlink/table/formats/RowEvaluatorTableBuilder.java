package uk.ac.starlink.table.formats;

import java.awt.datatransfer.DataFlavor;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.ConfigMethod;

/**
 * Partial TableBuilder implementation for TableBuilders based on a
 * RowEvaluator.  This manages a couple of the configuration options.
 *
 * @author   Mark Taylor
 * @since    24 Jun 2025
 */
public abstract class RowEvaluatorTableBuilder extends DocumentedTableBuilder {

    private int maxSample_;
    private RowEvaluator.Decoder<?>[] decoders_;

    /**
     * Constructor.
     *
     * @param  extensions  list of lower-cased filename extensions,
     *                     excluding the '.' character
     */
    protected RowEvaluatorTableBuilder( String[] extensions ) {
        super( extensions );
    }

    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    public void streamStarTable( InputStream in, TableSink sink, String pos )
            throws TableFormatException {
        throw new TableFormatException( "Can't stream " + getFormatName()
                                      + " format tables" );
    }

    public boolean canStream() {
        return false;
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
            + "This file format provides no header information about\n"
            + "column type, so the handler has to look at the column data\n"
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
        sequence = 10
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
