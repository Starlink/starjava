/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 * Copyright (C) 2008 Science and Technology Facilities Council
 *
 *  History:
 *     14-FEB-2001 (Peter W. Draper):
 *       Original version.
 *     26-JUN-2003 (Peter W. Draper):
 *       Divide always produced BAD values as test against divide by
 *       zero was flawed.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.Utilities;

/**
 * Interface to perform simple mathematical operations between two
 * spectra. The interface offers two views of the global lists of
 * spectra. These lists each allow the selection of one spectrum which
 * can then have a simple maths operator (+,-,*,/) applied to
 * them. The result is then generated as a new spectrum.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SimpleBinaryMaths
    extends JFrame
{
    /**
     * Content pane of frame.
     */
    protected JPanel contentPane = null;

    /**
     * Maths actions tool bar.
     */
    protected JPanel mathActionBar = new JPanel();

    /**
     * Window actions tool bar.
     */
    protected JPanel windowActionBar = new JPanel();

    /**
     * Reference to global list of spectra and plots.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     * First view of spectra.
     */
    protected JList viewOne = new JList();

    /**
     * Second view of spectra.
     */
    protected JList viewTwo = new JList();

    /**
     * The math operator action buttons.
     */
    protected JButton addButton = new JButton();
    protected JButton subButton = new JButton();
    protected JButton divButton = new JButton();
    protected JButton mulButton = new JButton();

    /**
     * Close window action button.
     */
    protected JButton closeButton = new JButton();

    /**
     * Menubar and various menus and items that it contains.
     */
    protected JMenuBar menuBar = new JMenuBar();
    protected JMenu fileMenu = new JMenu();
    protected JMenu opsMenu = new JMenu();

    /**
     * Split pane used for displaying global list side-by-side.
     */
    protected JSplitPane splitPane = null;

    /**
     * Create an instance.
     */
    public SimpleBinaryMaths()
    {
        contentPane = (JPanel) getContentPane();
        initUI();
        HelpFrame.createHelpMenu( "binary-maths", "Help on window",
                                  menuBar, null );
        setSize( new Dimension( 500, 400 ) );
        setTitle( Utilities.getTitle( "Simple maths of two spectra" ) );
        setVisible( true );
        splitPane.setDividerLocation( 0.5 );
    }

    /**
     * Initialise the user interface.
     */
    protected void initUI()
    {
        contentPane.setLayout( new BorderLayout() );

         // Add the menuBar.
        setJMenuBar( menuBar );

        // Create the File menu.
        fileMenu.setText( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );

        // Operations menu.
        opsMenu.setText( "Operations" );
        opsMenu.setMnemonic( KeyEvent.VK_O );
        menuBar.add( opsMenu );

        //  Put both lists of spectra in scroll panes.
        JScrollPane scrollerOne = new JScrollPane( viewOne );
        TitledBorder viewOneTitle =
            BorderFactory.createTitledBorder( "Left operand:" );
        scrollerOne.setBorder( viewOneTitle );

        JScrollPane scrollerTwo = new JScrollPane( viewTwo );
        TitledBorder viewTwoTitle =
            BorderFactory.createTitledBorder( "Right operand:" );
        scrollerTwo.setBorder( viewTwoTitle );

        //  Set the JList models to show the spectra (SplatListModel
        //  interacts with the global list).
        viewOne.setModel( new SpecListModel(viewOne.getSelectionModel()) );
        viewTwo.setModel( new SpecListModel(viewTwo.getSelectionModel()) );

        //  These only allow the selection of one item at a time.
        viewOne.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        viewTwo.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        //  Add these to a JSplitPane.
        splitPane = new JSplitPane();
        splitPane.setOneTouchExpandable( false );

        splitPane.setLeftComponent( scrollerOne );
        splitPane.setRightComponent( scrollerTwo );

        //  Add actions to the math operator buttons.
        ImageIcon image =
            new ImageIcon( ImageHolder.class.getResource( "plus24.gif" ) );
        LocalAction addAction = new LocalAction ( "Add", image,
                                                  "Add selected spectra",
                                                  "control D" );
        addButton.setAction( addAction );
        opsMenu.add( addAction ).setMnemonic( KeyEvent.VK_D );

        image =
            new ImageIcon( ImageHolder.class.getResource( "minus24.gif" ) );
        LocalAction subAction =
            new LocalAction ( "Subtract", image,
                              "Subtract left from right selected spectra",
                              "control S" );
        subButton.setAction( subAction );
        opsMenu.add( subAction ).setMnemonic( KeyEvent.VK_S );

        image =
            new ImageIcon( ImageHolder.class.getResource( "multiply.gif" ) );
        LocalAction mulAction = new LocalAction ( "Multiply", image,
                                                  "Multiply selected spectra",
                                                  "control M" );
        mulButton.setAction( mulAction );
        opsMenu.add( mulAction ).setMnemonic( KeyEvent.VK_M );

        image =
            new ImageIcon( ImageHolder.class.getResource( "divide.gif" ) );
        LocalAction divAction =
            new LocalAction ( "Divide", image,
                              "Divide left by right selected spectra",
                              "control I" );
        divButton.setAction( divAction );
        opsMenu.add( divAction ).setMnemonic( KeyEvent.VK_I );

        //  And place together in a panel.
        mathActionBar.setLayout( new BoxLayout( mathActionBar,
                                                BoxLayout.X_AXIS ) );
        mathActionBar.setBorder( BorderFactory.createEmptyBorder(3,3,3,3) );
        mathActionBar.add( Box.createHorizontalGlue() );
        mathActionBar.add( addButton );
        mathActionBar.add( Box.createHorizontalGlue() );
        mathActionBar.add( subButton );
        mathActionBar.add( Box.createHorizontalGlue() );
        mathActionBar.add( divButton );
        mathActionBar.add( Box.createHorizontalGlue() );
        mathActionBar.add( mulButton );
        mathActionBar.add( Box.createHorizontalGlue() );

        //  Window control action.
        // Add an action to close the window (appears in File menu
        // and action bar).
        image =
            new ImageIcon( ImageHolder.class.getResource( "close.gif" ) );
        LocalAction closeAction = new LocalAction( "Close", image,
                                                   "Close window",
                                                   "control W" );
        fileMenu.add( closeAction ).setMnemonic( KeyEvent.VK_C );
        closeButton = new JButton( closeAction );

        windowActionBar.setLayout( new BoxLayout( windowActionBar,
                                                  BoxLayout.X_AXIS ) );
        windowActionBar.setBorder( BorderFactory.createEmptyBorder(3,3,3,3) );
        windowActionBar.add( Box.createGlue() );
        windowActionBar.add( closeButton );
        windowActionBar.add( Box.createGlue() );

        //  Panel for action bars.
        JPanel actionPanel = new JPanel( new BorderLayout() );
        actionPanel.add( mathActionBar, BorderLayout.NORTH );
        actionPanel.add( windowActionBar, BorderLayout.SOUTH );

        //  Add components to main window.
        contentPane.setLayout( new BorderLayout() );
        contentPane.add( splitPane, BorderLayout.CENTER );
        contentPane.add( actionPanel, BorderLayout.SOUTH );
    }

    /**
     * Constants indicating the type of operation.
     */
    public static final int ADD = 0;
    public static final int SUBTRACT = 1;
    public static final int DIVIDE = 2;
    public static final int MULTIPLY = 3;

    /**
     * Get the spectrum selected in list one.
     */
    public SpecData getViewOneSpectrum()
    {
        int[] indices = viewOne.getSelectedIndices();
        if ( indices.length > 0 ) {
            return globalList.getSpectrum( indices[0] );
        }
        return null;
    }

    /**
     * Get the spectrum selected in list two.
     */
    public SpecData getViewTwoSpectrum()
    {
        int[] indices = viewTwo.getSelectedIndices();
        if ( indices.length > 0 ) {
            return globalList.getSpectrum( indices[0] );
        }
        return null;
    }

    /**
     * Start the required math operation on the selected spectra.
     */
    public void operate( int function )
    {
        // Get the spectra.
        SpecData one = getViewOneSpectrum();
        SpecData two = getViewTwoSpectrum();
        if ( one != null && two != null ) {

            //  XXX Should match the coordinates and data units using AST.

            //  The X coordinates of one must match those of two.
            double[] coords = one.getXData();
            double[] oneData = one.getYData();
            double[] twoData = two.evalYDataArray( coords );

            //  Now create the resultant data.
            double[] newData = null;
            String operation = null;
            String operator = null;
            switch (function) {
               case ADD:
                   newData = addData( oneData, twoData );
                   operation = "Sum: ";
                   operator = String.valueOf( '+' );
                   break;
               case SUBTRACT:
                   newData = subtractData( oneData, twoData );
                   operation = "Diff: ";
                   operator = String.valueOf( '-' );
                   break;
               case DIVIDE:
                   newData = divideData( oneData, twoData );
                   operation = "Ratio: ";
                   operator = String.valueOf( '/' );
                   break;
               case MULTIPLY:
                   newData = multiplyData( oneData, twoData );
                   operation = "Product: ";
                   operator = String.valueOf( '*' );
                   break;
            }
            String name = operation + " (" + one.getShortName() + ") " +
                          operator + " (" + two.getShortName() + ") ";
            createNewSpectrum( name, one, newData );
        }
    }

    /**
     *  Add two data arrays together.
     */
    protected double[] addData( double[] one, double[] two )
    {
        double[] result = new double[one.length];
        for ( int i = 0; i < one.length; i++ ) {
            if ( one[i] != SpecData.BAD && two[i] != SpecData.BAD ) {
                result[i] = one[i] + two[i];
            }
            else {
                result[i] = SpecData.BAD;
            }
        }
        return result;
    }

    /**
     *  Subtract two data arrays.
     */
    protected double[] subtractData( double[] one, double[] two )
    {
        double[] result = new double[one.length];
        for ( int i = 0; i < one.length; i++ ) {
            if ( one[i] != SpecData.BAD && two[i] != SpecData.BAD ) {
                result[i] = one[i] - two[i];
            }
            else {
                result[i] = SpecData.BAD;
            }
        }
        return result;
    }

    /**
     *  Multiply two data arrays.
     */
    protected double[] multiplyData( double[] one, double[] two )
    {
        double[] result = new double[one.length];
        for ( int i = 0; i < one.length; i++ ) {
            if ( one[i] != SpecData.BAD && two[i] != SpecData.BAD ) {
                result[i] = one[i] * two[i];
            }
            else {
                result[i] = SpecData.BAD;
            }
        }
        return result;
    }

    /**
     *  Divide two data arrays.
     */
    protected double[] divideData( double[] one, double[] two )
    {
        double[] result = new double[one.length];
        for ( int i = 0; i < one.length; i++ ) {
            if ( one[i] != SpecData.BAD &&
                 two[i] != SpecData.BAD &&
                 two[i] != 0.0 ) {
                result[i] = one[i] / two[i];
            }
            else {
                result[i] = SpecData.BAD;
            }
        }
        return result;
    }

    /**
     * Create a new spectrum that is a clone of an existing spectrum, but with
     * a new data array and add it to the global list.
     */
    protected void createNewSpectrum( String name, SpecData spec,
                                      double[] data )
    {
        try {
            //  Create a memory spectrum to contain the fit.
            EditableSpecData newSpec = SpecDataFactory.getInstance()
                .createEditable( name, spec );
            FrameSet frameSet = ASTJ.get1DFrameSet(spec.getAst().getRef(), 1);
            String units = spec.getCurrentDataUnits();
            double[] coords = spec.getXData();
            newSpec.setSimpleUnitData( frameSet, coords, units, data );
            globalList.add( newSpec );

            //  Spectral lines create here are red.
            globalList.setKnownNumberProperty( newSpec, SpecData.LINE_COLOUR,
                                               new Integer(Color.red.getRGB()));
        }
        catch (Exception e) {
            //  Do nothing (could pop up dialog).
            e.printStackTrace();
        }
    }

    /**
     *  Close the window.
     */
    protected void closeWindow()
    {
        this.dispose();
    }

    /**
     * Inner class defining Action for performing a maths operation,
     * or closing a window.
     */
    protected class LocalAction extends AbstractAction
    {
        public LocalAction( String name, Icon icon, String shortHelp )
        {
            super( name, icon  );
            putValue( SHORT_DESCRIPTION, shortHelp );
        }

        public LocalAction( String name, Icon icon, String shortHelp,
                            String accel )
        {
            this( name, icon, shortHelp );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( accel ) );
        }

        /**
         * Respond to actions from the buttons.
         */
        public void actionPerformed( ActionEvent ae )
        {
            String cmd = ae.getActionCommand();
            if ( "Add".equals( cmd ) ) {
                operate( ADD );
            }
            else if ( "Subtract".equals( cmd ) ) {
                operate( SUBTRACT );
            }
            else if ( "Divide".equals( cmd ) ) {
                operate( DIVIDE );
            }
            else if ( "Multiply".equals( cmd ) ) {
                operate( MULTIPLY );
            }
            else {
                closeWindow(); // Only other action.
            }
        }
    }
}
