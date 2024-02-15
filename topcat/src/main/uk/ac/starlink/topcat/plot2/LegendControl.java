package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.topcat.LineBox;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.LegendEntry;
import uk.ac.starlink.ttools.plot2.LegendIcon;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;

/**
 * Control for defining legend characteristics.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public class LegendControl extends TabberControl {

    /* This class could perhaps be written in a more generic way
     * (subclassing ConfigControl). */

    private final ToggleButtonModel visibleModel_;
    private final ToggleButtonModel opaqueModel_;
    private final ToggleButtonModel borderModel_;
    private final ToggleButtonModel insideModel_;
    private final SquarePusher pusher_;
    private boolean everMadeVisible_;
    private boolean everMadeInvisible_;

    /**
     * Constructor.
     */
    public LegendControl() {
        super( "Legend", ResourceIcon.LEGEND );
        final ActionListener forwarder = getActionForwarder();

        /* Set up control for selecting whether legend is visible at all. */
        visibleModel_ = new ToggleButtonModel( "Show Legend", null,
                                               "Whether to display legend "
                                             + "near plot" );
        visibleModel_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                boolean isVis = visibleModel_.isSelected();
                if ( isVis ) {
                    everMadeVisible_ = true;
                }
                else {
                    everMadeInvisible_ = true;
                }
                forwarder.actionPerformed( evt );
            }
        } );

        /* Set up control for background opacity. */
        opaqueModel_ =
            new ToggleButtonModel( "Opaque", null,
                                   "Whether background is opaque white, "
                                 + "or transparent" );
        opaqueModel_.addActionListener( forwarder );
        opaqueModel_.setSelected( true );

        /* Set up control for whether to draw line border. */
        borderModel_ =
            new ToggleButtonModel( "Border", null,
                                   "Whether to draw border around legend box" );
        borderModel_.setSelected( true );
        borderModel_.addActionListener( forwarder );

        /* Set up control for internal/external legend. */
        insideModel_ =
            new ToggleButtonModel( "Internal", null,
                                   "Draw legend within the plot bounds"
                                 + " or outside them" );
        insideModel_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                pusher_.setEnabled( insideModel_.isSelected() );
            }
        } );
        insideModel_.addActionListener( forwarder );

        /* Set up control for internal legend position. */
        pusher_ = new SquarePusher();
        pusher_.setEnabled( insideModel_.isSelected() );
        pusher_.addActionListener( forwarder );

        /* Style panel. */
        JComponent styleBox = Box.createVerticalBox();
        styleBox.add( new LineBox( visibleModel_.createCheckBox() ) );
        styleBox.add( new LineBox( opaqueModel_.createCheckBox() ) );
        styleBox.add( new LineBox( borderModel_.createCheckBox() ) );
        addControlTab( "Style", styleBox, true );

        /* Position panel. */
        JComponent posBox = new JPanel( new BorderLayout() );
        JComponent topBox = Box.createVerticalBox();
        JRadioButton[] butts =
            insideModel_.createRadioButtons( "External", "Internal" );
        topBox.add( new LineBox( butts[ 0 ] ) );
        topBox.add( new LineBox( butts[ 1 ] ) );
        posBox.add( topBox, BorderLayout.NORTH );
        posBox.add( pusher_, BorderLayout.CENTER );
        addControlTab( "Location", posBox, false );
    }

    /**
     * Returns the legend icon for the current state of the stack model.
     *
     * @param   entries   entries to include in legend
     * @param   config   legend configuration options to control text style etc
     * @return  legend icon, or null if not visible
     */
    public LegendIcon createLegendIcon( LegendEntry[] entries,
                                        ConfigMap config ) {

        /* Update visibility defaults based on how many entries the legend
         * would have - it's not very useful if it only has one entry.
         * But once it's appeared once keep it, because it's more distracting
         * to have it keep appearing and disappearing. 
         * This behaviour is invoked as a side effect of some other method
         * requesting the icon.  That's a bit untidy, but in practice
         * icon request will only happen when a legend is about to be
         * displayed. */
        if ( entries.length > 1 && ! everMadeInvisible_ ) {
            visibleModel_.setSelected( true );
        }

        /* Construct and return the icon, if required. */
        if ( visibleModel_.isSelected() ) {
            if ( entries.length == 0 ) {
                return null;
            }
            else {
                Captioner captioner = StyleKeys.CAPTIONER.createValue( config );
                boolean border = borderModel_.isSelected();
                Color bgColor = opaqueModel_.isSelected() ? Color.WHITE : null;
                return new LegendIcon( entries, captioner, border, bgColor );
            }
        }
        else {
            return null;
        }
    }

    /**
     * Returns the requested legend fractional position.
     *
     * @return   2-element array giving x, y fractional positions for legend
     *           (each in range 0..1), or null for absent or external legend
     */
    public float[] getLegendPosition() {
        return insideModel_.isSelected()
             ? new float[] { pusher_.getXPosition(),
                             1f - pusher_.getYPosition() }
             : null;
    }

    /**
     * Returns the model used to specify whether the legend is internal
     * or external to the plot bounds.
     *
     * @return  inside model
     */
    public ToggleButtonModel getInsideModel() {
        return insideModel_;
    }
}
