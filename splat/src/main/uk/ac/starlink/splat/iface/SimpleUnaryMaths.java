/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     14-FEB-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
import uk.ac.starlink.ast.gui.DecimalField;
import uk.ac.starlink.ast.gui.ScientificFormat;

/**
 * Interface to perform simple mathematical operations one a spectrum.
 * The interface offers a view of the global list of spectra and an
 * entry area for a single constant. The constant can be added,
 * subtracted, multiplied or divided into the spectrum. The result is
 * then generated as a new spectrum.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SimpleUnaryMaths
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
     * View of all the spectra.
     */
    protected JList globalView = new JList();

    /**
     * The math operator action buttons.
     */
    protected JButton addButton = new JButton();
    protected JButton subButton = new JButton();
    protected JButton divButton = new JButton();
    protected JButton mulButton = new JButton();

    /**
     * Entry field for the constant.
     */
    protected DecimalField constantField = null;

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
     * Create an instance.
     */
    public SimpleUnaryMaths()
    {
        contentPane = (JPanel) getContentPane();
        initUI();
        HelpFrame.createHelpMenu( "unary-maths", "Help on window",
                                  menuBar, null );
        setSize( new Dimension( 500, 300 ) );
        setTitle( Utilities.getTitle( "Simple constant based maths" ) );
        setVisible( true );
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

        //  Put the list of spectra in a scroll pane.
        JPanel centerPanel = new JPanel( new BorderLayout() );
        JScrollPane globalScroller = new JScrollPane( globalView );
        TitledBorder globalViewTitle =
            BorderFactory.createTitledBorder( "Global list of spectra:" );
        globalScroller.setBorder( globalViewTitle );
        centerPanel.add( globalScroller, BorderLayout.CENTER );

        //  Set the JList model to show the spectra (SplatListModel
        //  interacts with the global list).
        globalView.setModel
            ( new SpecListModel( globalView.getSelectionModel() ) );

        //  Only allow the selection of one item at a time.
        globalView.setSelectionMode
            ( ListSelectionModel.SINGLE_INTERVAL_SELECTION );

        //  Create the field for accepting a decimal.
        JPanel constantPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel constantLabel = new JLabel( "Constant:" );
        ScientificFormat scientificFormat = new ScientificFormat();
        constantField = new DecimalField( 1.0, 5, scientificFormat );
        constantField.setToolTipText( "Constant to add/subtract/multiply"
                                      + " or divide spectrum" );
        constantPanel.add( constantLabel );
        constantPanel.add( constantField );
        centerPanel.add( constantPanel, BorderLayout.SOUTH );

        //  Add actions to the math operator buttons.
        ImageIcon image =
            new ImageIcon( ImageHolder.class.getResource( "plus24.gif" ) );
        LocalAction addAction = 
            new LocalAction ( "Add", image,
                              "Add constant to selected spectrum",
                              "control D" );
        addButton.setAction( addAction );
        opsMenu.add( addAction ).setMnemonic( KeyEvent.VK_D );

        image =
            new ImageIcon( ImageHolder.class.getResource( "minus24.gif" ) );
        LocalAction subAction = 
            new LocalAction ( "Subtract", image,
                              "Subtract constant from selected spectrum",
                              "control S" );
        subButton.setAction( subAction );
        opsMenu.add( subAction ).setMnemonic( KeyEvent.VK_S );

        image =
            new ImageIcon( ImageHolder.class.getResource( "multiply.gif" ) );
        LocalAction mulAction = 
            new LocalAction ( "Multiply", image,
                              "Multiply selected spectrum by constant",
                              "control M" );
        mulButton.setAction( mulAction );
        opsMenu.add( mulAction ).setMnemonic( KeyEvent.VK_M );

        image =
            new ImageIcon( ImageHolder.class.getResource( "divide.gif" ) );
        LocalAction divAction = 
            new LocalAction ( "Divide", image,
                              "Divide selected spectrum by constant",
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
        contentPane.add( centerPanel, BorderLayout.CENTER );
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
     * Get the spectrum selected in the list.
     */
    public SpecData getSelectedSpectrum()
    {
        int[] indices = globalView.getSelectedIndices();
        if ( indices.length > 0 ) {
            return globalList.getSpectrum( indices[0] );
        }
        return null;
    }

    /**
     * Start the required math operation on the selected spectrum.
     */
    public void operate( int function )
    {
        // Get the spectrum.
        SpecData spec = getSelectedSpectrum();
        if ( spec != null ) {

            //  Get the constant.
            double constant = 0.0;
            try {
                constant = constantField.getDoubleValue();
            }
            catch ( Exception e ) {
                e.printStackTrace();
                return;
            }

            //  Get the data.
            double[] inData = spec.getYData();
            double[] inErrors = spec.getYDataErrors();
            double[] data = new double[inData.length];
            double[] errors = null;
            if ( inErrors != null ) {
                errors = new double[inErrors.length];
                System.arraycopy( inErrors, 0, errors, 0, inErrors.length );

            }
            System.arraycopy( inData, 0, data, 0, inData.length );

            //  Now create the new spectrum.
            String operation = null;
            String operator = null;
            switch (function) {
               case ADD:
                   addConstant( data, errors, constant );
                   operation = "Add: ";
                   operator = String.valueOf( '+' );
                   break;
               case SUBTRACT:
                   addConstant( data, errors, -1.0 * constant );
                   operation = "Sub: ";
                   operator = String.valueOf( '-' );
                   break;
               case DIVIDE:
                   if ( constant != 0.0 ) {
                       multiplyConstant( data, errors, 1.0 / constant );
                   }
                   operation = "Div: ";
                   operator = String.valueOf( '\u00f7' ); // unicode divide.
                   break;
               case MULTIPLY:
                   multiplyConstant( data, errors, constant );
                   operation = "Mult: ";
                   operator = String.valueOf( '\u00d7' ); //  unicode multiply
                   break;
            }
            String name = operation + " (" + spec.getShortName() + ") " +
                          operator + " (" + constant + ") ";
            createNewSpectrum( name, spec, data, errors );
        }
    }

    /**
     *  Add constant to data and error arrays.
     */
    protected void addConstant( double[] data, double[] errors,
                                double constant )
    {
        for ( int i = 0; i < data.length; i++ ) {
            if ( data[i] != SpecData.BAD ) {
                data[i] += constant;
            }
        }
    }

    /**
     *  Multiply a data and error array by a constant.
     */
    protected void multiplyConstant( double[] data, double[] errors,
                                     double constant )
    {
        if ( errors != null ) {
            for ( int i = 0; i < data.length; i++ ) {
                if ( data[i] != SpecData.BAD ) {
                    data[i] *= constant;
                    errors[i] *= constant;
                }
            }
        }
        else {
            for ( int i = 0; i < data.length; i++ ) {
                if ( data[i] != SpecData.BAD ) {
                    data[i] *= constant;
                }
            }
        }
    }

    /**
     * Create a new spectrum from two data arrays of coordinates and
     * values and add it to the global list.
     */
    protected void createNewSpectrum( String name, SpecData spec,
                                      double[] data, double[] errors )
    {
        try {
            //  Create a memory spectrum to contain the fit. This is a clone
            //  of "spec", we replace the data and errors.
            EditableSpecData newSpec = SpecDataFactory.getInstance()
                .createEditable( name, spec );

            FrameSet frameSet = ASTJ.get1DFrameSet(spec.getAst().getRef(), 1);
            double[] coords = spec.getXData();
            String units = spec.getCurrentDataUnits();
            newSpec.setSimpleUnitData( frameSet, coords, units, data, errors );
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
            else if (  "Multiply".equals( cmd ) ) {
                operate( MULTIPLY );
            }
            else {
                closeWindow(); // Only other action.
            }      
        }
    }
}
