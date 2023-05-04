package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Table load dialogue for positional DAL queries.
 *
 * @author   Mark Taylor
 * @since    17 Jan 2011
 */
public abstract class SkyDalTableLoadDialog extends DalTableLoadDialog
                                            implements DalLoader {

    private final String protoName_;
    private SkyPositionEntry skyEntry_;

    /**
     * Constructor.
     *
     * @param   name  dialogue name
     * @param   protoName   short name (perhaps acronym) for protocol
     * @param   description  dialogue description
     * @param   capability   service capability type
     * @param   showCapabilities  true to display the capabilities JTable as
     *          well as the Resource one; sensible if resource:capabilities
     *          relationship may not be 1:1
     * @param   autoQuery  populate service table with full registry query
     *          on initial display
     */
    protected SkyDalTableLoadDialog( String name, String protoName,
                                     String description, Capability capability,
                                     boolean showCapabilities,
                                     boolean autoQuery ) {
        super( name, protoName, description, capability,
               showCapabilities, autoQuery );
        protoName_ = protoName;
    }

    protected Component createQueryComponent() {
        final Component superPanel = super.createQueryComponent();
        JComponent queryPanel = new JPanel( new BorderLayout() ) {
            public void setEnabled( boolean enabled ) {
                super.setEnabled( enabled );
                superPanel.setEnabled( enabled );
                skyEntry_.setEnabled( enabled );
            }
        };
        queryPanel.add( superPanel, BorderLayout.CENTER );

        /* Add a spatial position selector component. */
        skyEntry_ = new SkyPositionEntry( "J2000" );
        getControlBox().add( skyEntry_ );
        return queryPanel;
    }

    /**
     * Takes a sky position and may update this component's sky entry
     * fields with the supplied values.
     *
     * @param  raDegrees  right ascension in degrees
     * @param  decDegrees declination in degrees
     * @return  true iff the position was used
     */
    public boolean acceptSkyPosition( double raDegrees, double decDegrees ) {
        if ( isComponentShowing() ) {
            getSkyEntry().setPosition( raDegrees, decDegrees, false );
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Returns the SkyPositionEntry component used by this dialog.
     *
     * @return  sky position entry
     */
    public SkyPositionEntry getSkyEntry() {
        return skyEntry_;
    }

    /**
     * Returns a short string summarising the current query.
     *
     * @param  serviceUrl  service URL for the query to be labelled
     *         - may or may not be that of the currently selected
     *         resource/capability
     * @param  sizeDeg   size in degrees of the spatial query to be labelled
     * @return  query label
     */
    public String getQuerySummary( String serviceUrl, double sizeDeg ) {

        /* Get the name of the astronomical object being searched around,
         * if any. */
        String objName = getSkyEntry().getResolveField().getText();

        /* Get the short name for the registry resource providing the query
         * capability, if there is one. */
        RegistryPanel regPanel = getRegistryPanel();
        RegResource[] resources = regPanel.getSelectedResources();
        RegCapabilityInterface[] caps = regPanel.getSelectedCapabilities();
        String shortName = null;
        if ( resources.length == 1 && caps.length == 1 &&
             serviceUrl.equals( caps[ 0 ].getAccessUrl() ) ) {
            shortName = resources[ 0 ].getShortName();
            if ( shortName != null ) {
                shortName = shortName.replace( '/', '_' );
            }
        }
        if ( shortName == null || shortName.trim().length() == 0 ) {
            shortName = protoName_;
        }

        /* Get a short string summarising the query spatial extent. */
        String size;
        if ( sizeDeg > 0 ) {
            if ( sizeDeg > 1 ) {
                size = ((int) sizeDeg) + "d";
            }
            else if ( sizeDeg * 60 >= 1 ) {
                size = ((int) (sizeDeg * 60)) + "m";
            }
            else {
                size = ((int) (sizeDeg * 60 * 60)) + "s";
            }
        }
        else {
            size = null;
        }

        /* Combine and return the known information. */
        StringBuffer sbuf = new StringBuffer();
        if ( objName != null && objName.trim().length() > 0 ) {
            sbuf.append( objName )
                .append( '-' );
        }
        sbuf.append( shortName );
        if ( size != null ) {
            sbuf.append( '-' );
            sbuf.append( size );
        }
        return sbuf.toString();
    }
}
