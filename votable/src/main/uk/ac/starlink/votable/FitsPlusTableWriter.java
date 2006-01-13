package uk.ac.starlink.votable;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import uk.ac.starlink.fits.FitsConstants;
import uk.ac.starlink.fits.FitsTableSerializer;
import uk.ac.starlink.fits.FitsTableWriter;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableWriter;

/**
 * TableWriter which writes table data into the first extension of a FITS file, 
 * Unlike {@link uk.ac.starlink.fits.FitsTableWriter} however, the
 * primary extension is not left contentless, instead it gets the
 * text of a DATA-less VOTable written into it.  This VOTable describes
 * the metadata of the table, as if the DATA element contained a FITS
 * element referencing the first extension HDU of the file.
 * Tables stored in this format have all the rich metadata associated
 * with VOTables, and benefit from the compactness of FITS tables,
 * without the considerable disdvantage of being split into two files.
 *
 * <p>The header cards in the primary HDU look like this:
 * <pre>
 *     SIMPLE  =                    T / Standard FITS format
 *     BITPIX  =                    8 / Character data
 *     NAXIS   =                    1 / Text string
 *     NAXIS1  =                 nnnn / Number of characters
 *     VOTMETA =                    T / Table metadata in VOTABLE format
 *     EXTEND  =                    T / There are standard extensions
 * </pre>
 * the VOTMETA card in particular marking that this HDU contains VOTable
 * metadata.
 *
 * @author   Mark Taylor (Starlink)
 * @since    26 Aug 2004
 */
public class FitsPlusTableWriter extends FitsTableWriter {

    private static String XML_ENCODING = "UTF-8";
    private static Logger logger = 
        Logger.getLogger( "uk.ac.starlink.table.formats" );
    private String formatName_ = "fits-plus";

    public String getFormatName() {
        return formatName_;
    }

    /**
     * Returns true if <tt>location</tt> ends with something like ".fit"
     * or ".fits".
     *
     * @param  location  filename
     * @return true if it sounds like a fits file
     */
    public boolean looksLikeFile( String location ) {
        int dotPos = location.lastIndexOf( '.' );
        if ( dotPos > 0 ) {
            String exten = location.substring( dotPos + 1 ).toLowerCase();
            if ( exten.startsWith( "fit" ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Attempts to write a VOTable containing metadata for <tt>table</tt>
     * into the primary HDU.
     */
    protected void writePrimary( StarTable table, FitsTableSerializer fitser,
                                 DataOutputStream strm )
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
        if ( thrown != null ) {
            logger.log( Level.WARNING, 
                        "Failed to write VOTable metadata to primary HDU",
                        thrown );
        }
        super.writePrimary( table, fitser, strm );
    }

    /**
     * Writes a primary that consists of a character array holding a 
     * VOTable which holds the table metadata.
     */
    private void writeVOTablePrimary( StarTable table,
                                      FitsTableSerializer fitser,
                                      DataOutputStream strm ) 
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
        long nrow = table.getRowCount();
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
        hdr.addValue( "VOTMETA", true, "Table metadata in VOTABLE format" );
        hdr.addValue( "EXTEND", true, "There are standard extensions" );
        String[] comments = new String[] {
            " ",
            "This header consists of bytes which comprise a VOTABLE document.",
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
        assert headerOK( hdr );
        FitsConstants.writeHeader( strm, hdr );

        /* Write the character data itself. */
        strm.write( textBytes );

        /* Writer padding to the end of the FITS block. */
        int partial = textBytes.length % FitsConstants.FITS_BLOCK;
        if ( partial > 0 ) {
            int pad = FitsConstants.FITS_BLOCK - partial;
            strm.write( new byte[ pad ] );
        }
    }

    /**
     * Returns a list of FITS-plus table writers with variant values of
     * attributes.
     * In fact this just returns two functionally identical instances
     * but with different format names: one is "fits" and the other is
     * "fits-plus".
     *
     * @return  table writers
     */
    public static StarTableWriter[] getStarTableWriters() {
        FitsPlusTableWriter w1 = new FitsPlusTableWriter();
        FitsPlusTableWriter w2 = new FitsPlusTableWriter();
        w1.formatName_ = "fits";
        return new StarTableWriter[] { w1, w2 };
    }

    /**
     * Check that the header we have written is the same as the one that
     * the corresponding input handler is expecting to see.
     */
    private static boolean headerOK( Header hdr ) {
        boolean ok = true;
        ByteArrayOutputStream bstrm = new ByteArrayOutputStream();
        for ( Iterator it = hdr.iterator(); it.hasNext(); ) {
            String card = ((HeaderCard) it.next()).toString();
            ok = ok && card.length() == 80;
            for ( int i = 0; i < card.length(); i++ ) {
                bstrm.write( (byte) card.charAt( i ) );
            }
        }
        return ok && FitsPlusTableBuilder.isMagic( bstrm.toByteArray() );
    }
}
