package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.BlankColumn;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.ConcatStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Window for concatenating two tables.
 *
 * @author   Mark Taylor (Starlink)
 * @since    25 Mar 2004
 */
public class ConcatWindow extends AuxWindow {

    private final JComboBox<TopcatModel> t1selector;
    private final JComboBox<TopcatModel> t2selector;
    private final JScrollPane colScroller;
    private final Action goAct;
    private ColumnDataComboBoxModel[] colSelectorModels_;

    /**
     * Constructs a new concatenation window.
     *
     * @param  parent  parent window, may be used for window positioning
     */
    @SuppressWarnings("this-escape")
    public ConcatWindow( Component parent ) {
        super( "Concatenate Tables", parent );

        JComponent main = getMainArea();
        main.setLayout( new BorderLayout() );
        int maxWidth = 250;

        /* Construct base table selection control. */
        t1selector = new TablesListComboBox( maxWidth );
        t1selector.setToolTipText( "Table supplying the columns and top rows" );

        /* Construct added table selection control. */
        t2selector = new TablesListComboBox( maxWidth );
        t2selector.setToolTipText( "Table supplying the bottom rows" );

        /* Reconfigure display if either table is reselected. */
        ItemListener tableSelectionListener = new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                updateDisplay();
            }
        };
        t1selector.addItemListener( tableSelectionListener );
        t2selector.addItemListener( tableSelectionListener );

        /* Place table selection controls. */
        final JLabel label2 = new JLabel( "Appended Table: " );
        JLabel label1 = new JLabel( "Base Table: " ) {
            @Override
            public Dimension getPreferredSize() {
                return label2.getPreferredSize();
            }
            @Override
            public Dimension getMaximumSize() {
                return label2.getMaximumSize();
            }
            @Override
            public Dimension getMinimumSize() {
                return label2.getMinimumSize();
            }
        };
        JComponent line1 = Box.createHorizontalBox();
        line1.add( label1 );
        line1.add( t1selector );
        JComponent line2 = Box.createHorizontalBox();
        line2.add( label2 );
        line2.add( t2selector );
        line1.setAlignmentX( 0 );
        line2.setAlignmentX( 0 );
        JComponent tBox = Box.createVerticalBox();
        tBox.add( line1 );
        tBox.add( Box.createVerticalStrut( 5 ) );
        tBox.add( line2 );
        tBox.add( Box.createVerticalStrut( 5 ) );
        tBox.setAlignmentX( 0 );
        main.add( tBox, BorderLayout.NORTH );

        /* Place the column correspondance box. */
        colScroller = new JScrollPane();
        colScroller.setPreferredSize( new Dimension( 300, 250 ) );
        colScroller.setBorder( makeTitledBorder( "Column Assignments" ) );
        main.add( colScroller, BorderLayout.CENTER );

        /* Place go button. */
        goAct = new ConcatAction( "Concatenate", null,
                                  "Create new concatenated table" );
        Box controlBox = Box.createHorizontalBox();
        getControlPanel().add( controlBox );
        controlBox.add( Box.createHorizontalGlue() );
        controlBox.add( new JButton( goAct ) );
        controlBox.add( Box.createHorizontalGlue() );

        /* Add standard help actions. */
        addHelp( "ConcatWindow" );

        /* Initialise state. */
        updateDisplay();
    }

    /**
     * Returns the selected base table.
     *
     * @return  base topcat model
     */
    private TopcatModel getBaseTable() {
        return (TopcatModel) t1selector.getSelectedItem();
    }

    /**
     * Returns the selected table for adding.
     *
     * @return  added topcat model
     */
    private TopcatModel getAddedTable() {
        return (TopcatModel) t2selector.getSelectedItem();
    }

    /**
     * Ensures the display shows the right thing for a given base and
     * additional table.
     */
    public void updateDisplay() {
        TopcatModel tc1 = getBaseTable();
        TopcatModel tc2 = getAddedTable();
        JPanel colPanel = new JPanel( new GridLayout( 0, 2 ) ) {
            public Dimension getMaximumSize() {
                return new Dimension( super.getMaximumSize().width, 
                                      super.getPreferredSize().height );
            }
        };
        Box colBox = Box.createVerticalBox();
        colBox.add( colPanel );
        colBox.add( Box.createVerticalGlue() );
        colScroller.setViewportView( colBox );
        if ( tc1 != null && tc2 != null ) {

            /* Title. */
            JLabel tl1 = new JLabel( "Base Table", SwingConstants.CENTER );
            JLabel tl2 = new JLabel( "Appended Table", SwingConstants.CENTER );
            tl1.setBorder( BorderFactory
                          .createBevelBorder( BevelBorder.RAISED ) );
            tl2.setBorder( BorderFactory
                          .createBevelBorder( BevelBorder.RAISED ) );
            colPanel.add( tl1 );
            colPanel.add( tl2 );

            /* One selector for each column. */
            TableColumnModel colModel1 = tc1.getColumnModel();
            TableColumnModel colModel2 = tc2.getColumnModel();
            int ncol = colModel1.getColumnCount();
            colSelectorModels_ = new ColumnDataComboBoxModel[ ncol ];
            for ( int icol = 0; icol < ncol; icol++ ) {
                ColumnInfo cinfo = 
                    ((StarTableColumn) colModel1.getColumn( icol ))
                   .getColumnInfo();
                colPanel.add( new JLabel( cinfo.getName() + ": " ) );
                Class<?> clazz = cinfo.getContentClass();
                if ( Number.class.isAssignableFrom( clazz ) ) {
                    clazz = Number.class;
                }
                ColumnDataComboBoxModel comboModel =
                    new ColumnDataComboBoxModel( tc2, clazz, true );
                comboModel.setSelectedItem( comboModel.getElementAt( 0 ) );
                colSelectorModels_[ icol ] = comboModel;
                JComboBox<ColumnData> comboBox = new ColumnDataComboBox();
                comboBox.setModel( comboModel );
                colPanel.add( comboBox );
            }

            /* Check if the tables have fully matching columns (same number,
             * names and classes). */
            boolean matching = false;
            if ( colModel1.getColumnCount() == colModel2.getColumnCount() ) {
                matching = true;
                for ( int icol = 0; icol < ncol && matching; icol++ ) {
                    ColumnInfo info1 =
                        ((StarTableColumn) colModel1.getColumn( icol ))
                                                    .getColumnInfo();
                    ColumnInfo info2 =
                        ((StarTableColumn) colModel2.getColumn( icol ))
                                                    .getColumnInfo();
                    matching = matching 
                            && info1.getName()
                                    .equalsIgnoreCase( info2.getName() )
                            && isMatchingClass( info1.getContentClass(),
                                                info2.getContentClass() );
                }
            }

            /* If so, align each one with the corresponding one from the
             * other table. */
            if ( matching ) {
                for ( int icol = 0; icol < ncol; icol++ ) {
                    StarTableColumn tcol =
                        (StarTableColumn) colModel2.getColumn( icol );
                    ColumnData cdata =
                        ColumnDataComboBoxModel
                       .createSimpleColumnData( tc2, tcol );
                    ColumnDataComboBoxModel model = colSelectorModels_[ icol ];
                    model.setSelectedItem( cdata );
                }
            }

            /* Otherwise, just make the best guess about how to match
             * them up. */
            else { 
                for ( int ic1 = 0; ic1 < ncol; ic1++ ) {
                    ColumnDataComboBoxModel cdModel2 =
                        colSelectorModels_[ ic1 ];
                    ColumnInfo info1 =
                        ((StarTableColumn) colModel1.getColumn( ic1 ))
                       .getColumnInfo();
                    ColumnData guessData =
                        cdModel2.getUniqueMatchColumnData( info1 );
                    cdModel2.setSelectedItem( guessData );
                }
            }
        }
    }

    /**
     * Creates a new StarTable based on the selections made by the user.
     *
     * @return  concatenated table
     */
    private StarTable makeTable() throws IOException {
        StarTable t1 = getBaseTable().getApparentStarTable();
        final ViewerTableModel t2base = getAddedTable().getViewModel();
        int ncol = colSelectorModels_.length;
        ColumnStarTable t2 = ColumnStarTable
                            .makeTableWithRows( t2base.getRowCount() );
        final int[] rowMap2 = t2base.getRowMap();
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo info = t1.getColumnInfo( icol );
            Object selObj = colSelectorModels_[ icol ].getSelectedItem();
            ColumnData selData = selObj instanceof ColumnData
                               ? (ColumnData) selObj
                               : null;
            final ColumnData baseData = getColumnData( selData, info );
            ColumnData cdata =
                  rowMap2 == null
                ? baseData
                : new ColumnData( baseData.getColumnInfo() ) {
                      public Object readValue( long lrow ) throws IOException {
                          int jrow = AbstractStarTable.checkedLongToInt( lrow );
                          return baseData.readValue( rowMap2[ jrow ] );
                      }
                  };
            t2.addColumn( cdata );
        }
        return new ConcatStarTable( t1, new StarTable[] { t1, t2 } );
    }

    /**
     * Indicates whether two classes have similar types.
     * Similarity means that integer types match with each other
     * and so do floating point types.
     *
     * @param  clazz1  first type
     * @param  clazz2  second type
     * @return   true iff classes are similar
     */
    private static boolean isMatchingClass( Class<?> clazz1, Class<?> clazz2 ) {
        return clazz1.equals( clazz2 )
            || isIntegerClass( clazz1 ) && isIntegerClass( clazz2 )
            || isFloatingClass( clazz1 ) && isFloatingClass( clazz2 );
    }

    /**
     * Indicates whether a class is an integer numeric type.
     *
     * @param  clazz  type
     * @return  true if integer wrapper type
     */
    private static boolean isIntegerClass( Class<?> clazz ) {
        return Byte.class.equals( clazz )
            || Short.class.equals( clazz )
            || Integer.class.equals( clazz )
            || Long.class.equals( clazz );
    }

    /**
     * Indicates whether a class is a floating point numeric type.
     *
     * @param  clazz  type
     * @return  true if floating point wrapper type
     */
    private static boolean isFloatingClass( Class<?> clazz ) {
        return Float.class.equals( clazz )
            || Double.class.equals( clazz );
    }

    /**
     * Returns a column data object based data from a selected one,
     * but with a given metadata object.
     *
     * @param  cdata   object supplying data (may be null)
     * @param  object supplying metadata
     * @return   column data (not null)
     */
    private static ColumnData getColumnData( final ColumnData cdata,
                                             ColumnInfo info ) {
        if ( cdata == null ) {
            return new BlankColumn( info );
        }
        else {
            Class<?> dataClazz = cdata.getColumnInfo().getContentClass();
            Class<?> reqClazz = info.getContentClass();
            if ( dataClazz.equals( reqClazz ) ) {
                return cdata;
            }
            else if ( Number.class.isAssignableFrom( reqClazz ) &&
                      Number.class.isAssignableFrom( dataClazz ) ) {
                ColumnData tncdata = TranslateNumericTypeColumnData
                                    .createInstance( cdata, info );
                if ( tncdata != null ) {
                    return tncdata;
                }
            }
        }
        throw new IllegalArgumentException( "Incompatible data types "
                                          + cdata.getColumnInfo()
                                          + " and " + info );
    }

    /**
     * Action definitions for ConcatWindow.
     */
    private class ConcatAction extends BasicAction {
        ConcatAction( String name, Icon icon, String description ) {
            super( name, icon, description );
        }
        public void actionPerformed( ActionEvent evt ) {
            Component parent = ConcatWindow.this;
            if ( this == goAct ) {
                try {
                    String label = "concat(" + getBaseTable().getID()
                                 + "+" + getAddedTable().getID() + ")";
                    TopcatModel tcModel = 
                        ControlWindow.getInstance()
                                     .addTable( makeTable(), label, true );
                    String title = "Tables Concatenated";
                    String msg =
                        "New concatenated table " + tcModel + " created";
                    JOptionPane.showMessageDialog( parent, msg, title,
                                                   JOptionPane
                                                  .INFORMATION_MESSAGE );
                    dispose();
                }
                catch ( Exception e ) {
                    ErrorDialog.showError( parent, "No Concatenation: " + e,
                                           e );
                }
            }
            else {
                throw new AssertionError();
            }
        }
    }

    /**
     * ColumnData implementation that can translate between input and
     * a required numeric data type.
     */
    private static abstract class TranslateNumericTypeColumnData
            extends ColumnData {
        final ColumnData cdata_;

        /**
         * Constructor.
         *
         * @param  cdata  input column data
         * @param   required metadata
         */
        TranslateNumericTypeColumnData( ColumnData cdata, ColumnInfo info ) {
            super( info );
            cdata_ = cdata;
        }

        /**
         * Translates from input to required numeric type.
         *
         * @param  value  input numeric value (not null)
         * @return  numeric value of required type
         */
        protected abstract Object translate( Number value );

        public Object readValue( long irow ) throws IOException {
            Object v = cdata_.readValue( irow );
            return v instanceof Number && ! Tables.isBlank( v )
                 ? translate( (Number) v )
                 : null;
        }

        /**
         * Creates an instance of this class.
         *
         * @param  cdata  object supplying input data
         * @param  info   object supplying required metadata
         * @return  new ColumnData instance
         */
        public static TranslateNumericTypeColumnData
                createInstance( ColumnData cdata, ColumnInfo info ) {
            Class<?> clazz = info.getContentClass();
            if ( Byte.class.equals( clazz ) ) {
                return new TranslateNumericTypeColumnData( cdata, info ) {
                    protected Object translate( Number value ) {
                        return Byte.valueOf( value.byteValue() );
                    }
                };
            }
            else if ( Short.class.equals( clazz ) ) {
                return new TranslateNumericTypeColumnData( cdata, info ) {
                    protected Object translate( Number value ) {
                        return Short.valueOf( value.shortValue() );
                    }
                };
            }
            else if ( Integer.class.equals( clazz ) ) {
                return new TranslateNumericTypeColumnData( cdata, info ) {
                    protected Object translate( Number value ) {
                        return Integer.valueOf( value.intValue() );
                    }
                };
            }
            else if ( Long.class.equals( clazz ) ) {
                return new TranslateNumericTypeColumnData( cdata, info ) {
                    protected Object translate( Number value ) {
                        return Long.valueOf( value.longValue() );
                    }
                };
            }
            else if ( Float.class.equals( clazz ) ) {
                return new TranslateNumericTypeColumnData( cdata, info ) {
                    protected Object translate( Number value ) {
                        return Float.valueOf( value.floatValue() );
                    }
                };
            }
            else if ( Double.class.equals( clazz ) ) {
                return new TranslateNumericTypeColumnData( cdata, info ) {
                    protected Object translate( Number value ) {
                        return Double.valueOf( value.doubleValue() );
                    }
                };
            }
            else {
                return null;
            }
        }
    }
}
