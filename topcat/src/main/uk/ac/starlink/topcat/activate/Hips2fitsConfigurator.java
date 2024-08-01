package uk.ac.starlink.topcat.activate;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.ColumnSelector;
import uk.ac.starlink.topcat.ImageWindow;
import uk.ac.starlink.topcat.IntSelector;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.Safety;
import uk.ac.starlink.topcat.TopcatModel;

/**
 * Partial ActivatorConfigurator implementation for activators that
 * will use the CDS Hips2fits service.
 * This class manages the UI components required to select a HiPS survey
 * and make cutout queries on it.
 *
 * @author   Mark Taylor
 * @since    22 Oct 2019
 */
public abstract class Hips2fitsConfigurator extends SkyPosConfigurator {

    private final TopcatModel tcModel_;
    private final ColumnSelector fovSelector_;
    private final HipsSelector hipsSelector_;
    private final IntSelector npixSelector_;
    private ImageWindow imwin_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.activate" );

    private static final DefaultValueInfo FOV_INFO =
        new DefaultValueInfo( "Field of View", Number.class,
                              "Angular size of required image" );
    static {
        FOV_INFO.setUnitString( "radians" );
        FOV_INFO.setNullable( false );
        FOV_INFO.setUCD( "pos.angDistance" );
    }

    private static final String FOVCOL_KEY = "fov_col";
    private static final String FOVUNIT_KEY = "fov_unit";
    private static final String HIPS_KEY = "hips";
    private static final String NPIX_KEY = "npix";

    /**
     * Constructor.
     *
     * @param  tinfo   topcat model information
     * @param  filter   indicates which Hips image surveys are suitable
     */
    @SuppressWarnings("this-escape")
    protected Hips2fitsConfigurator( TopcatModelInfo tinfo,
                                     final Predicate<HipsSurvey> filter ) {
        super( tinfo );
        tcModel_ = tinfo.getTopcatModel();

        /* Construct and initialise field of view selector. */
        fovSelector_ =
            new ColumnSelector( tcModel_.getColumnSelectorModel( FOV_INFO ),
                                false );
        boolean fovStatus =
            fovSelector_.getModel().setTextValue( "1.0", "degrees" );
        if ( ! fovStatus ) {
            logger_.warning( "FOV configuration failed" );
        }

        /* Construct HiPS selector and populate it with options
         * synchronously or asynchronously. */
        hipsSelector_ = new HipsSelector();
        final Downloader<HipsSurvey[]> hipsDownloader =
            HipsSurvey.getImageHipsListDownloader();
        if ( ! updateHips( hipsDownloader, filter ) ) {
            hipsDownloader.start();
            hipsDownloader.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    updateHips( hipsDownloader, filter );
                }
            } );
        }

        /* Construct and initialise the pixel count selector. */
        npixSelector_ = new IntSelector( new int[] {
            25, 50, 75, 100, 150, 200, 300, 400, 500, 600,
        } );
        npixSelector_.setValue( 300 );

        /* Listen to components. */
        ActionForwarder forwarder = getActionForwarder();
        fovSelector_.addActionListener( forwarder );
        hipsSelector_.addActionListener( forwarder );
        npixSelector_.getComboBox().addActionListener( forwarder );

        /* Place components. */
        LabelledComponentStack stack = getStack();
        JComponent[] hipsLines = hipsSelector_.getLines();
        stack.addLine( "Field of View", fovSelector_ );
        stack.addLine( "HiPS Survey", null, hipsLines[ 0 ], true );
        for ( int i = 1; i < hipsLines.length; i++ ) {
            stack.addLine( null, null, hipsLines[ i ], true );
        }
        stack.addLine( "Size in Pixels", npixSelector_ );
    }

    /**
     * Invoked on activation with the configured HiPS details.
     *
     * @param  hipsId  hips survey ID or match string
     * @param  raDeg   RA position in degrees
     * @param  decDeg  Dec position in degrees
     * @param  fovDeg  field of view in degrees
     * @param  npix    linear dimension of cutout in pixels
     * @return   outcome
     */
    protected abstract Outcome useHips( String hipsId,
                                        double raDeg, double decDeg,
                                        double fovDeg, int npix );

    public Safety getSafety() {
        return Safety.SAFE;
    }

    public ConfigState getState() {
        ConfigState state = getSkyPosState();
        state.saveSelection( FOVCOL_KEY, fovSelector_.getColumnComponent() );
        state.saveSelection( FOVUNIT_KEY, fovSelector_.getUnitComponent() );
        state.saveText( HIPS_KEY, hipsSelector_.getTextField() );
        state.saveSelection( NPIX_KEY, npixSelector_.getComboBox() );
        return state;      
    }

    public void setState( ConfigState state ) {
        setSkyPosState( state );
        state.restoreSelection( FOVCOL_KEY, fovSelector_.getColumnComponent() );
        state.restoreSelection( FOVUNIT_KEY, fovSelector_.getUnitComponent() );
        state.restoreText( HIPS_KEY, hipsSelector_.getTextField() );
        state.restoreSelection( NPIX_KEY, npixSelector_.getComboBox() );
    }

    public Activator createActivator( ColumnData raData, ColumnData decData ) {
        final String hipsId = getHipsId();
        if ( hipsId == null ) {
            return null;
        }
        final ColumnData fovData = fovSelector_.getColumnData();
        if ( fovData == null ) {
            return null;
        }
        final int npix = npixSelector_.getValue();
        return new SkyPosActivator( raData, decData, false, false ) {
            protected Outcome useSkyPos( double raDeg, double decDeg,
                                         long lrow ) {
                Object fovObj;
                try {
                    fovObj = fovData.readValue( lrow );
                }
                catch ( IOException e ) {
                    return Outcome.failure( e );
                }
                double fovDeg = fovObj instanceof Number
                              ? ((Number) fovObj).doubleValue() * 180 / Math.PI 
                              : Double.NaN;
                if ( Double.isNaN( fovDeg ) ) {
                    return Outcome.failure( "No field of view value" );
                }
                return useHips( hipsId, raDeg, decDeg, fovDeg, npix );
            }
        };
    }

    public String getSkyConfigMessage() {
        if ( getHipsId() == null ) {
            return "No HiPS selected";
        }
        else if ( fovSelector_.getColumnData() == null ) {
            return "No Field of View selected";
        }
        else {
            return null;
        }
    }

    /**
     * Updates the hips selection GUI with the data from a downloader,
     * if available.
     *
     * @param  downloader  acquires hips list
     * @param  filter    indicates which hips to use
     * @return  true iff surveys have been added
     */
    private boolean updateHips( Downloader<HipsSurvey[]> downloader,
                                Predicate<HipsSurvey> filter ) {
        HipsSurvey[] surveys = downloader.getData();
        List<HipsSurvey> list = new ArrayList<HipsSurvey>();
        if ( surveys != null ) {
            for ( HipsSurvey survey : surveys ) {
                if ( filter.test( survey ) ) {
                    list.add( survey );
                }
            }
        }
        hipsSelector_.setSurveys( list.toArray( new HipsSurvey[ 0 ] ) );
        return surveys != null;
    }

    /**
     * Returns the string by which the Hips to use can be identified to
     * the hips2fits service.  If not specified, null is returned.
     *
     * @return   hips ID, or null
     */
    private String getHipsId() {
        Object hipsObj = hipsSelector_.getTextField().getText();
        if ( hipsObj instanceof String ) {
            String hipsId = (String) hipsObj;
            return hipsId.trim().length() == 0 ? null : hipsId;
        }
        else if ( hipsObj instanceof HipsSurvey ) {
            return ((HipsSurvey) hipsObj).getShortName();
        }
        else {
            return null;
        }
    }
}
