package uk.ac.starlink.vo;

import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.TableLoader;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Table load dialogue which allows cone searches.  Cone search services
 * are obtained from a registry.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Dec 2004
 */
public class ConeSearchDialog extends SkyDalTableLoadDialog {

    private final ContentCoding coding_;
    private DoubleValueField raField_;
    private DoubleValueField decField_;
    private DoubleValueField srField_;
    private JComboBox<ConeVerbosity> verbSelector_;
    private static final ValueInfo SR_INFO =
        new DefaultValueInfo( "Radius", Double.class, "Search Radius" );

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public ConeSearchDialog() {
        super( "Cone Search", "Cone",
               "Obtain source catalogues using cone search web services",
               Capability.CONE, true, false );
        coding_ = ContentCoding.GZIP;
        setIcon( ResourceIcon.TLD_CONE );
    }

    protected Component createQueryComponent() {
        Component queryPanel = super.createQueryComponent();
        SkyPositionEntry skyEntry = getSkyEntry();
        raField_ = skyEntry.getRaDegreesField();
        decField_ = skyEntry.getDecDegreesField();
        srField_ = DoubleValueField.makeSizeDegreesField( SR_INFO );
        skyEntry.addField( srField_ );

        /* Add selector for verbosity. */
        JComponent verbLine = Box.createHorizontalBox();
        verbSelector_ = new JComboBox<>( ConeVerbosity.getOptions() );
        verbSelector_.setSelectedIndex( 1 );
        assert getVerbosity() == 2;
        verbLine.add( new JLabel( "Verbosity: " ) );
        verbLine.add( new ShrinkWrapper( verbSelector_ ) );
        verbLine.add( Box.createHorizontalGlue() );
        getControlBox().add( Box.createVerticalStrut( 5 ) );
        getControlBox().add( verbLine );
        return queryPanel;
    }

    public TableLoader createTableLoader() {
        String serviceUrl = getServiceUrl();
        checkUrl( serviceUrl );
        final ConeSearch coner = new ConeSearch( serviceUrl, coding_ );
        final double ra = raField_.getValue();
        final double dec = decField_.getValue();
        final double sr = srField_.getValue();
        final int verb = getVerbosity();
        final List<DescribedValue> metadata = new ArrayList<DescribedValue>();
        metadata.addAll( Arrays.asList( new DescribedValue[] {
            raField_.getDescribedValue(),
            decField_.getDescribedValue(),
            srField_.getDescribedValue(),
        } ) );
        metadata.addAll( Arrays.asList( getResourceMetadata( serviceUrl ) ) );
        final String summary = getQuerySummary( serviceUrl, sr );
        return new TableLoader() {
            public TableSequence loadTables( StarTableFactory factory )
                    throws IOException {
                StarTable st = coner.performSearch( ra, dec, sr, verb,
                                                    factory );
                st.getParameters().addAll( metadata );
                return Tables.singleTableSequence( st );
            }
            public String getLabel() {
                return summary;
            }
        };
    }

    /**
     * Returns the currently selected verbosity level.
     *
     * @return  verbosity level
     */
    public int getVerbosity() {
        return verbSelector_.getItemAt( verbSelector_.getSelectedIndex() )
                            .getLevel();
    }

    /**
     * Returns the verbosity selector component.
     *
     * @return  verbosity selector
     */
    public JComboBox<ConeVerbosity> getVerbositySelector() {
        return verbSelector_;
    }

    /**
     * Returns the search radius selected.
     *
     * @return  search radius value
     */
    public double getSearchRadius() {
        try {
            return srField_.getValue();
        }
        catch ( RuntimeException e ) {
            return Double.NaN;
        }
    }
}
