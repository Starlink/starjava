package uk.ac.starlink.votable;

import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.votable.dom.DelegatingDocument;
import uk.ac.starlink.votable.dom.DelegatingElement;
import uk.ac.starlink.votable.dom.DelegatingNode;

/**
 * Document implementation which holds a VOTable-specific DOM.
 * The elements in it are all instances of {@link VOElement},
 * or of <tt>VOElement</tt> subclasses according to their element names.
 *
 * @author   Mark Taylor (Starlink)
 * @since    13 Sep 2004
 */
public class VODocument extends DelegatingDocument {

    private final String systemId_;
    private final Map idMap_ = new HashMap();
    private StoragePolicy storagePolicy_ = StoragePolicy.PREFER_MEMORY;

    /**
     * Constructs a VODocument based on a Document from an existing DOM.
     *
     * @param  base  document from the DOM implementation this uses as
     *         delegates
     * @param  systemId  system ID for the VOTable document represented 
     *         by this DOM (sometimes used for resolving URLs)
     */
    VODocument( Document base, String systemId ) {
        super( base );
        systemId_ = systemId;
    }

    /**
     * Returns the system ID associated with this document.
     *
     * @return   system ID if there is one
     */
    public String getSystemId() {
        return systemId_;
    }

    /**
     * Returns the storage policy used for storing bulk table data found
     * as elements in the DOM into a usable form.
     *
     * @return   current policy
     */
    public StoragePolicy getStoragePolicy() {
        return storagePolicy_;
    }

    /**
     * Sets the storage policy used for storing bulk table data found
     * as elements in the DOM into a usable form.
     * The default value is 
     * {@link uk.ac.starlink.table.StoragePolicy#PREFER_MEMORY}.
     *
     * @param  policy  new policy
     */
    public void setStoragePolicy( StoragePolicy policy ) {
        storagePolicy_ = policy;
    }

    /**
     * Stores an element as the referent of a given ID string.
     * This affects the return value of the DOM {@link #getElementById} 
     * method.
     */
    public void setElementId( Element el, String id ) {
        idMap_.put( id, el );
    }

    public Element getElementById( String elementId ) {
        return (Element) idMap_.get( elementId );
    }

    public DelegatingNode getDelegator( Node base ) {
        return super.getDelegator( base );
    }

    protected DelegatingElement createDelegatingElement( Element node ) {
        String tagName = node.getTagName();
        if ( "FIELD".equals( tagName ) ) {
            return new FieldElement( node, this );
        }
        else if ( "LINK".equals( tagName ) ) {
            return new LinkElement( node, this );
        }
        else if ( "PARAM".equals( tagName ) ) {
            return new ParamElement( node, this );
        }
        else if ( "TABLE".equals( tagName ) ) {
            return new TableElement( node, this );
        }
        else if ( "VALUES".equals( tagName ) ) {
            return new ValuesElement( node, this );
        }
        else {
            return new VOElement( node, this );
        }
    }

}
