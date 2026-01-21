package uk.ac.starlink.table.formats;

import java.awt.datatransfer.DataFlavor;
import java.io.InputStream;
import java.nio.charset.Charset;
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
    private Charset encoding_;
    private RowEvaluator.Decoder<?>[] decoders_;

    /**
     * Constructor.
     *
     * @param  extensions  list of lower-cased filename extensions,
     *                     excluding the '.' character
     * @param  dfltEncoding  default character encoding for this format
     */
    protected RowEvaluatorTableBuilder( String[] extensions,
                                        Charset dfltEncoding ) {
        super( extensions );
        encoding_ = dfltEncoding;
        decoders_ = RowEvaluator.getStandardDecoders();
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

    /**
     * Sets the character encoding for the input stream.
     *
     * @param  encoding  character encoding
     */
    @ConfigMethod(
        property = "encoding",
        usage = "ASCII|UTF-8|UTF-16|...",
        example = "ASCII",
        doc = "<p>Specifies the character encoding used to interpret "
            + "the input file.\n"
            + "</p>"
    )
    public void setEncoding( Charset encoding ) {
        encoding_ = encoding;
    }

    /**
     * Returns the character encoding for the input stream.
     *
     * @return  character encoding
     */
    public Charset getEncoding() {
        return encoding_;
    }

    /**
     * Sets the list of permitted decoders.
     *
     * <p>Note the order of the supplied decoder list is significant;
     * a type earlier in the list will be preferred over one later in
     * the list where the data is consistent with both.
     *
     * <p>In case of no match, a string decoder will be used,
     * even if it does not appear in the supplied list.
     *
     * @param  decoders  decoders that may be used to interpret CSV columns
     */
    public void setDecoders( RowEvaluator.Decoder<?>[] decoders ) {
        decoders_ = decoders;
    }

    /**
     * Returns the list of permitted decoders.
     *
     * @return   decoders that may be used to interpret text columns
     */
    public RowEvaluator.Decoder<?>[] getDecoders() {
        return decoders_;
    }

    /**
     * Sets the list of decoders from a user-supplied string naming
     * decoders not to use.
     *
     * @param  excludeSemicolonList  semicolon-separated list of decoder
     *                               names not to use
     */
    @ConfigMethod(
        property = "notypes",
        doc = "<p>Specifies a semicolon-separated list of names for "
            + "datatypes that will <em>not</em> appear in the columns "
            + "of the table as read. "
            + "Type names that can be excluded are <code>blank</code>, "
            + "<code>boolean</code>, <code>short</code>, <code>int</code>, "
            + "<code>long</code>, <code>float</code>, <code>double</code>, "
            + "<code>date</code>, <code>hms</code> and <code>dms</code>. "
            + "So if you want to make sure that all integer and floating-point "
            + "columns are 64-bit "
            + "(i.e. <code>long</code> and <code>double</code> respectively) "
            + "you can set this value to \"<code>short;int;float</code>\"."
            + "</p>",
        usage = "<type>[;<type>...]",
        example = "short;float",
        sequence = 11
    )
    public void setDecoderExcludeList( String excludeSemicolonList ) {
        Map<String,RowEvaluator.Decoder<?>> decoderMap = new LinkedHashMap<>();
        for ( RowEvaluator.Decoder<?> decoder :
              RowEvaluator.getStandardDecoders() ) {
            decoderMap.put( decoder.getName(), decoder );
        }
        String optList = decoderMap.keySet().toString();
        List<RowEvaluator.Decoder<?>> decoders = new ArrayList<>();
        for ( String excludeName : excludeSemicolonList.split( ";" ) ) {
            if ( decoderMap.remove( excludeName ) == null ) {
                String msg = "Unknown type name \"" + excludeName + "\"; "
                           + "options are " + optList;
                throw new IllegalArgumentException( msg );
            }
        }
        setDecoders( decoderMap.values()
                               .toArray( new RowEvaluator.Decoder<?>[ 0 ] ) );
    }
}
