package uk.ac.starlink.votable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.SourceReader;

/**
 * An object representing the TABLE element of a VOTable.
 * This contains fields, links and rows; the actual data from the table
 * body may be obtained by iterating over rows using the
 * {@link #hasNextRow} and {@link #nextRow} methods.
 * <p>
 * This class itself implements a table with no body (no rows); 
 * the static <tt>makeTable</tt> methods should be used to obtain 
 * a table object, this will return an instance of one of the 
 * data-bearing subclasses of Table according to the data described
 * by the XML.
 *
 * @author   Mark Taylor (Starlink)
 */
public class Table extends VOElement {

    private List fields = new ArrayList();
    private List links = new ArrayList();
    private final int ncols;

    /**
     * Protected constructor which constructs a bare Table object, 
     * containing no table rows, from a TABLE element.
     *
     * @param   xsrc  the TABLE element node in a VOTable DOM.
     */
    protected Table( Source xsrc ) {
        super( xsrc, "TABLE" );
        Element tableEl = getElement();
        for ( Node nd = tableEl.getFirstChild(); nd != null;
              nd = nd.getNextSibling() ) {
            if ( nd instanceof Element ) {
                Element el = (Element) nd;
                String elname = el.getTagName();
                if ( elname.equals( "FIELD" ) ) {
                    Field field = 
                        new Field( new DOMSource( el, getSystemId() ) );
                    fields.add( field );
                }
                if ( elname.equals( "LINK" ) ) {
                    Link link =
                        new Link( new DOMSource( el, getSystemId() ) );
                    links.add( link );
                }
            }
        }
        ncols = fields.size();
    }

    /**
     * Constructs a Table object from an XML source representing the
     * TABLE element of a VOTable.  The object returned may 
     * implement the {@link RandomTable} interface if random access
     * is provided for by the underlying implementation (for instance,
     * FITS tables may well provide this, while streamed ones may 
     * not).
     *
     * @param   xsrc  an XML source representing a TABLE node
     * @return  an object containing the data of this table
     */
    public static Table makeTable( Source xsrc ) {
        try {
            Element tab = new SourceReader().getElement( xsrc );
            String systemId = xsrc.getSystemId();
            return makeTable( tab, systemId );
        }
        catch ( TransformerException e  ){
            throw new RuntimeException( "Unsuitable source", e );
        }
    }

    /**
     * Constructs a Table object from a DOM element representing the TABLE
     * element of a VOTable.
     *
     * @param   tablEl  the TABLE element
     * @param   systemId  the system ID of the document, which may be
     *          used to resolve relative URIs
     */
    static Table makeTable( Element tableEl, String systemId ) {
        Element dataEl = DOMUtils.getChildElementByName( tableEl, "DATA" );
        Element tdEl = DOMUtils.getChildElementByName( dataEl, "TABLEDATA" );
        if ( tdEl != null ) {
            return new TabledataTable( tableEl, tdEl );
        }
        Element fitsEl = DOMUtils.getChildElementByName( dataEl, "FITS" );
        if ( dataEl != null ) {
            return FitsTable.makeFitsTable( tableEl, fitsEl, systemId );
        }
        Element binEl = DOMUtils.getChildElementByName( dataEl, "BINARY" );
        if ( binEl != null ) {
            return new BinaryTable( tableEl, binEl, systemId );
        }
        return new Table( new DOMSource( tableEl, systemId ) );
    }

    /**
     * Returns the number of columns in this table.
     *
     * @return  the number of columns
     */
    public int getColumnCount() {
        return ncols;
    }

    /**
     * Returns the number of rows in this table.
     * If this cannot be determined, or cannot be determined efficiently,
     * the value -1 may be returned.
     *
     * @return  the number of rows, or -1 if unknown
     */
    public int getRowCount() {
        return 0;
    }

    /**
     * Returns one of the Field objects associated with this table.
     *
     * @param  index  the index of the field to return 
     * @return  the filed at index <tt>index</tt>
     * @throws  IndexOutOfBoundsException unless 0&lt;=index&lt;numColumns
     */
    public Field getField( int index ) {
        return (Field) fields.get( index );
    }

    /**
     * Returns the next row of elements in the table.
     * The returned row is an array of Objects, with numColumns elements.
     * Each element will be one of the following:
     * <ul>
     * <li>a primitive wrapper object (<tt>Integer</tt>, <tt>Float</tt> etc)
     *     if the element is a scalar
     * <li>a java array of primitives (<tt>int[]</tt>, <tt>float[]</tt> etc)
     *     if the element is an array.  This is stored in column-major
     *     order, where that makes a difference (for arrays with more than
     *     one diemension).
     * </ul>
     * Complex types are treated by adding an extra dimension to the 
     * shape of the data, the most rapidly varying, of size 2.
     *
     * @return  an array of Objects representing the next row to be accesssed.
     * @throws  NoSuchElementException  if {@link #hasNextRow} would return 
     *          <tt>false</tt>
     * @throws  IOException  if there is some read error
     */
    public Object[] nextRow() throws IOException {
        throw new NoSuchElementException();
    }

    /**
     * Indicates whether there are more rows to be obtained using the
     * {@link #nextRow} method.
     *
     * @return  <tt>true</tt> iff <tt>nextRow</tt> can be called
     */
    public boolean hasNextRow() {
        return false;
    }
}
