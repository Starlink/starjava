package uk.ac.starlink.fits;

import java.io.BufferedOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;
import uk.ac.starlink.table.MultiStarTableWriter;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StreamStarTableWriter;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.ConfigMethod;
import uk.ac.starlink.util.IOUtils;

/**
 * Abstract table writer superclass designed for writing FITS tables.
 *
 * <p>A couple of Auxiliary metadata items of the ColumnInfo metadata
 * from written tables are respected:
 * <ul>
 * <li>{@link uk.ac.starlink.table.Tables#NULL_VALUE_INFO}:
 *     sets the value of <code>TNULLn</code> "magic" blank value for
 *     integer columns</li>
 * <li>{@link uk.ac.starlink.table.Tables#UBYTE_FLAG_INFO}:
 *     if set to <code>Boolean.TRUE</code> and if the column has content class
 *     <code>Short</code> or <code>short[]</code>, the data will be written
 *     as unsigned bytes (<code>TFORMn='B'</code>)
 *     not 16-bit signed integers (<code>TFORMn='I'</code>).</li>
 * <li>{@link BintableStarTable#LONGOFF_INFO}:
 *     if this is set to a string representation of an integer value,
 *     and the column has content class String or String[],
 *     then the data will be written as long integers (<code>TFORMn='K'</code>)
 *     with the given offset (<code>TZEROn=...</code>).
 *     This option supports round-tripping of offset long values
 *     (typically representing unsigned longs) which are converted to
 *     strings on read.</li>
 * </ul>
 *
 * @author   Mark Taylor
 * @since    27 Jun 2006
 */
public abstract class AbstractFitsTableWriter extends StreamStarTableWriter
                                              implements MultiStarTableWriter {

    private String formatName_;
    private boolean writeDate_;
    private boolean allowSignedByte_;
    private boolean allowZeroLengthString_;
    private WideFits wide_;
    private byte padChar_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Constructor.
     *
     * @param  formatName  format name
     */
    protected AbstractFitsTableWriter( String formatName ) {
        setFormatName( formatName );
        allowSignedByte_ = true;
        allowZeroLengthString_ = true;
        wide_ = WideFits.DEFAULT;
        padChar_ = (byte) '\0';
        writeDate_ = true;
    }

    public String getFormatName() {
        return formatName_;
    }

    /**
     * Sets the declared format name.
     *
     * @param  formatName  format name
     */
    public void setFormatName( String formatName ) {
        formatName_ = formatName;
    }

    /**
     * Returns "application/fits".
     *
     * @return MIME type
     */
    public String getMimeType() {
        return "application/fits";
    }

    /**
     * Writes a single table.
     * Invokes {@link #writeStarTables}.
     */
    public void writeStarTable( StarTable table, OutputStream out )
            throws IOException {
        writeStarTables( Tables.singleTableSequence( table ), out );
    }

    /**
     * Writes tables.  Calls {@link #writePrimaryHDU(java.io.DataOutput)}
     * to write the primary HDU.
     * Subclasses which want to put something related to the input tables
     * into the primary HDU will need to override this method
     * (writeStarTables).
     */
    public void writeStarTables( TableSequence tableSeq, OutputStream out )
            throws IOException {
        DataOutputStream ostrm = new DataOutputStream( out );
        writePrimaryHDU( ostrm );
        for ( StarTable table; ( table = tableSeq.nextTable() ) != null; ) {
            writeTableHDU( table, createSerializer( table ), ostrm );
        }
        ostrm.flush();
    }

    /**
     * Invokes {@link #writeStarTables(uk.ac.starlink.table.TableSequence,
     *                                 java.io.OutputStream)}.
     */
    public void writeStarTables( TableSequence tableSeq, String location,
                                 StarTableOutput sto ) throws IOException {
        OutputStream out = sto.getOutputStream( location );
        try {
            out = new BufferedOutputStream( out );
            writeStarTables( tableSeq, out );
            out.flush();
        }
        finally {
            out.close();
        }
    }

    /**
     * Writes the primary HDU.  This cannot contain a table since BINTABLE
     * HDUs can only be extensions.
     * The AbstractFitsTableWriter implementation writes a minimal, data-less
     * HDU.
     *
     * @param  out  destination stream
     */
    public void writePrimaryHDU( DataOutput out ) throws IOException {
        FitsConstants.writeEmptyPrimary( out );
    }

    /**
     * Writes a data HDU.
     *
     * @param   table  the table to be written into the HDU
     * @param   fitser  fits serializer initalised from <code>table</code>
     * @param   out  destination stream
     */
    public void writeTableHDU( StarTable table, FitsTableSerializer fitser,
                               DataOutput out ) throws IOException {
        try {
            Header hdr = fitser.getHeader();
            addMetadata( hdr );
            FitsConstants.writeHeader( out, hdr );
        }
        catch ( FitsException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
        fitser.writeData( out );
    }

    /**
     * Returns the configuration details for writing FITS files.
     * Its content can be controlled using single-config-item
     * mutator methods
     * (which may also be labelled as
     * {@link uk.ac.starlink.util.ConfigMethod}s)
     * on this class.
     * This covers things that are generally orthogonal to the type
     * of serialization, so may be set for any kind of FITS output,
     * which is why it makes sense to manage them in the
     * <code>AbstractFitsTableWriter</code> abstract superclass.
     *
     * @return   object representing the FITS serialization options
     *           currently configured for this writer
     */
    public FitsTableSerializerConfig getConfig() {
        final boolean allowSignedByte = allowSignedByte_;
        final boolean allowZeroLengthString = allowZeroLengthString_;
        final WideFits wide = wide_;
        final byte padChar = padChar_;
        return new FitsTableSerializerConfig() {
            public boolean allowSignedByte() {
                return allowSignedByte;
            }
            public boolean allowZeroLengthString() {
                return allowZeroLengthString;
            }
            public WideFits getWide() {
                return wide;
            }
            public byte getPadCharacter() {
                return padChar;
            }
        };
    }

    /**
     * Provides a suitable serializer for a given table.
     * Note this should throw an IOException if it can be determined that
     * the submitted table cannot be written by this writer, for instance
     * if it has too many columns.
     *
     * @param   table  table to serialize
     * @return  FITS serializer
     * @throws  IOException  if the table can't be written
     */
    protected abstract FitsTableSerializer createSerializer( StarTable table )
            throws IOException;

    /**
     * Adds some standard metadata header cards to a FITS table header.
     * This includes date stamp, STIL version, etc.
     *
     * @param   hdr  header to modify
     */
    protected void addMetadata( Header hdr ) {
        try {
            if ( getWriteDate() ) {
                hdr.addValue( "DATE-HDU", getCurrentDate(),
                              "Date of HDU creation (UTC)" );
            }
            hdr.addValue( "STILVERS",
                          IOUtils.getResourceContents( StarTable.class,
                                                       "stil.version", null ),
                          "Version of STIL software" );
            hdr.addValue( "STILCLAS", getClass().getName(),
                          "STIL Author class" );
        }
        catch ( HeaderCardException e ) {
            logger_.warning( "Trouble adding metadata header cards " + e );
        }
    }

    /**
     * Configures whether a datestamp is written to output FITS files.
     *
     * @param  writeDate  true to include DATE-HDU, false to omit it
     */
    @ConfigMethod(
        property = "date",
        doc = "<p>If true, the DATE-HDU header is filled in with the current "
            + "date; otherwise it is not included.</p>"
    )
    public void setWriteDate( boolean writeDate ) {
        writeDate_ = writeDate;
    }

    /**
     * Indicates whether a datestamp is written to output FITS files.
     *
     * @return   true to include DATE-HDU, false to omit it
     */
    public boolean getWriteDate() {
        return writeDate_;
    }

    /**
     * Configures how Byte-valued columns are written.
     * This is a bit fiddly, since java bytes are signed,
     * but FITS 8-bit integers are unsigned.
     * If true, they are written as FITS unsigned 8-bit integers
     * with an offset, as discussed in the FITS standard
     * (<code>TFORMn='B'</code>, <code>TZERO=-128</code>).
     * If false, they are written as FITS signed 16-bit integers.
     *
     * @param  allowSignedByte  true to write offset bytes,
     *                          false to write shorts
     */
    public void setAllowSignedByte( boolean allowSignedByte ) {
        allowSignedByte_ = allowSignedByte;
    }

    /**
     * Returns a flag indicating how Byte-valued columns are written.
     *
     * @return  true to write offset bytes, false to write shorts
     */
    public boolean getAllowSignedByte() {
        return allowSignedByte_;
    }

    /**
     * Sets whether zero-length string columns may be written.
     * Such columns (<code>TFORMn='0A'</code>) are explicitly permitted
     * by the FITS standard, but they can cause crashes because of
     * divide-by-zero errors when encountered by some versions of CFITSIO
     * (v3.50 and earlier), so FITS writing code may wish to avoid them.
     * If this is set false, then A repeat values will always be &gt;=1.
     *
     * @param  allowZeroLengthString  false to prevent zero-length
     *                                string columns
     */
    public void setAllowZeroLengthString( boolean allowZeroLengthString ) {
        allowZeroLengthString_ = allowZeroLengthString;
    }

    /**
     * Indicates whether zero-length string columns may be output.
     *
     * @return  false if zero-length string columns are avoided
     */
    public boolean getAllowZeroLengthString() {
        return allowZeroLengthString_;
    }

    /**
     * Sets the convention for representing over-wide (&gt;999 column) tables.
     *
     * @param  wide   wide-table representation policy,
     *                null to avoid wide tables
     */
    public void setWide( WideFits wide ) {
        wide_ = wide;
    }

    /**
     * Indicates the convention in use for representing over-wide tables.
     *
     * @return   wide-table representation policy, null for no wide tables
     */
    public WideFits getWide() {
        return wide_;
    }

    /**
     * Sets the byte value with which under-length string (character array)
     * values should be padded.
     * This should normally be one of 0x00 (ASCII NUL) or 0x20 (space).
     * The function of an ASCII NUL is to terminate the string early,
     * as described in Section 7.3.3.1 of the FITS 4.0 standard;
     * space characters pad it to its declared length with whitespace.
     * Other values in the range 0x21-0x7E are permitted but probably
     * not sensible.
     *
     * @param  padChar  character data padding byte
     */
    public void setPadCharacter( byte padChar ) {
        padChar_ = padChar;
    }

    /**
     * Returns the byte value with which under-length string (character array)
     * values will be padded.
     * This will normally be one of 0x00 (ASCII NUL) or 0x20 (space).
     *
     * @return  character data padding byte
     */
    public byte getPadCharacter() {
        return padChar_;
    }

    /**
     * Returns an ISO-8601 data string representing the time at which this
     * method is called.
     *
     * @return date string
     */
    public static String getCurrentDate() {
        DateFormat fmt = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss" );
        TimeZone utc = TimeZone.getTimeZone( "UTC" );
        fmt.setTimeZone( utc );
        fmt.setCalendar( new GregorianCalendar( utc, Locale.UK ) );
        return fmt.format( new Date() );
    }
}
