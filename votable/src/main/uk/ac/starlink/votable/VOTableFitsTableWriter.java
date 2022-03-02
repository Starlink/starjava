package uk.ac.starlink.votable;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import uk.ac.starlink.fits.AbstractFitsTableWriter;
import uk.ac.starlink.fits.FitsConstants;
import uk.ac.starlink.fits.FitsTableSerializer;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.util.DataBufferedOutputStream;

/**
 * TableWriter which writes table data into the first extension of a FITS file,
 * Unlike {@link uk.ac.starlink.fits.FitsTableWriter} however, the
 * primary extension is not left contentless, instead it gets the
 * text of a DATA-less VOTable written into it.  This VOTable describes
 * the metadata of the table.
 * Tables stored using this (non-standard) mechanism have all the rich
 * metadata associated with VOTables, and benefit from the compactness
 * of FITS tables, withouth the considerable disadvantage of being split
 * into two files.
 *
 * @author   Mark Taylor (Starlink)
 * @since    26 Aug 2004
 */
public abstract class VOTableFitsTableWriter extends AbstractFitsTableWriter {

    private VOTableVersion votVersion_;
    private static String XML_ENCODING = "UTF-8";
    private static Logger logger =
        Logger.getLogger( "uk.ac.starlink.votable" );

    /**
     * Constructor.
     *
     * @param  formatName  handler format name
     */
    protected VOTableFitsTableWriter( String formatName ) {
        super( formatName );
        setAllowSignedByte( false );
        votVersion_ = VOTableVersion.getDefaultVersion();
    }

    @Override
    public void setAllowSignedByte( boolean allowSignedByte ) {
        if ( allowSignedByte ) {              
            throw new IllegalArgumentException( "Not recommended "
                                              + "for fits-plus" );
        }
        super.setAllowSignedByte( allowSignedByte );
    }

    public void writeStarTables( TableSequence tableSeq, OutputStream out )
            throws IOException {

        /* Get all the input tables and serializers up front.
         * This does have negative implications for scalability
         * (can't stream one table at a time), but it's necessary
         * to write the header. */
        List<StarTable> tableList = new ArrayList<StarTable>();
        for ( StarTable table; ( table = tableSeq.nextTable() ) != null; ) {
            tableList.add( table );
        }
        StarTable[] tables = tableList.toArray( new StarTable[ 0 ] );
        int ntable = tables.length;
        FitsTableSerializer[] fitsers = new FitsTableSerializer[ ntable ];
        for ( int i = 0; i < ntable; i++ ) {
            fitsers[ i ] = createSerializer( tables[ i ] );
        }

        /* Prepare destination stream. */
        DataBufferedOutputStream dout = new DataBufferedOutputStream( out );
        out = null;

        /* Write the primary HDU. */
        writePrimaryHDU( tables, fitsers, dout );

        /* Write the data. */
        for ( int i = 0; i < ntable; i++ ) {
            writeTableHDU( tables[ i ], fitsers[ i ], dout );
        }

        /* Tidy up. */
        dout.flush();
    }

    /**
     * Sets the version of the VOTable standard to use for encoding metadata
     * in the primary HDU.
     *
     * @param   votVersion   VOTable version to use
     */
    public void setVotableVersion( VOTableVersion votVersion ) {
        votVersion_ = votVersion;
    }

    /**
     * Writes the primary HDU for a number of tables.
     *
     * @param  tables  array of tables to write
     * @param  fitsers array of serializers corresponding to <code>tables</code>
     * @param  strm    destination stream
     */
    private void writePrimaryHDU( StarTable[] tables,
                                  FitsTableSerializer[] fitsers,
                                  DataOutput strm )
            throws IOException {

        /* Try to write the metadata as VOTable text in the primary HDU. */
        Exception thrown = null;
        try {
            writeVOTablePrimary( tables, fitsers, strm );
            return;
        }
        catch ( IOException e ) {
            thrown = e;
        }
        catch ( FitsException e ) {
            thrown = e;
        }

        /* But if it fails, just write an empty one. */
        assert thrown != null;
        logger.log( Level.WARNING,
                    "Failed to write VOTable metadata to primary HDU",
                    thrown );
        FitsConstants.writeEmptyPrimary( strm );
    }

    /**
     * Writes a primary that consists of a byte array holding a 
     * UTF8-encoded VOTable which holds the table metadata.
     *
     * @param  tables  tables to write
     * @param  fitsers   FITS serializers
     * @param  out   destination stream
     */
    private void writeVOTablePrimary( StarTable[] tables,
                                      FitsTableSerializer[] fitsers,
                                      DataOutput out )
            throws IOException, FitsException {

        /* Get a serializer that knows how to write VOTable metadata for
         * this table. */
        int ntable = tables.length;
        if ( fitsers.length != ntable ) {
            throw new IllegalArgumentException( "table/serializer count "
                                              + "mismatch" );
        }

        /* Get a buffer to hold the VOTable character data. */
        StringWriter textWriter = new StringWriter();

        /* Turn it into a BufferedWriter. */
        BufferedWriter writer = new BufferedWriter( textWriter );

        /* Get an object that knows how to write a VOTable document. */
        VOTableWriter votWriter =
            new VOTableWriter( (DataFormat) null, false, votVersion_ );

        /* Output preamble. */
        votWriter.writePreTableXML( writer );
        String comment = new StringBuffer()
            .append( "<!-- " )
            .append( "Describes BINTABLE extensions in the following " )
            .append( ntable == 1 ? "HDU" : ( ntable + " HDUs" ) )
            .append( "." )
            .append( "-->" )
            .toString();
        writer.write( comment );
        writer.newLine();

        /* Output table elements containing the metadata with the help of
         * the VOTable serializer. */
        for ( int i = 0; i < ntable; i++ ) {
            StarTable table = tables[ i ];
            FitsTableSerializer fitser = fitsers[ i ];
            VOSerializer voser =
                VOSerializer.makeFitsSerializer( tables[ i ], fitsers[ i ],
                                                 votVersion_ );
            voser.writePreDataXML( writer );
            writer.write( "<!-- Dummy VOTable - no DATA element -->" );
            writer.newLine();
            voser.writePostDataXML( writer );
        }

        /* Output trailing tags and flush. */
        votWriter.writePostTableXML( writer );
        writer.flush();

        /* Get a byte array containing the VOTable text. */
        byte[] textBytes = textWriter.getBuffer().toString()
                                     .getBytes( XML_ENCODING );
        int nbyte = textBytes.length;

        /* Prepare and write a FITS header describing the character data. */
        Header hdr = FitsConstants.createUnsortedHeader();
        hdr.addValue( "SIMPLE", true, "Standard FITS format" );
        hdr.addValue( "BITPIX", 8, "Character data" );
        hdr.addValue( "NAXIS", 1, "Text string" );
        hdr.addValue( "NAXIS1", nbyte, "Number of characters" );
        customisePrimaryHeader( hdr );
        hdr.addValue( "EXTEND", true, "There are standard extensions" );
        String plural = ntable == 1 ? "" : "s";
        String[] comments = new String[] {
            " ",
            "The data in this primary HDU consists of bytes which",
            "comprise a VOTABLE document.",
            "The VOTable describes the metadata of the table" + plural
                 + " contained",
            "in the following BINTABLE extension" + plural + ".",
            "Such a BINTABLE extension can be used on its own as a perfectly",
            "good table, but the information from this HDU may provide some",
            "useful additional metadata.",
            ( ntable == 1 ? "There is one following BINTABLE."
                          : "There are " + ntable + " following BINTABLEs." ),
        };
        for ( int i = 0; i < comments.length; i++ ) {
            hdr.insertComment( comments[ i ] );
        }
        hdr.addValue( "NTABLE", ntable, "Number of following BINTABLE HDUs" );
        hdr.insertCommentStyle( "END", "" );
        assert primaryHeaderOK( hdr );
        FitsConstants.writeHeader( out, hdr );

        /* Write the character data itself. */
        out.write( textBytes );

        /* Write padding to the end of the FITS block. */
        int partial = textBytes.length % FitsConstants.FITS_BLOCK;
        if ( partial > 0 ) {
            int pad = FitsConstants.FITS_BLOCK - partial;
            out.write( new byte[ pad ] );
        }
    }

    /**
     * Hook for adding custom entries to the FITS header which is written
     * to the primary HDU.  This is called just after the required 
     * cards (SIMPLE, BITPIX, NAXIS, NAXIS1 ) are added and just before
     * the EXTEND card.
     *
     * @param   hdr  header
     */
    protected abstract void customisePrimaryHeader( Header hdr )
            throws HeaderCardException;

    /**
     * Performs assertion-type checks on a primary HDU header written by 
     * an this object.
     *
     * @param  hdr  header to check
     * @return   true iff the header looks OK
     */
    private boolean primaryHeaderOK( Header hdr ) {
        boolean ok = true;
        ByteArrayOutputStream bstrm = new ByteArrayOutputStream();
        for ( @SuppressWarnings("unchecked")
              Iterator<HeaderCard> it = hdr.iterator(); it.hasNext(); ) {
            String card = it.next().toString();
            ok = ok && card.length() == 80;
            for ( int i = 0; i < card.length(); i++ ) {
                bstrm.write( (byte) card.charAt( i ) );
            }
        }
        return ok && isMagic( bstrm.toByteArray() );
    }

    /**
     * Determines whether a given byte buffer looks like it contains
     * the start of a primary header written by this writer.
     * Calls the protected 
     * {@link #isMagic(int,java.lang.String,java.lang.String)} method.
     *
     * @param  buffer  start of a file
     * @return  true  iff <code>buffer</code> looks like it contains a 
     *          file written by this handler
     */
    public boolean isMagic( byte[] buffer ) {
        if ( buffer.length < 2 * 80 ) {
            return false;
        }
        final int ntest = FitsConstants.FITS_BLOCK / 80;
        int pos = 0;
        int ncard = 0;
        for ( int il = 0; il < ntest && buffer.length > pos + 80; il++ ) {
            char[] cbuf = new char[ 80 ];
            for ( int ic = 0; ic < 80; ic++ ) {
                cbuf[ ic ] = (char) ( buffer[ pos++ ] & 0xff );
            }
            HeaderCard card =
                FitsConstants.createHeaderCard( new String( cbuf ) );
            if ( ! isMagic( il, card.getKey(), card.getValue() ) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests a header card to see if it looks like part of the magic number
     * for the format written by this handler.
     * The <code>VOTableFitsTableWriter</code> implementation tests that
     * the first four cards read:
     * <pre>
     *    SIMPLE = T
     *    BITPIX = 8
     *    NAXIS  = 1
     *    NAXIS1 = ???
     * </pre>
     * Subclasses may override this to add tests for later cards
     * (as written in {@link #customisePrimaryHeader}).
     *
     * @param   icard  0-based card index
     * @param   key    card name
     * @param   value  card value
     * @return   true iff the presented card is one that could have been
     *           written by this writer
     */
    protected boolean isMagic( int icard, String key, String value ) {
        switch ( icard ) {
            case 0:
                return "SIMPLE".equals( key ) && "T".equals( value );
            case 1: 
                return "BITPIX".equals( key ) && "8".equals( value );
            case 2:
                return "NAXIS".equals( key ) && "1".equals( value );
            case 3:
                return "NAXIS1".equals( key );
            default:
                return true;
        }
    }
}
