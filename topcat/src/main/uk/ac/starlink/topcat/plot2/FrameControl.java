package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.topcat.ResourceIcon;

/**
 * Control for defining characteristics of the external frame within which
 * the plot is painted.
 *
 * @author   Mark Taylor
 * @since    8 Jan 2015
 */
public class FrameControl extends ConfigControl {

    private final PlotPositionSpecifier posSpecifier_;

    /**
     * Constructor.
     *
     * @param  hasInsets  whether the Size control (PlotPositionSpecifier)
     *                    supports insets or just external dimensions
     */
    public FrameControl( boolean hasInsets ) {
        super( "Frame", ResourceIcon.FRAME_CONFIG );
        posSpecifier_ = new PlotPositionSpecifier( hasInsets );
        addControlTab( "Size", posSpecifier_.getComponent(), true );
        posSpecifier_.addActionListener( getActionForwarder() );
    }

    /**
     * Returns an object that can provide explicit settings for plot icon
     * dimensions and positioning.
     *
     * @return   plot position
     */
    public PlotPosition getPlotPosition() {
        return posSpecifier_.getSpecifiedValue();
    }
}
