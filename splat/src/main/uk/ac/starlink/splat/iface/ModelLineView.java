/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *    01-APR-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import diva.canvas.interactor.SelectionEvent;
import diva.canvas.interactor.SelectionListener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.starlink.ast.gui.AstCellEditor;
import uk.ac.starlink.ast.gui.AstDouble;
import uk.ac.starlink.diva.interp.Interpolator;
import uk.ac.starlink.splat.plot.DivaPlot;
import uk.ac.starlink.splat.plot.DivaPlotGraphicsPane;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.splat.util.GaussianInterp;
import uk.ac.starlink.splat.util.LorentzInterp;
import uk.ac.starlink.splat.util.VoigtInterp;
import uk.ac.starlink.util.AsciiFileParser;
import uk.ac.starlink.util.gui.BasicFileChooser;
import uk.ac.starlink.util.gui.BasicFileFilter;
import uk.ac.starlink.diva.InterpolatedCurveFigure;

/**
 * ModelLineView is controller and view for any properties associated
 * with ModelLine spectra that are drawn on a {@link DivaPlot}.  the X
 * dimension of a DivaPlot (and the current spectrum displayed.  The
 * view is a {@link JTable} that shows the current spectra and their
 * descriptions, the controllers are buttons to add new spectra and
 * delete existing ones. 
 * <p>
 * To encapsulate often required facilities this class also provides utility
 * methods for reading and writing the spectra from/to disk file and
 * an Action for initiating a file chooser to control this process.
 * <p>
 * Store all this in a VOTable.
 *
 * @author Peter W. Draper
 * @version $Id$
 *
 * @see ModelLine
 * @see DivaPlot
 * @see ModelLineTableModel
 * @see DeblendFrame
 */
public class ModelLineView
    extends JPanel
    implements SelectionListener
{
    /**
     * The DivaPlot that we are working with.
     */
    protected DivaPlot plot = null;

    /**
     * The JList showing the properties.
     */
    protected JTable table = new JTable();

    /**
     * The selection model used by the JTable.
     */
    ListSelectionModel selectionModel = null;

    /**
     * GraphicsPane that contains all the figures.
     */
    protected DivaPlotGraphicsPane pane = null;

    /**
     * The colour of any figures that are created.
     */
    protected Color colour = Color.green;

    /**
     * Create all visual components.
     */
    protected TitledBorder title =
        BorderFactory.createTitledBorder( "Components:" );
    protected JScrollPane scroller = new JScrollPane();
    protected ModelLineTableModel model = null;
    protected JPanel actionBar = new JPanel();

    /**
     * Create an instance.
     */
    public ModelLineView( DivaPlot plot )
    {
        setPlot( plot );
        initUI();
    }

    /**
     * Create an instance with a given colour and constained property.
     *
     * @param plot the DivaPlot that we're drawing into.
     * @param colour the colour that any figures should be drawn using.
     */
    public ModelLineView( DivaPlot plot, Color colour )
    {
        setPlot( plot );
        setColour( colour );
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
        pane = (DivaPlotGraphicsPane) plot.getGraphicsPane();

        //  Listen out for figures being selected.
        pane.addSelectionListener( this );
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

        //  Set the model of ranges used by the JTable.
        model = new ModelLineTableModel( plot );
        table.setModel( model );

        //  Add a sensible cell renderer for numbers. Also define the
        //  ranges columns to formatted by the DivaPlot (so see
        //  appropriate precision etc.).
        NumberCellRenderer ncr = new NumberCellRenderer();
        table.setDefaultRenderer( AstDouble.class, ncr );
        table.setDefaultRenderer( Double.class, ncr );
        table.setDefaultRenderer( Float.class, ncr );
        table.setDefaultRenderer( Integer.class, ncr );

        table.setDefaultEditor( AstDouble.class, new AstCellEditor() );

        //  Selecting a row makes that range current.
        selectionModel = table.getSelectionModel();
        selectionModel.setSelectionMode(
            ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
        selectionModel.addListSelectionListener(
            new ListSelectionListener()
            {
                public void valueChanged( ListSelectionEvent e )
                {
                    selectionChanged( e );
                }
            } );

        //  Add action for Gaussian.
        GaussianAction gaussAction = new GaussianAction( "Gaussian" );
        JButton gaussButton = new JButton( gaussAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( gaussButton );
        gaussButton.setToolTipText( "Draw a new Gaussian component" );

        //  Add action for Lorentzian.
        LorentzAction lorentzAction = new LorentzAction( "Lorentz" );
        JButton lorentzButton = new JButton( lorentzAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( lorentzButton );
        lorentzButton.setToolTipText( "Draw a new Lorentzian component" );

        //  Add action for Voigt profile.
        VoigtAction voigtAction = new VoigtAction( "Voigt" );
        JButton voigtButton = new JButton( voigtAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( voigtButton );
        voigtButton.setToolTipText( "Draw a new Voigt component" );


        //  Add action for "delete" button.
        DeleteAction deleteAction = new DeleteAction( "Delete" );
        JButton deleteButton = new JButton( deleteAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( deleteButton );
        deleteButton.setToolTipText
            ( "Delete the selected components from list" );
        actionBar.add( Box.createGlue() );

        //  Add components.
        setBorder( title );
        add( scroller, BorderLayout.CENTER );
        scroller.getViewport().add( table, null );
        add( actionBar, BorderLayout.SOUTH );
    }

    /**
     * Invoked when the selected spectrum changes.
     */
    protected void selectionChanged( ListSelectionEvent e )
    {
        int index = e.getFirstIndex();
    }

    /**
     * Get the currently selected rows as a list.
     *
     * @return indices of the rows that are selected.
     */
    public int[] getSelectedIndices()
    {
        int min = selectionModel.getMinSelectionIndex();
        int max = selectionModel.getMaxSelectionIndex();

        //  -1 means no selection.
        if ( ( min < 0 ) || ( max < 0 ) ) {
            return null;
        }

        //  Traverse the selection range picking out the selected
        //  indices.
        int[] tmp = new int[max - min + 1];
        int n = 0;
        for ( int i = min; i <= max; i++ ) {
            if ( selectionModel.isSelectedIndex( i ) ) {
                tmp[n++] = i;
            }
        }

        //  Copy the results to a new (truncated) array, if needed.
        if ( n != ( max - min + 1 ) ) {
            int[] newtmp = new int[n];
            System.arraycopy( tmp, 0, newtmp, 0, n );
            return newtmp;
        }
        else {
            return tmp;
        }
    }


    /**
     * Create a new spectrum and arrange to have it added to the model, when
     * drawn.
     */
    protected void createSpectrum( Interpolator interpolator )
    {
        new ModelLine( plot, model, colour, interpolator, true );
    }

    /**
     * Create a new spectrum with the given properties.
     *
     * @param props an array of properties in world coordinates (not
     *              graphics).
     */
    protected void createSpectrum( Interpolator interpolator, 
                                   double[] props )
    {
        new ModelLine( plot, model, colour, interpolator, props );
    }

    /**
     * Delete any selected spectra.
     */
    protected void deleteSelected()
    {
        int[] selected = getSelectedIndices();
        if ( selected != null ) {
            for ( int i = selected.length - 1; i >= 0; i-- ) {
                delete( selected[i] );
            }
        }
    }

    /**
     * Delete all spectra (typical when containing window withdrawn).
     */
    public void deleteAll()
    {
        for ( int i = model.getRowCount() - 1; i >= 0; i-- ) {
            delete( i );
        }
    }

    /**
     * Delete a spectrum (index of position in list).
     */
    public void delete( int index )
    {
        model.remove( index );
    }

    /**
     * Add action. Adds a new spectral component to the plot.
     */
    protected class GaussianAction extends AbstractAction
    {
        public GaussianAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            GaussianInterp interpolator = new GaussianInterp();
            createSpectrum( interpolator );
        }
    }

    protected class LorentzAction extends AbstractAction
    {
        public LorentzAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            LorentzInterp interpolator = new LorentzInterp();
            createSpectrum( interpolator );
        }
    }

    protected class VoigtAction extends AbstractAction
    {
        public VoigtAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            VoigtInterp interpolator = new VoigtInterp();
            createSpectrum( interpolator );
        }
    }

    /**
     * Delete action. Removes selected spectra from plot.
     */
    protected class DeleteAction extends AbstractAction
    {
        public DeleteAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            deleteSelected();
        }
    }

    /**
     * Set the colour of any figures.
     *
     * @param colour The new colour value
     */
    public void setColour( Color colour )
    {
        this.colour = colour;
    }

    /**
     * Get the colour of any figures.
     *
     * @return The colour value
     */
    public Color getColour()
    {
        return colour;
    }

    //
    //  Utilities for interacting with disk files.
    //
    /**
     * File chooser used for reading and writing text files.
     */
    protected BasicFileChooser fileChooser = null;

    /**
     * Initialise the file chooser to have the necessary filters.
     */
    protected void initFileChooser()
    {
        if ( fileChooser == null ) {
            fileChooser = new BasicFileChooser( false );
            fileChooser.setMultiSelectionEnabled( false );

            //  Add a filter for text files.
            BasicFileFilter textFileFilter =
                new BasicFileFilter( "txt", "TEXT files" );
            fileChooser.addChoosableFileFilter( textFileFilter );

            //  But allow all files as well.
            fileChooser.addChoosableFileFilter
                ( fileChooser.getAcceptAllFileFilter() );
        }
    }

    /**
     * Initiate a file selection dialog and choose a file that contains a list
     * of fitting ranges.
     */
    public void getFileWithSpectra()
    {
        initFileChooser();
        int result = fileChooser.showOpenDialog( this );
        if ( result == fileChooser.APPROVE_OPTION ) {
            File file = fileChooser.getSelectedFile();
            readSpectraFromFile( file );
        }
    }

    /**
     * Initiate a file selection dialog and choose a file to contain the
     * ranges.
     */
    public void getFileForSpectra()
    {
        initFileChooser();
        int result = fileChooser.showSaveDialog( this );
        if ( result == fileChooser.APPROVE_OPTION ) {
            File file = fileChooser.getSelectedFile();

            //  If file exists then we need to overwrite. Permission
            //  please.
            if ( file.exists() ) {
                if ( file.canWrite() && file.isFile() ) {
                    int choice = JOptionPane.showConfirmDialog
                        ( this, "The file '" + file.getName() +
                        "' already exists.\nDo you want to overwrite it?",
                        "File exists",
                        JOptionPane.YES_NO_OPTION );
                    if ( choice == JOptionPane.NO_OPTION ) {
                        return;
                    }
                }
                else {

                    //  File exists, but is directory or read-only, so
                    //  cannot overwrite.
                    JOptionPane.showMessageDialog
                        ( this, "The file '" + file.getName() +
                        "' cannot be overwritten",
                        "Error writing ",
                        JOptionPane.ERROR_MESSAGE );
                    return;
                }
            }
            writeSpectraToFile( file );
        }
    }

    /**
     * Read a set of spectra from a file. These are added to the existing
     * spectra. The file should be simple and have four
     * fields, separated by whitespace or commas. Comments are
     * indicated by lines starting with a hash (#) and are ignored.
     *
     * @param file reference to the file.
     */
    public void readSpectraFromFile( File file )
    {
        //  Check file exists.
        if ( ! file.exists() && file.canRead() && file.isFile() ) {
            return;
        }
        AsciiFileParser parser = new AsciiFileParser( file );
        int nfields = parser.getNFields();
        if ( nfields != 4 ) {
            JOptionPane.showMessageDialog
                ( this,
                "The format of ranges file requires four fields + (" +
                nfields + " were found)",
                "Error reading " + file.getName(),
                JOptionPane.ERROR_MESSAGE );
        }

        int nrows = parser.getNRows();
        double[] props = new double[4];
        for ( int i = 0; i < nrows; i++ ) {
            for ( int j = 0; j < nfields; j++ ) {
                props[j] = parser.getDoubleField( i, j );
            }

            //  Create the new range.
            if ( props[2] == 0.0 ) {
                createSpectrum( new LorentzInterp(), props );
            }
            else if ( props[3] == 0.0 ) {
                createSpectrum( new GaussianInterp(), props );
            }
            else {
                createSpectrum( new VoigtInterp(), props );
            }
        }
    }

    /**
     * Write the current spectra to a simple text file.
     *
     * @param file reference to the file.
     */
    public void writeSpectraToFile( File file )
    {
        //  Get a BufferedWriter to write the file line-by-line.
        FileOutputStream f = null;
        BufferedWriter r = null;
        try {
            f = new FileOutputStream( file );
            r = new BufferedWriter( new OutputStreamWriter( f ) );
        }
        catch ( Exception e ) {
            e.printStackTrace();
            return;
        }

        // Add a header to the file.
        try {
            r.write( "# File created by "+ Utilities.getReleaseName() +"\n" );
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }

        // Now write the data.
        int[] indices = getSelectedIndices();
        for ( int i = 0; i < indices.length; i++ ) {
            ModelLine line = model.getLine( i );
            double[] props = line.getProps();
            try {
                r.write( props[i] + " " + props[i + 1] +
                         props[i + 2] + " " + props[i + 3] + "\n" );
            }
            catch ( Exception e ) {
                e.printStackTrace();
            }
        }
        try {
            r.newLine();
            r.close();
            f.close();
        }
        catch ( Exception e ) {
            //  Do nothing.
        }
    }

    /**
     * Create an action for starting to read a file from disk.
     */
    public Action getReadAction( String name )
    {
        return new ReadAction( name );
    }

    /**
     * Create an action for starting to write to a disk file.
     */
    public Action getWriteAction( String name )
    {
        return new WriteAction( name );
    }

    /**
     * Inner Action that read ranges from disk file
     */
    protected class ReadAction extends AbstractAction
    {
        public ReadAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            getFileWithSpectra();
        }
    }

    /**
     * Inner Action that writes ranges to a disk file.
     */
    protected class WriteAction extends AbstractAction
    {
        public WriteAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            getFileForSpectra();
        }
    }


    //
    //  SelectionListener interface.
    //
    /**
     * Called when a figure is selected on the graphics pane. We're only
     * interested in InterpolatedCurveFigures that we've created.
     */
    public void selectionChanged( SelectionEvent e )
    {
        Object[] ranges = pane.getSelectionAsArray();
        int[] indices = new int[ranges.length];
        int nok = 0;
        for ( int i = 0; i < ranges.length; i++ ) {
            if ( ranges[i] instanceof InterpolatedCurveFigure ) {
                // Check this is "ours".
                int index = 
                    model.findFigure( (InterpolatedCurveFigure) ranges[i] );
                if ( index != -1 ) {
                    indices[nok++] = index;
                }
            }
        }
        if ( nok == 0 ) {
            //  Nothing selected.
            table.clearSelection();
        }
        else {
            if ( nok > ranges.length ) {

                //  Remove trailing values, indices array not completely used.
                int[] temp = new int[nok];
                for ( int i = 0; i < nok; i++ ) {
                    temp[i] = indices[i];
                }
                indices = temp;
            }

            //  Select the affected rows.
            selectionModel.clearSelection();
            for ( int i = 0; i < indices.length; i++ ) {
                selectionModel.addSelectionInterval( indices[i], indices[i] );
            }
        }
    }
}
