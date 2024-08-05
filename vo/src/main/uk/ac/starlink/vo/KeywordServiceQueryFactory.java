package uk.ac.starlink.vo;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import uk.ac.starlink.util.URLUtils;
import uk.ac.starlink.util.gui.RenderingComboBox;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * RegistryQueryFactory implementation which combines a fixed base query
 * for a particular service type with a freeform keyword search.
 *
 * @author   Mark Taylor
 * @since    19 Dec 2008
 */
public class KeywordServiceQueryFactory implements RegistryQueryFactory {

    private final Capability capability_;
    private final JComponent queryPanel_;
    private final RegistrySelector regSelector_;
    private final JTextField keywordField_;
    private final AndButton andButton_;
    private final Map<ResourceField,JCheckBox> fieldSelMap_;

    /**
     * Constructs a query factory which looks for services with a particular
     * standard ID.
     *
     * @param  capability  description of capability which all results must have
     */
    public KeywordServiceQueryFactory( Capability capability ) {
        capability_ = capability;

        /* Registry endpoint selector. */
        regSelector_ = new RegistrySelector();

        /* Registry protocol selector. */
        final JComboBox<RegistrySelectorModel> protoSelector =
                new RenderingComboBox<RegistrySelectorModel>() {
            protected String getRendererText( RegistrySelectorModel item ) {
                return item.getProtocol().getShortName();
            }
        };
        for ( RegistryProtocol proto :
              Arrays.asList( RegistryProtocol.PROTOCOLS ) ) {
            protoSelector.addItem( new RegistrySelectorModel( proto ) );
        }
        protoSelector.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                regSelector_.setModel( (RegistrySelectorModel)
                                       protoSelector.getSelectedItem() );
            }
        } ); 
        protoSelector.setToolTipText( "Registry access protocol" );
        protoSelector.setSelectedItem( protoSelector.getItemAt( 0 ) );

        /* Place registry selection components. */
        JComponent urlLine = Box.createHorizontalBox();
        urlLine.add( regSelector_ );
        urlLine.add( Box.createHorizontalStrut( 5 ) );
        urlLine.add( new ShrinkWrapper( protoSelector ) );

        /* Prepare panel. */
        queryPanel_ = new Box( BoxLayout.Y_AXIS ) {
            public void setEnabled( boolean enabled ) {
                super.setEnabled( enabled );
                regSelector_.setEnabled( enabled );
                protoSelector.setEnabled( enabled );
                keywordField_.setEnabled( enabled );
                andButton_.setEnabled( enabled );
                for ( JCheckBox button : fieldSelMap_.values() ) {
                    button.setEnabled( enabled );
                }
            }
        };
        queryPanel_.add( urlLine );
        queryPanel_.add( Box.createVerticalStrut( 5 ) );

        /* And/Or toggle button. */
        andButton_ = new AndButton( true );
 
        /* Keywords selector. */
        JComponent keywordLine = Box.createHorizontalBox();
        keywordField_ = new JTextField();
        keywordField_.setToolTipText( "Space-separated list of keywords"
                                    + " to match in resource title,"
                                    + " description, IVORN, etc" );
        keywordLine.add( new JLabel( "Keywords: " ) );
        keywordLine.add( keywordField_ );
        keywordLine.add( Box.createHorizontalStrut( 5 ) );
        keywordLine.add( andButton_ );
        queryPanel_.add( keywordLine );

        /* Checkboxes for resource fields to match keywords against. */
        fieldSelMap_ = new LinkedHashMap<ResourceField,JCheckBox>();
        JComponent matchLine = Box.createHorizontalBox();
        matchLine.add( new JLabel( "Match Fields: " ) );
        ResourceField[] fields = {
            ResourceField.SHORTNAME,
            ResourceField.TITLE,
            ResourceField.SUBJECTS,
            ResourceField.ID,
            ResourceField.PUBLISHER,
            ResourceField.DESCRIPTION,
        };
        for ( ResourceField rf : fields ) {
            JCheckBox checkBox = new JCheckBox( rf.getLabel() );
            checkBox.setSelected( rf != ResourceField.DESCRIPTION );
            checkBox.setToolTipText( "Match keywords against VOResource \""
                                   + rf.getXpath() + " field?" );
            matchLine.add( Box.createHorizontalStrut( 5 ) );
            matchLine.add( checkBox );
            fieldSelMap_.put( rf, checkBox );
        }
        matchLine.add( Box.createHorizontalGlue() );
        queryPanel_.add( matchLine );
    }

    public RegistryQuery getQuery() throws MalformedURLException {
        boolean isAnd = andButton_.isAnd();
        String conjunction = isAnd ? "and" : "or";
        String keyText = keywordField_.getText();
        String[] keywords = ( keyText == null || keyText.trim().length() == 0 )
                          ? new String[ 0 ]
                          : keyText.trim().split( "\\s+" );
        List<ResourceField> rfList = new ArrayList<ResourceField>();
        for ( ResourceField rf : fieldSelMap_.keySet() ) {
            if ( fieldSelMap_.get( rf ).isSelected() ) {
                rfList.add( rf );
            }
        }
        RegistryProtocol proto = regSelector_.getModel().getProtocol();
        ResourceField[] fields = rfList.toArray( new ResourceField[ 0 ] );
        return proto.createKeywordQuery( keywords, fields, ! isAnd, capability_,
                                         getUrl() );
    }

    public RegistryQuery getIdListQuery( String[] ivoids )
            throws MalformedURLException {
        RegistryProtocol proto = regSelector_.getModel().getProtocol();
        return proto.createIdListQuery( ivoids, capability_, getUrl() );
    }

    public JComponent getComponent() {
        return queryPanel_;
    }

    public void addEntryListener( ActionListener listener ) {
        keywordField_.addActionListener( listener );
    }

    public void removeEntryListener( ActionListener listener ) {
        keywordField_.removeActionListener( listener );
    }

    public RegistrySelector getRegistrySelector() {
        return regSelector_;
    }

    /**
     * Returns the currently selected registry endpoint URL.
     *
     * @return   registry URL
     */
    private URL getUrl() throws MalformedURLException {
        return URLUtils.newURL( regSelector_.getUrl() );
    }
}
