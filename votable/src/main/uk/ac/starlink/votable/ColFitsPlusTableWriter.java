package uk.ac.starlink.votable;

import java.io.IOException;
import uk.ac.starlink.fits.CardFactory;
import uk.ac.starlink.fits.CardImage;
import uk.ac.starlink.fits.ColFitsTableSerializer;
import uk.ac.starlink.fits.FitsTableSerializer;
import uk.ac.starlink.fits.WideFits;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.formats.DocumentedIOHandler;

/**
 * Handles writing of a <code>StarTable</code> in a column-oriented
 * FITS binary table format.
 * The table data is stored in a BINTABLE extension which has a single row;
 * each cell in this row contains the data for an entire column of the
 * represented table.  The primary HDU is a byte array containing a
 * VOTable representation of the table metadata, as for 
 * {@link FitsPlusTableWriter}.
 *
 * <p>This rather specialised format may provide good performance for
 * certain operations on very large, especially very wide, tables.
 * Although it is FITS and can therefore be used in principle for data
 * interchange, in practice most non-STIL processors are unlikely to
 * be able to do much useful with it.
 *
 * @deprecated  Use {@link UnifiedFitsTableWriter} instead
 *
 * @author   Mark Taylor
 * @since    21 Jun 2006
 */
@Deprecated
public class ColFitsPlusTableWriter extends VOTableFitsTableWriter
                                    implements DocumentedIOHandler {

    /**
     * Default constructor.
     */
    public ColFitsPlusTableWriter() {
        super( "colfits-plus" );
    }

    /**
     * Deprecated custom constructor.
     *
     * @deprecated  allows some configuration options but not others;
     *              use no-arg constructor and configuration methods instead
     */
    @Deprecated
    public ColFitsPlusTableWriter( String name, WideFits wide ) {
        this();
        setFormatName( name );
        setWide( wide );
    }

    public String[] getExtensions() {
        return new String[] { "colfits" };
    }

    public boolean looksLikeFile( String location ) {
        return DocumentedIOHandler.matchesExtension( this, location );
    }

    public boolean docIncludesExample() {
        return false;
    }

    public String getXmlDescription() {
        return readText( "/uk/ac/starlink/fits/ColFitsTableWriter.xml" );
    }

    protected CardImage[] getCustomPrimaryHeaderCards() {
        CardFactory cf = CardFactory.STRICT;
        return new CardImage[] {
            cf.createLogicalCard( "COLFITS", true,
                                  "Table extension stored column-oriented" ),
            cf.createLogicalCard( "VOTMETA", true,
                                  "Table metadata in VOTable format" ),
        };
    }

    @Override
    protected boolean isMagic( int icard, String key, Object value ) {
        switch ( icard ) {
            case 4:
                return "COLFITS".equals( key ) && Boolean.TRUE.equals( value );
            case 5:
                return "VOTMETA".equals( key );
            default:
                return super.isMagic( icard, key, value );
        }
    }

    protected FitsTableSerializer createSerializer( StarTable table )
            throws IOException {
        return new ColFitsTableSerializer( getConfig(), table );
    }
}
