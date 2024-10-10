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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.ColumnSelector;
import uk.ac.starlink.ttools.cone.ConeSearcher;
import uk.ac.starlink.ttools.cone.ConeServiceType;
import uk.ac.starlink.ttools.cone.Coverage;
import uk.ac.starlink.ttools.cone.SiaConeSearcher;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.gui.ShrinkWrapper;
import uk.ac.starlink.vo.Capability;
import uk.ac.starlink.vo.ConeVerbosity;
import uk.ac.starlink.vo.RegCapabilityInterface;
import uk.ac.starlink.vo.RegistryPanel;
import uk.ac.starlink.vo.SiaFormatOption;
import uk.ac.starlink.vo.SiaVersion;
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
    @SuppressWarnings("this-escape")
    public SiaMultiWindow( Component parent ) {
        super( parent, new SiaMultiService(), true );
        addHelp( "SiaMultiWindow" );
    }

    /**
     * DalMultiService implementation for SIA service type.
     */
    private static class SiaMultiService implements DalMultiService {

        private final JComboBox<Object> formatSelector_;
        private final JComboBox<SiaVersion> versionSelector_;
        private final JComponent controlBox_;
        private final JComponent versionBox_;

        /**
         * Constructor.
         */
        SiaMultiService() {
            formatSelector_ =
                new JComboBox<Object>( SiaFormatOption.getStandardOptions() );
            formatSelector_.setSelectedIndex( 0 );
            formatSelector_.setEditable( true );

            versionSelector_ = new JComboBox<>( SiaVersion.values() );

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

            versionBox_ = Box.createHorizontalBox();
            versionBox_.add( new JLabel( "SIA Version: " ) );
            versionBox_.add( new ShrinkWrapper( versionSelector_ ) );
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

        public ConeServiceType getServiceType() {
            SiaVersion version = 
                versionSelector_
               .getItemAt( versionSelector_.getSelectedIndex() );
            switch ( version ) {
                case V10:
                    return ConeServiceType.SIA1;
                case V20:
                    return ConeServiceType.SIA2;
                default:
                    assert false;
                    return ConeServiceType.SIA;
            }
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

        public boolean allowNullSize() {
            return false;
        }

        public JComponent getControlPanel() {
            return controlBox_;
        }

        public JComponent getVersionComponent() {
            return versionBox_;
        }

        public void init( final RegistryPanel regPanel ) {
            ListSelectionListener servListener = new ListSelectionListener() {
                public void valueChanged( ListSelectionEvent evt ) {
                    RegCapabilityInterface[] intfs =
                        regPanel.getSelectedCapabilities();
                    if ( intfs.length == 1 ) {
                        SiaVersion vers = SiaVersion.forInterface( intfs[ 0 ] );
                        if ( vers != null ) {
                            versionSelector_.setSelectedItem( vers );
                        }   
                    }
                }
            };
            regPanel.getResourceSelectionModel()
                    .addListSelectionListener( servListener );
            regPanel.getCapabilitySelectionModel()
                    .addListSelectionListener( servListener );
        }

        public ConeSearcher createSearcher( URL url, StarTableFactory tfact,
                                            ContentCoding coding ) {
            SiaVersion version = 
                versionSelector_
               .getItemAt( versionSelector_.getSelectedIndex() );
            SiaFormatOption format =
                SiaFormatOption.fromObject( formatSelector_.getSelectedItem() );
            return new SiaConeSearcher( url.toString(), version, format,
                                        false, tfact, coding );
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
            versionSelector_.addActionListener( l );
        }

        public void removeActionListener( ActionListener l ) {
            formatSelector_.removeActionListener( l );
            versionSelector_.removeActionListener( l );
        }
    }
}
