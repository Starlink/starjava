package uk.ac.starlink.splat.iface;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * GraphicsEdgesControls creates a "page" of widgets that are a view
 * of a GraphicsHints object. They provide the ability to configure
 * all the properties of the object (i.e. whether the edges are
 * clipped and how much space to reserve for the labelling).
 *
 * @since $Date$
 * @since 28-NOV-2000
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 * @see GraphicsHints, PlotConfigFrame.
 */
public class GraphicsEdgesControls extends JPanel implements ChangeListener
{
    /**
     * GraphicsEdges model for current state.
     */
    protected GraphicsEdges edges = null;

    /**
     * Whether the spectrum lines should be clipped.
     */
    protected JCheckBox clip = new JCheckBox();

    /**
     * Slider for controlling the space reserved for X labels.
     */
    protected FloatJSlider xFraction = null;

    /**
     * Slider model for X fraction.
     */
    protected FloatJSliderModel xFractionModel = new FloatJSliderModel();

    /**
     * Slider for controlling the space reserved for Y labels.
     */
    protected FloatJSlider yFraction = null;

    /**
     * Slider model for Y fraction.
     */
    protected FloatJSliderModel yFractionModel = new FloatJSliderModel();

    /**
     * GridBagConstraints object.
     */
    protected GridBagConstraints gbc = new GridBagConstraints();

    /**
     * Label Insets.
     */
    protected Insets labelInsets = new Insets( 10, 5, 5, 10 );

    /**
     * Create an instance.
     */
    public GraphicsEdgesControls( GraphicsEdges edges )
    {
        initUI();
        setGraphicsEdges( edges );
    }

    /**
     * Reset interface to default configuration.
     */
    public void reset()
    {
        edges.setDefaults();
    }

    /**
     * Create and initialise the user interface.
     */
    protected void initUI()
    {
        setLayout( new GridBagLayout() );

        //  Clip spectrum lines to inside axes.
        clip.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    matchClip();
                }
            });

        //  Set X fraction.
        xFraction = new FloatJSlider( xFractionModel );
        xFraction.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchXFraction();
                }
            });

        //  Set Y fraction.
        yFraction = new FloatJSlider( yFractionModel );
        yFraction.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    matchYFraction();
                }
            });

        //  Add labels for all fields.
        addLabel( "Clip Spectrum:", 0 );
        addLabel( "X Reserve:", 1 );
        addLabel( "Y Reserve:", 2 );

        gbc.insets = new Insets( 0, 0, 0, 0 );
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weighty = 0.0;
        gbc.weightx = 1.0;
        gbc.gridx = 1;

        //  Current row for adding components.
        int row = 0;

        //  Clipped
        gbc.gridy = row++;
        gbc.fill = GridBagConstraints.NONE;
        add( clip, gbc );

        //  X fraction.
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = row++;
        add( xFraction, gbc );

        //  Y fraction.
        gbc.gridy = row++;
        add( yFraction, gbc );

        //  Eat up all spare vertical space (pushes widgets to top).
        Component filly = Box.createVerticalStrut( 5 );
        gbc.gridy = row++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        add( filly, gbc );

        //  Set tooltips.
        clip.setToolTipText( "Clip spectral lines to lie with axes" );
        xFraction.setToolTipText(
           "Set space reserved for X labels (fraction)");
        yFraction.setToolTipText(
           "Set space reserved for Y labels (fraction)");
    }

    /**
     * Set the GraphicsEdges object (only after UI is initiliased).
     */
    public void setGraphicsEdges( GraphicsEdges edges )
    {
        this.edges = edges;
        edges.addChangeListener( this );
        updateFromGraphicsEdges();
    }

    /**
     * Update interface to reflect values of GraphicsEdges object.
     */
    protected void updateFromGraphicsEdges()
    {
        edges.removeChangeListener( this );
        clip.setSelected( edges.isClipped() );
        xFractionModel.setApparentValues( edges.getXFrac(),
                                          GraphicsEdges.GAP_MIN,
                                          GraphicsEdges.GAP_MAX,
                                          GraphicsEdges.GAP_STEP );
        yFractionModel.setApparentValues( edges.getYFrac(),
                                          GraphicsEdges.GAP_MIN,
                                          GraphicsEdges.GAP_MAX,
                                          GraphicsEdges.GAP_STEP );
        edges.addChangeListener( this );
    }

    /**
     * Get copy of reference to current GraphicsEdges
     */
    public GraphicsEdges getGraphicsEdges()
    {
        return edges;
    }

    /**
     * Add a new UI description label. This is added to the front of
     * the given row.
     */
    private void addLabel( String text, int row )
    {
        JLabel label = new JLabel( text );
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.insets = labelInsets;
        add( label, gbc );
    }

    /**
     * Match whether to clip.
     */
    protected void matchClip()
    {
        edges.setClipped( clip.isSelected() );
    }

    /**
     * Match X label fraction.
     */
    protected void matchXFraction()
    {
        edges.setXFrac( xFraction.getValue() );
    }

    /**
     * Match Y label fraction.
     */
    protected void matchYFraction()
    {
        edges.setYFrac( yFraction.getValue() );
    }

//
// Implement the ChangeListener interface
//
    /**
     * If the GraphicsEdges object changes then we need to update the
     * interface.
     */
    public void stateChanged( ChangeEvent e )
    {
        updateFromGraphicsEdges();
    }
}
