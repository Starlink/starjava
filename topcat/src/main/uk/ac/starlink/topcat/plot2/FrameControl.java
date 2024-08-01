package uk.ac.starlink.topcat.plot2;

import uk.ac.starlink.ttools.plot2.config.BooleanConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.StringConfigKey;
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
    private final Specifier<ConfigMap> titleSpecifier_;

    private static final ConfigKey<String> TITLE_KEY =
        new StringConfigKey( new ConfigMeta( "title", "Plot Title" ), null );
    private static final ConfigKey<Boolean> TITLE_VIS_KEY =
        new BooleanConfigKey( new ConfigMeta( "titlevis", "Title Visible" ),
                              true );

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public FrameControl() {
        super( "Frame", ResourceIcon.FRAME_CONFIG );

        /* Size tab. */
        posSpecifier_ = new PlotPositionSpecifier();
        addControlTab( "Size", posSpecifier_.getComponent(), true );
        posSpecifier_.addActionListener( getActionForwarder() );

        /* Title tab. */
        titleSpecifier_ =
            new ConfigSpecifier( new ConfigKey<?>[] { TITLE_KEY,
                                                      TITLE_VIS_KEY } );
        addSpecifierTab( "Title", titleSpecifier_ );
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

    /**
     * Returns a plot title.
     *
     * @return  plot title, or null
     */
    public String getPlotTitle() {
        ConfigMap config = titleSpecifier_.getSpecifiedValue();
        boolean vis = config.get( TITLE_VIS_KEY );
        String txt = config.get( TITLE_KEY );
        return vis && txt != null && txt.trim().length() > 0
             ? txt
             : null;
    }
}
