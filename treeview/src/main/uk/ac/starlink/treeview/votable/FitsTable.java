package uk.ac.starlink.treeview.votable;

import java.io.IOException;
import java.util.NoSuchElementException;
import javax.xml.transform.dom.DOMSource;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.TableHDU;
import org.w3c.dom.Element;
import uk.ac.starlink.util.DOMUtils;

/**
 * Implements the VOTable TABLE element whose child is a FITS element.
 * This represents table data held in an external FITS BINTABLE extension.
 */
class FitsTable extends Table {

    private int nrows;
    private int ncols;
    private int irow = 0;
    private TableHDU tabhdu;

    FitsTable( Element tableEl, Element fitsEl, String systemId ) 
            throws VOTableFormatException {
        super( tableEl );
        ncols = getNumColumns();

        /* Get the location of the FITS data. */
        Element streamEl = DOMUtils.getChildElementByName( fitsEl, "STREAM" );
        Stream stream = new Stream( new DOMSource( streamEl, systemId ) );

        /* Get the extension number in which the BINTABLE is stored. 
         * We assume 1 if none is specified - it can't be zero since 
         * that must be a SIMPLE not BINTABLE HDU. */
        int extNum;
        if ( fitsEl.hasAttribute( "extnum" ) ) {
            extNum = Integer.parseInt( fitsEl.getAttribute( "extnum" ) );
        }
        else {
            extNum = 1;
        }

        /* Get the HDU object containing table data. */
        BasicHDU hdu;
        try {
            Fits fits = new Fits( stream.getInputStream() );
            hdu = fits.getHDU( extNum );
        }
        catch ( IOException e ) {
            throw new VOTableFormatException( e );
        }
        catch ( FitsException e ) {
            throw new VOTableFormatException( e );
        }

        if ( hdu == null ) {
            throw new VOTableFormatException( 
                "HDU#" + extNum + " does not exist in " + stream );
        }
        if ( ! ( hdu instanceof TableHDU ) ) {
            throw new VOTableFormatException( 
                "HDU#" + extNum + " in " + stream + " " + hdu + 
                " is not a table HDU" );
        }

        /* Check this looks like the table we are looking for. */
        tabhdu = (TableHDU) hdu;
        if ( tabhdu.getNCols() != ncols ) {
            throw new VOTableFormatException(
                "Number of columns in FITS table does not match number in " +
                "TABLE element (" + tabhdu.getNCols()  + "<>" + ncols );
        }
        nrows = tabhdu.getNRows();

    }

    public int getNumRows() {
        return nrows;
    }

    public Object[] nextRow() {
        if ( irow >= nrows ) {
            throw new NoSuchElementException();
        }
        Object[] rowContents = new Object[ ncols ];
        try {
            for ( int icol = 0; icol < ncols; icol++ ) {
                Object cell = tabhdu.getElement( irow, icol );
                rowContents[ icol ] = getField( icol ).getDatatype()
                                     .decodeArrayOfArrays( cell );
            }
        }
        catch ( FitsException e ) {
            throw new AssertionError( "Wrong number of columns or rows??" );
        }
        irow++;
        return rowContents;
    }

    public boolean hasNextRow() {
        return irow < nrows;
    }
 
}
