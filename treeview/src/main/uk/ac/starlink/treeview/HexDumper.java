package uk.ac.starlink.treeview;

import java.awt.Component;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableCellRenderer;

/**
 * A component which shows a hexadecimal plus character dump of 
 * a byte stream.
 *
 * @author   Mark Taylor (Starlink)
 */
public class HexDumper extends JTable {

    public static final int BYTES_PER_ROW = 16;

    /**
     * Constructs a HexDumper given a random access file.
     */
    public HexDumper( final RandomAccessFile raf ) throws IOException {
        this( new ByteSource() {
            private int length = (int) raf.length();
            public int read( int pos ) throws IOException {
                raf.seek( (long) pos );
                return raf.readByte();
            }
            public int length() {
                return length;
            }
            public boolean isRandom() {
                return true;
            }
        } );
    }

    /**
     * Constructs a HexDumper given an input stream.
     *
     * @param  strm   the InputStream to dump
     * @param  length the number of bytes in the stream.  If this is not
     *                 known, the value -1 should be submitted
     */
    public HexDumper( final InputStream strm, final long length ) 
            throws IOException {
        
        this( new ByteSource() {
            private int loc = 0;
            public int read( int pos ) throws IOException {

                /* The general contract of skip is rather loose, so try to 
                 * make sure that we actually do end up where we want to. */
                while ( pos > loc ) {
                    int inc = (int) strm.skip( pos - loc );
                    if ( inc == 0 ) {
                        strm.read();
                        inc++;
                    }
                    loc += inc;
                }
                int val = strm.read();
                assert ( loc == pos ) || ( val == -1 );
                loc++;
                return val;
            }
            public int length() {
                if ( length > Integer.MAX_VALUE ) {
                    System.err.println( "truncating " + length + " to "
                                      + Integer.MAX_VALUE );
                    return Integer.MAX_VALUE;
                }
                else {
                    return (int) length;
                }
            }
            public boolean isRandom() {
                return false;
            }
        } );
    }

    /**
     * Constructs a HexDumper given a ByteSource.
     */
    public HexDumper( final ByteSource src ) throws IOException {

        /* Construct and set a TableModel from the given ByteSource. */
        TableModel model;
        if ( src.length() >= 0 ) {
            final int leng = src.length();
            if ( src.isRandom() ) {
                model = new HexDumperTableModel() {
                    public int getByteCount() {
                        return leng;
                    }
                    public byte getByteAt( int pos ) throws IOException {
                        return (byte) src.read( pos );
                    }
                };
            }
            else {
                model = new HexDumperTableModel() {
                    private ByteList bytebag = new ByteList();
                    public int getByteCount() {
                        return leng;
                    }
                    public byte getByteAt( int pos ) throws IOException {
                        for ( int i = bytebag.length(); i <= pos; i++ ) {
                            bytebag.append( (byte) src.read( i ) );
                        }
                        return bytebag.get( pos );
                    }
                };
            }
        }

        /* Length is not known. */
        else {
            model = new HexDumperTableModel() {
                private int knownsize = 0;
                private final int overshoot = BYTES_PER_ROW * 256;
                private final int nearEnd = overshoot / 2;
                private boolean foundEnd = false;
                private ByteList bytebag = new ByteList( overshoot );
                { getByteAt( 0 ); }
                public int getByteCount() {
                    return knownsize;
                }
                public byte getByteAt( int pos ) throws IOException {
                    if ( pos >= knownsize ||
                         ( knownsize - pos < knownsize / 4 ) && ! foundEnd ) {
                        int firstrow = knownsize / BYTES_PER_ROW;
                        if ( foundEnd ) {
                            throw new IllegalStateException();
                        }
                        for ( int limit = knownsize + overshoot;
                              knownsize < limit; knownsize++ ) {
                            int b = src.read( knownsize );
                            if ( b < 0 ) {
                                foundEnd = true;
                                break;
                            }
                            bytebag.append( (byte) b );
                        }
                        int lastrow = knownsize / BYTES_PER_ROW - 1;
                        fireTableRowsInserted( firstrow, lastrow );
                    }
                    return bytebag.get( pos );
                }
            };
        }
        setModel( model );

        setAutoResizeMode( AUTO_RESIZE_OFF );
        setShowGrid( false );
        setTableHeader( null );

        /* We need a fixed width font. */
        Font fixfont = new Font( "Monospaced", getFont().getStyle(),
                                 getFont().getSize() );
        setFont( fixfont );

        /* Cell renderer for column 0. */
        DefaultTableCellRenderer rend0 = new DefaultTableCellRenderer();
        rend0.setHorizontalAlignment( JLabel.RIGHT );
        rend0.setBackground( UIManager.getColor( "TableHeader.background" ) );

        /* Cell renderer for other columns. */
        DefaultTableCellRenderer rend = new DefaultTableCellRenderer();
        rend.setHorizontalAlignment( JLabel.CENTER );

        /* Cell renderer for the character column. */
        DefaultTableCellRenderer rendc = new DefaultTableCellRenderer();
        rend.setHorizontalAlignment( JLabel.CENTER );

        /* Work out the size of one character. */
        Component renderedM = 
            rend.getTableCellRendererComponent( this, "m", false, false, 1, 1 );
        int charWidth = renderedM.getPreferredSize().width;

        /* Set up columns. */
        TableColumnModel tcm = getColumnModel();
        TableColumn c0 = tcm.getColumn( 0 );
        c0.setCellRenderer( rend0 );
        c0.setPreferredWidth( 8 * charWidth );
        for ( int i = 0; i < BYTES_PER_ROW; i++ ) {
            TableColumn col = tcm.getColumn( i + 1 );
            col.setCellRenderer( rend );
            col.setPreferredWidth( (int) ( 2.5 * charWidth ) );
        }
        TableColumn blankcol = tcm.getColumn( BYTES_PER_ROW + 1 );
        blankcol.setCellRenderer( rend );
        blankcol.setPreferredWidth( 1 * charWidth );
        TableColumn cc = tcm.getColumn( BYTES_PER_ROW + 2 );
        cc.setCellRenderer( rendc );
        cc.setPreferredWidth( ( 1 + BYTES_PER_ROW ) * charWidth );

    }

    private abstract static class HexDumperTableModel 
            extends AbstractTableModel {
        public abstract int getByteCount();
        public abstract byte getByteAt( int pos ) throws IOException;
        public int getColumnCount() {
            return BYTES_PER_ROW + 3;
        }
        public int getRowCount() {
            return ( getByteCount() + BYTES_PER_ROW - 1 ) / BYTES_PER_ROW;
        }
        public Object getValueAt( int row, int column ) {
            try {
                if ( column == 0 ) {
                    return Integer.toHexString( row * BYTES_PER_ROW );
                }
                else if ( column > 0 && column <= BYTES_PER_ROW ) {
                    int pos = BYTES_PER_ROW * row + column - 1;
                    if ( pos < getByteCount() ) {
                        byte b = getByteAt( pos );
                        return Integer.toHexString( 0x100 | ( b & 0xff ) )
                                      .substring( 1 );
                    }
                    else {
                        return null;
                    }
                }
                else if ( column == BYTES_PER_ROW + 1 ) {
                    return null;
                }
                else if ( column == BYTES_PER_ROW + 2 ) {
                    StringBuffer sb = new StringBuffer( BYTES_PER_ROW );
                    int start = BYTES_PER_ROW * row;
                    int end = Math.min( start + BYTES_PER_ROW, getByteCount() );
                    for ( int pos = start; pos < end; pos++ ) {
                        byte b = getByteAt( pos );
                        if ( b > 31 && b < 127 ) {
                            sb.append( (char) b );
                        }
                        else {
                            sb.append( '\u00b7' );
                        }
                    }

                    // For some reason this sometimes appears wrong in the
                    // table viewer - e.g. first line of "<HTML>\n<HEAD>\n<T"
                    // comes out "..      ".  No luck tracking this down - 
                    // the string leaves this method and is processed 
                    // through the cell renderer correctly.  Weird JTree bug??
                    return sb.toString();
                }
                else {
                    throw new AssertionError( "Unexpected column " + column );
                }
            }
            catch ( IOException e ) {
                return "ERROR";
            }
        }
    }


    /**
     * Defines the data model used to construct a HexDumper.
     */
    public static interface ByteSource {

        /**
         * Returns the byte at the given position, or -1 if <tt>pos</tt>
         * is beyond the end of the file.
         * Will only be invoked on <tt>pos</tt>
         * values in strictly monotonically increasing order unless
         * <tt>isRandom</tt> returns false.
         *
         * @param  pos  the position of the byte to read
         * @return  the byte value at <tt>pos</tt>, or -1
         * @throws  IOException  if somethiing goes wrong
         */
        int read( int pos ) throws IOException;

        /**
         * Returns the number of bytes in this object, or -1 if the number
         * is not known.
         *
         * @return the length of the source, or -1 if not known
         */
        int length();

        /**
         * Indicates whether random access is available.
         *
         * @return   true iff <tt>read</tt> may be invoked in non-monotonic
         *           increasing order
         */
        boolean isRandom();
    }
}
