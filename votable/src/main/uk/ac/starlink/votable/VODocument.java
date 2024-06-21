package uk.ac.starlink.votable;

import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.votable.dom.DelegatingAttr;
import uk.ac.starlink.votable.dom.DelegatingDocument;
import uk.ac.starlink.votable.dom.DelegatingElement;
import uk.ac.starlink.votable.dom.DelegatingNode;

/**
 * Document implementation which holds a VOTable-specific DOM.
 * The elements in it are all instances of {@link VOElement},
 * or of <code>VOElement</code> subclasses according to their element names,
 * that is any element with a tagname of "TABLE" in this DOM will be
 * an instance of the class {@link TableElement} and so on.
 *
 * @author   Mark Taylor (Starlink)
 * @since    13 Sep 2004
 */
public class VODocument extends DelegatingDocument {

    private final String systemId_;
    private final Map<String,Element> idMap_ = new HashMap<String,Element>();
    private final Namespacing namespacing_;
    private final IntMap elCountMap_ = new IntMap();
    private StoragePolicy storagePolicy_ = StoragePolicy.PREFER_MEMORY;
    private boolean strict_;

    /**
     * Constructs a VODocument based on a Document from an existing DOM.
     *
     * @param  base  document from the DOM implementation this uses as
     *         delegates
     * @param  systemId  system ID for the VOTable document represented 
     *         by this DOM (sometimes used for resolving URLs)
     * @param  strict  whether to enforce the VOTable standard strictly
     *         or in some cases do what is probably meant 
     *         (see {@link VOElementFactory#setStrict})
     */
    VODocument( Document base, String systemId, boolean strict ) {
        super( base, systemId );
        systemId_ = systemId;
        strict_ = strict;
        namespacing_ = Namespacing.getInstance();
    }

    /**
     * Constructs a new VODocument with a specified System ID.
     *
     * @param  systemId  system ID for the VOTable document represented by
     *         this DOM (sometimes used for resolving URLs) - may be null
     * @param  strict  whether to enforce the VOTable standard strictly
     *         or in some cases do what is probably meant 
     *         (see {@link VOElementFactory#setStrict})
     */
    public VODocument( String systemId, boolean strict ) {
        super( systemId );
        systemId_ = systemId;
        strict_ = strict;
        namespacing_ = Namespacing.getInstance();
    }

    /**
     * Constructs a new VODocument.
     * No system ID is registered, so that all URLs in the document will
     * be considered as absolute ones.  A default level of strictness is used.
     */
    public VODocument() {
        this( (String) null, VOElementFactory.isStrictByDefault() );
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
        return idMap_.get( elementId );
    }

    public DelegatingNode getDelegator( Node base ) {
        return super.getDelegator( base );
    }

    /**
     * Returns the number of elements of a given name
     * which have so far been added to this document.
     *
     * @param  voTagName  VOTable-domain tag name
     * @return  number of elements of that type
     */
    public int getElementCount( String voTagName ) {
        return elCountMap_.getValue( voTagName );
    }

    protected DelegatingElement createDelegatingElement( Element node ) {
        String tagName = getVOTagName( node );
        elCountMap_.incValue( tagName );
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
        else if ( "GROUP".equals( tagName ) ) {
            return new GroupElement( node, this );
        }
        else if ( "FIELDref".equals( tagName ) ) {
            return new FieldRefElement( node, this );
        }
        else if ( "PARAMref".equals( tagName ) ) {
            return new ParamRefElement( node, this );
        }
        else if ( "TIMESYS".equals( tagName ) ) {
            return new TimesysElement( node, this );
        }
        else {
            return new VOElement( node, this );
        }
    }

    protected DelegatingAttr createDelegatingAttr( Attr baseNode ) {
        return "ID".equals( baseNode.getName() )
             ? super.createDelegatingAttr( baseNode, true )
             : super.createDelegatingAttr( baseNode );
    }

    /**
     * Indicates whether this document enforces a strict reading of the
     * VOTable standard.
     *
     * @return   true if strictness is enforced
     * @see   VOElementFactory#setStrict
     */
    boolean isStrict() {
        return strict_;
    }

    /**
     * Returns the unqualified tag name for an element in the VOTable namespace,
     * taking care of namespacing issues.
     *
     * @param   el  element 
     * @return  unqualified VOTable tag name, e.g. "TABLE"
     */
    public String getVOTagName( Element el ) {
        return namespacing_.getVOTagName( el );
    }

    /**
     * Helper class that keeps track of an integer value for a number 
     * of string-valued keys.  If unset, a value is considered to be
     * equal to zero.
     */
    private static class IntMap {
        private final Map<String,int[]> map_ = new HashMap<String,int[]>();

        /**
         * Returns the value for a given key.
         *
         * @param  key  key
         * @return   value; zero if not previously set
         */
        int getValue( String key ) {
            return map_.containsKey( key ) ? map_.get( key )[ 0 ] : 0;
        }

        /**
         * Sets the value for a given key.
         *
         * @param  key  key 
         * @param  ival   new value
         */
        void putValue( String key, int ival ) {
            if ( ! map_.containsKey( key ) ) {
                map_.put( key, new int[ 1 ] );
            }
            map_.get( key )[ 0 ] = ival;
        }

        /**
         * Increments the value for a given key.
         *
         * @param  key  key
         */
        void incValue( String key ) {
            if ( ! map_.containsKey( key ) ) {
                map_.put( key, new int[ 1 ] );
            }
            map_.get( key )[ 0 ]++;
        }
    }
}
