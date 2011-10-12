package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.topcat.func.Sdss;
import uk.ac.starlink.topcat.func.SuperCosmos;
import uk.ac.starlink.topcat.func.TwoMass;

/**
 * Component for selecting how image cutout requests will be submitted
 * to remote cutout services.  This component can return an Activator
 * {@link #makeActivator} based on its current settings.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Oct 2004
 */
public class CutoutSelector extends JPanel implements ItemListener {

    private final TopcatModel tcModel_;
    private final JComboBox serviceSelector_;
    private final ColumnSelector raSelector_;
    private final ColumnSelector decSelector_;
    private final IntSelector npixSelector_;
    private final JLabel pixsizeLabel_;
    private final Component[] enablables_;

    /** List of available cutout services. */
    private static final CutoutService[] SERVICES = new CutoutService[] {
        new CutoutService( "SuperCOSMOS All-Sky Blue", 0.67 ) {
            String displayCutout( int tableID, double ra, double dec, 
                                  int npix ) {
                return SuperCosmos.sssShowBlue( Math.toDegrees( ra ),
                                                Math.toDegrees( dec ), npix );
            }
        },
        new CutoutService( "SuperCOSMOS All-Sky Red", 0.67 ) {
            String displayCutout( int tableID, double ra, double dec,
                                  int npix ) {
                return SuperCosmos.sssShowRed( Math.toDegrees( ra ),
                                               Math.toDegrees( dec ), npix );
            }
        },
        new TwoMassCutoutService( 'J' ),
        new TwoMassCutoutService( 'H' ),
        new TwoMassCutoutService( 'K' ),
        new CutoutService( "SDSS Colour Images", 0.4 ) {
            String displayCutout( int tableID, double ra, double dec,
                                  int npix ) {
                return Sdss.sdssShowCutout( "SDSS (" + tableID + ")",
                                            Math.toDegrees( ra ),
                                            Math.toDegrees( dec ), npix );
            }
        },
    };

    /**
     * Constructs a new cutout selector.
     *
     * @param  tcModel  table this selector is to work for
     */
    public CutoutSelector( TopcatModel tcModel ) {
        super( new BorderLayout() );
        tcModel_ = tcModel;

        /* Set up components for user interaction. */
        JLabel serviceLabel = new JLabel( "Cutout Service: " );
        serviceSelector_ = new JComboBox( SERVICES );
        serviceSelector_.addItemListener( this );
        raSelector_ = new ColumnSelector( 
                          tcModel_.getColumnSelectorModel( Tables.RA_INFO ),
                                                           true );
        decSelector_ = new ColumnSelector(
                           tcModel_.getColumnSelectorModel( Tables.DEC_INFO ),
                                                            true );
        int[] sizes = new int[] { 25, 50, 75, 100, 150, 200, 300, 400, 500 };
        JLabel npixLabel = new JLabel( "Width/Height in Pixels: " );
        npixSelector_ = new IntSelector( sizes );
        npixSelector_.setValue( 100 );
        pixsizeLabel_ = new JLabel( "" );

        /* Maintain list for {dis,en}ablement. */
        enablables_ = new Component[] {
            serviceLabel, serviceSelector_,
            raSelector_,
            decSelector_,
            npixLabel, npixSelector_, pixsizeLabel_,
        };

        /* Place components. */
        Box box = Box.createVerticalBox();
        add( box );

        /* Cutout service selector. */
        Box cutoutLine = Box.createHorizontalBox();
        cutoutLine.add( serviceLabel );
        cutoutLine.add( serviceSelector_ );
        box.add( cutoutLine );
        box.add( Box.createVerticalStrut( 5 ) );

        /* RA column selector. */
        Box raLine = Box.createHorizontalBox();
        raLine.add( raSelector_ );
        box.add( raLine );
        box.add( Box.createVerticalStrut( 5 ) );

        /* Declination column selector. */
        Box decLine = box.createHorizontalBox();
        decLine.add( decSelector_ );
        box.add( decLine );
        box.add( Box.createVerticalStrut( 5 ) );

        /* Cutout image size selector. */
        Box npixLine = Box.createHorizontalBox();
        npixLine.add( npixLabel );
        npixLine.add( npixSelector_ );
        npixLine.add( pixsizeLabel_ );
        box.add( npixLine );

        /* Trigger events to put this component into a normal state. */
        CutoutService serv = (CutoutService) serviceSelector_.getSelectedItem();
        serviceSelector_.setSelectedItem( null );
        serviceSelector_.setSelectedItem( serv );
    }

    /**
     * Returns an Activator based on the current settings of this widget.
     * The consequence of the activation will be an image popping up 
     * around the given row's RA,Dec position.
     * If not enough state has been specified to create a suitable activator,
     * <tt>null</tt> will be returned.
     *
     * @return  new activator
     */
    public Activator makeActivator() {
        CutoutService serv = (CutoutService) serviceSelector_.getSelectedItem();
        String trouble;
        if ( serv != null ) {
            ColumnData raData = raSelector_.getColumnData();
            ColumnData decData = decSelector_.getColumnData();
            int npix = npixSelector_.getValue();
            int tableID = tcModel_.getID();
            if ( raData == null ) {
                trouble = "No RA column defined";
            }
            else if ( decData == null ) {
                trouble = "No Declination column defined";
            }
            else if ( npix <= 0 ) {
                trouble = "Non-positive number of pixels";
            }
            else {
                return serv.makeActivator( tableID, raData, decData, npix );
            }
        }
        else {
            trouble = "No Cutout Service selected";
        }

        /* If we've got this far, we can't come up with an activator -
         * message the user and return null. */
        JOptionPane.showMessageDialog( this, trouble, 
                                       "Underspecified Action Error",
                                       JOptionPane.ERROR_MESSAGE );
        return null;
    }

    public void setEnabled( boolean enabled ) {
        for ( int i = 0; i < enablables_.length; i++ ) {
            enablables_[ i ].setEnabled( enabled );
        }
    }

    public void itemStateChanged( ItemEvent evt ) {
        if ( evt.getSource() == serviceSelector_ ) {
            CutoutService serv = 
                (CutoutService) serviceSelector_.getSelectedItem();
            if ( serv != null ) {
                pixsizeLabel_.setText( " (" + serv.getPixelSize() + 
                                       " arcsec)" );
            }
            else {
                pixsizeLabel_.setText( "     " );
            }
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
         * @param  ra   right ascension in radians
         * @param  dec  declination in radians
         * @param  npix linear dimension of image in pixels
         * @return log message for display operation
         */
        abstract String displayCutout( int tableID, double ra, double dec,
                                       int npix );

        /**
         * Returns an activator for this service in accordance with the 
         * current settings of the containing CutoutSelector.
         */
        public Activator makeActivator( final int tableID,
                                        final ColumnData raData,
                                        final ColumnData decData,
                                        final int npix ) {
            final String activatorName = name_ + "($ra, $dec, " + npix + ")";
            return new Activator() {
                public String activateRow( long lrow ) {
                    Object raObj;
                    Object decObj;
                    try {
                        raObj = raData.readValue( lrow );
                        decObj = decData.readValue( lrow );
                    }
                    catch ( IOException e ) {
                        return "Error reading position " + e;
                    }
                    if ( raObj instanceof Number &&
                         decObj instanceof Number ) {
                        double ra = ((Number) raObj).doubleValue();
                        double dec = ((Number) decObj).doubleValue();
                        if ( ! Double.isNaN( ra ) && ! Double.isNaN( dec ) ) {
                            return displayCutout( tableID, ra, dec, npix );
                        }
                    }
                    return "No position at (" + raObj + ", " + decObj + ")";
                }
                public String toString() {
                    return activatorName;
                }
            };
        }

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
                                            Math.toDegrees( ra ),
                                            Math.toDegrees( dec ), npix, 
                                            Character.toLowerCase( band_ ) );
        }
    }
}
