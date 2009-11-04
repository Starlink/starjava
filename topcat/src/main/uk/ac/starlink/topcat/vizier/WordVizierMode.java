package uk.ac.starlink.topcat.vizier;

import java.awt.Component;
import javax.swing.JComponent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * Vizier mode that allows the user to search for available catalogues 
 * by free text keywords.
 *
 * @author   Mark Taylor
 * @since    19 Oct 2009
 */
public class WordVizierMode extends SearchVizierMode {

    private final JTextField wordField_;

    /**
     * Constructor.
     *
     * @param   vizinfo  Vizier query interface
     * @param   tld  controlling load dialogue
     */
    public WordVizierMode( VizierInfo vizinfo,
                           VizierTableLoadDialog tld ) {
        super( "By Keyword", vizinfo, tld, false );
        wordField_ = new JTextField();
    }

    protected Component createSearchComponent() {
        final JLabel wordLabel = new JLabel( "Keywords: " );
        JComponent line = new Box( BoxLayout.X_AXIS ) {
            public void setEnabled( boolean enabled ) {
                super.setEnabled( enabled );
                wordLabel.setEnabled( enabled );
                wordField_.setEnabled( enabled );
            }
        };
        line.add( wordLabel );
        line.add( wordField_ );
        wordField_.addActionListener( getSearchAction() );
        return line;
    }

    protected String getSearchArgs() {
        String txt = wordField_.getText();
        return txt.trim().length() > 0
             ? VizierTableLoadDialog.encodeArg( "-words", txt.trim() )
             : "";
    }
}
