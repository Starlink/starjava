package uk.ac.starlink.topcat.vizier;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX content handler which can make sense of a VizieR query for catalogues.
 * The stream expected is of the kind you get if you make a query like
 * <code>.../viz-bin/?votable&amp;-meta</code> to vizier (as at Nov 2009).
 * May 2010: <code>.../viz-bin?votable&amp;-meta=t</code> gives information
 * about sub-tables too.
 *
 * @author   Mark Taylor
 * @since    4 nov 2009
 */
public abstract class CatalogSaxHandler extends DefaultHandler {

    private final StringBuffer txtbuf_;
    private final boolean includeObsolete_;
    private final Stack<String> elStack_;
    private Entry entry_;
    private SubTable subTable_;

    /**
     * Constructor.
     *
     * @param  includeObsolete  true to include all results, false to omit
     *         older versions of the same catalog
     */
    public CatalogSaxHandler( boolean includeObsolete ) {
        includeObsolete_ = includeObsolete;
        elStack_ = new Stack<String>();
        txtbuf_ = new StringBuffer();
    }

    /**
     * Called when a catalogue has been obtained from the SAX stream.
     *
     * @param  cat  newly acquired catlogue
     */
    protected abstract void gotCatalog( VizierCatalog cat ) throws SAXException;

    public void characters( char[] ch, int start, int length ) {
        txtbuf_.append( ch, start, length );
    }

    public void startElement( String uri, String localName, String qName,
                              Attributes atts ) {
        txtbuf_.setLength( 0 );
        String tagName = getTagName( uri, localName, qName );
        if ( "RESOURCE".equals( tagName ) ) {

            /* Ignore all but the outermost RESOURCE elements.
             * This works around a (temporary?) hacky change in VizieR output
             * that occurred in July 2012. */
            if ( ! elStack_.contains( tagName ) ) {
                entry_ = new Entry();
                entry_.name_ = atts.getValue( "name" );
            }
        }
        else if ( "INFO".equals( tagName ) ) {
            String name = atts.getValue( "name" );
            String value = atts.getValue( "value" );
            if ( entry_ != null && name != null && value != null ) {
                entry_.addInfo( name, value );
            }
        }
        else if ( "TABLE".equals( tagName ) && entry_ != null ) {
            subTable_ = new SubTable();
            subTable_.name_ = atts.getValue( "name" );
            String nr = atts.getValue( "nrows" );
            if ( nr != null ) {
                try {
                    subTable_.nrows_ = Long.valueOf( nr );
                }
                catch ( NumberFormatException e ) {
                }
            }
        }
        elStack_.push( tagName );
    }

    public void endElement( String uri, String localName, String qName )
            throws SAXException {
        String tagName = getTagName( uri, localName, qName );
        assert tagName.equals( elStack_.peek() );
        elStack_.pop();
        if ( "RESOURCE".equals( tagName ) ) {

            /* Ignore all but the outermost RESOURCE elements.  See above. */
            if ( ! elStack_.contains( tagName ) ) {
                String status = entry_.getStringValue( "status" );
                if ( includeObsolete_ || ! "obsolete".equals( status ) ) {
                    VizierCatalog[] cats = createCatalogs( entry_ );
                    for ( int ic = 0; ic < cats.length; ic++ ) {
                        gotCatalog( cats[ ic ] );
                    }
                }
                entry_ = null;
            }
        }
        else if ( "TABLE".equals( tagName ) ) {
            entry_.tableList_.add( subTable_ );
            subTable_ = null;
        }
        else if ( "DESCRIPTION".equals( tagName ) ) {
            if ( txtbuf_.length() > 0 ) {
                String desc = txtbuf_.toString().trim();
                if ( "TABLE".equals( elStack_.peek() ) ) {
                    subTable_.description_ = desc;
                }
                else if ( "RESOURCE".equals( elStack_.peek() ) ) {
                    entry_.description_ = desc;
                }
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
     * Turns an Entry object into one or more VizierCatalog objects.
     *
     * @param  entry  entry
     * @return  catalog array
     */
    private static VizierCatalog[] createCatalogs( Entry entry ) {
        String name = entry.name_;
        String desc = entry.description_;
        Integer density = entry.getIntValue( "-density" );
        String[] waves = entry.getStringsValue( "-kw.Wavelength" );
        String[] asts = entry.getStringsValue( "-kw.Astronomy" );
        Integer cpopu = entry.getIntValue( "cpopu" );
        Float ipopu = entry.getFloatValue( "ipopu" );
        SubTable[] subTables = entry.tableList_.toArray( new SubTable[ 0 ] );
        List<VizierCatalog> catList = new ArrayList<VizierCatalog>();
        int nsub = subTables.length;
        if ( nsub == 0 ) {
            catList.add( new VizierCatalog( name, desc, density,
                                            waves, asts, cpopu, ipopu ) );
        }
        else if ( nsub == 1 ) {
            VizierCatalog cat =
                new VizierCatalog( name, desc, density,
                                   waves, asts, cpopu, ipopu );
            cat.setTableCount( 1 );
            cat.setRowCount( subTables[ 0 ].nrows_ );
            catList.add( cat );
        }
        else {
            VizierCatalog rcat = 
                new VizierCatalog( name, desc + " (" + nsub + " tables)",
                                   density, waves, asts, cpopu, ipopu );
            rcat.setTableCount( nsub );
            catList.add( rcat );
            for ( int is = 0; is < nsub; is++ ) {
                SubTable sub = subTables[ is ];
                VizierCatalog scat =
                    new VizierCatalog( sub.name_,
                                       desc + " (" + sub.description_ + ")",
                                       density, waves, asts, cpopu, ipopu );
                scat.setRowCount( sub.nrows_ );
                catList.add( scat );
            }
        }
        return catList.toArray( new VizierCatalog[ 0 ] );
    }

    /**
     * Encapsulates what is known about the catalogue entry (corresponding
     * to a top-level RESOURCE element) currently being processed.
     */
    private static class Entry {
        private String name_;
        private String description_;
        private final List<String[]> infoList_ = new ArrayList<String[]>();
        private final List<SubTable> tableList_ = new ArrayList<SubTable>();

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
            for ( String[] info : infoList_ ) {
                if ( name.equals( info[ 0 ] ) ) {
                    try {
                        return Integer.valueOf( info[ 1 ] );
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
            for ( String[] info : infoList_ ) {
                if ( name.equals( info[ 0 ] ) ) {
                    try {
                        return Float.valueOf( info[ 1 ] );
                    }
                    catch ( NumberFormatException e ) {
                        return null;
                    }
                }
            }
            return null;
        }

        /**
         * Returns the single value of a in info with a given name,
         * in string form.
         *
         * @param  name  info name
         * @return  info value
         */
        public String getStringValue( String name ) {
            for ( String[] info : infoList_ ) {
                if ( name.equals( info[ 0 ] ) ) {
                    return info[ 1 ];
                }
            }
            return null;
        }

        /**
         * Returns the list value of an info with a given name,
         * as an array of strings.
         *
         * @param  name  info name
         * @param   value list, possibly with zero elements
         */
        public String[] getStringsValue( String name ) {
            List<String> sList = new ArrayList<String>();
            for ( String[] info : infoList_ ) {
                if ( name.equals( info[ 0 ] ) ) {
                    sList.add( info[ 1 ] );
                }
            }
            return sList.toArray( new String[ 0 ] );
        }
    }

    /**
     * Encapsulates what is known about a sub table of a catalogue entry
     * (corresponding to a RESOURCE/TABLE element).
     */
    private static class SubTable {
        private String name_;
        private String description_;
        private Long nrows_;
    }
}
