/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     27-FEB-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import uk.ac.starlink.ast.gui.DecimalField;
import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.splat.util.ExceptionDialog;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.ast.MathMap;
import uk.ac.starlink.ast.AstException;

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
    protected static String[][] templates = null;

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
    protected static int NAMECOUNT = 10;

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
        panel.setLayout( new GridBagLayout() );
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.15;
        gbc.gridwidth = 1;
        panel.add( mainName, gbc );
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;
        panel.add( new JLabel( " = " ), gbc );
        gbc.weightx = 0.85;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        panel.add( mainValue, gbc );

        for ( int i = 0; i < NAMECOUNT; i++ ) {
            gbc.anchor = GridBagConstraints.EAST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.15;
            gbc.gridwidth = 1;
            panel.add( itemNames[i], gbc );
            gbc.anchor = GridBagConstraints.WEST;
            gbc.weightx = 0.0;
            panel.add( new JLabel( " = " ), gbc );
            gbc.weightx = 0.85;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            panel.add( itemValues[i], gbc );
        }

        //  Eat remaining space to keep packed at top.
        eatSpare( panel, gbc );

        panel.setBorder( BorderFactory.createTitledBorder
                         ( "Symbolic transformation:" ) );
    }

    /**
     * East spare space at bottom of panel using GridBagLayout.
     */
    private void eatSpare( JPanel panel, GridBagConstraints gbc )
    {
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH    ;
        panel.add( Box.createVerticalGlue(), gbc );
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
     * Apply a general transformation to any of the possible data
     * sources.
     */
    protected double[] doAstMathMap( String[] fwd )
    {
        double[] coord = specData.getXData();
        double[] value = specData.getYData();
        double[] error = specData.getYDataErrors();

        //  Possible input coordinates... (XXX index).
        String inv[] = null;
        if ( error == null ) {
            inv = new String[2];
        }
        else {
            inv = new String[3];
            inv[2] = "error";
        }
        inv[0] = "coord";
        inv[1] = "data";

        //  Generate index vector if needed (cached, look for index token?).
        //  ....

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

            double[][] result = map.tranP( value.length, inv.length,
                                           in, true, 1 );
            return result[0];
        }
        catch (AstException e) {
            new ExceptionDialog( this, e );
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
