package uk.ac.starlink.topcat.contrib.cds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX content handler which can make sense of a VizieR query for catalogues.
 * The stream expected is of the kind you get if you make a query like
 * <code>.../viz-bin/?votable&-meta</code> to vizier (as at Nov 2009).
 *
 * @author   Mark Taylor
 * @since    4 nov 2009
 */
public class CatalogSaxHandler extends DefaultHandler {

    private final StringBuffer txtbuf_;
    private final List catList_;
    private Entry entry_;

    /**
     * Constructor.
     */
    public CatalogSaxHandler() {
        txtbuf_ = new StringBuffer();
        catList_ = new ArrayList();
    }

    /**
     * Following a successful parse, this returns a list of the catalogues
     * represented in the stream.
     *
     * @return  catalogue list
     */
    public VizierCatalog[] getCatalogs() {
        return (VizierCatalog[]) catList_.toArray( new VizierCatalog[ 0 ] );
    }

    public void characters( char[] ch, int start, int length ) {
        txtbuf_.append( ch, start, length );
    }

    public void startElement( String uri, String localName, String qName,
                              Attributes atts ) {
        txtbuf_.setLength( 0 );
        String tagName = getTagName( uri, localName, qName );
        if ( "RESOURCE".equals( tagName ) ) {
            entry_ = new Entry();
            entry_.name_ = atts.getValue( "name" );
        }
        else if ( "INFO".equals( tagName ) ) {
            String name = atts.getValue( "name" );
            String value = atts.getValue( "value" );
            if ( entry_ != null && name != null && value != null ) {
                entry_.addInfo( name, value );
            }
        }
    }

    public void endElement( String uri, String localName, String qName ) {
        String tagName = getTagName( uri, localName, qName );
        if ( "RESOURCE".equals( tagName ) ) {
            catList_.add( createCatalog( entry_ ) );
            entry_ = null;
        }
        else if ( "DESCRIPTION".equals( tagName ) ) {
            if ( entry_ != null && txtbuf_.length() > 0 ) {
                entry_.description_ = txtbuf_.toString();
            }
        }
        txtbuf_.setLength( 0 );
    }

    /**
     * Returns the unadorned tag name.
     * Calling this means you don't have to worry about namespace issues.
     *
     * @param  uri   namespace URI
     * @param  localName  local name without prefix, or empty string
     * @param  qName  qualified name with prefix, or empty string
     */
    private static String getTagName( String uri, String localName,
                                      String qName ) {
        return qName == null ? localName : qName;
    }

    /**
     * Turns an Entry object into a VizierCatalog object.
     *
     * @param  entry  entry
     * @return  catalog
     */
    private static VizierCatalog createCatalog( Entry entry ) {
        return new VizierCatalog( entry.name_, entry.description_,
                                  entry.getIntValue( "-density" ),
                                  entry.getStringsValue( "-kw.Wavelength" ),
                                  entry.getStringsValue( "kw.Astronomy" ),
                                  entry.getIntValue( "ipopu" ),
                                  entry.getFloatValue( "cpopu" ) );
    }

    /**
     * Encapsulates what is known about the catalogue entry (corresponding
     * to a top-level RESOURCE element) currently being processed.
     */
    private static class Entry {
        private String name_;
        private String description_;
        private final List infoList_ = new ArrayList();

        /**
         * Adds a name/value pair.  Note that it may be called multiple times
         * with the same name.
         *
         * @param  name  info name
         * @param  value  info value
         */
        public void addInfo( String name, String value ) {
            infoList_.add( new String[] { name, value } );
        }

        /**
         * Returns the single value of an info with a given name,
         * in integer form.
         *
         * @param   name  info name
         * @return   integer value, or null
         */
        public Integer getIntValue( String name ) {
            for ( Iterator it = infoList_.iterator(); it.hasNext(); ) {
                String[] info = (String[]) it.next();
                if ( name.equals( info[ 0 ] ) ) {
                    try {
                        return new Integer( Integer.parseInt( info[ 1 ] ) );
                    }
                    catch ( NumberFormatException e ) {
                        return null;
                    }
                }
            }
            return null;
        }

        /**
         * Returns the single value of an info with a given name,
         * in floating point form.
         *
         * @param  name  info name
         * @return  float value, or null
         */
        public Float getFloatValue( String name ) {
            for ( Iterator it = infoList_.iterator(); it.hasNext(); ) {
                String[] info = (String[]) it.next();
                if ( name.equals( info[ 0 ] ) ) {
                    try {
                        return new Float( Float.parseFloat( info[ 1 ] ) );
                    }
                    catch ( NumberFormatException e ) {
                        return null;
                    }
                }
            }
            return null;
        }

        /**
         * Returns the list value of an infor with a given name,
         * as an array of strings.
         *
         * @param  name  info name
         * @param   value list, possibly with zero elements
         */
        public String[] getStringsValue( String name ) {
            List sList = new ArrayList();
            for ( Iterator it = infoList_.iterator(); it.hasNext(); ) {
                String[] info = (String[]) it.next();
                if ( name.equals( info[ 0 ] ) ) {
                    sList.add( info[ 1 ] );
                }
            }
            return (String[]) sList.toArray( new String[ 0 ] );
        }
    }
}

