package uk.ac.starlink.votable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.fits.FitsTableBuilder;
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
    public abstract static class SequentialTabularData implements TabularData {
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

        public abstract RowStepper getRowStepper() throws IOException;

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
    public static class EmptyTabularData extends SequentialTabularData {
        public EmptyTabularData( Class[] classes ) {
            super( classes );
        }
        public long getRowCount() {
            return 0L;
        }
        public RowStepper getRowStepper() {
            return new RowStepper() {
                public Object[] nextRow() {
                    return null;
                }
            };
        }
    }

    /**
     * TabularData implementation which gets its data from a StarTable.
     */
    public static class StarTableTabularData implements TabularData {
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

        public RowStepper getRowStepper() throws IOException {
            return new RowStepper() {
                RowSequence rseq = startab.getRowSequence();
                public Object[] nextRow() throws IOException {
                    if ( rseq.hasNext() ) {
                        rseq.next();
                        return rseq.getRow();
                    }
                    else {
                        return null;
                    }
                }
            };
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
     * TabularData implementation which stores its data in an array of
     * rows.
     */
    public static class RowListTabularData implements TabularData {

        Class[] classes;
        List rows;

        public RowListTabularData() {
        }

        public RowListTabularData( Class[] classes, List rows ) {
            this.classes = classes;
            this.rows = rows;
        }

        public int getColumnCount() {
            return classes.length;
        }

        public long getRowCount() {
            return (long) rows.size();
        }

        public Class getContentClass( int icol ) {
            return classes[ icol ];
        }

        public RowStepper getRowStepper() {
            return new RowStepper() {
                int irow = 0;
                public Object[] nextRow() {
                    return irow < rows.size() ? (Object[]) rows.get( irow++ ) 
                                              : null;
                }
            };
        }

        public boolean isRandom() {
            return true;
        }

        public Object getCell( long lrow, int icol ) {
            return ((Object[]) rows.get( (int) lrow ))[ icol ];
        }

        public Object[] getRow( long lrow ) {
            return (Object[]) rows.get( (int) lrow );
        }
    }

    /**
     * TabularData implementation for a BINARY STREAM element with an
     * <tt>href</tt> attribute pointing to the data.
     */
    public static class HrefBinaryTabularData extends SequentialTabularData {
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

        public RowStepper getRowStepper() throws IOException {
            InputStream istrm = new BufferedInputStream( url.openStream() );
            return new BinaryRowStepper( decoders, istrm, encoding );
        }
    }

    /**
     * TabularData implementation for a BINARY STREAM element in which 
     * the data stream itself will be supplied by writing a base64-encoded
     * stream of data down a piped output stream.
     */
    public static class InlineBinaryTabularData extends RowListTabularData {

        final Thread streamReader;
        Exception caught;

        public InlineBinaryTabularData( final Decoder[] decoders,
                                        final PipedOutputStream b64out ) 
                throws IOException {
            classes = getClasses( decoders );
            rows = new ArrayList();

            streamReader = new Thread( "BINARY stream reader" ) {
                PipedInputStream b64in = new PipedInputStream( b64out );
                InputStream datain = new BufferedInputStream( b64in );
                RowStepper rstep = 
                    new BinaryRowStepper( decoders, datain, "base64" );
                public void run() {
                    Object[] row;
                    try {
                        while ( ( row = rstep.nextRow() ) != null ) {
                            rows.add( row );
                        }
                    }
                    catch ( IOException e ) {
                        caught = e;
                    }
                    finally {
                        try {
                            datain.close();
                        }
                        catch ( IOException e ) {
                        }
                    }
                }
            };
            streamReader.start();
        }

        public void finishReading() throws IOException {
            try {
                streamReader.join();
            }
            catch ( InterruptedException e ) {
                if ( caught == null ) {
                    caught = e;
                }
            }
            if ( caught instanceof IOException ) {
                throw (IOException) caught;
            }
            else if ( caught != null ) {
                throw (IOException) new IOException( "Pipe trouble" )
                                   .initCause( caught );
            }
        }
    }

 
    /**
     * TabularData implementation for a TABLEDATA DOME element which 
     * contains the data as TR and TD descendants.
     */
    public static class TabledataTabularData extends SequentialTabularData {
        final Decoder[] decoders;
        final Element tabledataEl;
        final int ncol;
 
        TabledataTabularData( Decoder[] decoders, Element tabledataEl ) {
            super( getClasses( decoders ) );
            this.decoders = decoders;
            this.tabledataEl = tabledataEl;
            this.ncol = decoders.length;
        }

        public long getRowCount() {
            long nrow = 0;
            for ( Node node = tabledataEl.getFirstChild(); node != null;
                  node = node.getNextSibling() ) {
                if ( node instanceof Element &&
                     ((Element) node).getTagName().equals( "TR" ) ) {
                    nrow++;
                }
            }
            return nrow;
        }

        public RowStepper getRowStepper() {
            return new RowStepper() {
                Element trEl = 
                    firstSibling( "TR", tabledataEl.getFirstChild() );
                public Object[] nextRow() {
                    if ( trEl == null ) {
                        return null;
                    }
                    Object[] row = new Object[ ncol ];
                    int icol = 0;
                    for ( Element tdEl = 
                              firstSibling( "TD", trEl.getFirstChild() );
                          tdEl != null && icol < ncol; 
                          tdEl = firstSibling( "TD", tdEl.getNextSibling() ) ) {
                        String txt = DOMUtils.getTextContent( tdEl );
                        if ( txt != null && txt.length() > 0 ) {
                            row[ icol ] = decoders[ icol ].decodeString( txt );
                        }
                        icol++;
                    }
                    trEl = firstSibling( "TR", trEl.getNextSibling() );
                    return row;
                }
                Element firstSibling( String tag, Node node ) {
                    if ( node == null ) {
                        return null;
                    }
                    else if ( node instanceof Element &&
                              ((Element) node).getTagName().equals( tag ) ) {
                        return (Element) node;
                    }
                    else {
                        return firstSibling( tag, node.getNextSibling() );
                    }
                }
            };
        }
    }

    /**
     * TabularData implementation for a FITS STREAM element in which the
     * data stream itself will be supplied by writing a base64-encoded
     * stream of data down a piped output stream.
     */
    public static class InlineFITSTabularData extends RowListTabularData
                                              implements TableSink {
        private final Thread streamReader;
        private Exception caught;

        public InlineFITSTabularData( final PipedOutputStream b64out,
                                      final String extnum ) throws IOException {

            streamReader = new Thread( "FITS stream reader" ) {
                final InputStream datain =
                    new Base64InputStream( 
                        new BufferedInputStream( 
                            new PipedInputStream( b64out ) ) );
                public void run() {
                    try {
                        TableSink sink = InlineFITSTabularData.this;
                        new FitsTableBuilder().copyStarTable( datain, sink,
                                                              extnum );
                    }
                    catch ( IOException e ) {
                        caught = e;
                    }
                    finally {
                        try {
                            datain.close();
                        }
                        catch ( IOException e ) {
                        }
                    }
                }
            };
            streamReader.start();
        }

        public void acceptMetadata( StarTable meta ) {
            int ncol = meta.getColumnCount();
            rows = new ArrayList( Math.max( (int) meta.getRowCount(), 1 ) );
            
            classes = new Class[ ncol ];
            for ( int icol = 0; icol < ncol; icol++ ) {
                classes[ icol ] = meta.getColumnInfo( icol ).getContentClass();
            }
        }

        public void acceptRow( Object[] row ) {
            rows.add( row );
        }

        public void endRows() {
        }

        public void finishReading() throws IOException {
            try {
                streamReader.join();
            }
            catch ( InterruptedException e ) {
                if ( caught == null ) {
                    caught = e;
                }
            }
            if ( caught instanceof IOException ) {
                throw (IOException) caught;
            }
            else if ( caught != null ) {
                throw (IOException) new IOException( "Pipe trouble" )
                                   .initCause( caught );
            }
        }
    }

    /**
     * Returns the column content classes associated with an array of decoders.
     */
    private static Class[] getClasses( Decoder[] decoders ) {
        int ncol = decoders.length;
        Class[] classes = new Class[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            classes[ icol ] = decoders[ icol ].getContentClass();
        }
        return classes;
    }

}
