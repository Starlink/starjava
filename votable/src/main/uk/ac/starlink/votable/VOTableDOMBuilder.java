package uk.ac.starlink.votable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.util.URLUtils;

/**
 * Custom DOM builder for parsing VOTable documents or fragments.
 * For the most part it builds a DOM, but within data-heavy elements
 * (those which represent table data) it intercepts SAX events 
 * directly to construct the table data
 * which it stores separately, rather than installing bulk data
 * as CDATA or Element nodes within the resulting DOM.
 * <p>
 * To access the {@link Table} elements which hold the data parsed by
 * this builder, use the static {@link #getData} method.
 * This accesses a weak hash map written to by this parser when it
 * reads the cell contents of a table - when the
 * DOM that gave rise to the data is no longer (strongly) reachable, the
 * table may be garbage collected.
 *
 * @author   Mark Taylor (Starlink)
 */
class VOTableDOMBuilder extends CustomDOMBuilder {

    private final ContentHandler basicHandler;
    private final ContentHandler defaultHandler;
    private final static Map tableDataMap = new WeakHashMap();
    private String systemId;
    private Element tableEl;
    private List fieldList;

    /**
     * Constructs a new builder.
     */
    public VOTableDOMBuilder() {
        basicHandler = new BasicContentHandler();
        defaultHandler = new DefaultContentHandler();
        setCustomHandler( basicHandler );
    }

    /**
     * Returns the cell data object corresponding to a given TABLE element.
     *
     * @param  tableEl  a DOM Element constructed by this builder for a
     *         TABLE element of the XML document
     * @return  the <tt>TabularData</tt> object which holds the data for 
     *          <tt>tableEl</tt>
     *          if one has been built, or <tt>null</tt> if it hasn't for 
     *          any reason
     */
    public static TabularData getData( Element tableEl ) {
        return (TabularData) tableDataMap.get( tableEl );
    }

    /**
     * Stores a cell data object corresponding to a given TABLE element.
     *
     * @param  tableEl  a DOM element constructed by this builder for a 
     *         TABLE element of the XML document
     * @param  tdata  the data object containing the cell data for 
     *         <tt>tableEl</tt>
     */
    private void storeData( Element tableEl, TabularData tdata ) {
        tableDataMap.put( tableEl, tdata );
    }

    /**
     * Returns the name of an element as a normal string like "TABLE" in
     * the VOTable namespace, given the various name items that 
     * SAX provides for a start/end element event. 
     * 
     * @param  namespaceURI  namespaceURI
     * @param  localName   local name
     * @param  qName   qualified name
     */
    private String getTagName( String namespaceURI, String localName,
                               String qName ) {
        if ( localName != null && localName.length() > 0 ) {
            return localName;
        }
        else {
            return qName;
        }
    }

    /**
     * Returns the value of an attribute.
     *
     * @param  atts  attribute set
     * @param  name  normal VOTable name of the attribute
     * @return  value of attribute <tt>name</tt> or null if it doesn't exist
     */
    private String getAttribute( Attributes atts, String name ) {
        String val = atts.getValue( name );
        return val != null ? val : atts.getValue( "", name );
    }

    /**
     * Returns an array of the Decoder objects for the current table.
     *
     * @return  array of decoders for current table
     */
    private Decoder[] getDecoders() {
        int ncol = fieldList.size();
        Decoder[] decoders = new Decoder[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            DOMSource dsrc = new DOMSource( (Element) fieldList.get( icol ), 
                                            systemId );
            decoders[ icol ] = new Field( dsrc ).getDecoder();
        }
        return decoders;
    }


    /**
     * Marshalling handler - watches for elements which we know how to
     * do custom processing on, and installs the appropriate custom 
     * handler on start element events.
     */
    class BasicContentHandler extends DefaultContentHandler {

        public void startDocument() throws SAXException {
            super.startDocument();
            systemId = getLocator().getSystemId();
        }

        public void startElement( String namespaceURI, String localName,
                                  String qName, Attributes atts ) 
                throws SAXException {
            super.startElement( namespaceURI, localName, qName, atts );
            String tagName = getTagName( namespaceURI, localName, qName );
            if ( "TABLE".equals( tagName ) ) {
                tableEl = (Element) getNewestNode();
                fieldList = new ArrayList();
            }
            else if ( fieldList != null && "FIELD".equals( tagName ) ) {
                fieldList.add( getNewestNode() );
            }
            else if ( "TABLEDATA".equals( tagName ) ) {
                setCustomHandler( new TabledataHandler() );
            }
            else if ( "STREAM".equals( tagName ) ) {
                String parentName = ((Element) getNewestNode().getParentNode())
                                   .getTagName();
                String href = getAttribute( atts, "href" );
                try {
                    if ( parentName.equals( "BINARY" ) ) {
                        if ( href != null ) {
                            setCustomHandler( 
                                new HrefBinaryStreamHandler( atts ) );
                        }
                        else {
                            setCustomHandler( new InlineBinaryStreamHandler() );
                        }
                    }
                    else if ( parentName.equals( "FITS" ) ) {
                        if ( href != null ) {
                            setCustomHandler( 
                                new HrefFITSStreamHandler( atts ) );
                        }
                        else {
                            setCustomHandler( 
                                new InlineFITSStreamHandler( atts ) );
                        }
                    }
                }
                catch ( IOException e ) {
                    throw (SAXException)
                          new SAXException( "Trouble setting up data handler",
                                            e ).initCause( e );
                }
            }
        }

        public void endElement( String namespaceURI, String localName,
                                String qName ) throws SAXException {
            super.endElement( namespaceURI, localName, qName );
            String tagName = getTagName( namespaceURI, localName, qName );
            if ( "TABLE".equals( tagName ) ) {
                tableEl = null;
                fieldList = null;
            }
        }
    }

    /**
     * Custom handler for TABLEDATA element.
     */
    class TabledataHandler extends NullContentHandler {
        final Decoder[] decoders;
        final int ncol;
        StringBuffer cell;
        Object[] row;
        int icol;
        boolean inCell;
        List rows = new ArrayList();

        TabledataHandler() {
            decoders = getDecoders();
            ncol = decoders.length;
            rows = new ArrayList();
            cell = new StringBuffer();
            Element tabledataEl = (Element) getNewestNode();
            String comment = "Invisible data nodes were parsed directly";
            tabledataEl.appendChild( tabledataEl.getOwnerDocument()
                                                .createComment( comment ) );
        }

        public void startElement( String namespaceURI, String localName,
                                  String qName, Attributes atts ) {
            String tagName = getTagName( namespaceURI, localName, qName );
            if ( "TD".equals( tagName ) ) {
                cell.setLength( 0 );
                inCell = true;
            }
            else if ( "TR".equals( tagName ) ) {
                row = new Object[ ncol ];
                icol = 0;
            }
        }

        public void characters( char[] ch, int start, int length ) {
            if ( inCell ) {
                cell.append( ch, start, length );
            }
        }

        public void ignorableWhitespace( char[] ch, int start, int length ) {
            if ( inCell ) {
                cell.append( ch, start, length );
            }
        }

        public void endElement( String namespaceURI, String localName,
                                String qName ) throws SAXException {
            String tagName = getTagName( namespaceURI, localName, qName );
            if ( "TD".equals( tagName ) && icol < ncol ) {
                row[ icol ] = cell.length() > 0 
                            ? decoders[ icol ].decodeString( cell.toString() )
                            : null;
                icol++;
                inCell = false;
            }
            else if ( "TR".equals( tagName ) ) {
                rows.add( row );
            }
            if ( "TABLEDATA".equals( tagName ) ) {
                defaultHandler.endElement( namespaceURI, localName, qName );
                setCustomHandler( basicHandler );
                Class[] classes = new Class[ ncol ];
                for ( int icol = 0; icol < ncol; icol++ ) {
                    classes[ icol ] = decoders[ icol ].getContentClass();
                }
                TabularData tdata = 
                    new TableBodies.RowListTabularData( classes, rows );
                storeData( tableEl, tdata );
            }
        }
    }

    /**
     * Custom handler for STREAM child of a BINARY element which has
     * an href to an external data stream.
     */
    class HrefBinaryStreamHandler extends NullContentHandler {

        HrefBinaryStreamHandler( Attributes atts ) {
            URL url = URLUtils.makeURL( systemId, 
                                        getAttribute( atts, "href" ) );
            String encoding = getAttribute( atts, "encoding" );
            TabularData tdata = 
                new TableBodies.HrefBinaryTabularData( getDecoders(), url, 
                                                       encoding );
            storeData( tableEl, tdata );
        }

        public void endElement( String namespaceURI, String localName,
                                String qName ) throws SAXException {
            String tagName = getTagName( namespaceURI, localName, qName );
            if ( "STREAM".equals( tagName ) ) {
                defaultHandler.endElement( namespaceURI, localName, qName );
                setCustomHandler( basicHandler );
            }
        }
    }

    /**
     * Custom handler for STREAM child of a FITS element which has
     * an href to an external data stream.
     */
    class HrefFITSStreamHandler extends NullContentHandler {

        HrefFITSStreamHandler( Attributes atts ) throws IOException {
            URL url = URLUtils.makeURL( systemId,
                                        getAttribute( atts, "href" ) );
            DataSource datsrc = new URLDataSource( url );
            datsrc.setPosition( getAttribute( atts, "extnum" ) );
            StarTable startab = new FitsTableBuilder()
                               .makeStarTable( datsrc, false );
            TabularData tdata = new TableBodies.StarTableTabularData( startab );
            storeData( tableEl, tdata );
        }

        public void endElement( String namespaceURI, String localName,
                                String qName ) throws SAXException {
            String tagName = getTagName( namespaceURI, localName, qName );
            if ( "STREAM".equals( tagName ) ) {
                defaultHandler.endElement( namespaceURI, localName, qName );
                setCustomHandler( basicHandler );
            }
        }
    }

    /**
     * Custom handler for a STREAM child of a BINARY element which has
     * data base64-encoded inline.
     */
    class InlineBinaryStreamHandler extends NullContentHandler {

        TableBodies.InlineBinaryTabularData tdata;
        Writer out;

        public InlineBinaryStreamHandler() throws IOException {

            /* Set up a stream we can write encountered characters into. */
            PipedOutputStream b64out = new PipedOutputStream();
            tdata = new TableBodies.InlineBinaryTabularData( getDecoders(),
                                                             b64out );
            out = new OutputStreamWriter( new BufferedOutputStream( b64out ) );
        }

        public void startElement( String namespaceURI, String localName,
                                  String qName, Attributes atts )
                throws SAXException {
            String tagName = getTagName( namespaceURI, localName, qName );
            throw new SAXParseException( "Unwelcome child <" + tagName +
                                         "> of STREAM", getLocator() );
        }

        public void characters( char[] ch, int start, int length )
                throws SAXException {
            try {
                out.write( ch, start, length );
            }
            catch ( IOException e ) {
                throw (SAXException)
                      new SAXParseException( e.getMessage(), getLocator(), e )
                     .initCause( e );
            }
        }

        public void endElement( String namespaceURI, String localName,
                                String qName ) throws SAXException {
            String tagName = getTagName( namespaceURI, localName, qName );
            if ( "STREAM".equals( tagName ) ) {
                defaultHandler.endElement( namespaceURI, localName, qName );
                setCustomHandler( basicHandler );

                /* Tidy up, wait for the reader to finish reading, and
                 * rethrow any exceptions that we encounter in either thread. */
                try {
                    out.close();
                    tdata.finishReading();
                }
                catch ( IOException e ) {
                    throw (SAXException)
                          new SAXParseException( e.getMessage(), getLocator(),
                                                 e ).initCause( e );
                }

                /* If OK so far, associate a ContentData object based on
                 * the data we've read with this table. */
                storeData( tableEl, tdata );
            }
        }
    }

    /**
     * Custom handler for a STREAM child of a FITS element which has
     * data base64-encoded inline.
     */
    class InlineFITSStreamHandler extends NullContentHandler {

        TableBodies.InlineFITSTabularData tdata;
        Writer out;

        public InlineFITSStreamHandler( Attributes atts ) throws IOException {
            String extnum = getAttribute( atts, "extnum" );
            if ( extnum != null && ! extnum.matches( "[0-9]+" ) ) {
                extnum = null;
            }

            /* Set up a stream we can write encountered characters to. */
            PipedOutputStream b64out = new PipedOutputStream();
            tdata = new TableBodies.InlineFITSTabularData( b64out, extnum );
            out = new OutputStreamWriter( new BufferedOutputStream( b64out ) );
        }

        public void startElement( String namespaceURI, String localName,
                                  String qName, Attributes atts )
                throws SAXException {
            String tagName = getTagName( namespaceURI, localName, qName );
            throw new SAXParseException( "Unwelcome child <" + tagName +
                                         "> of STREAM", getLocator() );
        }

        public void characters( char[] ch, int start, int length )
                throws SAXException {
            try {
                out.write( ch, start, length );
            }
            catch ( IOException e ) {
                throw (SAXParseException)
                      new SAXParseException( e.getMessage(), getLocator(), e )
                     .initCause( e );
            }
        }

        public void endElement( String namespaceURI, String localName,
                                String qName ) throws SAXException {
            String tagName = getTagName( namespaceURI, localName, qName );
            if ( "STREAM".equals( tagName ) ) {
                defaultHandler.endElement( namespaceURI, localName, qName );
                setCustomHandler( basicHandler );

                /* Tidy up, wait for the reader to finish reading, and
                 * rethrow any exceptions that we encounter in either thread. */
                try {
                    out.close();
                    tdata.finishReading();
                }
                catch ( IOException e ) {
                    throw (SAXException)
                          new SAXParseException( e.getMessage(), getLocator(),
                                                 e ).initCause( e );
                }
                storeData( tableEl, tdata );
            }
        }
    }
}
