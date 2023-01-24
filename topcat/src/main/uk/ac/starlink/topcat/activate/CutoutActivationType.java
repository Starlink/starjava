package uk.ac.starlink.topcat.activate;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.IntSelector;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.Safety;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.func.Sdss;
import uk.ac.starlink.topcat.func.SuperCosmos;

/**
 * Activation type for showing an image from one of a fixed list
 * of cutout services.
 * This is primitive: the data services are old, there are not many
 * of them, and the viewing application is not configurable.
 *
 * @author   Mark Taylor
 * @since    29 Mar 2018
 */
public class CutoutActivationType implements ActivationType {

    /** List of available cutout services. */
    private static final CutoutService[] SERVICES = new CutoutService[] {
        new CutoutService( "SuperCOSMOS All-Sky Blue", 0.67 ) {
            String displayCutout( int tableID, double ra, double dec,
                                  int npix ) {
                return SuperCosmos.sssShowBlue( ra, dec, npix );
            }
        },
        new CutoutService( "SuperCOSMOS All-Sky Red", 0.67 ) {
            String displayCutout( int tableID, double ra, double dec,
                                  int npix ) {
                return SuperCosmos.sssShowRed( ra, dec, npix );
            }
        },
        new TwoMassCutoutService( 'J' ),
        new TwoMassCutoutService( 'H' ),
        new TwoMassCutoutService( 'K' ),
        new CutoutService( "SDSS Colour Images", 0.4 ) {
            String displayCutout( int tableID, double ra, double dec,
                                  int npix ) {
                return Sdss.sdssShowCutout( "SDSS (" + tableID + ")",
                                            ra, dec, npix );
            }
        },
    };

    public String getName() {
        return "Display Cutout Image";
    }

    public String getDescription() {
        return "Displays a cutout image retrieved from "
             + "an external image service";
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return Suitability.AVAILABLE;
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        return new CutoutConfigurator( tinfo );
    }

    /**
     * Configurator for use with CutoutActivationType.
     */
    private static class CutoutConfigurator extends SkyPosConfigurator {

        private final TopcatModel tcModel_;
        private final JComboBox<CutoutService> serviceSelector_;
        private final IntSelector npixSelector_;
        private final JLabel pixsizeLabel_;
        private static final String SERVICE_KEY = "service";
        private static final String NPIX_KEY = "npix";

        /**
         * Constructor.
         *
         * @param  tinfo  table information
         */
        CutoutConfigurator( TopcatModelInfo tinfo ) {
            super( tinfo );
            tcModel_ = tinfo.getTopcatModel();
            ActionForwarder forwarder = getActionForwarder();
            serviceSelector_ = new JComboBox<>( SERVICES );
            serviceSelector_.addActionListener( forwarder );
            npixSelector_ = new IntSelector( new int[] {
                25, 50, 75, 100, 150, 200, 300, 400, 500,
            } );
            npixSelector_.setValue( 100 );
            pixsizeLabel_ = new JLabel();
            serviceSelector_.addItemListener( new ItemListener() {
                public void itemStateChanged( ItemEvent evt ) {
                    updateState();
                }
            } );
            LabelledComponentStack stack = getStack();
            stack.addLine( "Cutout Service", serviceSelector_ );
            Box npixLine = Box.createHorizontalBox();
            npixLine.add( npixSelector_ );
            npixLine.add( pixsizeLabel_ );
            stack.addLine( "Dimension in pixels", npixLine );
            updateState();
        }

        public Activator createActivator( ColumnData raData,
                                          ColumnData decData ) {
            final CutoutService serv = getSelectedService();
            final int npix = npixSelector_.getValue();
            final int tableId = tcModel_.getID();
            return new SkyPosActivator( raData, decData, false, true ) {
                protected Outcome useSkyPos( double raDeg, double decDeg,
                                             long lrow ) {
                    serv.displayCutout( tableId, raDeg, decDeg, npix );
                    return Outcome.success();
                }
            };
        }

        public String getSkyConfigMessage() {
            return null;
        }

        public Safety getSafety() {
            return Safety.SAFE;
        }

        public ConfigState getState() {
            ConfigState state = getSkyPosState();
            state.saveSelection( SERVICE_KEY, serviceSelector_ );
            state.saveSelection( NPIX_KEY, npixSelector_.getComboBox() );
            return state;
        }

        public void setState( ConfigState state ) {
            setSkyPosState( state );
            state.restoreSelection( SERVICE_KEY, serviceSelector_ );
            state.restoreSelection( NPIX_KEY, npixSelector_.getComboBox() );
        }

        /**
         * Updates display state; should be called any time service selection
         * may have changed.
         */
        private void updateState() {
            CutoutService serv = getSelectedService();
            pixsizeLabel_.setText( serv != null
                                 ? " (" + serv.getPixelSize() + " arcsec)"
                                 : "     " );
        }

        /**
         * Returns the currently selected cutout service.
         */
        private CutoutService getSelectedService() {
            return serviceSelector_
                  .getItemAt( serviceSelector_.getSelectedIndex() );
        }
    }

    /**
     * Defines a service which can display cutout images for RA/Dec pairs.
     */
    private static abstract class CutoutService {
        final String name_;
        final float pixelSize_;

        /**
         * Constructor for cutout set.
         *
         * @param  name  cutout service name
         * @param  pixelSize  linear dimension of pixels in arcsec
         */
        protected CutoutService( String name, double pixelSize ) {
            name_ = name;
            pixelSize_ = (float) pixelSize;
        }

        /**
         * Returns pixel size in arcseconds.
         *
         * @return pixel size
         */
        public float getPixelSize() {
            return pixelSize_;
        }

        /**
         * Displays an image centred around a given position.
         * 
         * @param  tableID  id value for table being displayed
         * @param  raDeg   right ascension in degrees
         * @param  decDeg  declination in degrees
         * @param  npix linear dimension of image in pixels
         * @return log message for display operation
         */
        abstract String displayCutout( int tableID, double raDeg, double decDeg,
                                       int npix );

        @Override
        public String toString() {
            return name_;
        }
    }

    /**
     * Helper class for accessing the 2MASS cutout server.
     */
    private static class TwoMassCutoutService extends CutoutService {
        private final char band_;

        /**
         * Constructor.
         *
         * @param   band   band identifier - one of 'J', 'H' or 'K'
         */
        TwoMassCutoutService( char band ) {
            super( "2MASS Quick-Look " + band + "-band", 2.25 );
            band_ = band;
        }

        String displayCutout( int tableID, double ra, double dec, int npix ) {
            return TwoMass.showCutout2Mass( "2MASS "
                                          + Character.toUpperCase( band_ )
                                          + " (" + tableID + ")",
                                            ra, dec, npix,
                                            Character.toLowerCase( band_ ) );
        }
    }
}
