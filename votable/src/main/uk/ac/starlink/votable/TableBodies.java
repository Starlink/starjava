package uk.ac.starlink.votable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.table.EmptyRowSequence;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.DOMUtils;

/**
 * Class providing various implementations of {@link TabularData} 
 * and associated bits and pieces.
 * There isn't any very strong reason why these are gathered together
 * in the same container class, except that they have some of their
 * implementations in common.
 *
 * @author   Mark Taylor (Starlink)
 */
class TableBodies {

    /**
     * Abstract superclass for TabularData implementations which only 
     * allow sequential access.
     */
    abstract static class SequentialTabularData implements TabularData {
        final int ncol;
        final Class[] classes;
                            
        public SequentialTabularData( Class[] classes ) {
            this.classes = classes;
            this.ncol = classes.length;
        }           

        public int getColumnCount() {
            return ncol;
        }
                                
        public long getRowCount() {
            return -1L;
        }

        public Class getContentClass( int icol ) {
            return classes[ icol ];
        }

        public abstract RowSequence getRowSequence() throws IOException;

        public boolean isRandom() {
            return false;
        }

        public Object getCell( long irow, int icol ) {
            throw new UnsupportedOperationException( "Not random" );
        }

        public Object[] getRow( long irow ) {
            throw new UnsupportedOperationException( "Not random" );
        }
    }

    /**
     * TabularData implementation with no rows.
     */
    static class EmptyTabularData extends SequentialTabularData {
        public EmptyTabularData( Class[] classes ) {
            super( classes );
        }
        public long getRowCount() {
            return 0L;
        }
        public RowSequence getRowSequence() {
            return EmptyRowSequence.getInstance();
        }
    }

    /**
     * TabularData implementation which gets its data from a StarTable.
     */
    static class StarTableTabularData implements TabularData {
        private final StarTable startab;

        public StarTableTabularData( StarTable startab ) {
            this.startab = startab;
        }

        public int getColumnCount() {
            return startab.getColumnCount();
        }

        public long getRowCount() {
            return startab.getRowCount();
        }

        public Class getContentClass( int icol ) {
            return startab.getColumnInfo( icol ).getContentClass();
        }

        public RowSequence getRowSequence() throws IOException {
            return startab.getRowSequence();
        }

        public boolean isRandom() {
            return startab.isRandom();
        }
 
        public Object getCell( long irow, int icol ) throws IOException {
            return startab.getCell( irow, icol );
        }

        public Object[] getRow( long irow ) throws IOException {
            return startab.getRow( irow );
        }
    }

    /**
     * TabularData implementation for a BINARY STREAM element with an
     * <tt>href</tt> attribute pointing to the data.
     */
    static class HrefBinaryTabularData extends SequentialTabularData {
        private final Decoder[] decoders;
        private final URL url;
        private final String encoding;

        public HrefBinaryTabularData( Decoder[] decoders, URL url,
                                      String encoding ) {
            super( getClasses( decoders ) );
            this.decoders = decoders;
            this.url = url;
            this.encoding = encoding;
        }

        public RowSequence getRowSequence() throws IOException {
            InputStream istrm = new BufferedInputStream( url.openStream() );
            return new BinaryRowSequence( decoders, istrm, encoding );
        }
    }

    /**
     * TabularData implementation for a TABLEDATA DOM element which 
     * contains the data as TR and TD descendants.
     */
    static class TabledataTabularData extends SequentialTabularData {
        final Decoder[] decoders;
        final Element tabledataEl;
        final int ncol;
        Element[] rows;
 
        TabledataTabularData( Decoder[] decoders, Element tabledataEl ) {
            super( getClasses( decoders ) );
            this.decoders = decoders;
            this.tabledataEl = tabledataEl;
            this.ncol = decoders.length;
        }

        public long getRowCount() {
            if ( rows == null ) {
                long nrow = 0;
                for ( Node node = tabledataEl.getFirstChild(); node != null;
                      node = node.getNextSibling() ) {
                    if ( node instanceof Element &&
                         getVOTagName( (Element) node ).equals( "TR" ) ) {
                        nrow++;
                    }
                }
                return nrow;
            }
            else {
                return rows.length;
            }
        }

        public boolean isRandom() {
            return true;
        }

        private Element[] getRows() {
            List rowList = new ArrayList();
            if ( rows == null ) {
                for ( Element trEl = 
                          firstSibling( "TR", tabledataEl.getFirstChild() );
                      trEl != null;
                      trEl = firstSibling( "TR", trEl.getNextSibling() ) ) {
                     rowList.add( trEl );
                }
                rows = (Element[]) rowList.toArray( new Element[ 0 ] );
            }
            return rows;
        }

        public Object[] getRow( long irow ) {
            return getRow( getRows()[ (int) irow ] );
        }

        public Object getCell( long irow, int icol ) {
            Element trEl = getRows()[ (int) irow ];
            int jcol = 0;
            for ( Element tdEl = firstSibling( "TD", trEl.getFirstChild() );
                  tdEl != null && icol < ncol;
                  tdEl = firstSibling( "TD", tdEl.getNextSibling() ) ) {
                if ( jcol == icol ) {
                    String txt = DOMUtils.getTextContent( tdEl );
                    if ( txt != null && txt.length() > 0 ) {
                        return decoders[ icol ].decodeString( txt );
                    }
                    else {
                        return null;
                    }
                }
                jcol++;
            }
            return null;
        }

        public RowSequence getRowSequence() {
            return new RowSequence() {
                Element trEl;
                boolean done;

                public boolean next() {
                    if ( done ) {
                        return false;
                    }
                    Node prev = trEl == null ? tabledataEl.getFirstChild()
                                             : trEl.getNextSibling();
                    trEl = firstSibling( "TR", prev );
                    if ( trEl == null ) {
                        done = true;
                    }
                    return ! done;
                }

                public Object[] getRow() {
                    if ( trEl == null || done ) {
                        throw new IllegalStateException();
                    }
                    return TabledataTabularData.this.getRow( trEl );
                }

                public Object getCell( int icol ) {
                    if ( trEl == null || done ) {
                        throw new IllegalStateException();
                    }
                    Element tdEl = firstSibling( "TD", trEl.getFirstChild() );
                    while ( icol-- > 0 ) {
                        tdEl = firstSibling( "TD", tdEl.getNextSibling() );
                    }
                    if ( tdEl == null ) {
                        return null;
                    }
                    String txt = DOMUtils.getTextContent( tdEl );
                    return ( txt == null || txt.length() == 0 )
                         ? null
                         : decoders[ icol ].decodeString( txt );
                }

                public void close() {
                    trEl = null;
                }
            };
        }

        private Object[] getRow( Element trEl ) {
            Object[] row = new Object[ ncol ];
            int icol = 0;
            for ( Element tdEl = firstSibling( "TD", trEl.getFirstChild() );
                  tdEl != null && icol < ncol;
                  tdEl = firstSibling( "TD", tdEl.getNextSibling() ) ) {
                String txt = DOMUtils.getTextContent( tdEl );
                if ( txt != null && txt.length() > 0 ) {
                    row[ icol ] = decoders[ icol ].decodeString( txt );
                }
                icol++;
            }
            return row;
        }

        private static Element firstSibling( String tag, Node node ) {
            if ( node == null ) {
                return null;
            }
            else if ( node instanceof Element &&
                      getVOTagName( (Element) node ).equals( tag ) ) {
                return (Element) node;
            }
            else {
                return firstSibling( tag, node.getNextSibling() );
            }
        }
    }

    /**
     * Returns the column content classes associated with an array of decoders.
     */
    static Class[] getClasses( Decoder[] decoders ) {
        int ncol = decoders.length;
        Class[] classes = new Class[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            classes[ icol ] = decoders[ icol ].getContentClass();
        }
        return classes;
    }

    /**
     * Returns the tag name in the VOTable namespace for an element.
     *
     * @param  element
     * @return   unqualified VOTable element name
     */
    private static String getVOTagName( Element element ) {
        if ( element == null ) {
            return null;
        }
        else if ( element instanceof VOElement ) {
            return ((VOElement) element).getVOTagName();
        }
        else {
            return element.getTagName();
        }
    }

}
