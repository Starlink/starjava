package uk.ac.starlink.treeview.votable;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.SourceReader;

/**
 * An object representing the TABLE element of a VOTable.
 * This contains fields, links and rows; the actual data from the table
 * body may be obtained by iterating over rows using the
 * {@link #hasNextRow} and {@link #nextRow} methods.
 * <p>
 * This class itself implements a table with no body (no rows); 
 * the static {@link #makeTable} method should be used to obtain 
 * a table object, this will return an instance of one of the 
 * data-bearing subclasses of Table according to the data described
 * by the XML.
 *
 * @author   Mark Taylor (Starlink)
 */
public class Table extends GenericElement {

    private List fields = new ArrayList();
    private List links = new ArrayList();
    private final int ncols;

    /**
     * Protected constructor which constructs a bare Table object, 
     * containing no table rows, from a TABLE element.
     *
     * @param   tableEl  the TABLE element node in a VOTable DOM.
     */
    protected Table( Element tableEl ) {
        super( tableEl );
        for ( Node nd = tableEl.getFirstChild(); nd != null;
              nd = nd.getNextSibling() ) {
            if ( nd instanceof Element ) {
                Element el = (Element) nd;
                String elname = el.getTagName();
                if ( elname.equals( "FIELD" ) ) {
                    Field field = new Field( el );
                    fields.add( field );
                }
                if ( elname.equals( "LINK" ) ) {
                    Link link = new Link( el );
                    links.add( link );
                }
            }
        }
        ncols = fields.size();
    }

    /**
     * Constructs a Table object from an XML source representing the
     * TABLE element of a VOTable.
     *
     * @param   xsrc  an XML source representing a TABLE node
     */
    public static Table makeTable( Source xsrc ) throws TransformerException {
        Node tab = new SourceReader().getDOM( xsrc );
        Element tableEl = ( tab instanceof Document )
                        ? ((Document) tab).getDocumentElement()
                        : (Element) tab;
        Element dataEl = DOMUtils.getChildElementByName( tableEl, "DATA" );
        Element tdEl = DOMUtils.getChildElementByName( dataEl, "TABLEDATA" );
        if ( tdEl != null ) {
            return new TabledataTable( tableEl, tdEl );
        }
        Element binEl = DOMUtils.getChildElementByName( dataEl, "BINARY" );
        if ( binEl != null ) {
            // return new BinaryTable( tableEl, binEl );
            throw new UnsupportedOperationException( "No BINARY support" );
        }
        Element fitsEl = DOMUtils.getChildElementByName( dataEl, "FITS" );
        if ( dataEl != null ) {
            // return new FitsTable( tableEl, fitsEl );
            throw new UnsupportedOperationException( "No FITS support" );
        }
        return new Table( tableEl );
    }

    /**
     * Returns the number of columns in this table.
     *
     * @return  the number of columns
     */
    public int getNumColumns() {
        return ncols;
    }

    /**
     * Returns the number of rows in this table.
     * If this cannot be determined, or cannot be determined efficiently,
     * the value -1 may be returned.
     *
     * @return  the number of rows, or -1 if unknown
     */
    public int getNumRows() {
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
     * Returns an array of all the Link objects associated with this Table.
     *
     * @return  the links
     */
    public Link[] getLinks() {
        return (Link[]) links.toArray( new Link[ 0 ] );
    }

    /**
     * Returns the next row of elements in the table.
     * The returned row is an array of Objects, with numColumns elements.
     * Each element will be one of the following:
     * <ul>
     * <li>a primitive wrapper object (<tt>Integer</tt>, <tt>Float</tt> etc)
     *     if the element is a scalar
     * <li>a java array of primitives (<tt>int[]</tt>, <tt>float[]</tt> etc)
     *     if the element is a one-dimensional array
     * <li>an {@link uk.ac.starlink.array.NDArray} if the element is an
     *     array of &gt;1 dimension
     * </ul>
     * Complex types are treated by adding an extra dimension to the 
     * shape of the data, the most rapidly varying, of size 2.
     *
     * @return  an array of Objects representing the next row to be accesssed.
     * @throws  NoSuchElementException  if {@link #hasNextRow} would return 
     *          <tt>false</tt>
     */
    public Object[] nextRow() {
        throw new NoSuchElementException();
    }

    /**
     * Indicates whether there are more rows to be obtained using the
     * {@link #hasNextRow} method.
     *
     * @return  <tt>true</tt> iff <tt>hasNextRow</tt> can be called
     */
    public boolean hasNextRow() {
        return false;
    }
}
