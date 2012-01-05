package uk.ac.starlink.topcat.join;

import java.awt.Component;
import java.net.URL;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.ColumnSelector;
import uk.ac.starlink.ttools.cone.ConeSearcher;
import uk.ac.starlink.ttools.cone.Footprint;
import uk.ac.starlink.ttools.cone.SiaConeSearcher;
import uk.ac.starlink.util.gui.ShrinkWrapper;
import uk.ac.starlink.vo.Capability;
import uk.ac.starlink.vo.SiapTableLoadDialog;

/**
 * DalMultiWindow subclass for Simple Image Access services.
 *
 * @author   Mark Taylor
 * @since    30 Sep 2009
 */
public class SiaMultiWindow extends DalMultiWindow {

    /**
     * Constructor.
     *
     * @param  parent  parent component
     */
    public SiaMultiWindow( Component parent ) {
        super( parent, new SiaMultiService(), true );
        addHelp( "SiaMultiWindow" );
    }

    /**
     * DalMultiService implementation for SIA service type.
     */
    private static class SiaMultiService implements DalMultiService {

        private final JComboBox formatSelector_;
        private final JComponent controlBox_;

        /**
         * Constructor.
         */
        SiaMultiService() {
            formatSelector_ =
                new JComboBox( SiapTableLoadDialog.getFormatOptions() );
            formatSelector_.setSelectedIndex( 0 );
            formatSelector_.setEditable( true );

            final JLabel formatLabel = new JLabel( "Image Format: " );
            controlBox_ = new JPanel() {
                public void setEnabled( boolean enabled ) {
                    super.setEnabled( enabled );
                    formatSelector_.setEnabled( enabled );
                    formatLabel.setEnabled( enabled );
                }
            };
            controlBox_.setLayout( new BoxLayout( controlBox_,
                                                  BoxLayout.Y_AXIS ) );
            JComponent line = Box.createHorizontalBox();
            line.add( formatLabel );
            line.add( new ShrinkWrapper( formatSelector_ ) );
            line.add( Box.createHorizontalGlue() );
            controlBox_.add( line );
        }

        public String getName() {
            return "SIA";
        }

        public String getLabel() {
            return "sia";
        }

        public String getResourceListType() {
            return "siap";
        }

        public Capability getCapability() {
            return Capability.SIA;
        }

        public ValueInfo getSizeInfo() {
            DefaultValueInfo info =
                new DefaultValueInfo( "Search Radius", Number.class,
                                      "Angular radius of the search region; "
                                    + "zero means any image covering "
                                    + "the position" );
            info.setUnitString( "radians" );
            info.setUCD( "pos.angDistance" );
            return info;
        }

        public void setSizeDefault( ColumnSelector sizeSelector ) {
            sizeSelector.setStringValue( "0" );
        }

        public JComponent getControlPanel() {
            return controlBox_;
        }

        public ConeSearcher createSearcher( URL url, StarTableFactory tfact ) {
            String format = (String) formatSelector_.getSelectedItem();
            return new SiaConeSearcher( url.toString(), format, false, tfact );
        }

        public boolean hasFootprints() {
            return false;
        }

        public Footprint getFootprint( URL url ) {
            return null;
        }
    }
}
