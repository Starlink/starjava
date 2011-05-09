package uk.ac.starlink.vo;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
    private final MatchField[] matchFields_;
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
                for ( int im = 0; im < matchFields_.length; im++ ) {
                    matchFields_[ im ].button_.setEnabled( enabled );
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

        /* Match field selectors.  See the VOResource schema
         * (http://www.ivoa.net/Documents/latest/VOResource.html) for
         * path definitions. */
        matchFields_ = new MatchField[] {
            new MatchField( "shortName", "Short Name", true ),
            new MatchField( "title", "Title", true ),
            new MatchField( "content/subject", "Subjects", true ),
            new MatchField( "identifier", "ID", true ),
            new MatchField( "curation/publisher", "Publisher", true ),
            new MatchField( "content/description", "Description", false ),
        };
        JComponent matchLine = Box.createHorizontalBox();
        matchLine.add( new JLabel( "Match Fields: " ) );
        for ( int im = 0; im < matchFields_.length; im++ ) {
            if ( im > 0 ) {
                matchLine.add( Box.createHorizontalStrut( 5 ) );
            }
            matchLine.add( matchFields_[ im ].button_ );
        }
        matchLine.add( Box.createHorizontalGlue() );
        queryPanel_.add( matchLine );
    }

    public RegistryQuery getQuery() throws MalformedURLException {
        String conjunction = or_ ? "or" : "and";
        String keyText = keywordField_.getText();
        String[] keywords = ( keyText == null || keyText.trim().length() == 0 )
                          ? new String[ 0 ]
                          : keyText.trim().split( "\\s+" );
        List<String> matchList = new ArrayList<String>();
        for ( int im = 0; im < matchFields_.length; im++ ) {
            MatchField mf = matchFields_[ im ];
            if ( mf.button_.isSelected() ) {
                matchList.add( mf.fieldPath_ );
            }
        }
        String[] matchPaths = matchList.toArray( new String[ 0 ] );
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( capability_.getAdql() );
        if ( keywords.length > 0 ) {
            sbuf.append( " and ( " );
            for ( int iw = 0; iw < keywords.length; iw++ ) {
                if ( iw > 0 ) {
                    sbuf.append( conjunction );
                }
                sbuf.append( "(" );
                for ( int ip = 0; ip < matchPaths.length; ip++ ) {
                    if ( ip > 0 ) {
                        sbuf.append( " or " );
                    }
                    sbuf.append( matchPaths[ ip ] )
                        .append( " like " )
                        .append( "'%" )
                        .append( keywords[ iw ] )
                        .append( "%'" );
                }
                sbuf.append( ")" );
            }
            sbuf.append( " )" );
        }
        String adql = sbuf.toString();
        String url = new URL( (String) urlSelector_.getUrl() ).toString();
        return new RegistryQuery( url, adql ); 
    }

    public RegistryQuery getIdListQuery( String[] ivoids )
            throws MalformedURLException {
        if ( ivoids == null || ivoids.length == 0 ) {
            return null;
        }
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "(" )
            .append( capability_.getAdql() )
            .append( ")" )
            .append( " and " )
            .append( "(" );
        for ( int i = 0; i < ivoids.length; i++ ) {
            if ( i > 0 ) {
                sbuf.append( " or " );
            }
            sbuf.append( "identifier = '" )
                .append( ivoids[ i ] )
                .append( "'" );
        }
        sbuf.append( ")" );
        String adql = sbuf.toString();
        String url = new URL( (String) urlSelector_.getUrl() ).toString();
        return new RegistryQuery( url, adql );
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
     * Takes care of selection of one path element to match by.
     * Aggregates a checkbox button and some metadata.
     */
    private static class MatchField {
        final JCheckBox button_;
        final String fieldPath_;
        final String fieldLabel_;

        /**
         * Constructor.
         *
         * @param   xpath  string for matching against the VOResource schema
         * @param   label  human-readable label for data element
         * @param   on     initial selection status
         */
        MatchField( String path, String label, boolean on ) {
            fieldPath_ = path;
            fieldLabel_ = label;
            button_ = new JCheckBox( label, on );
            button_.setToolTipText( "Match keywords against \"" + path
                                  + "\" field?" );
        }
    }
}
