package uk.ac.starlink.vo;

import java.awt.Dimension;
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
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

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
    private final RegistrySelector urlSelector_;
    private final JTextField keywordField_;
    private final JButton andButton_;
    private final Map<ResourceField,JCheckBox> fieldSelMap_;
    private boolean or_;

    /**
     * Constructs a query factory which looks for services with a particular
     * standard ID.
     *
     * @param  capability  description of capability which all results must have
     */
    public KeywordServiceQueryFactory( Capability capability ) {
        capability_ = capability;
        queryPanel_ = new Box( BoxLayout.Y_AXIS ) {
            public void setEnabled( boolean enabled ) {
                super.setEnabled( enabled );
                urlSelector_.setEnabled( enabled );
                keywordField_.setEnabled( enabled );
                andButton_.setEnabled( enabled );
                for ( JCheckBox button : fieldSelMap_.values() ) {
                    button.setEnabled( enabled );
                }
            }
        };

        /* Registry endpoint selector. */
        JComponent urlLine = Box.createHorizontalBox();
        urlSelector_ = new RegistrySelector();
        urlLine.add( urlSelector_ );
        queryPanel_.add( urlLine );
        queryPanel_.add( Box.createVerticalStrut( 5 ) );

        /* And/Or toggle button. */
        andButton_ = new JButton() {
            private Dimension prefSize_ = new Dimension( 0, 0 );
            public String getText() {
                return or_ ? "Or" : "And";
            }
            public Dimension getPreferredSize() {
                Dimension psize = super.getPreferredSize();
                if ( psize == null ) {
                    return null;
                }
                prefSize_ =
                    new Dimension( Math.max( prefSize_.width, psize.width ),
                                   Math.max( prefSize_.height, psize.height ) );
                return prefSize_;
            }
        };
        andButton_.setToolTipText( "Toggles whether keywords are combined "
                                 + "using AND or OR for registry search" );
        andButton_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                or_ = ! or_;
            }
        } );
 
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
        queryPanel_.add( matchLine );
    }

    public RegistryQuery getQuery() throws MalformedURLException {
        String conjunction = or_ ? "or" : "and";
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
        RegistryProtocol proto = urlSelector_.getModel().getProtocol();
        ResourceField[] fields = rfList.toArray( new ResourceField[ 0 ] );
        return proto.createKeywordQuery( keywords, fields, or_, capability_,
                                         getUrl() );
    }

    public RegistryQuery getIdListQuery( String[] ivoids )
            throws MalformedURLException {
        RegistryProtocol proto = urlSelector_.getModel().getProtocol();
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
        return urlSelector_;
    }

    /**
     * Returns the currently selected registry endpoint URL.
     *
     * @return   registry URL
     */
    private URL getUrl() throws MalformedURLException {
        return new URL( urlSelector_.getUrl() );
    }
}
