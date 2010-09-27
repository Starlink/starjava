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
import uk.ac.starlink.util.IOUtils;

/**
 * Abstract table writer superclass designed for writing FITS tables.
 *
 * @author   Mark Taylor
 * @since    27 Jun 2006
 */
public abstract class AbstractFitsTableWriter extends StreamStarTableWriter
                                              implements MultiStarTableWriter {

    private String formatName_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.fits" );

    /**
     * Constructor.
     *
     * @param  formatName  format name
     */
    protected AbstractFitsTableWriter( String formatName ) {
        setFormatName( formatName );
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
                                       java.io.OutputStream)}.
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
     * Provides a suitable serializer for a given table.
     *
     * @param   table  table to serialize
     * @return  FITS serializer
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
            hdr.addValue( "DATE-HDU", getCurrentDate(),
                          "Date of HDU creation (UTC)" );
            hdr.addValue( "STILVERS",
                          IOUtils.getResourceContents( StarTable.class,
                                                       "stil.version" ),
                          "Version of STIL software" );
            hdr.addValue( "STILCLAS", getClass().getName(),
                          "Author class in STIL software" );
        }
        catch ( HeaderCardException e ) {
            logger_.warning( "Trouble adding metadata header cards " + e );
        }
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
