package uk.ac.starlink.votable;

import java.io.IOException;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.util.DataSource;

/**
 * Implementation of the <tt>TableBuilder</tt> interface which 
 * gets <tt>StarTable</tt>s from VOTable documents.
 *
 * @author   Mark Taylor (Starlink)
 */
public class VOTableBuilder implements TableBuilder {

    public StarTable makeStarTable( DataSource datsrc ) throws IOException {

        /* Try to get a VOTable object from this source. */
        VOTable votable;
        try {
            votable = new VOTable( datsrc, false );
        }

        /* If we have got a SAXException it's probably because it wasn't XML.  
         * Return null to indicate it wasn't our kind of input. */
        catch ( SAXException e ) {
            return null;
        }

        /* If we have got an IllegalArgumentException it probably wasn't
         * a VOTable. */
        catch ( IllegalArgumentException e ) {
            return null;
        }

        /* Find the first TABLE element within the VOTable.  This is a
         * short-term measure - in due course there should be some way
         * (XPath) of indicating which TABLE we want to look at. */
        Table tableEl = findTableElement( votable );

        /* If it's null, then we have a pathological VOTable. */
        if ( tableEl == null ) {
            throw new IOException( 
                "VOTable document contained no TABLE elements" );
        }

        /* Adapt the TABLE element to a StarTable. */
        return new VOStarTable( tableEl );
    }


    /**
     * Performs a (breadth-first) search to locate any descendents of the
     * given VOElement which are Table elements.
     *
     * @param  voel  a starting element
     * @return  a Table element which is a descendent of <tt>voel</tt>,
     *          or <tt>null</tt> if there isn't one
     */
    private static Table findTableElement( VOElement voel ) {

        /* Get all the RESOURCE children of this node. */
        VOElement[] children = voel.getChildrenByName( "RESOURCE" );

        /* Try to find a TABLE child of one of them. */
        for ( int i = 0; i < children.length; i++ ) {
            Table tab = (Table) children[ i ].getChildByName( "TABLE" );
            if ( tab != null ) {
                return tab;
            }
        }

        /* If we haven't succeeded, recurse into any RESOURCE children. */
        for ( int i = 0; i < children.length; i++ ) {
            Table tab = findTableElement( children[ i ] );
            if ( tab != null ) {
                return tab;
            }
        }

        /* No table elements at this level - return null. */
        return null;
    }
    
}
