import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import uk.ac.starlink.splat.data.*;
import uk.ac.starlink.splat.iface.*;

/**
 * Example plugin to show how to write complex tools. This offers a
 * display showing two lists of all the available spectra and provides
 * the ability to do simple maths on two of them, one being selected
 * from each list.
 *
 * @since $Date$
 * @since 14-FEB-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research
 *            Councils 
 */
public class BasicMaths extends JFrame implements ActionListener
{
    /**
     * Content pane of frame.
     */
    protected JPanel contentPane = null;

    /**
     * Maths actions tool bar.
     */
    JPanel mathActionBar = new JPanel();

    /**
     * Window actions tool bar.
     */
    JPanel windowActionBar = new JPanel();

    /**
     * Reference to global list of spectra and plots.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getReference();

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
    protected JButton addButton = new JButton( "Add" );
    protected JButton subButton = new JButton( "Sub" );
    protected JButton divButton = new JButton( "Div" );
    protected JButton mulButton = new JButton( "Mul" );

    /**
     * Close window action button.
     */
    protected JButton exitButton = new JButton( "Exit" );

    /**
     * The main SplatBrowser window.
     */
    protected SplatBrowser browser = null;

    /**
     * Create an instance.
     */
    public BasicMaths( SplatBrowser browser )
    {
        this.browser = browser;
        contentPane = (JPanel) getContentPane();
        initUI();
        pack();
        show();
    }

    /**
     * Initialise the user interface.
     */
    protected void initUI()
    {
        //  Put both lists of spectra in scroll panes.
        JScrollPane scrollerOne = new JScrollPane( viewOne );
        JScrollPane scrollerTwo = new JScrollPane( viewTwo );

        //  Set the JList models to show the spectra (SplatListModel
        //  interacts with the global list).
        viewOne.setModel( new SplatListModel(viewOne.getSelectionModel()) );
        viewTwo.setModel( new SplatListModel(viewTwo.getSelectionModel()) );

        //  These only allow the selection of one item at a time.
        viewOne.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        viewTwo.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        //  Add these to a JPanel.
        JPanel viewPanel = new JPanel( new BorderLayout() );
        viewPanel.add( scrollerOne, BorderLayout.EAST );
        viewPanel.add( scrollerTwo, BorderLayout.WEST );

        //  Add actions to the math operator buttons.
        addButton.addActionListener( this );
        subButton.addActionListener( this );
        divButton.addActionListener( this );
        mulButton.addActionListener( this );
        
        //  And place together in a panel.
        mathActionBar.setLayout( new GridLayout( 1, 0 ) );
        mathActionBar.add( addButton );
        mathActionBar.add( subButton );
        mathActionBar.add( divButton );
        mathActionBar.add( mulButton );

        //  Window control action.
        exitButton.addActionListener( this );
        windowActionBar.add( exitButton );

        //  Panel for action bars.
        JPanel actionPanel = new JPanel( new BorderLayout() );
        actionPanel.add( mathActionBar, BorderLayout.NORTH );
        actionPanel.add( windowActionBar, BorderLayout.SOUTH );

        //  Add components to main window.
        contentPane.setLayout( new BorderLayout() );
        contentPane.add( viewPanel, BorderLayout.CENTER );
        contentPane.add( actionPanel, BorderLayout.SOUTH );
    }

    /**
     * Respond to actions from the buttons.
     */
    public void actionPerformed( ActionEvent e )
    {
        JButton source = (JButton) e.getSource();
        if ( source.equals( addButton ) ) {
            operate( ADD );
        } else if ( source.equals( subButton ) ) {
            operate( SUBTRACT );
        } else if ( source.equals( divButton ) ) {
            operate( DIVIDE );
        } else if ( source.equals( mulButton ) ) {
            operate( MULTIPLY );
        } else if ( source.equals( exitButton ) ) {
            closeWindow();
        }
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
            
            //  The X coordinates of one must match those of two.
            double[] coords = one.getXData();
            double[] oneData = one.getYData();
            double[] twoData = two.evalYDataArray( coords );
            
            //  Now create the resultant data.
            double[] newData = null;
            switch (function) {
               case ADD: 
                   newData = addData( oneData, twoData );
                   break;
               case SUBTRACT: 
                   newData = subtractData( oneData, twoData );
                   break;
               case DIVIDE: 
                   newData = divideData( oneData, twoData );
                   break;
               case MULTIPLY: 
                   newData = multiplyData( oneData, twoData );
                   break;
            }
            createNewSpectrum( coords, newData );
        }
    }

    /**
     *  Add two data arrays together.
     */
    protected double[] addData( double[] one, double[] two )
    {
        double[] result = new double[one.length];
        for ( int i = 0; i < one.length; i++ ) {
            result[i] = one[i] + two[i];
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
            result[i] = one[i] - two[i];
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
            result[i] = one[i] * two[i];
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
            result[i] = one[i] / two[i];
        }
        return result;
    }

    /**
     * Create a new spectrum from two data arrays of coordinates and
     * values and add it to the global list.
     */
    protected void createNewSpectrum( double[] coords, double[] data )
    {
        //  Create a memory spectrum to contain the fit.
        MEMSpecDataImpl memSpecImpl = new MEMSpecDataImpl( "Maths Plugin" );
        memSpecImpl.setData( coords, data );
        try {
            SpecData newSpec = new SpecData( memSpecImpl );
            globalList.add( newSpec );
            
            //  Spectral lines are red.
            globalList.set( newSpec, SpecData.LINE_COLOUR,
                            new Integer( Color.red.getRGB() ) );
        } catch (Exception e) {
            //  DO nothing (could pop up dialog).
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
}
