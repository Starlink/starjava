package uk.ac.starlink.fits;

import java.io.IOException;
import java.io.OutputStream;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.util.IOConsumer;

/**
 * Handles writing of a StarTable in FITS binary format.
 * Not all columns can be written to a FITS table, only those ones
 * whose <code>contentClass</code> is in the following list:
 * <ul>
 * <li>Boolean
 * <li>Character
 * <li>Byte
 * <li>Short
 * <li>Integer
 * <li>Long
 * <li>Float
 * <li>Double
 * <li>Character
 * <li>String
 * <li>boolean[]
 * <li>char[]
 * <li>byte[]
 * <li>short[]
 * <li>int[]
 * <li>long[]
 * <li>float[]
 * <li>double[]
 * <li>String[]
 * </ul>
 * In all other cases a warning message will be logged and the column
 * will be ignored for writing purposes.
 * <p>
 * Output is currently to fixed-width columns only.  For StarTable columns
 * of variable size, a first pass is made through the table data to
 * determine the largest size they assume, and the size in the output
 * table is set to the largest of these.  Excess space is padded
 * with some sort of blank value (NaN for floating point values,
 * spaces for strings, zero-like values otherwise).
 *
 * @author   Mark Taylor (Starlink)
 */
public class FitsTableWriter extends AbstractFitsTableWriter {

    private boolean isColfits_ = false;
    private PrimaryType primaryType_ = PrimaryType.BASIC;
    private VarArrayMode varArray_ = VarArrayMode.FALSE;
    private StoragePolicy storage_ = StoragePolicy.getDefaultPolicy();

    /**
     * Default constructor.
     */
    public FitsTableWriter() {
        super( "fits-basic" );
    }

    /**
     * Deprecated custom constructor.
     *
     * @deprecated  allows some configuration options but not others;
     *              use no-arg constructor and configuration methods instead
     */
    @Deprecated
    @SuppressWarnings("this-escape")
    public FitsTableWriter( String name, boolean allowSignedByte,
                            WideFits wide ) {
        this();
        setFormatName( name );
        setAllowSignedByte( allowSignedByte );
        setWide( wide );
    }

    /**
     * Returns true if <code>location</code> ends with something like ".fit"
     * or ".fits" or ".fts".
     *
     * @param  location  filename
     * @return true if it sounds like a fits file
     */
    public boolean looksLikeFile( String location ) {
        int dotPos = location.lastIndexOf( '.' );
        if ( dotPos > 0 ) {
            String ext = location.substring( dotPos + 1 ).toLowerCase();
            return isColfits_
                 ? "colfits".equalsIgnoreCase( ext )
                 : ( "fit".equalsIgnoreCase( ext ) ||
                     "fits".equalsIgnoreCase( ext ) );
        }
        else {
            return false;
        }
    }

    /**
     * Sets the type of Primary HDU that will precede the extension HDU(s)
     * corresponding to the table(s) being written.
     *
     * @param   primaryType  type of primary HDU
     */
    public void setPrimaryType( PrimaryType primaryType ) {
        primaryType_ = primaryType;
    }

    /**
     * Returns the type of Primary HDU that will precede the extension HDU(s)
     * corresponding to the table(s) being written.
     *
     * @return  type of primary HDU
     */
    public PrimaryType getPrimaryType() {
        return primaryType_;
    }

    /**
     * Sets whether table data should be written in column-oriented form.
     * If true, the output will be a single-row table of array-valued columns,
     * in which each cell is an array with <code>nrow</code> elements.
     *
     * @param  colfits  true for column-oriented, false for row-oriented
     */
    public void setColfits( boolean colfits ) {
        isColfits_ = colfits;
    }

    /**
     * Returns whether table data will be written in column-oriented form.
     *
     * @return  true for column-oriented, false for row-oriented
     */
    public boolean isColfits() {
        return isColfits_;
    }

    /**
     * Configures how variable-length array values will be stored.
     *
     * @param  varArray  variable-length array storage mode
     */
    public void setVarArray( VarArrayMode varArray ) {
        varArray_ = varArray;
    }

    /**
     * Indicates how variable-length array valued columns will be stored.
     *
     * @return  variable-length array storage mode
     */
    public VarArrayMode getVarArray() {
        return varArray_;
    }

    /**
     * Sets the storage policy to use where required.
     * This is only relevant for column-oriented output,
     * for which data has to be cached before rewriting it.
     *
     * @param  storage  storage policy to use when writing colfits
     */
    public void setStoragePolicy( StoragePolicy storage ) {
        storage_ = storage;
    }

    /**
     * Returns the storage policy that will be used if required.
     *
     * @return  storage policy used when writing colfits
     */
    public StoragePolicy getStoragePolicy() {
        return storage_;
    }

    @Override
    public boolean getAllowSignedByte() {
        return super.getAllowSignedByte() && primaryType_.allowSignedByte();
    }

    public FitsTableSerializer createSerializer( StarTable table )
            throws IOException {
        FitsTableSerializerConfig config = getConfig();
        if ( isColfits_ ) {
            // I could add vararray config for ColFits
            return new ColFitsTableSerializer( config, table );
        }
        else {
            if ( varArray_.isVarArray_ ) {
                Boolean longIndexing = varArray_.longIndexing_;
                VariableFitsTableSerializer fitser =
                    new VariableFitsTableSerializer( config, table, storage_ );
                if ( longIndexing != null ) {
                    fitser.set64BitMode( longIndexing.booleanValue() );
                }
                return fitser;
            }
            else {
                return new StandardFitsTableSerializer( config, table );
            }
        }
    }

    @Override
    public void writeStarTables( TableSequence tableSeq, OutputStream out )
            throws IOException {
        primaryType_.writeTables( this, tableSeq, out );
    }

    /**
     * Creates a PrimaryType corresponding to a PHDU with content
     * that does not depend on the table contents of the extension HDUs.
     *
     * @param  name  type name
     * @param  primaryWriter  writes primary HDU to output stream
     */
    private static PrimaryType
            createStaticPrimaryType( String name,
                                     IOConsumer<OutputStream> primaryWriter ) {
        return new PrimaryType( name ) {
            public boolean allowSignedByte() {
                return true;
            }
            public void writeTables( FitsTableWriter fitsWriter,
                                     TableSequence tseq, OutputStream out )
                    throws IOException {
                primaryWriter.accept( out );
                for ( StarTable table; ( table = tseq.nextTable() ) != null; ) {
                    fitsWriter
                   .writeTableHDU( table, fitsWriter.createSerializer( table ),
                                   out );
                }
            }
        };
    }

    /**
     * Enumeration for variable-length array value storage options.
     * Usually just {@link #TRUE} and {@link #FALSE} will be used,
     * {@link #P} and {@link #Q} are rather special interest.
     */
    public enum VarArrayMode {

        /**
         * All arrays are stored as fixed-length in the body of the table.
         */
        FALSE( false, null ),

        /**
         * Variable-length arrays are stored on the heap,
         * using a suitable pointer size for indirection.
         */
        TRUE( true, null ),

        /**
         * Variable-length arrays are stored on the heap,
         * using a 32-bit pointer size ('P' array descriptor) for indirection.
         */
        P( true, Boolean.FALSE ),

        /**
         * Variable-length arrays are stored on the heap,
         * using a 64-bit pointer size ('Q' array descriptor) for indirection.
         */
        Q( true, Boolean.TRUE );

        private final boolean isVarArray_;
        private final Boolean longIndexing_;

        /**
         * Constructor.
         *
         * @param  isVarArray  true for variable-length array on heap,
         *                     false for fixed-length array in table body
         * @param  longIndexing  TRUE for long, FALSE for short;
         *                       if null, type is determined from the data
         */
        VarArrayMode( boolean isVarArray, Boolean longIndexing ) {
            isVarArray_ = isVarArray;
            longIndexing_ = longIndexing;
        }
    }

    /**
     * Characterises the type of Primary HDU that will precede table
     * extensions written by this writer.
     */
    public static abstract class PrimaryType {

        private final String name_;

        /**
         * Basic Primary HDU, containing minimal headers and no data part.
         */
        public static final PrimaryType BASIC =
            createStaticPrimaryType( "basic",
                                     out -> FitsUtil.writeEmptyPrimary( out ) );

        /**
         * No Primary HDU is written.  The resulting output will therefore
         * not be a legal FITS file, but it can be appended to an existing
         * FITS file with a valid Primary HDU and perhaps other extension HDUs.
         */
        public static final PrimaryType NONE =
            createStaticPrimaryType( "none", out -> {} );

        /**
         * Constructor.
         */
        protected PrimaryType( String name ) {
            name_ = name;
        }

        /**
         * Indicates whether signed byte values will be permitted
         * in files with this PHDU type.
         *
         * @return  whether signed bytes are allowed
         */
        public abstract boolean allowSignedByte();

        /**
         * Writes a sequence of tables to an output stream.
         *
         * @param  writer  fits writer instance
         * @param  tseq   sequence of tables
         * @param  out   destination stream
         */
        public abstract void writeTables( FitsTableWriter writer,
                                          TableSequence tseq, OutputStream out )
                throws IOException;

        @Override
        public String toString() {
            return name_;
        }
    }
}
