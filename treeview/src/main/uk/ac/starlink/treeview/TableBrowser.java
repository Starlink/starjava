package uk.ac.starlink.treeview;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import nom.tam.fits.FitsException;
import nom.tam.fits.TableHDU;

class TableBrowser extends JTable {

    private TableCellRenderer rowHeadRend;
    private Class[] colclasses;

    public TableBrowser( final TableHDU thdu, final FitsTableColumn[] cols ) {

        final int ncols = thdu.getNCols();
        final int nrows = thdu.getNRows();
        
        TableModel tmodel = new AbstractTableModel() {
            public int getRowCount() {
                return nrows;
            }
            public int getColumnCount() {
                return ncols + 1;
            }
            public Object getValueAt( int row, int col ) {
                if ( col == 0 ) {
                    return new Integer( row + 1 );
                }
                else {
                    try {
                        Object obj = thdu.getElement( row, col - 1 );
                        Class cls = obj.getClass().getComponentType();
                        if ( cls != null ) {
                            boolean scaled = cols[ col - 1 ].isScaled();
                            double scale = cols[ col - 1 ].getScale();
                            double zero = cols[ col - 1 ].getZero();
                            if ( cls == byte.class ) {
                                byte val = ((byte[]) obj)[ 0 ];
                                return scaled  
                                    ? (Number) new Double( val * scale + zero )
                                    : (Number) new Byte( val );
                            }
                            else if ( cls == short.class ) {
                                short val = ((short[]) obj)[ 0 ];
                                return scaled 
                                    ? (Number) new Double( val * scale + zero )
                                    : (Number)  new Short( val );
                            }
                            else if ( cls == int.class ) {
                                int val = ((int[]) obj)[ 0 ];
                                return scaled 
                                    ? (Number) new Double( val * scale + zero )
                                    : (Number) new Integer( val );
                            }
                            else if ( cls == long.class ) {
                                long val = ((long[]) obj)[ 0 ];
                                return scaled 
                                    ? (Number) new Double( val * scale + zero )
                                    : (Number) new Long( val );
                            }
                            else if ( cls == float.class ) {
                                float val = ((float[]) obj)[ 0 ];
                                return scaled 
                                    ? (Number) new Double( val * scale + zero )
                                    : (Number) new Float( val );
                            }
                            else if ( cls == double.class ) {
                                double val = ((double[]) obj)[ 0 ];
                                return scaled 
                                    ? (Number) new Double( val * scale + zero )
                                    : (Number) new Double( val );
                            }
                            else if ( cls == boolean.class ) {
                                return new Boolean( ((boolean[]) obj)[ 0 ] );
                            }
                            else if ( cls == String.class ) {
                                return ((String[]) obj)[ 0 ];
                            }
                        }
                        return obj;
                    }
                    catch ( FitsException e ) {
                        return ".";
                    }
                }
            }
            public String getColumnName( int col ) {
                if ( col == 0 ) {
                    return "";
                }
                else {
                    String name = cols[ col - 1 ].getType();
                    String unit = cols[ col - 1 ].getUnit();
                    if ( unit != null ) {
                        name += "\n(" + unit + ")";
                    }
                    return name;
                }
            }
            public Class getColumnClass( int col ) {
                if ( col > 0 && cols[ col - 1 ].isScaled() ) {
                    return Double.class;
                }
                else {
                    return getValueAt( 0, col ).getClass();
                }
            }
        };
        setModel( tmodel );
        setAutoResizeMode( AUTO_RESIZE_OFF );

        rowHeadRend = new DefaultTableCellRenderer();
        ((DefaultTableCellRenderer) rowHeadRend)
       .setBackground( UIManager.getColor( "TableHeader.background" ) );
        ((DefaultTableCellRenderer) rowHeadRend)
       .setForeground( UIManager.getColor( "TableHeader.foreground" ) );

        TableCellRenderer colHeadRend = new DefaultTableCellRenderer() {
            Map heads = new HashMap();
            JLabel dflt = (JLabel) 
                super.getTableCellRendererComponent( TableBrowser.this, "", 
                                                     false, false, 0, 0 );
            public Component getTableCellRendererComponent( 
                    JTable table, Object value, boolean isselected,
                    boolean hasFocus, int row, int column ) {
                if ( ! heads.containsKey( value ) ) {
                    StringTokenizer st = 
                        new StringTokenizer( (String) value, "\n" );
                    String s1;
                    String s2;
                    if ( st.countTokens() >= 2 ) {
                        s1 = st.nextToken();
                        s2 = st.nextToken();
                    }
                    else {
                        s1 = (String) value;
                        s2 = " ";
                    }
                    JPanel head = new JPanel();
                    head.setLayout( new BoxLayout( head, BoxLayout.Y_AXIS ) );
                    head.add( new JLabel( s1 ) );
                    head.add( new JLabel( s2 ) );
                    head.setBorder( BorderFactory
                                   .createBevelBorder( BevelBorder.RAISED ) );

                    // The following configuration doesn't seem to work.
                    // head.setBorder( dflt.getBorder() );
                    // head.setFont( dflt.getFont() );

                    heads.put( value, head );
                }
                return (Component) heads.get( value );
            }
        };

        TableColumnModel tcm = getColumnModel();
        TableCellRenderer bodyrend = new DefaultTableCellRenderer();
        TableCellRenderer headrend = colHeadRend;
        for ( int col = 0; col < tcm.getColumnCount(); col++ ) {
            TableColumn tc = tcm.getColumn( col );
            tc.setPreferredWidth( getColumnWidth( tmodel, bodyrend, 
                                                  headrend, col ) );
            tc.setHeaderRenderer( colHeadRend );
        }
    }

    public TableCellRenderer getCellRenderer( int row, int col ) {
        return ( col == 0 ) ? rowHeadRend : super.getCellRenderer( row, col );
    }

    private static int getColumnWidth( TableModel tmodel, 
                                       TableCellRenderer bodyrend,
                                       TableCellRenderer headrend, int col ) {
        JTable dummyTable = new JTable();

        Object itemobj = tmodel.getValueAt( 0, col );
        Component itemcomp =
            bodyrend
           .getTableCellRendererComponent( dummyTable, itemobj, false, false, 
                                           0, col );
        int itemwidth = itemcomp.getPreferredSize().width;

        String headobj = tmodel.getColumnName( col );
        Component headcomp =
            headrend
           .getTableCellRendererComponent( dummyTable, headobj, false, false,
                                           0, col );
        int headwidth = headcomp.getPreferredSize().width;

        return Math.max( Math.max( itemwidth, headwidth) + 10, 50 );
    }
}
