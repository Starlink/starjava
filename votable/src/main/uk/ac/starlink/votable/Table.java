package uk.ac.starlink.votable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.util.URLUtils;

/**
 * An object representing the TABLE element of a VOTable.
 * This contains fields, links and rows; the actual data from the table
 * body may be obtained using the {@link #getData} method.
 * Note that depending on exactly how this element was obtained,
 * the nodes bearing the bulk data (e.g. text content of a &lt;STREAM&gt;
 * element or the &lt;TR&gt; children of a &lt;TABLEDATA&gt; element)
 * may not actually be available from this node - for efficiency
 * the VOTable parser may convert them into a {@link TabularData} object
 * and discard the content of the original (STREAM or TABLEDATA) nodes 
 * which contained the data from the DOM.
 *
 * @author   Mark Taylor (Starlink)
 */
public class Table extends VOElement {

    private final Field[] fields;
    private final Link[] links;
    private final TabularData tdata;

    private final static Logger logger = 
        Logger.getLogger( "uk.ac.starlink.votable" );

    public Table( Source xsrc ) throws TransformerException {
        this( transformToDOM( xsrc ) );
    }

    /**
     * Constructs a Table object from a TABLE element.
     *
     * @param   dsrc  DOM Source containing a TABLE element
     */
    public Table( DOMSource dsrc ) {
        super( dsrc, "TABLE" );

        /* Deal with known metadata children. */
        Element tableEl = getElement();
        List fieldList = new ArrayList();
        List linkList = new ArrayList();
        for ( Node nd = tableEl.getFirstChild(); nd != null;
              nd = nd.getNextSibling() ) {
            if ( nd instanceof Element ) {
                Element el = (Element) nd;
                String elname = el.getTagName();
                if ( elname.equals( "FIELD" ) ) {
                    Field field = 
                        new Field( new DOMSource( el, getSystemId() ) );
                    fieldList.add( field );
                }
                if ( elname.equals( "LINK" ) ) {
                    Link link =
                        new Link( new DOMSource( el, getSystemId() ) );
                    linkList.add( link );
                }
            }
        }
        fields = (Field[]) fieldList.toArray( new Field[ 0 ] );
        links = (Link[]) linkList.toArray( new Link[ 0 ] );

        /* Obtain and store the data access object. */ 
        TabularData td;
        try {
            td = getTabularData( tableEl, fields, getSystemId() );
        }
        catch ( IOException e ) {
            int ncol = fields.length;
            Class[] classes = new Class[ ncol ];
            for ( int icol = 0; icol < ncol; icol++ ) {
                classes[ icol ] = fields[ icol ].getDecoder().getContentClass();
            }
            logger.warning( "Error reading table data: " + e );
            td = new TableBodies.EmptyTabularData( classes );
        }
        tdata = td;
    }

    /**
     * Returns the number of columns in this table.
     *
     * @return  the number of columns
     */
    public int getColumnCount() {
        return tdata.getColumnCount();
    }

    /**
     * Returns the number of rows in this table.
     * If this cannot be determined, or cannot be determined efficiently,
     * the value -1 may be returned.
     *
     * @return  the number of rows, or -1 if unknown
     */
    public long getRowCount() {
        return tdata.getRowCount();
    }

    /**
     * Returns an object which can be used to access the actual cell
     * data in the body of this table.
     *
     * @return   bulk data access object
     */
    public TabularData getData() {
        return tdata;
    }

    /**
     * Returns one of the Field objects associated with this table.
     *
     * @param  index  the index of the field to return 
     * @return  the filed at index <tt>index</tt>
     * @throws  IndexOutOfBoundsException unless 0&lt;=index&lt;numColumns
     */
    public Field getField( int index ) {
        return fields[ index ];
    }

    /**
     * Obtains the data access object for a given TABLE element.
     * In the case this table has no body, a TabularData with 0 rows
     * will be returned.
     *
     * @param  tableEl  DOM TABLE element
     * @param  fields   array of the fields in the table
     * @param  systemId  sytem identifier of the document
     * @return  data access object for the table in <tt>tableEl</tt>
     */
    private static TabularData getTabularData( Element tableEl, Field[] fields,
                                               String systemId )
            throws IOException {
        int ncol = fields.length;
        final Decoder[] decoders = new Decoder[ ncol ];
        final Class[] classes = new Class[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            decoders[ icol ] = fields[ icol ].getDecoder();
            classes[ icol ] = decoders[ icol ].getContentClass();
        }

        /* See if it has been stashed away by the custom VOTable parser. */
        TabularData storedData = VOTableDOMBuilder.getData( tableEl );
        if ( storedData != null ) {
            return storedData;
        }

        /* Otherwise we will have to find it in the DOM and decode it here.
         * Any relevant nodes will be inside the DATA element. */
        Element dataEl = DOMUtils.getChildElementByName( tableEl, "DATA" );

        /* If there's no DATA child, there's no data. */
        if ( dataEl == null ) {
            return new TableBodies.EmptyTabularData( classes );
        }

        /* Try TABLEDATA format. */
        Element tdEl = DOMUtils.getChildElementByName( dataEl, "TABLEDATA" );
        if ( tdEl != null ) {
            return new TableBodies.TabledataTabularData( decoders, tdEl );
        }

        /* Try BINARY format. */
        Element binaryEl = DOMUtils.getChildElementByName( dataEl, "BINARY" );
        if ( binaryEl != null ) {
            final Element streamEl = 
                DOMUtils.getChildElementByName( binaryEl, "STREAM" );
            String href = streamEl.getAttribute( "href" );
            if ( href != null && href.length() > 0 ) {
                URL url = URLUtils.makeURL( systemId, href );
                String encoding = streamEl.getAttribute( "encoding" );
                return new TableBodies
                          .HrefBinaryTabularData( decoders, url, encoding );
            }
            else {
                return new TableBodies.SequentialTabularData( classes ) {
                    public RowStepper getRowStepper() throws IOException {
                        InputStream istrm = getTextChildrenStream( streamEl );
                        return new BinaryRowStepper( decoders, istrm,
                                                     "base64" );
                    }
                };
            }
        }

        /* Try FITS format. */
        Element fitsEl = DOMUtils.getChildElementByName( dataEl, "FITS" );
        if ( fitsEl != null ) {
            String extnum = fitsEl.getAttribute( "extnum" );
            if ( extnum != null && extnum.trim().length() == 0 ) {
                extnum = null;
            }
            final Element streamEl =
                DOMUtils.getChildElementByName( fitsEl, "STREAM" );
            String href = streamEl.getAttribute( "href" );

            /* Construct a DataSource which knows how to retrieve the
             * input stream corresponding to the FITS data.  Ignore the
             * stated encoding here, since DataSource can work out 
             * compression formats on its own. */
            DataSource datsrc;
            if ( href != null && href.length() > 0 ) {
                URL url = URLUtils.makeURL( systemId, href );
                datsrc = new URLDataSource( url );
            }
            else {
                datsrc = new DataSource() {
                    protected InputStream getRawInputStream() {
                        return new BufferedInputStream( 
                                   new Base64InputStream(
                                       getTextChildrenStream( streamEl ) ) );
                    }
                };
                datsrc.setName( "STREAM" );
            }
            datsrc.setPosition( extnum );
            StarTable startab = new FitsTableBuilder()
                               .makeStarTable( datsrc, false );
            if ( startab == null ) {
                throw new VOTableFormatException( 
                    "STREAM element does not contain a FITS table" );
            }
            return new TableBodies.StarTableTabularData( startab );
        }

        /* We don't seem to have any data. */
        return new TableBodies.EmptyTabularData( classes );
    }

    /**
     * Returns the concatenated content of all the text-type children of
     * a given node as an input stream.
     *
     * @param   node  node whose children we are interested in
     * @return  concatenation of textual data as a stream
     */
    private static InputStream getTextChildrenStream( Node node ) {
        List tchildren = new ArrayList();
        for ( Node child = node.getFirstChild(); child != null; 
              child = child.getNextSibling() ) {
            if ( child instanceof Text ) {
                tchildren.add( child );
            }
        }
        final Iterator it = tchildren.iterator();
        return new SequenceInputStream( new Enumeration() {
            public boolean hasMoreElements() {
                return it.hasNext();
            }
            public Object nextElement() {
                Text textNode = (Text) it.next();
                String text = textNode.getData();
                InputStream tstrm = new StringInputStream( text );
                return tstrm;
            }
        } );
    }

    /**
     * Utility class which reads a String as an InputStream.  
     * Normally this isn't a respectable thing to do because of encodings,
     * but this is used for base64-encoded strings, so we know that a 
     * simple cast of char to byte is the right way to do the conversion.
     */
    static class StringInputStream extends InputStream {
        private final String text;
        private final int leng;
        private int pos = 0;
        private int marked = 0;
        public StringInputStream( String text ) {
            this.text = text;
            this.leng = text.length();
        }
        public int available() {
            return leng - pos;
        }
        public int read() {
            return pos < leng ? (int) text.charAt( pos++ )
                              : -1;
        }
        public int read( byte[] b, int off, int size ) {
            int i = 0;
            for ( ; i < size && pos < leng; i++ ) {
                b[ off++ ] = (byte) text.charAt( pos++ );
            }
            return i;    
        }
        public int read( byte[] b ) {
            return read( b, 0, b.length );
        }
        public long skip( long n ) {
            long i = Math.min( n, (long) ( leng - pos ) );
            pos += (int) i;
            return i;
        }
        public void mark( int limit ) {
            marked = pos;
        }
        public void reset() {
            pos = marked;
        }
        public boolean markSupported() {
            return true;
        }
    }
}
