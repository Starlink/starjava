package uk.ac.starlink.topcat.contrib.cds;

import cds.vizier.VizieRQueryInterface;
import java.awt.Component;
import javax.swing.Box;
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
     * @param   vqi  Vizier query interface
     * @param   tld  controlling load dialogue
     */
    public WordVizierMode( VizieRQueryInterface vqi,
                           VizierTableLoadDialog tld ) {
        super( "By Keyword", vqi, tld, false );
        wordField_ = new JTextField();
    }

    protected Component createSearchComponent() {
        Box line = Box.createHorizontalBox();
        line.add( new JLabel( "Keywords: " ) );
        line.add( wordField_ );
        return line;
    }

    protected String getSearchArgs() {
        String txt = wordField_.getText();
        return txt.trim().length() > 0
             ? VizierTableLoadDialog.encodeArg( "-words", txt.trim() )
             : "";
    }
}
