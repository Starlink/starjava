package uk.ac.starlink.votable;

import java.io.IOException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Element;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.DataSource;

/**
 * Implements the VOTable TABLE element whose child is a FITS element.
 * This represents table data held in an external FITS TABLE/BINTABLE extension.
 *
 * @author   Mark Taylor (Starlink)
 */
class FitsTable extends Table {

    private final StarTable starTable;
    private final int nrows;
    private RowSequence rseq;

    FitsTable( Source xsrc, StarTable starTable ) 
            throws VOTableFormatException {
        super( xsrc );
        this.starTable = starTable;

        /* Check this looks like the table we are looking for. */
        if ( getColumnCount() != starTable.getColumnCount() ) {
            throw new VOTableFormatException(
                "Number of columns in FITS table does not match number in " +
                "TABLE element (" + starTable.getColumnCount()  + " != " + 
                getColumnCount() );
        }

        /* Store the number of rows. */
        long nr = starTable.getRowCount();
        nrows = ( nr >= 0 && nr < Integer.MAX_VALUE ) ? ((int) nr) : -1;
    }

    public int getRowCount() {
        return nrows;
    }

    public boolean hasNextRow() {
        try {
            return getRowSequence().hasNext();
        }
        catch ( IOException e ) {
            e.printStackTrace();
            return false;
        }
    }

    public Object[] nextRow() throws IOException {
        RowSequence rseq = getRowSequence();
        rseq.next();
        return rseq.getRow();
    }

    private RowSequence getRowSequence() throws IOException {
        if ( rseq == null ) {
            rseq = starTable.getRowSequence();
        }
        return rseq;
    }

    public static FitsTable makeFitsTable( Element tableEl, Element fitsEl,
                                           String systemId )
            throws VOTableFormatException {

        /* Get the location of the FITS data. */
        Element streamEl = DOMUtils.getChildElementByName( fitsEl, "STREAM" );
        Stream stream = new Stream( new DOMSource( streamEl, systemId ) );
        DataSource datsrc = null;
        try {

            /* Make a DataSource containing the stream's data. */
            datsrc = stream.getDataSource();

            /* If we have information about which HDU the data is stored in,
             * update the DataSource with this. */
            if ( fitsEl.hasAttribute( "extnum" ) ) {
                datsrc.setPosition( fitsEl.getAttribute( "extnum" ) );
            }

            /* Make a FitsStarTable from the data source. */
            StarTable starTable =
                new FitsTableBuilder().makeStarTable( datsrc );

            /* Construct a table object from the startable. */
            Source xsrc = new DOMSource( tableEl, systemId );
            return starTable.isRandom() 
                       ? new RandomFitsTable( xsrc, starTable )
                       : new FitsTable( xsrc, starTable );
        }
        catch ( IOException e ) {
            String name = ( datsrc == null ) ? "FITS file" : datsrc.getName();
            throw new VOTableFormatException( "Failed to read " + name, e );
        }
    }

    private static class RandomFitsTable extends FitsTable 
                                         implements RandomTable {

        private final StarTable starTable;

        RandomFitsTable( Source xsrc, StarTable starTable )
                throws VOTableFormatException {
            super( xsrc, starTable );
            this.starTable = starTable;
        }
        
        public Object getCell( int irow, int icol ) throws IOException {
            return starTable.getCell( irow, icol );
        }

        public Object[] getRow( int irow ) throws IOException {
            return starTable.getRow( irow );
        }

    }
   
}
