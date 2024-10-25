/*
 * Copyright (C) 2002-2004 Central Laboratory of the Research Councils
 *
 *  History:
 *    07-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import diva.canvas.interactor.SelectionEvent;
import diva.canvas.interactor.SelectionListener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.starlink.ast.gui.AstCellEditor;
import uk.ac.starlink.ast.gui.AstDouble;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.plot.DivaPlot;
import uk.ac.starlink.splat.plot.DivaPlotGraphicsPane;
import uk.ac.starlink.splat.util.Sort;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.util.AsciiFileParser;
import uk.ac.starlink.util.gui.BasicFileChooser;
import uk.ac.starlink.util.gui.BasicFileFilter;

import uk.ac.starlink.diva.XRangeFigure;

/**
 * XGraphicsRangesView is controller and view for any ranges associated with
 * the X dimension of a DivaPlot (and the current spectrum displayed
 * in it). The view is a JList that shows the current ranges and their
 * descriptions, the controllers are buttons to add new ranges and
 * delete existing ranges. <p>
 *
 * To encapsulate often required facilities this class also provides utility
 * methods for reading and writing ranges from/to disk file and an Action for
 * initiating a file chooser to control this process.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see DivaPlot
 * @see XGraphicsRange
 * @see XGraphicsRangesModel
 */
public class XGraphicsRangesView
    extends JPanel
    implements SelectionListener
{
    /** The DivaPlot that we are working with. */
    protected DivaPlot plot = null;

    /** The JList containing the region descriptions. */
    protected JTable table = new JTable();

    /** The selection model used by the JTable. */
    protected ListSelectionModel selectionModel = null;

    /** GraphicsPane that contains all the figures. */
    protected DivaPlotGraphicsPane pane = null;

    /** The colour of any figures that are created. */
    protected Color colour = Color.green;

    /** Whether XRangeFigures are free to transform. */
    protected boolean constrain = true;

    /** Model for table. */
    protected XGraphicsRangesModel model = null;

    /** Whether interactive creation is allowed */
    protected boolean interactive = true;

    /**
     * Create an instance.
     *
     * @param plot the DivaPlot that we're drawing into.
     * @param menu a JMenu to add the local Actions to (may be null).
     */
    public XGraphicsRangesView( DivaPlot plot, JMenu menu )
    {
        setPlot( plot );
        initUI( null, menu, false );
    }

    /**
     * Create an instance with a given colour and constained property.
     *
     * @param plot the DivaPlot that we're drawing into.
     * @param menu a JMenu to add the local Actions to (may be null).
     * @param colour the colour that any figures should be drawn using.
     * @param constrain whether the figure moves just X and show a full range
     *      in Y or not.
     */
    public XGraphicsRangesView( DivaPlot plot, JMenu menu, Color colour, 
                                boolean constrain )
    {
        this( plot, menu, colour, constrain, null );
    }

    /**
     * Create an instance with a given colour and constained property and
     * XGraphicsRangesModel instance.
     *
     * @param plot the DivaPlot that we're drawing into.
     * @param menu a JMenu to add the local Actions to (may be null).
     * @param colour the colour that any figures should be drawn using.
     * @param constrain whether the figure moves just X and show a full range
     *      in Y or not.
     */
    public XGraphicsRangesView( DivaPlot plot, JMenu menu, Color colour,
                                boolean constrain, XGraphicsRangesModel model )
    {
        setPlot( plot );
        setColour( colour );
        setConstrain( constrain );
        initUI( model, menu, false );
    }
    
    /**
     * XGraphicsRange view with an additional menu item, 
     * to select the whole spectrum
     *      
     * @param plot the DivaPlot that we're drawing into.
     * @param menu a JMenu to add the local Actions to (may be null).
     * @param colour the colour that any figures should be drawn using.
     * @param constrain whether the figure moves just X and show a full range
     *      in Y or not.
     * @param fullrange - if true add option fulrange(X)  menu.
     */
    public XGraphicsRangesView( DivaPlot plot, JMenu menu, Color colour,
            boolean constrain, XGraphicsRangesModel model,  boolean fullrange) {
    	
    	setPlot( plot );
        setColour( colour );
        setConstrain( constrain );
        initUI( model, menu, true );
       
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
    protected void initUI( XGraphicsRangesModel model, JMenu menu, boolean fullrange )
    {
        setLayout( new BorderLayout() );

        //  Set up the actionBar.
        JPanel actionBar = new JPanel();
        actionBar.setLayout( new BoxLayout( actionBar, BoxLayout.X_AXIS ) );
        actionBar.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );

        //  Set the model of ranges used by the JTable.
        if ( model == null ) {
            model = new XGraphicsRangesModel( plot );
        }
        this.model = model;
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

        //  Get action bar icons.
        ImageIcon addImage = new ImageIcon
            ( ImageHolder.class.getResource( "add.gif" ) );
        ImageIcon deleteImage = new ImageIcon
            ( ImageHolder.class.getResource( "delete.gif" ) );

        //  Add action for "add" button.
        AddAction addAction = new AddAction( "Add", addImage );
        JButton addButton = new JButton( addAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( addButton );
        addButton.setToolTipText
            ( "Select a region of spectrum and add to list" );
        if ( menu != null ) {
            menu.add( addAction ).setMnemonic( KeyEvent.VK_A );
        }

        //  Add action for "delete" button.
        DeleteAction deleteAction = new DeleteAction( "Delete", deleteImage );
        JButton deleteButton = new JButton( deleteAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( deleteButton );
        deleteButton.setToolTipText
            ( "Delete the selected regions from list" );
        actionBar.add( Box.createGlue() );
        if ( menu != null ) {
            menu.add( deleteAction ).setMnemonic( KeyEvent.VK_D );
        }

        //  Allow the interactive state to be changed (but only from menu).
        if ( menu != null ) {
            InteractiveAction interactiveAction = 
                new InteractiveAction( "Interactive Add" );
            JCheckBoxMenuItem mi = new JCheckBoxMenuItem( interactiveAction );
            mi.setState( interactive );
            menu.add( mi ).setMnemonic( KeyEvent.VK_I );
        }
        if (fullrange) {
        	  //  Add action for "full range" button.
            FullRangeAction fullRangeAction = new FullRangeAction( "Full Range" );
            JButton fullRangeButton = new JButton( fullRangeAction );
            actionBar.add( Box.createGlue() );
            actionBar.add( fullRangeButton );
            fullRangeButton.setToolTipText( "Add the whole X range" );
            actionBar.add( Box.createGlue());
            if ( menu != null ) {
                menu.add( fullRangeAction ).setMnemonic( KeyEvent.VK_F );
            }
        }
        	

        //  Add components.
        setBorder( BorderFactory.createTitledBorder( "Coordinate ranges:" ) );
        JScrollPane scroller = new JScrollPane( table );
        add( scroller, BorderLayout.CENTER );
        add( actionBar, BorderLayout.SOUTH );
    }
    
  
    /**
     * Invoked when the selected range changes. Should select the 
     * corresponding graphic.
     */
    protected void selectionChanged( ListSelectionEvent e )
    {
        int index = e.getFirstIndex();
        // XXX do something?
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
     * Create a new region and arrange to have it added to the model, when
     * drawn.
     */
    protected void createRange()
    {
        if ( interactive ) {
            //  Raise the plot to indicate that an interaction should begin.
            SwingUtilities.getWindowAncestor( plot ).toFront();
            new XGraphicsRange( plot, model, colour, constrain, null );
        }
        else {

            //  Accessible creation. Figure that spans visible part of plot.
            float[] graphbox = plot.getGraphicsLimits();
            double graph[] = new double[4];
            graph[0] = graphbox[0];
            graph[1] = graphbox[1];
            graph[2] = graphbox[2];
            graph[3] = graphbox[3];

            double tmp[][]= plot.transform( graph, true );
            double range[] = new double[2];
            range[0] = tmp[0][0];
            range[1] = tmp[0][1];
            createRange( range );
        }
    }

    /**
     * Create a new region with the given coordinate range.
     *
     * @param range an array of two world coordinates (not graphics) that
     *      define the extent of the range to be created.
     */
    protected void createRange( double[] range )
    {
        new XGraphicsRange( plot, model, colour, constrain, range );
    }

    /**
     * Delete any selected regions.
     */
    protected void deleteSelectedRanges()
    {
        int[] selected = getSelectedIndices();
        if ( selected != null ) {
            for ( int i = selected.length - 1; i >= 0; i-- ) {
                deleteRange( selected[i] );
            }
        }
    }

    /**
     * Delete all regions (typical when containing window withdrawn).
     */
    public void deleteAllRanges()
    {
        for ( int i = model.getRowCount() - 1; i >= 0; i-- ) {
            deleteRange( i );
        }
    }

    /**
     * Delete a region (index of position in list).
     */
    public void deleteRange( int index )
    {
        model.removeRange( index );
    }

    /**
     * Return an array of the ranges currently being displayed. If asked just
     * return the selected ones.
     *
     * @param selected whether to just return the selected ranges.
     * @return The ranges value
     */
    public double[] getRanges( boolean selected )
    {
        double[] ranges = model.getRanges();
        if ( selected ) {
            int[] indices = getSelectedIndices();
            if ( indices != null ) {
                double[] selectedRanges = new double[indices.length * 2];
                for ( int i = 0, j = 0; i < indices.length; i++, j += 2 ) {
                    selectedRanges[j] = ranges[indices[i] * 2];
                    selectedRanges[j + 1] = ranges[indices[i] * 2 + 1];
                }
                ranges = selectedRanges;
            }
        }
        return ranges;
    }

    /**
     * Return an array of indices that map the ranges of the regions into
     * indices of a data array (that contains the spectral coordinates).
     *
     * @param selected whether to just use the selected ranges.
     * @param merge whether to merge ranges so that overlapping ranges are
     *              considered as one range.
     * @param specCoords coordinates of the spectrum, that are to be matched
     *                   against the ranges (typically getXData of a SpecData).
     * @return pairs of indices in specCoords that cover the
     *         ranges. Returned as null if none exist.
     */
    public int[] extractRanges( boolean selected, boolean merge, 
                                double[] specCoords )
    {
        double[] worldRanges = getRanges( selected );
        if ( worldRanges != null && worldRanges.length != 0 ) {
            if ( merge ) {
                worldRanges = Sort.sortAndMerge( worldRanges );
            }
            int[] arrayRanges = new int[worldRanges.length];
            int temp;
            for ( int i = 0; i < arrayRanges.length; i+=2 ) {
                arrayRanges[i] = Sort.lookup( specCoords, worldRanges[i] );
                arrayRanges[i+1] = Sort.lookup( specCoords, worldRanges[i+1] );

                //  Check ordering, these can be reversed.
                if ( arrayRanges[i] > arrayRanges[i+1] ) {
                    temp = arrayRanges[i];
                    arrayRanges[i] = arrayRanges[i+1];
                    arrayRanges[i+1] = temp;
                }
            }
            return arrayRanges;
        }
        return null;
    }

    /**
     * Toggle the interactive creation flag.
     */
    public void toggleInteractive()
    {
        interactive = !interactive;
    }

    /**
     * Add action. Adds a new region to the plot.
     */
    protected class AddAction extends AbstractAction
    {
        public AddAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control D" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            createRange();
        }
    }

    /**
     * Delete action. Removes selected regions from plot.
     */
    protected class DeleteAction extends AbstractAction
    {
        public DeleteAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control E" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            deleteSelectedRanges();
        }
    }
    
    /**
     * FillRange action. selects full range plot.
     */
    protected class FullRangeAction extends AbstractAction
    {
        public FullRangeAction( String name )
        {
            super( name );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control F" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            selectFullRange();
        }
	
    }


    /**
     * Interactive creation action.
     */
    protected class InteractiveAction extends AbstractAction
    {
        public InteractiveAction( String name )
        {
            super( name );
            putValue( SHORT_DESCRIPTION, "Add creates range interactively" );
        }
        public void actionPerformed( ActionEvent ae )
        {
            toggleInteractive();
        }
    }
    
    
	private void selectFullRange() {
		boolean int1 = interactive;
		interactive = false;
		createRange();
		interactive=int1;
		
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

    /**
     * Whether figures are XRangeFigure with a free or constrained geometry or
     * not.
     *
     * @param constrain The new constrain value
     */
    public void setConstrain( boolean constrain )
    {
        this.constrain = constrain;
    }

    /**
     * Find out if the XRangeFigures are contrained.
     *
     * @return The constrain value
     */
    public boolean getConstrain()
    {
        return constrain;
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
    public void getFileWithRanges()
    {
        initFileChooser();
        int result = fileChooser.showOpenDialog( this );
        if ( result == fileChooser.APPROVE_OPTION ) {
            File file = fileChooser.getSelectedFile();
            readRangesFromFile( file );
        }
    }

    /**
     * Initiate a file selection dialog and choose a file to contain the
     * ranges.
     */
    public void getFileForRanges()
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
            writeRangesToFile( file );
        }
    }

    /**
     * Read a set of ranges from a file. These are added to the existing
     * ranges. The file should be simple and have two fields, separated by
     * whitespace or commas. Comments are indicated by lines starting with a
     * hash (#) and are ignored.
     *
     * @param file reference to the file.
     */
    public void readRangesFromFile( File file )
    {
        //  Check file exists.
        if ( ! file.exists() && file.canRead() && file.isFile() ) {
            return;
        }
        AsciiFileParser parser = new AsciiFileParser( file );
        if ( parser.getNFields() != 2 ) {
            JOptionPane.showMessageDialog
                ( this,
                "The format of ranges file requires just two fields + (" +
                parser.getNFields() + " were found)",
                "Error reading " + file.getName(),
                JOptionPane.ERROR_MESSAGE );
        }

        int nrows = parser.getNRows();
        double[] range = new double[2];
        for ( int i = 0; i < nrows; i++ ) {
            for ( int j = 0; j < 2; j++ ) {
                range[j] = parser.getDoubleField( i, j );
            }

            //  Create the new range.
            createRange( range );
        }
    }

    /**
     * Write the current ranges to a simple text file.
     *
     * @param file reference to the file.
     */
    public void writeRangesToFile( File file )
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
        double[] ranges = getRanges( false );
        for ( int i = 0; i < ranges.length; i += 2 ) {
            try {
                r.write( ranges[i] + " " + ranges[i + 1] + "\n" );
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
    public Action getReadAction( String name, Icon icon )
    {
        return new ReadAction( name, icon );
    }

    /**
     * Create an action for starting to write to a disk file.
     */
    public Action getWriteAction( String name, Icon icon )
    {
        return new WriteAction( name, icon );
    }

    /**
     * Inner Action that read ranges from disk file
     */
    protected class ReadAction extends AbstractAction
    {
        public ReadAction( String name, Icon icon )
        {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae )
        {
            getFileWithRanges();
        }
    }

    /**
     * Inner Action that writes ranges to a disk file.
     */
    protected class WriteAction extends AbstractAction
    {
        public WriteAction( String name, Icon icon )
        {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae )
        {
            getFileForRanges();
        }
    }


    //
    //  SelectionListener interface.
    //
    /**
     * Called when a figure is selected on the graphics pane. We're only
     * interested in XRangeFigures that we've created.
     */
    public void selectionChanged( SelectionEvent e )
    {
        Object[] ranges = pane.getSelectionAsArray();
        int[] indices = new int[ranges.length];
        int nok = 0;
        for ( int i = 0; i < ranges.length; i++ ) {
            if ( ranges[i] instanceof XRangeFigure ) {
                // Check this is "ours".
                int index = model.findFigure( (XRangeFigure) ranges[i] );
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

	public int[] extractRanges(boolean selected, boolean merge, double[] specCoords, double[] ranges4) {
	       double[] worldRanges = ranges4;
	        if ( worldRanges != null && worldRanges.length != 0 ) {
	            if ( merge ) {
	                worldRanges = Sort.sortAndMerge( worldRanges );
	            }
	            int[] arrayRanges = new int[worldRanges.length];
	            int temp;
	            for ( int i = 0; i < arrayRanges.length; i+=2 ) {
	                arrayRanges[i] = Sort.lookup( specCoords, worldRanges[i] );
	                arrayRanges[i+1] = Sort.lookup( specCoords, worldRanges[i+1] );

	                //  Check ordering, these can be reversed.
	                if ( arrayRanges[i] > arrayRanges[i+1] ) {
	                    temp = arrayRanges[i];
	                    arrayRanges[i] = arrayRanges[i+1];
	                    arrayRanges[i+1] = temp;
	                }
	            }
	            return arrayRanges;
	        }
// TODO Auto-generated method stub
		return null;
	}
}
