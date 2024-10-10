package uk.ac.starlink.topcat.join;

import java.awt.Component;
import java.awt.event.ActionListener;
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
import uk.ac.starlink.ttools.cone.ConeServiceType;
import uk.ac.starlink.ttools.cone.Coverage;
import uk.ac.starlink.ttools.cone.SsaConeSearcher;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.gui.ShrinkWrapper;
import uk.ac.starlink.vo.Capability;
import uk.ac.starlink.vo.ConeVerbosity;
import uk.ac.starlink.vo.RegistryPanel;
import uk.ac.starlink.vo.SsapTableLoadDialog;

/**
 * DalMultiWindow subclass for Simple Spectral Access services.
 *
 * @author   Mark Taylor
 * @since    30 Sep 2009
 */
public class SsaMultiWindow extends DalMultiWindow {

    /**
     * Constructor.
     *
     * @param  parent  parent component
     */
    @SuppressWarnings("this-escape")
    public SsaMultiWindow( Component parent ) {
        super( parent, new SsaMultiService(), true );
        addHelp( "SsaMultiWindow" );
    }

    /**
     * DalMultiService implementation for SSA service type.
     */
    private static class SsaMultiService implements DalMultiService {

        private final JComboBox<String> formatSelector_
            = new JComboBox<String>( SsapTableLoadDialog.getFormatOptions() );

        private final JComponent controlBox_;

        /**
         * Constructor.
         */
        SsaMultiService() {
            formatSelector_.setSelectedIndex( 0 );
            formatSelector_.setEditable( true );

            final JLabel formatLabel = new JLabel( "Spectrum Format: " );
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
            return "SSA";
        }

        public String getLabel() {
            return "ssa";
        }

        public String getResourceListType() {
            return "ssap";
        }

        public Capability getCapability() {
            return Capability.SSA;
        }

        public ConeServiceType getServiceType() {
            return ConeServiceType.SSA;
        }

        public ValueInfo getSizeInfo() {
            DefaultValueInfo info =
                new DefaultValueInfo( "Search Radius", Number.class,
                                      "Angular radius of the search region; "
                                    + "blank value should use a "
                                    + "service-dependent default" );
            info.setUnitString( "radians" );
            info.setUCD( "pos.angDistance" );
            return info;
        }

        public void setSizeDefault( ColumnSelector sizeSelector ) {
            sizeSelector.setStringValue( "" );
        }

        public boolean allowNullSize() {
            return true;
        }

        public JComponent getControlPanel() {
            return controlBox_;
        }

        public JComponent getVersionComponent() {
            return null;
        }

        public void init( RegistryPanel regPanel ) {
        }

        public ConeSearcher createSearcher( URL url, StarTableFactory tfact,
                                            ContentCoding coding ) {
            String format = (String) formatSelector_.getSelectedItem();
            return new SsaConeSearcher( url.toString(), format, false, tfact,
                                        coding );
        }

        public boolean hasCoverages() {
            return false;
        }

        public Coverage getCoverage( URL url ) {
            return null;
        }

        public ConeVerbosity getVerbosity() {
            return null;
        }

        public void addActionListener( ActionListener l ) {
            formatSelector_.addActionListener( l );
        }

        public void removeActionListener( ActionListener l ) {
            formatSelector_.removeActionListener( l );
        }
    }
}
