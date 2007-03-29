package uk.ac.starlink.votable;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
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
    }

    public void writePrimaryHDU( StarTable table, FitsTableSerializer fitser,
                                 DataOutput strm )
            throws IOException {

        /* Try to write the metadata as VOTable text in the primary HDU. */
        Exception thrown = null;
        try {
            writeVOTablePrimary( table, fitser, strm );
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
     * @param  table  table to write
     * @param  fitser   FITS serializer
     * @param  out   destination stream
     */
    private void writeVOTablePrimary( StarTable table,
                                      FitsTableSerializer fitser,
                                      DataOutput out )
            throws IOException, FitsException {

        /* Get a serializer that knows how to write VOTable metadata for
         * this table. */
        VOSerializer voser = VOSerializer.makeFitsSerializer( table, fitser );

        /* Get a buffer to hold the VOTable character data. */
        StringWriter textWriter = new StringWriter();

        /* Turn it into a BufferedWriter. */
        BufferedWriter writer = new BufferedWriter( textWriter );

        /* Output preamble. */
        writer.write( "<?xml version='1.0' encoding='" + XML_ENCODING + "'?>" );
        writer.newLine();
        writer.write( "<VOTABLE version='1.1'>" );
        writer.newLine();
        writer.write( "<!--" );
        writer.newLine();
        writer.write( " !  VOTable written by " +
                      voser.formatText( this.getClass().getName() ) );
        writer.newLine();
        writer.write( " !  Describes BINTABLE extension in following HDU" );
        writer.newLine();
        writer.write( " !-->" );
        writer.newLine();
        writer.write( "<RESOURCE>" );
        writer.newLine();

        /* Output table element containing the metadata with the help of
         * the VOTable serializer. */
        voser.writeDescription( writer );
        voser.writeParams( writer );
        writer.write( "<TABLE" );
        String tname = table.getName();
        if ( tname != null && tname.trim().length() > 0 ) {
            writer.write( voser.formatAttribute( "name", tname.trim() ) );
        }
        long nrow = fitser.getRowCount();
        if ( nrow > 0 ) {
            writer.write( voser.formatAttribute( "nrows",
                                                 Long.toString( nrow ) ) );
        }
        writer.write( ">" );
        writer.newLine();
        voser.writeFields( writer );
        writer.write( "<!-- Dummy VOTable - no DATA element -->" );
        writer.newLine();
        writer.write( "</TABLE>" );
        writer.newLine();

        /* Output traling tags and flush. */
        writer.write( "</RESOURCE>" );
        writer.newLine();
        writer.write( "</VOTABLE>" );
        writer.flush();

        /* Get a byte array containing the VOTable text. */
        byte[] textBytes = textWriter.getBuffer().toString()
                                     .getBytes( XML_ENCODING );
        int nbyte = textBytes.length;

        /* Prepare and write a FITS header describing the character data. */
        Header hdr = new Header();
        hdr.addValue( "SIMPLE", true, "Standard FITS format" );
        hdr.addValue( "BITPIX", 8, "Character data" );
        hdr.addValue( "NAXIS", 1, "Text string" );
        hdr.addValue( "NAXIS1", nbyte, "Number of characters" );
        customisePrimaryHeader( hdr );
        hdr.addValue( "EXTEND", true, "There are standard extensions" );
        String[] comments = new String[] {
            " ",
            "The data in this primary HDU consists of bytes which",
            "comprise a VOTABLE document.",
            "The VOTable describes the metadata of the table contained",
            "in the following BINTABLE extension.",
            "The BINTABLE extension can be used on its own as a perfectly",
            "good table, but the information from this HDU may provide some",
            "useful additional metadata.",
        };
        for ( int i = 0; i < comments.length; i++ ) {
            hdr.insertComment( comments[ i ] );
        }
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
     * @param   hdr
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
        for ( Iterator it = hdr.iterator(); it.hasNext(); ) {
            String card = ((HeaderCard) it.next()).toString();
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
        final int ntest = FitsConstants.FITS_BLOCK / 80;
        int pos = 0;
        int ncard = 0;
        for ( int il = 0; il < ntest && buffer.length > pos + 80; il++ ) {
            char[] cbuf = new char[ 80 ];
            for ( int ic = 0; ic < 80; ic++ ) {
                cbuf[ ic ] = (char) ( buffer[ pos++ ] & 0xff );
            }
            HeaderCard card = new HeaderCard( new String( cbuf ) );
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
