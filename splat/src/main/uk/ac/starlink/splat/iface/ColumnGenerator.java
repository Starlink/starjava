/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 * Copyright (C) 2008 Science and Technology Facilities Council
 *
 *  History:
 *     27-FEB-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import uk.ac.starlink.ast.AstException;
import uk.ac.starlink.ast.MathMap;
import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * Provides basic facilities for generating new data column values for
 * a spectrum (see the classes {@link DataColumnGenerator} and
 * {@link ErrorColumnGenerator} for concrete implementations). The
 * transformations are all represented by symbolic c-like statements,
 * some examples of which are available from pre-load (linear, special
 * noise functions etc.).
 * <p>
 * These facilities are provided by the AST MathMap class.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public abstract class ColumnGenerator
    extends JPanel
{
    /**
     * The spectrum we're about to modify.
     */
    protected EditableSpecData specData = null;

    /**
     * The listener for our change events. XXX extend for more than one.
     */
    protected ColumnGeneratorListener listener = null;

    /**
     * Known expression templates. These should be defined by a
     * concrete implementation. Each element of the array is a list of
     * strings that define the main expression and any required
     * sub-expressions. The first string is a symbolic name and the
     * second whether the function is known to MathMap, if not then
     * the expression is uneditable. An example follows:
     * <pre>
     * { { "Envelope function", "true",
     *                          "error*exp(-0.5*(radius/sigma)**2)",
     *                          "radius", "coord-center",
     *                          "center", "100.0",
     *                          "sigma", "50.0" }}
     * </pre>
     */
    protected String[][] templates = null;

    /**
     * Index of the current template.
     */
    protected int templateIndex = 0;

    /**
     * Create an instance.
     */
    public ColumnGenerator( EditableSpecData specData,
                            String[][] templates,
                            ColumnGeneratorListener listener )
    {
        setTemplates( templates );
        initUI();
        setListener( listener );

        //  Set the SpecData we're taking values from. Note this must
        //  happen after the interface is generated.
        setEditableSpecData( specData );
        setVisible( true );
    }

    /**
     * Add bespoke help for the generator.
     */
    public abstract void addHelp( JMenuBar menuBar );

    /**
     * Return a suitable title for a frame.
     */
    public abstract String getTitle();

    /**
     * Set the MathMap templates are available.
     */
    protected void setTemplates( String[][] templates )
    {
        this.templates = templates;
    }

    /**
     * Initialise the user interface.
     */
    protected void initUI()
    {
        setLayout( new BorderLayout() );
        addGeneral();
        JLabel helpText = 
            new JLabel( "Pre-defined variables: data, coords, error, index" );
        helpText.setBorder( BorderFactory.createLoweredBevelBorder() );
        helpText.setHorizontalAlignment( SwingConstants.CENTER );
        add( helpText, BorderLayout.NORTH );
    }

    /**
     * Add any pre-defined functions to a given menu.
     */
    public void addPreDefined( JMenu function )
    {
        for ( int i = 0; i < templates.length; i++ ) {
            function.add( new TemplateAction( i ) );
        }
    }

    /**
     * Set the spectrum we're working on.
     */
    public void setEditableSpecData( EditableSpecData specData )
    {
        this.specData = specData;

        //  Set the available data areas to grey out ones that are
        //  unavailable.
        //updateDescription();
    }

    /**
     * Set the listener for our results.
     */
    public void setListener( ColumnGeneratorListener listener )
    {
        this.listener = listener;
    }

    protected JTextField[] itemNames = null;
    protected JTextField[] itemValues = null;
    protected JLabel mainName = null;
    protected JTextField mainValue = null;
    protected final static int NAMECOUNT = 10;

    /**
     * Create a page of controls for getting the main function, plus
     * any sub-functions or symbolic values.
     */
    protected void addGeneral()
    {
        JPanel panel = new JPanel();
        add( panel, BorderLayout.CENTER );

        // Configuration
        itemNames = new JTextField[NAMECOUNT];
        itemValues = new JTextField[NAMECOUNT];
        mainName = new JLabel( "y" );
        mainValue = new JTextField();
        for ( int i = 0; i < NAMECOUNT; i++ ) {
            itemNames[i] = new JTextField();
            itemValues[i]= new JTextField();
        }

        //  Layout
        GridBagLayouter layouter = 
            new GridBagLayouter( panel, GridBagLayouter.SCHEME2 );

        layouter.add( mainName, false );
        layouter.add( new JLabel( " = " ), false );
        layouter.add( mainValue, true );

        for ( int i = 0; i < NAMECOUNT; i++ ) {
            layouter.add( itemNames[i], false );
            layouter.add( new JLabel( " = " ), false );
            layouter.add( itemValues[i], true );
        }
        layouter.eatSpare();

        panel.setBorder( BorderFactory.createTitledBorder
                         ( "Symbolic transformation:" ) );
    }

    /**
     * Inner class defining Action for showing a template.
     */
    protected class TemplateAction extends AbstractAction
    {
        private int index = 0;
        public TemplateAction( int index )
        {
            super( templates[index][0] );
            putValue( SHORT_DESCRIPTION, templates[index][2] );
            this.index = index;
        }

        /**
         * Respond to actions from the buttons.
         */
        public void actionPerformed( ActionEvent ae )
        {
            applyTemplate( index );
        }
    }

    /**
     * Clear the values.
     */
    protected void clearValues()
    {
        mainValue.setText( "" );
        for ( int i = 0; i < NAMECOUNT; i++ ) {
            itemValues[i].setText( "" );
        }
    }

    /**
     * Clear the names.
     */
    protected void clearNames()
    {
        for ( int i = 0; i < NAMECOUNT; i++ ) {
            itemNames[i].setText( "" );
        }
    }

    /**
     * Set the fields to values of a template.
     */
    protected void applyTemplate( int index )
    {
        clearValues();
        clearNames();
        templateIndex = index;

        //  Disable fields that should not be editted.
        boolean enabled = templates[index][1].equals( "true" );
        mainValue.setText( templates[index][2] );
        mainValue.setEditable( enabled );
        int i = 0;
        for ( int j = 3; j < templates[index].length; j += 2 ) {
            itemNames[i].setText( templates[index][j] );
            itemNames[i].setEditable( enabled );
            itemValues[i].setText( templates[index][j+1] );
            i++;
        }
        for ( int j = i; j < itemNames.length; j++ ) {
            itemNames[j].setEditable( enabled );
        }
    }

    /**
     * Generate the new data column from the expressions.
     */
    public void generate()
    {
        if ( "".equals( mainValue.getText() ) ) {
            JOptionPane.showMessageDialog( this,
                                           "No main expression is defined",
                                           "No expressions",
                                           JOptionPane.WARNING_MESSAGE );
            return;
        }
        double[] newdata = null;

        // If this isn't known to MathMap, then pass it back to the
        // real implementation.
        boolean enabled = templates[templateIndex][1].equals( "true" );
        if ( ! enabled ) {
            int count = templates[templateIndex].length - 2;
            String[] values = new String[count];
            for ( int i = 0; i < count; i++ ) {
                values[i] = itemValues[i].getText();
            }
            newdata = handleSpecialFunction( templateIndex, values );
        }
        else {
            // Count any sub-expressions.
            int count = 0;
            for ( int i = 0; i < NAMECOUNT; i++ ) {
                if ( ! "".equals( itemNames[i].getText() ) ) {
                    count++;
                }
            }
            String[] fwd = new String[count+1];
            int j = 0;
            for ( int i = count; i >= 0; i-- ) {
                if ( ! "".equals( itemNames[i].getText() ) ) {
                    fwd[j++] = itemNames[i].getText() + " = " +
                        itemValues[i].getText();
                }
            }
            fwd[j++] = "y = " + mainValue.getText();
            newdata = doAstMathMap( fwd );
        }
        listener.acceptGeneratedColumn( this, newdata );
    }

    /**
     * Cached index vector. Only updated when spectrum needs more
     * indices 
     */
    private double[] indexVector = null;

    /**
     * Apply a general transformation to any of the possible data
     * sources.
     */
    protected double[] doAstMathMap( String[] fwd )
    {
        double[] coord = specData.getXData();
        double[] value = specData.getYData();
        double[] error = specData.getYDataErrors();

        //  See if we probably need an index vector.
        boolean needIndex = false;
        for ( int i = 0; i < fwd.length; i++ ) {
            if ( fwd[i].indexOf( "index" ) != -1 ) {
                needIndex = true;
                break;
            }
        }

        //  Possible input coordinates.
        String inv[] = null;
        if ( error == null && ! needIndex ) {
            inv = new String[2];
        }
        else if ( error == null && needIndex ) {
            inv = new String[3];
            inv[2] = "index";
        }
        else if ( error != null && ! needIndex ) {
            inv = new String[3];
            inv[2] = "error";
        }
        else if ( error != null && needIndex ) {
            inv = new String[4];
            inv[2] = "error";
            inv[3] = "index";
        }
        inv[0] = "coord";
        inv[1] = "data";
        
        //  Generate index vector (if needed).
        if ( needIndex ) {
            if ( indexVector != null ) {
                if ( indexVector.length < coord.length ) {
                    indexVector = null;
                }
            }
            if ( indexVector == null ) {
                indexVector = new double[coord.length];
                for ( int i = 0; i < coord.length; i++ ) {
                    indexVector[i] = (double) i;
                }
            }
        }


        //  Create the MathMap with coord, value and error mapping to
        //  newvalue.
        try {
            MathMap map = new MathMap( inv.length, 1, fwd, inv );

            //  Do the transform.
            double[][] in = new double[inv.length][];
            in[0] = coord;
            in[1] = value;
            if ( error != null ) {
                in[2] = error;
            }
            if ( needIndex && error == null ) {
                in[2] = indexVector;
            }
            else if ( needIndex && error != null ) {
                in[3] = indexVector;
            }

            double[][] result = map.tranP( value.length, inv.length,
                                           in, true, 1 );
            return result[0];
        }
        catch (AstException e) {
            ErrorDialog.showError( this, e );
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Deal with any special functions. These are unknown to MathMap
     * and should be shown in an uneditable state (essential fixed
     * functions with known parameters).
     *
     * @param templateIndex the index of the template function
     * @param parameters array of values given as parameters
     */
    protected abstract double[] handleSpecialFunction( int templateIndex,
                                                       String[] parameters );
}
