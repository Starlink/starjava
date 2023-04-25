package uk.ac.starlink.ttools.mode;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.table.gui.TableRowHeader;
import uk.ac.starlink.table.gui.ViewHugeSelectionModel;
import uk.ac.starlink.table.gui.ViewHugeTableModel;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.util.gui.SizingScrollPane;

/**
 * Processing mode for displaying the table in a scrollable Swing window.
 *
 * @author   Mark Taylor
 * @since    6 Jul 2014
 */
public class SwingMode implements ProcessingMode, TableConsumer {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.mode" );

    public TableConsumer createConsumer( Environment env ) {
        return this;
    }

    public Parameter<?>[] getAssociatedParameters() {
        return new Parameter<?>[ 0 ];
    }

    public String getDescription() {
        return DocUtils.join( new String[] {
            "<p>Displays the table in a scrollable window.",
            "</p>",
        } );
    }

    public void consume( StarTable table ) throws IOException {
        long nrow = table.getRowCount();
        JScrollPane scroller = new SizingScrollPane();
        StarJTable jt = new StarJTable( table, false );
        final TableRowHeader rowHeader;
        final ListSelectionModel rowSelModel = new DefaultListSelectionModel();

        /* More than Integer.MAX_VALUE rows can't yet be handled.
         * It would be possible to address this with a similar approach
         * to the existing ViewHugeTableModel implementation. */
        if ( nrow > Integer.MAX_VALUE ) {
            throw new IOException( "Sorry, table is too big to view"
                                 + " (" + nrow + " > 2^31)" );
        }

        /* If required, make special arrangements for tables that would
         * result in a JTable with height >2^31 pixels. */
        if ( nrow > ViewHugeTableModel.VIEWSIZE ) {
            logger_.info( "Large table (" + nrow + " rows)"
                        + ": using ViewHugeTableModel" );
            JScrollBar vbar = scroller.getVerticalScrollBar();
            final ViewHugeTableModel vhModel =
                new ViewHugeTableModel( jt.getModel(), vbar );
            jt.setModel( vhModel );

            /* Resetting the JTable's TableModel also resets the contents of
             * the column model, in particular the cell renderers,
             * so take care to put them back. */
            int ncol = table.getColumnCount();
            TableColumnModel colModel = jt.getColumnModel();
            for ( int icol = 0; icol < ncol; icol++ ) {
                ColumnInfo cinfo = table.getColumnInfo( icol );
                colModel.getColumn( icol )
                        .setCellRenderer( StarTableColumn
                                         .createCellRenderer( cinfo ) );
            }
            ListSelectionModel vsModel =
                new ViewHugeSelectionModel( rowSelModel, vhModel );
            jt.setSelectionModel( vsModel );
            rowHeader = new TableRowHeader( jt ) {
                public long rowNumber( int irow ) {
                    return vhModel.getHugeRow( irow ) + 1;
                }
            };
            rowHeader.setLongestNumber( nrow );
            vhModel.addPropertyChangeListener( new PropertyChangeListener() {
                public void propertyChange( PropertyChangeEvent evt ) {
                    if ( ViewHugeTableModel.VIEWBASE_PROPERTY
                                           .equals( evt.getPropertyName() ) ) {
                        rowHeader.repaint();
                    }
                }
            } );
        }

        /* Normal size. */
        else {
            jt.setSelectionModel( rowSelModel );
            rowHeader = new TableRowHeader( jt ) {
                public long rowNumber( int irow ) {
                    return irow + 1;
                }
            };
        }
        scroller.setViewportView( jt );
        rowHeader.installOnScroller( scroller );
        StarJTable.configureColumnWidths( jt, 600, 500 );
 
        /* Post window to screen. */
        final JFrame frame = new JFrame();
        frame.getContentPane().add( scroller );
        frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        Object quitKey = "quit";
        jt.getInputMap().put( KeyStroke.getKeyStroke( 'q' ), quitKey );
        jt.getActionMap().put( quitKey, new AbstractAction() {
            public void actionPerformed( ActionEvent evt ) {
                frame.dispose();
            }
        } );
        frame.pack();
        frame.setVisible( true );
    }
}
