package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import uk.ac.starlink.util.gui.SizingScrollPane;

/**
 * Window that presents algebraic subsets for addition to multiple tables.
 * The user can make adjustments as required.
 *
 * @author   Mark Taylor
 * @since    18 Sep 2018
 */
public class MultiSubsetQueryWindow extends QueryWindow {

    private final JComboBox<String> nameSelector_;
    private final JTable jtable_;
    private final Entry[] entries_;
    private static final Color ENABLE_COLOR =
        UIManager.getColor( "Label.foreground" );
    private static final Color DISABLE_COLOR =
        UIManager.getColor( "Label.disabledForeground" );

    /**
     * Constructor.
     *
     * @param   title  window title
     * @param   parent  parent component
     * @param   entries  list of subset expressions that are potentially
     *                   to be added
     * @param   jelExpr  JEL expression giving generic description of subset
     * @param   adqlExpr  ADQL expression giving generic description of subset
     */
    @SuppressWarnings("this-escape")
    public MultiSubsetQueryWindow( String title, Component parent,
                                   Entry[] entries, String jelExpr,
                                   String adqlExpr ) {
        super( title, parent, true, true );
        entries_ = entries;

        /* Component for displaying generic expressions for subset. */
        TextItemPanel exprPanel = new TextItemPanel();
        exprPanel.addItem( "TOPCAT", jelExpr );
        exprPanel.addItem( "ADQL", adqlExpr );
        exprPanel.setBorder( BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder( 0, 0, 5, 0 ),
            makeTitledBorder( "Expressions" ) ) );

        /* Component for selecting subset name.  This provides options
         * from the first table in the list not the other tables,
         * but you can always enter a name by hand. */
        nameSelector_ = entries[ 0 ].tcModel_.createNewSubsetNameSelector();

        /* Component for displaying the subset expressions to be added
         * in a table. */
        List<MetaColumn> colList = new ArrayList<MetaColumn>();
        int icCreate = colList.size();
        colList.add( new MetaColumn( "Create", Boolean.class ) {
            public Object getValue( int irow ) {
                return Boolean.valueOf( entries_[ irow ].create_ );
            }
            public boolean isEditable( int irow ) {
                return true;
            }
            public void setValue( int irow, Object value ) {
                entries_[ irow ].create_ = Boolean.TRUE.equals( value );
            }
        } );
        int icTable = colList.size();
        colList.add( new MetaColumn( "Table", String.class ) {
            public Object getValue( int irow ) {
                return entries_[ irow ].tcModel_.toString();
            }
        } );
        int icExpr = colList.size();
        colList.add( new MetaColumn( "Expression", String.class ) {
            public Object getValue( int irow ) { 
                return entries_[ irow ].expr_;
            }
            public boolean isEditable( int irow ) {
                return true;
            }
            public void setValue( int irow, Object value ) {
                entries_[ irow ].setExpression( value instanceof String
                                              ? (String) value
                                              : null );
            }
        } );
        TableModel tableModel = new MetaColumnTableModel( colList ) {
            public int getRowCount() {
                return entries_.length;
            }
        };
        jtable_ = new JTable( tableModel );
        jtable_.setRowSelectionAllowed( false );
        jtable_.setColumnSelectionAllowed( false );
        TableColumnModel tcm = jtable_.getColumnModel();
        TableColumn tcolCreate = tcm.getColumn( icCreate );
        TableColumn tcolTable = tcm.getColumn( icTable );
        TableColumn tcolExpr = tcm.getColumn( icExpr );
        tcolExpr.setCellRenderer( createCheckCellRenderer() );
        tcolCreate.setMaxWidth( 50 );
        tcolCreate.setPreferredWidth( 50 );
        tcolTable.setMaxWidth( 150 );
        tcolTable.setPreferredWidth( 150 );
        tcolTable.setMaxWidth( 500 );
        jtable_.setAutoResizeMode( JTable.AUTO_RESIZE_LAST_COLUMN );

        /* Place components. */
        JComponent box = Box.createVerticalBox();
        box.add( exprPanel );
        box.add( new LineBox( "Subset Name", nameSelector_ ) );
        box.add( Box.createVerticalStrut( 10 ) );
        box.add( new SizingScrollPane( jtable_ ) );
        box.add( Box.createHorizontalStrut( 550 ) );
        getMainArea().add( box );

        /* Add actions. */
        getToolBar().add( MethodWindow.getWindowAction( this, false ) );
        getToolBar().addSeparator();

        /* Add help. */
        addHelp( "MultiSubsetQueryWindow" );
    }

    /**
     * Returns the name currently selected for the new subset(s).
     *
     * @return  user-selected subset name
     */
    private String getSubsetName() {
        Object nameObj = nameSelector_.getSelectedItem();
        return nameObj == null ? null : nameObj.toString();
    }

    protected boolean perform() {

        /* Ensure that any expression editing is complete.
         * If you don't do this, then it's quite easy to have text in
         * the expression field that's changed, but that hasn't been
         * passed to the Entry list. */
        if ( jtable_.isEditing() ) {
            jtable_.getCellEditor().stopCellEditing();
        }

        /* Get the name for the new subsets; exit if none. */
        String name = getSubsetName();
        if ( name == null || name.trim().length() == 0 ) {
            JOptionPane.showMessageDialog( this, "No subset name chosen",
                                           "Subset Creation Error",
                                           JOptionPane.ERROR_MESSAGE );
            return false;
        }

        /* Prepare a list of subsets to add to tables. */
        Map<TopcatModel,RowSubset> rsetMap =
            new LinkedHashMap<TopcatModel,RowSubset>();
        List<String> messages = new ArrayList<String>();
        int nfail = 0;
        for ( Entry entry : entries_ ) {
            if ( entry.create_ ) {
                if ( TopcatJELUtils.isSubsetReferenced( entry.tcModel_, name,
                                                        entry.expr_ ) ) {
                    nfail++;
                    messages.add( "Recursive subset expression disallowed:\n" +
                                  "\"" + entry.expr_ + "\"\n" +
                                  "directly or indirectly references subset " +
                                  name );
                }
                else {
                    try {
                        rsetMap.put( entry.tcModel_,
                                     entry.createSubset( name ) );
                    }
                    catch ( CompilationException e ) {
                        nfail++;
                        messages.add( e.getMessage() );
                    }
                }
            }
        }

        /* If there were any errors, present the errors and don't add
         * any subsets. */
        if ( nfail > 0 ) {
            List<JLabel> msgList = new ArrayList<JLabel>();
            boolean isMulti = messages.size() > 1;
            msgList.add( new JLabel( "Expression error"
                                   + ( isMulti ? ":" : "" ) + ":" ) );
            for ( String msg : messages ) {
                for ( String line : msg.split( "\\n" ) ) {
                    msgList.add( new JLabel( "   " + line ) );
                }
                if ( isMulti ) {
                    msgList.add( new JLabel( " " ) );
                }
            }
            JOptionPane.showMessageDialog( this, msgList.toArray(),
                                           "Subset Creation Error",
                                           JOptionPane.ERROR_MESSAGE );
            return false;
        }

        /* If all the subsets could be created successfully, add them. */
        else {
            for ( Map.Entry<TopcatModel,RowSubset> item : rsetMap.entrySet() ) {
                item.getKey().addSubset( item.getValue() );
            }
            return rsetMap.size() > 0;
        }
    
    }

    /**
     * Creates a table cell renderer that greys out text for cells that
     * correspond to bad JEL expressions.
     *
     * @return  table cell renderer
     */
    private TableCellRenderer createCheckCellRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent( JTable table,
                                                            Object value,
                                                            boolean isSel,
                                                            boolean hasFocus,
                                                            int irow,
                                                            int icol ) {
                Component c =
                    super.getTableCellRendererComponent( table, value, isSel,
                                                         hasFocus, irow, icol );
                c.setForeground( entries_[ irow ].canCompile_
                               ? ENABLE_COLOR : DISABLE_COLOR );
                return c;
            }
        };
    }

    /**
     * Represents a subset to add.
     */
    public static class Entry {
        private final TopcatModel tcModel_;
        private String expr_;
        private boolean create_;
        boolean canCompile_;

        /**
         * Constructor.
         *
         * @param  tcModel  target table
         * @param  expr   text of JEL expression defining subset
         */
        public Entry( TopcatModel tcModel, String expr ) {
            tcModel_ = tcModel;
            create_ = true;
            setExpression( expr );
        }

        /**
         * Attempts to create a synthetic subset based on this entry's
         * current expression.
         *
         * @param  name  subset name
         * @return  synthetic subset
         * @throws  CompilationException in case of failure
         */
        private RowSubset createSubset( String name )
                throws CompilationException {
            return new SyntheticRowSubset( name, tcModel_, expr_ );
        }

        /**
         * Sets the expression.  The canCompile flag is updated.
         *
         * @param  expr  new JEL expression
         */
        private void setExpression( String expr ) {
            expr_ = expr;
            try {
                createSubset( "dummy" );
                canCompile_ = true;
            }
            catch ( CompilationException e ) {
                canCompile_ = false;
            }
        }
    }
}
