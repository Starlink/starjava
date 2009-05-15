/*
 * Copyright (C) 2008-2009 Science and Technlogy Facilities Council
 *
 *  History:
 *     18-DEC-2008 (Peter W. Draper):
 *        Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import uk.ac.starlink.ast.gui.DecimalField;
import uk.ac.starlink.ast.gui.ScientificFormat;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.plot.PlotStacker;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * Window for controlling how the spectra displayed in a {@link DivaPlot}
 * are stacked. Stacking simply means displaying using an offset in
 * the physical coordinates of the spectra.
 * <p>
 * Note this will only work for spectra that have the same data value units.
 * <p>
 * Stacking can be performed in two ways, by establishing an order of stacking
 * and applying a fixed offset, or by deriving an offset.
 *
 * The order is determined by the relative value of a JEL expression and the
 * the derived offset by the JEL expression.
 *
 * Expressions can use FITS header keywords as values and any STILTS
 * supported JEL functions can be applied to them. A single keyword is
 * permitted, so an expression can be very simple, but can also include the
 * conversion of dates to MJD.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class PlotStackerFrame
    extends JFrame
{
    /**
     * Content pane of frame.
     */
    protected JPanel contentPane = null;

    /**
     *  Menubar and various menus and items that it contains.
     */
    protected JMenuBar menuBar = new JMenuBar();
    protected JMenu fileMenu = new JMenu();
    protected JMenuItem closeFileMenu = new JMenuItem();

    /**
     *  The PlotControl that specifies the current spectra.
     */
    protected PlotControl plot = null;

    /**
     *  Whether to apply offsets to the spectra.
     */
    protected JCheckBox applyOrderOffsets = new JCheckBox();

    /**
     *  The order expression.
     */
    protected JTextField orderExpression = new JTextField();

    /**
     *  Offset value.
     */
    protected DecimalField offsetField = null;

    /**
     *  Whether to apply expression offsets to the spectra.
     */
    protected JCheckBox applyExpressionOffsets = new JCheckBox();

    /**
     *  The offset expression.
     */
    protected JTextField offsetExpression = new JTextField();

    /**
     * Create an instance.
     */
    public PlotStackerFrame( PlotControl plot )
    {
        contentPane = (JPanel) getContentPane();
        setPlot( plot );
        initUI();
        initMenus();
        initFrame();
    }

    /**
     * Get the PlotControl that we're working with.
     *
     * @return the PlotControl
     */
    public PlotControl getPlot()
    {
        return plot;
    }

    /**
     * Set the PlotControl that we're working with.
     *
     * @param plot the PlotControl reference.
     */
    public void setPlot( PlotControl  plot )
    {
        this.plot = plot;
    }

    /**
     * Initialise the main part of the user interface.
     */
    protected void initUI()
    {
        JPanel centre = new JPanel();
        GridBagLayouter layouter =
            new GridBagLayouter( centre, GridBagLayouter.SCHEME4 );
        contentPane.add( centre, BorderLayout.CENTER );

        //  Order expression.
        //  ================

        //  Whether to apply order offsets.
        applyOrderOffsets.setToolTipText
            ( "Toggle whether order offsets are used" );
        layouter.add( new JLabel( "Apply order offsets:" ), false );
        layouter.add( applyOrderOffsets, true );
        applyOrderOffsets.setSelected( true );
        applyOrderOffsets.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    applyOrder();
                }
            });

        //  Expression for deciding the spectral ordering.
        JLabel exprLabel = new JLabel( "Order expression:" );
        layouter.add( exprLabel, false );
        orderExpression.setToolTipText( "Expression that evaluates FITS " +
                                        "keywords to give an ordered " +
                                        "number, empty for any order." );
        layouter.add( orderExpression, true );

        //  The offset in physical units of the current spectrum.
        JLabel offsetLabel = new JLabel( "Offset:" );
        layouter.add( offsetLabel, false );

        ScientificFormat scientificFormat = new ScientificFormat();
        offsetField = new DecimalField( 0.0, 5, scientificFormat );
        offsetField.setToolTipText
            ( "Offset between spectra, current spectrum physical units" );
        layouter.add( offsetField, true );


        //  Offset expression.
        //  ==================

        //  Whether to apply expression offsets.
        applyExpressionOffsets.setToolTipText
            ( "Toggle whether expression offsets are used" );
        layouter.add( new JLabel( "Apply expression offsets:" ), false );
        layouter.add( applyExpressionOffsets, true );
        applyExpressionOffsets.setSelected( false );
        applyExpressionOffsets.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    applyExpression();
                }
            });

        //  Expression for deciding the spectral offsets.
        exprLabel = new JLabel( "Offset expression:" );
        layouter.add( exprLabel, false );
        offsetExpression.setToolTipText( "Expression that evaluates FITS " +
                                         "keywords to give an offset " +
                                         "in physical coordinates." );
        layouter.add( offsetExpression, true );

    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle(Utilities.getTitle( "Display stacked spectra" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        setSize( new Dimension( 450, 220 ) );
        setVisible( true );
    }

    /**
     * Initialise the menu bar, action bar and related actions.
     */
    protected void initMenus()
    {
        //  Add the menuBar.
        setJMenuBar( menuBar );

        //  Action bar uses a BoxLayout.
        JPanel actionBar = new JPanel();

        //  Get icons.
        ImageIcon closeImage =
            new ImageIcon( ImageHolder.class.getResource( "close.gif" ) );
        ImageIcon applyImage =
            new ImageIcon( ImageHolder.class.getResource( "accept.gif" ) );

        //  Create the File menu.
        fileMenu.setText( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );

        //  Add an action to apply the current settings.
        ApplyAction applyAction = new ApplyAction( "Apply", applyImage );
        fileMenu.add( applyAction ).setMnemonic( KeyEvent.VK_A );
        JButton applyButton = new JButton( applyAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( applyButton );
        applyButton.setToolTipText( "Apply settings" );

        //  Add an action to close the window.
        CloseAction closeAction = new CloseAction( "Close", closeImage );
        fileMenu.add( closeAction ).setMnemonic( KeyEvent.VK_C );
        JButton closeButton = new JButton( closeAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( closeButton );
        closeButton.setToolTipText( "Close window" );

        actionBar.add( Box.createGlue() );
        contentPane.add( actionBar, BorderLayout.SOUTH );

        //  Create the Help menu.
        HelpFrame.createHelpMenu( "stacker-window", "Help on window",
                                  menuBar, null );
    }

    /**
     *  Apply the current settings to derive order offsets.
     */
    protected void applyOrder()
    {
        //  If this is active, then expressions are not.
        boolean selected = applyOrderOffsets.isSelected();
        boolean exprselected = applyExpressionOffsets.isSelected();
        plot.getPlot().setUsePlotStacker( selected || exprselected );
        PlotStacker stacker = plot.getPlot().getPlotStacker();
        stacker.setOrdering( true );
        if ( selected ) {
            applyExpressionOffsets.setSelected( false );
            stacker.setExpression( orderExpression.getText() );
            stacker.setShift( offsetField.getDoubleValue() );
        }
        plot.updatePlot();
    }

    /**
     *  Apply the current settings to derive expression offsets.
     */
    protected void applyExpression()
    {
        //  If this is active, then ordering is not.
        boolean selected = applyExpressionOffsets.isSelected();
        boolean orderselected = applyOrderOffsets.isSelected();
        plot.getPlot().setUsePlotStacker( selected || orderselected );
        PlotStacker stacker = plot.getPlot().getPlotStacker();
        stacker.setOrdering( false );
        if ( selected ) {
            applyOrderOffsets.setSelected( false );
            stacker.setExpression( offsetExpression.getText() );
        }
        plot.updatePlot();
    }

    /**
     *  Close the window.
     */
    protected void closeWindowEvent()
    {
        this.dispose();
    }

    /**
     * Apply action.
     */
    protected class ApplyAction extends AbstractAction
    {
        public ApplyAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control A" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            if ( applyOrderOffsets.isSelected() ) {
                applyOrder();
            }
            else if ( applyExpressionOffsets.isSelected() ) {
                applyExpression();
            }
        }
    }

    /**
     * Inner class defining Action for closing window.
     */
    protected class CloseAction extends AbstractAction
    {
        public CloseAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control W" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            closeWindowEvent();
        }
    }
}
