package uk.ac.starlink.votable;

import java.io.IOException;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.SourceReader;

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
        DOMSource tableSrc = 
            findTableElement( (DOMSource) votable.getSource() );

        /* If it's null, then we have a pathological VOTable. */
        if ( tableSrc == null ) {
            throw new IOException( 
                "VOTable document contained no TABLE elements" );
        }

        /* Adapt the TABLE element to a StarTable. */
        return new VOStarTable( tableSrc );
    }


    /**
     * Performs a (breadth-first) search to locate any descendents of the
     * given VOElement which are Table elements.
     *
     * @param  vosrc  a starting element
     * @return  a source representing a Table element which is a 
     *          descendent of <tt>vosrc</tt>,
     *          or <tt>null</tt> if there isn't one
     */
    private static DOMSource findTableElement( DOMSource vosrc ) {
        Node vonode = vosrc.getNode();
        Element voel;
        if ( vonode instanceof Element ) {
            voel = (Element) vonode;
        }
        else if ( vonode instanceof Document ) {
            voel = ((Document) vonode).getDocumentElement();
        }
        else {
            throw new IllegalArgumentException( "Not an Element or Document" );
        }
        NodeList tables = voel.getElementsByTagName( "TABLE" );
        if ( tables.getLength() > 0 ) {
            return new DOMSource( tables.item( 0 ), vosrc.getSystemId() );
        }
        else {
            return null;
        }
    }
    
}
