package uk.ac.starlink.splat.iface;

import diva.canvas.interactor.SelectionEvent;
import diva.canvas.interactor.SelectionListener;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;

import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.plot.DivaPlotGraphicsPane;
import uk.ac.starlink.splat.plot.PlotRectangle;
import uk.ac.starlink.splat.plot.DivaPlot;

/**
 * Component for displaying the results of a series of measurements of
 * spectral lines.
 *
 * @author Peter W. Draper
 * @created June 6, 2002
 * @since $Date$
 * @since 19-JAN-2001
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 */
public class LineView extends JPanel implements SelectionListener
{
    /**
     * The Plot that we are working with.
     */
    protected DivaPlot plot = null;

    /**
     * The JTable containing the line properties.
     */
    protected JTable table = new JTable();

    /**
     * The model of the spectral-line data.
     */
    LineViewModel model = new LineViewModel();

    /**
     * GraphicsPane that contains all the figures.
     */
    protected DivaPlotGraphicsPane pane = null;

    /**
     * Create all visual components.
     */
    protected JLabel title = new JLabel( "Lines:" );
    /**
     * Description of the Field
     */
    protected JScrollPane scroller = new JScrollPane();
    /**
     * Description of the Field
     */
    protected JPanel actionBar = new JPanel();

    /**
     * Create an instance.
     *
     * @param plot Description of the Parameter
     */
    public LineView( DivaPlot plot )
    {
        setPlot( plot );
        initUI();
    }

    /**
     * Set the plot used.
     *
     * @param plot The new plot value
     */
    protected void setPlot( DivaPlot plot )
    {
        this.plot = plot;
        this.pane = plot.getGraphicsPane();

        //  Listen out for figures being selected.
        this.pane.addSelectionListener( this );
    }

    /**
     * Initialise the various user interface components.
     */
    protected void initUI()
    {
        setLayout( new BorderLayout() );

        //  Set up the actionBar.
        actionBar.setLayout( new BoxLayout( actionBar, BoxLayout.X_AXIS ) );
        actionBar.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );

        //  Set the model of lines used by the JTables, columns
        //  have full size at start.
        table.setModel( model );
        table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        scroller.getViewport().add( table );

        //  Increase header size to allow two-line text.
        JTableHeader jhead = table.getTableHeader();
        Dimension size = jhead.getPreferredSize();
        size.setSize( size.width, size.height * 2 );
        jhead.setPreferredSize( size );

        //  Selecting a row makes that line current.
        table.getSelectionModel().addListSelectionListener
            (
            new ListSelectionListener()
            {
                public void valueChanged( ListSelectionEvent e )
                {
                    selectionChanged( e );
                }
            } );

        //  Get action bar icons.
        ImageIcon addImage = new ImageIcon(
            ImageHolder.class.getResource( "add.gif" ) );
        ImageIcon deleteImage = new ImageIcon(
            ImageHolder.class.getResource( "delete.gif" ) );

        //  Add action for "add" button.
        AddAction addAction = new AddAction( "Add", addImage );
        JButton addButton = new JButton( addAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( addButton );
        addButton.setToolTipText( "Select a line" );

        //  Add action for "delete" button.
        DeleteAction deleteAction = new DeleteAction( "Delete", deleteImage );
        JButton deleteButton = new JButton( deleteAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( deleteButton );
        deleteButton.setToolTipText( "Delete the selected lines" );
        actionBar.add( Box.createGlue() );

        //  Add components.
        add( title, BorderLayout.NORTH );
        add( scroller, BorderLayout.CENTER );
        add( actionBar, BorderLayout.SOUTH );
    }

    /**
     * Invoked when the selected line changes.
     *
     * @param e Description of the Parameter
     */
    protected void selectionChanged( ListSelectionEvent e )
    {
        int index = e.getFirstIndex();
    }

    /**
     * Create a new region and arrange to have it added to the model, when
     * drawn.
     */
    protected void createLine()
    {
        LineProperties props = new LineProperties( plot, model, true,
            false, false );
    }

    /**
     * Delete any selected regions.
     */
    protected void deleteSelectedLines()
    {
        int[] selected = table.getSelectedRows();
        for ( int i = selected.length - 1; i >= 0; i-- ) {
            deleteLine( selected[i] );
        }
    }

    /**
     * Delete all regions (typical when containing window withdrawn).
     */
    public void deleteAllLines()
    {
        for ( int i = model.getRowCount() - 1; i >= 0; i-- ) {
            deleteLine( i );
        }
    }

    /**
     * Delete a region (index of position in list).
     *
     * @param index Description of the Parameter
     */
    public void deleteLine( int index )
    {
        model.removeLine( index );
    }

    /**
     * Return an array of the lines currently being displayed.
     *
     * @return The lines value
     */
    public double[] getLines()
    {
        return model.getLines();
    }

    /**
     * Add or remove headers for gaussian properties.
     *
     * @param show The new showGaussians value
     */
    public void setShowGaussians( boolean show )
    {
        model.setShowGaussian( show );
    }

    /**
     * Add or remove headers for lorentzian properties.
     *
     * @param show The new showLorentzians value
     */
    public void setShowLorentzians( boolean show )
    {
        model.setShowLorentzian( show );
    }

    /**
     * Add or remove headers for voigt properties.
     *
     * @param show The new showVoigts value
     */
    public void setShowVoigts( boolean show )
    {
        model.setShowVoigts( show );
    }

    /**
     * Set the "quick" results for a line.
     *
     * @param index The new quickResults value
     * @param results The new quickResults value
     */
    public void setQuickResults( int index, double[] results )
    {
        model.setQuickResults( index, results );
    }

    /**
     * Set the "gaussian" results for a line.
     *
     * @param index The new gaussianResults value
     * @param results The new gaussianResults value
     */
    public void setGaussianResults( int index, double[] results )
    {
        model.setGaussianResults( index, results );
    }

    /**
     * Set the "lorentzian" results for a line.
     *
     * @param index The new lorentzianResults value
     * @param results The new lorentzianResults value
     */
    public void setLorentzianResults( int index, double[] results )
    {
        model.setLorentzianResults( index, results );
    }

    /**
     * Set the "voigt" results for a line.
     *
     * @param index The new voigtResults value
     * @param results The new voigtResults value
     */
    public void setVoigtResults( int index, double[] results )
    {
        model.setVoigtResults( index, results );
    }

    /**
     * Add action. Adds a new region to the plot.
     *
     * @author pdraper
     * @created June 6, 2002
     */
    protected class AddAction extends AbstractAction
    {
        /**
         * Constructor for the AddAction object
         *
         * @param name Description of the Parameter
         * @param icon Description of the Parameter
         */
        public AddAction( String name, Icon icon )
        {
            super( name, icon );
        }

        /**
         * Description of the Method
         *
         * @param ae Description of the Parameter
         */
        public void actionPerformed( ActionEvent ae )
        {
            createLine();
        }
    }

    /**
     * Delete action. Removes selected regions from plot.
     *
     * @author pdraper
     * @created June 6, 2002
     */
    protected class DeleteAction extends AbstractAction
    {
        /**
         * Constructor for the DeleteAction object
         *
         * @param name Description of the Parameter
         * @param icon Description of the Parameter
         */
        public DeleteAction( String name, Icon icon )
        {
            super( name, icon );
        }

        /**
         * Description of the Method
         *
         * @param ae Description of the Parameter
         */
        public void actionPerformed( ActionEvent ae )
        {
            deleteSelectedLines();
        }
    }

    //  SelectionListener interface.

    /**
     * Called when a figure is selected on the graphics pane.
     *
     * @param e Description of the Parameter
     */
    public void selectionChanged( SelectionEvent e )
    {
        Object[] ranges = pane.getSelectionAsArray();
        int[] indices = new int[ranges.length];
        int nok = 0;
        for ( int i = 0; i < ranges.length; i++ ) {
            if ( ranges[i] instanceof PlotRectangle ) {
                int index = model.findFigure( (PlotRectangle) ranges[i] );
                if ( index != -1 ) {
                    indices[nok++] = index;
                }
            }
        }
        table.clearSelection();
        if ( nok > 0 ) {
            for ( int i = 0; i < nok; i++ ) {
                table.addRowSelectionInterval( indices[i], indices[i] );
            }
        }
    }
}
