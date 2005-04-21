/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     27-SEP-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.FuzzyBoolean;

/**
 * SplatPlotTable provides a swing component that displays a list of
 * all the known plots and which of these are displaying the currently
 * selected spectrum or spectra. If any of the selected spectra are in
 * a plot, then a tick is shown, but the JCheckBox is coloured red.
 * <p>
 * The current spectrum or spectra are identified as those selected in
 * a given JList (whose indices are derived from the global
 * GlobalSpecPlotList object, of which this is also a kind of
 * view). The current plot is obviously the one selected in this window.
 *
 * @author Peter W. Draper (Starlink, Durham University)
 * @version $Id$
 */
public class SplatPlotTable
    extends JPanel
{
    /**
     * The JList containing primary view of available spectra
     * (provided from SplatBrowser).
     */
    protected JList list = null;

    /**
     *  Create all visual components.
     */
    protected JScrollPane scroller = new JScrollPane();
    protected JTable table = new JTable();

    /**
     *  Model that communicates with the global list of plots.
     */
    protected PlotTableModel tableModel = new PlotTableModel();

    /**
     *  Reference to global GlobalSpecPlotList object.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     *  Create an instance.
     *
     *  @param list a JList that contains a list of all the currently
     *              available spectra. The current spectra are
     *              selected here.
     */
    public SplatPlotTable( JList list )
    {
        this.list = list;
        initUI();
    }

    /**
     *  Add all the components for display the list of plots and which
     *  are showing the current spectra.
     */
    protected void initUI()
    {
        setBorder( BorderFactory.createTitledBorder
                   ( "Views of current spectra:" ) );
        setLayout( new BorderLayout() );
        setToolTipText( "Select to add the current spectra to a plot, "+
                        "deselect to remove them.");

        //  Set up the table to use a model based on the global lists
        //  of plots and spectra and to track the current spectrum
        //  when selected in list.
        table.setModel( tableModel );

        //  Hack the size of this so that it doesn't get too big for
        //  the values we know it's going to display, cell width
        //  control in JTable sucks (or I don't understand it!).
        table.setPreferredScrollableViewportSize( new Dimension( 250, 0 ) );

        //  Set the headers!
        TableColumnModel columns = table.getColumnModel();
        TableColumn column;
        column = columns.getColumn( table.convertColumnIndexToView( 0 ) );
        column.setHeaderValue( "View" );
        column = columns.getColumn( table.convertColumnIndexToView( 1 ) );
        column.setHeaderValue( "Displayed" );

        //  The table can have many rows selected.
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        //  Double click on a row raises the associated plot.
        table.addMouseListener( new MouseAdapter() {
                public void mouseClicked( MouseEvent e ) {
                    if ( e.getClickCount() >= 2 ) {
                        raiseCurrentPlot();
                    }
                }
            });

        //  Use a FuzzyBooleanCellRenderer for FuzzyBoolean objects in
        //  this table. This is tristate for dealing with partial
        //  spectra/plot relations.
        table.setDefaultRenderer( FuzzyBoolean.class, 
                                  new FuzzyBooleanCellRenderer() );
        table.setDefaultEditor( FuzzyBoolean.class, 
                                new FuzzyBooleanCellEditor() );

        //  Add components.
        scroller.getViewport().add( table, null );
        add( scroller, BorderLayout.CENTER );

        //  Set up the listSelectionListener so that we can update
        //  when the selected spectra change.
        list.addListSelectionListener( new ListSelectionListener()  {
                public void valueChanged( ListSelectionEvent e ) {
                    update( e );
                }
            });
    }

    /**
     *  Update the view for changes in the selected spectra.
     */
    protected void update( ListSelectionEvent e )
    {
        if ( ! e.getValueIsAdjusting() ) {
            int size = list.getModel().getSize();
            if ( size > 0 ) {
                int[] indices = list.getSelectedIndices();
                if ( indices.length > 0 && indices[0] > -1 ) {
                    tableModel.setSpectraSelectedIndices( indices );
                }
                else {
                    //  No selection.
                    tableModel.setSpectraSelectedIndices( null );
                }
            }
            else {
                //  No spectra.
                tableModel.setSpectraSelectedIndices( null );
            }
        }
    }

    /**
     *  Return a list of the plot indices of any selected rows.
     */
    public int[] getSelectedIndices()
    {
        int[] rows = table.getSelectedRows();
        if ( rows.length == 0 ) {
            return null;
        }
        return rows;
    }

    /**
     * Clear any selected rows.
     */
    public void clearSelection()
    {
        table.clearSelection();
    }

    /**
     * Select an interval of the table rows to the current selection.
     */
    public void addSelectionInterval( int lower, int upper )
    {
        table.addRowSelectionInterval( lower, upper );
    }

    /**
     *  Raise the current plot.
     */
    public void raiseCurrentPlot()
    {
        int row = table.getSelectedRow();
        if ( row > -1 ) {
            PlotControl currentPlot = globalList.getPlot( row );
            try {
                Frame baseFrame = (Frame)
                    SwingUtilities.getWindowAncestor( currentPlot );
                baseFrame.setState( Frame.NORMAL );
                baseFrame.setVisible( true );
            } 
            catch (Exception e) {
                // Do nothing.
            }
        }
    }

    /**
     * Return a reference to the table ListSelectionModel. This may
     * be used to listen for changes in the row selection (i.e. the
     * current Plot).
     */
    public ListSelectionModel getSelectionModel()
    {
        return table.getSelectionModel();
    }

    /**
     * Return a reference to the table ListSelectionModel. This may
     * be used to listen for changes in the row selection (i.e. the
     * current Plot).
     */
    public PlotTableModel getPlotTableModel()
    {
        return tableModel;
    }
}
