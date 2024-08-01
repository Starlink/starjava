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
import uk.ac.starlink.ttools.cone.Coverage;
import uk.ac.starlink.ttools.cone.UrlMocCoverage;
import uk.ac.starlink.ttools.cone.ServiceConeSearcher;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.gui.ShrinkWrapper;
import uk.ac.starlink.vo.Capability;
import uk.ac.starlink.vo.ConeSearch;
import uk.ac.starlink.vo.ConeVerbosity;
import uk.ac.starlink.vo.RegistryPanel;

/**
 * DalMultiWindow subclass for Cone Search services.
 *
 * @author   Mark Taylor
 * @since    30 Sep 2009
 */
public class ConeMultiWindow extends DalMultiWindow {

    /**
     * Constructor.
     *
     * @param  parent  parent component
     */
    @SuppressWarnings("this-escape")
    public ConeMultiWindow( Component parent ) {
        super( parent, new ConeMultiService(), false );
        addHelp( "ConeMultiWindow" );
    }

    /**
     * DalMultiService implementation for Cone Search service type.
     */
    private static class ConeMultiService implements DalMultiService {

        private final JComboBox<ConeVerbosity> verbSelector_ =
            new JComboBox<>( ConeVerbosity.getOptions() );

        private final JComponent controlBox_;

        ConeMultiService() {
            verbSelector_.setSelectedIndex( 1 );
            assert ((ConeVerbosity) verbSelector_.getSelectedItem()).getLevel()
                   == 2;
            final JLabel verbLabel = new JLabel( "Verbosity: " );
            controlBox_ = new JPanel() {
                public void setEnabled( boolean enabled ) {
                    super.setEnabled( enabled );
                    verbSelector_.setEnabled( enabled );
                    verbLabel.setEnabled( enabled );
                }
            };
            controlBox_.setLayout( new BoxLayout( controlBox_,
                                                  BoxLayout.Y_AXIS ) );
            JComponent line = Box.createHorizontalBox();
            line.add( verbLabel );
            line.add( new ShrinkWrapper( verbSelector_ ) );
            line.add( Box.createHorizontalGlue() );
            controlBox_.add( line );
        }

        public String getName() {
            return "Cone Search";
        }

        public String getLabel() {
            return "cone";
        }

        public Capability getCapability() {
            return Capability.CONE;
        }

        public String getResourceListType() {
            return "cone";
        }

        public ValueInfo getSizeInfo() {
            DefaultValueInfo info =
                new DefaultValueInfo( "Search Radius", Number.class,
                                      "Maximum distance from target position" );
            info.setUnitString( "radians" );
            info.setUCD( "pos.angDistance" );
            return info;
        }

        public void setSizeDefault( ColumnSelector sizeSelector ) {
            sizeSelector.setStringValue( "1.0" );
        }

        public boolean allowNullSize() {
            return false;
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
            int verb = verbSelector_
                      .getItemAt( verbSelector_.getSelectedIndex() )
                      .getLevel();
            return new ServiceConeSearcher( new ConeSearch( url.toString(),
                                                            coding ),
                                            verb, false, tfact );
        }

        public boolean hasCoverages() {
            return true;
        }

        public Coverage getCoverage( URL url ) {
            return UrlMocCoverage.getServiceMoc( url, -1 );
        }
    }
}
