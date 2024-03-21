package uk.ac.starlink.votable;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.fits.AbstractFitsTableWriter;
import uk.ac.starlink.fits.CardFactory;
import uk.ac.starlink.fits.CardImage;
import uk.ac.starlink.fits.FitsTableSerializer;
import uk.ac.starlink.fits.FitsUtil;
import uk.ac.starlink.fits.ParsedCard;
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
 * @deprecated  Use {@link UnifiedFitsTableWriter} instead
 *
 * @author   Mark Taylor (Starlink)
 * @since    26 Aug 2004
 */
@Deprecated
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
                                  OutputStream out )
            throws IOException {

        /* Try to write the metadata as VOTable text in the primary HDU. */
        Exception thrown = null;
        try {
            writeVOTablePrimary( tables, fitsers, out );
            return;
        }
        catch ( IOException e ) {
            thrown = e;
        }

        /* But if it fails, just write an empty one. */
        assert thrown != null;
        logger.log( Level.WARNING,
                    "Failed to write VOTable metadata to primary HDU",
                    thrown );
        FitsUtil.writeEmptyPrimary( out );
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
                                      OutputStream out )
            throws IOException {

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
        votWriter.setWriteDate( getWriteDate() );

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
            if ( i > 0 ) {
                votWriter.writeBetweenTableXML( writer );
            }
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
        List<CardImage> cards = new ArrayList<>();
        CardFactory cf = CardFactory.STRICT;
        cards.addAll( Arrays.asList( new CardImage[] {
            cf.createLogicalCard( "SIMPLE", true, "Standard FITS format" ),
            cf.createIntegerCard( "BITPIX", 8, "Character data" ),
            cf.createIntegerCard( "NAXIS", 1, "Text string" ),
            cf.createIntegerCard( "NAXIS1", nbyte, "Number of characters" ),
        } ) );
        cards.addAll( Arrays.asList( getCustomPrimaryHeaderCards() ) );
        cards.add( cf.createLogicalCard( "EXTEND", true,
                                         "There are standard extensions" ) );
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
            cards.add( cf.createCommentCard( comments[ i ] ) );
        }
        cards.add( cf.createIntegerCard( "NTABLE", ntable,
                                         "Number of following BINTABLE HDUs" ));
        cards.add( CardFactory.END_CARD );
        assert primaryHeaderOK( cards.toArray( new CardImage[ 0 ] ) );
        FitsUtil.writeHeader( cards.toArray( new CardImage[ 0 ] ), out );

        /* Write the character data itself. */
        out.write( textBytes );

        /* Write padding to the end of the FITS block. */
        int partial = textBytes.length % FitsUtil.BLOCK_LENG;
        if ( partial > 0 ) {
            int pad = FitsUtil.BLOCK_LENG - partial;
            out.write( new byte[ pad ] );
        }
    }

    /**
     * Returns implementation-specific header cards to be added
     * to the Primary HDU of FITS files written by this writer.
     *
     * @return   header cards
     */
    protected abstract CardImage[] getCustomPrimaryHeaderCards();

    /**
     * Performs assertion-type checks on a primary HDU header written by 
     * an this object.
     *
     * @param  hdr  header to check
     * @return   true iff the header looks OK
     */
    private boolean primaryHeaderOK( CardImage[] cards ) {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            FitsUtil.writeHeader( cards, bout );
            bout.close();
            return isMagic( bout.toByteArray() );
        }
        catch ( IOException e ) {
            assert false;
            return false;
        }
    }

    /**
     * Determines whether a given byte buffer looks like it contains
     * the start of a primary header written by this writer.
     * Calls the protected 
     * {@link #isMagic(int,java.lang.String,java.lang.Object)} method.
     *
     * @param  buffer  start of a file
     * @return  true  iff <code>buffer</code> looks like it contains a 
     *          file written by this handler
     */
    public boolean isMagic( byte[] buffer ) {
        final int ntest = 6;
        if ( buffer.length < ntest * 80 ) {
            return false;
        }
        byte[] cbuf = new byte[ 80 ];
        for ( int il = 0; il < ntest; il++ ) {
            System.arraycopy( buffer, il * 80, cbuf, 0, 80 );
            ParsedCard<?> card = FitsUtil.parseCard( cbuf );
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
     * (as provided by {@link #getCustomPrimaryHeaderCards}).
     *
     * @param   icard  0-based card index
     * @param   key    card name
     * @param   value  card value
     * @return   true iff the presented card is one that could have been
     *           written by this writer
     */
    protected boolean isMagic( int icard, String key, Object value ) {
        switch ( icard ) {
            case 0:
                return "SIMPLE".equals( key ) && Boolean.TRUE.equals( value );
            case 1: 
                return "BITPIX".equals( key )
                    && value instanceof Number
                    && ((Number) value).intValue() == 8;
            case 2:
                return "NAXIS".equals( key )
                    && value instanceof Number
                    && ((Number) value).intValue() == 1;
            case 3:
                return "NAXIS1".equals( key );
            default:
                return true;
        }
    }
}
