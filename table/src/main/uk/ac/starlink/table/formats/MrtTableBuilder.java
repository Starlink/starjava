package uk.ac.starlink.table.formats;

import java.awt.datatransfer.DataFlavor;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.ConfigMethod;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.IOSupplier;

/**
 * Input handler for the so-called "Machine-Readable Table" format
 * used by AAS journals.
 * This is a horrid format, based on the VizieR readMe files,
 * which are themselves underdocumented, but picking and leaving
 * whatever bits and pieces they feel like, without any careful
 * documentation of that process.  This reader has mainly been
 * put together by looking at given instances of files claimed to be
 * in this format.
 *
 * @author   Mark Taylor
 * @since    30 Apr 2021
 * @see  <a href="https://journals.aas.org/mrt-standards/"
 *               >https://journals.aas.org/mrt-standards/</a>
 */
public class MrtTableBuilder extends DocumentedTableBuilder {

    private ErrorMode errorMode_;
    private boolean checkMagic_;
    private boolean useFloat_;

    /** Should be present at the start of all MRT files. */
    public static final String MAGIC_TXT = "Title: ";

    /**
     * Default constructor.
     */
    public MrtTableBuilder() {
        this( ErrorMode.WARN, true, false );
    }

    /**
     * Constructor with configuration options.
     *
     * @param  errorMode  error handling mode
     * @param  checkMagic  whether to require finding the magic number
     *                     before attempting to parse
     * @param  useFloat   true to attempt use of single-precision floating
     *                    point values where it looks like they should be
     *                    appropriate
     */
    public MrtTableBuilder( ErrorMode errorMode, boolean checkMagic,
                            boolean useFloat ) {
        super( new String[] { "mrt", "cds", } );
        errorMode_ = errorMode;
        checkMagic_ = checkMagic;
        useFloat_ = useFloat;
    }

    /**
     * Returns "MRT".
     */
    public String getFormatName() {
        return "MRT";
    }

    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    /**
     * Sets the error handling mode.
     *
     * @param  errorMode  error handling mode
     */
    @ConfigMethod(
        property = "errmode",
        doc = "<p>Indicates what action should be taken if formatting errors\n"
            + "are detected in the file at read time.\n"
            + "</p>",
        example = "FAIL"
    )
    public void setErrorMode( ErrorMode errorMode ) {
        errorMode_ = errorMode;
    }

    /**
     * Returns the error handling mode.
     *
     * @return  error handling mode
     */
    public ErrorMode getErrorMode() {
        return errorMode_;
    }

    /**
     * Sets whether the handler will attempt to guess by looking at
     * the file whether it appears to be an MRT file before attempting
     * to parse it as one.  This is generally a good idea,
     * since otherwise it will attempt to make MRT sense of any old file,
     * but you can set it false to try to parse MRT files with
     * unexpected first few bytes.
     *
     * @param   checkMagic  true to require magic number presence
     */
    @ConfigMethod(
        property = "checkmagic",
        doc = "<p>Determines whether an initial test is made to see whether\n"
            + "the file looks like MRT before attempting to read it as one;\n"
            + "the test is that it starts with the string\n"
            + "\"<code>Title: </code>\".\n"
            + "Setting this true is generally a good idea\n"
            + "to avoid attempting to parse non-MRT files,\n"
            + "but you can set it false to attempt to read an MRT file\n"
            + "that starts with the wrong sequence.\n"
            + "</p>",
        example = "false"
    )
    public void setCheckMagic( boolean checkMagic ) {
        checkMagic_ = checkMagic;
    }

    /**
     * Indicates whether the handler will attempt to guess by looking at
     * the file whether it is an MRT file.
     *
     * @return  check magic flag
     */
    public boolean getCheckMagic() {
        return checkMagic_;
    }

    /**
     * Sets whether this handler will use a 32-bit float type for reading
     * sufficiently narrow floating point fields.  This is usually a good
     * idea since it reduces storage requirements when only a few significant
     * figures are provided, but can fail if the column contains any
     * very large absolute values (&gt;~1e38), which cannot be represented
     * in a float.  So it's safer to set it false.
     *
     * <p>If it is set true, then encountering values outside the
     * representable range will be reported in accordance with the current
     * ErrorMode.
     *
     * @param useFloat  true to sometimes use 32-bit floats where it looks
     *                  like a good idea,
     *                  false to always use 64-bit doubles
     */
    @ConfigMethod(
        property = "usefloat",
        doc = "<p>Sets whether this handler will use a 32-bit float type\n"
            + "for reading sufficiently narrow floating point fields.\n"
            + "This is usually a good idea\n"
            + "since it reduces storage requirements\n"
            + "when only a few significant figures are provided,\n"
            + "but can fail if the column contains any\n"
            + "very large absolute values (&gt;~1e38),\n"
            + "which cannot be represented in a 32-bit IEEE float.\n"
            + "So it's safer to set it false.\n"
            + "</p>\n"
            + "<p>If it is set true,\n"
            + "then encountering values outside the representable range\n"
            + "will be reported in accordance with the current ErrorMode.\n"
            + "</p>",
        example = "true"
    )
    public void setUseFloat( boolean useFloat ) {
        useFloat_ = useFloat;
    }

    /**
     * Indicates whether this handler will attempt to use 32-bit float
     * columns for narrow floating point fields.
     *
     * @return  true to sometimes use 32-bit floats,
     *          false to always use 64-bit doubles
     */
    public boolean getUseFloat() {
        return useFloat_;
    }

    public StarTable makeStarTable( final DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storage )
            throws IOException {
        if ( checkMagic_ ) {
            byte[] intro = datsrc.getIntro();
            if ( ! isMagic( intro ) ) {
                throw new TableFormatException( "Does not start with sequence: "
                                              + "\"" + MAGIC_TXT + "\"" );
            }
        }
        IOSupplier<MrtReader> rdrFact = () ->
            new MrtReader( new BufferedInputStream( datsrc.getInputStream() ),
                           errorMode_, useFloat_ );
        MrtReader rdr = rdrFact.get();
        rdr.close();
        return createStarTable( rdr, rdrFact );
    }

    public void streamStarTable( InputStream in, TableSink sink, String pos )
            throws IOException {
        try ( MrtReader rdr = new MrtReader( in, errorMode_, useFloat_ ) ) {
            sink.acceptMetadata( createStarTable( rdr, () -> {
                throw new UnsupportedOperationException();
            } ) );
            while ( rdr.next() ) {
                sink.acceptRow( rdr.getRow() );
            }
            sink.endRows();
        }
    }

    public boolean canStream() {
        return true;
    }

    public boolean docIncludesExample() {
        return false;
    }

    public String getXmlDescription() {
        return readText( "MrtTableBuilder.xml" );
    }

    /**
     * Indicates whether the given buffer starts with the MRT magic number
     * <code>{@value #MAGIC_TXT}</code>.
     *
     * @param   intro  byte buffer
     * @return  true iff magic number is present
     */
    public static boolean isMagic( byte[] intro ) {
        int mleng = MAGIC_TXT.length();
        if ( intro.length > mleng ) {
            for ( int i = 0; i < mleng; i++ ) {
                if ( (char) intro[ i ] != MAGIC_TXT.charAt( i ) ) {
                    return false;
                }
            }
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Constructs a table given a reader and a RowSequence supplier.
     *
     * @param  rdr  MRT reader, used for supplying metadata
     * @param  rseqFact   factory for RowSequences
     * @return   table
     */
    private static StarTable
            createStarTable( final MrtReader rdr,
                             final IOSupplier<? extends RowSequence> rseqFact) {
        StarTable table = new AbstractStarTable() {
            public int getColumnCount() {
                return rdr.getColumnCount();
            }
            public ColumnInfo getColumnInfo( int icol ) {
                return rdr.getColumnInfo( icol );
            }
            public long getRowCount() {
                return -1L;
            }
            public RowSequence getRowSequence() throws IOException {
                return rseqFact.get();
            }
            public void close() throws IOException {
                rdr.close();
            }
        };
        table.getParameters().addAll( Arrays.asList( rdr.getParameters() ) );
        return table;
    }
}
