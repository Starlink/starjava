package uk.ac.starlink.votable;

import java.io.IOException;
import uk.ac.starlink.fits.CardFactory;
import uk.ac.starlink.fits.CardImage;
import uk.ac.starlink.fits.FitsTableSerializer;
import uk.ac.starlink.fits.StandardFitsTableSerializer;
import uk.ac.starlink.fits.WideFits;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.formats.DocumentedIOHandler;

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
public class FitsPlusTableWriter extends VOTableFitsTableWriter
                                 implements DocumentedIOHandler {

    /**
     * Default constructor.
     */
    public FitsPlusTableWriter() {
        super( "fits-plus" );
    }

    /**
     * Deprecated custom constructor.
     *
     * @deprecated  allows some configuration options but not others;
     *              use no-arg constructor and configuration methods instead
     */
    @Deprecated
    public FitsPlusTableWriter( String name, WideFits wide ) {
        this();
        setWide( wide );
    }

    public String[] getExtensions() {
        return new String[] { "fit", "fits", "fts" };
    }

    public boolean looksLikeFile( String location ) {
        return DocumentedIOHandler.matchesExtension( this, location );
    }

    public boolean docIncludesExample() {
        return false;
    }

    public String getXmlDescription() {
        return readText( "/uk/ac/starlink/fits/FitsTableWriter.xml" );
    }

    protected CardImage[] getCustomPrimaryHeaderCards() {
        return new CardImage[] {
            CardFactory.STRICT
           .createLogicalCard( "VOTMETA", true,
                               "Table metadata in VOTable format" ),
        };
    }

    @Override
    protected boolean isMagic( int icard, String key, Object value ) {
        switch ( icard ) {
            case 4:
                return "VOTMETA".equals( key ) && Boolean.TRUE.equals( value );
            default:
                return super.isMagic( icard, key, value );
        }
    }

    protected FitsTableSerializer createSerializer( StarTable table ) 
            throws IOException {
        return new StandardFitsTableSerializer( getConfig(), table );
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
        w1.setFormatName( "fits" );
        return new StarTableWriter[] { w1, w2 };
    }
}
