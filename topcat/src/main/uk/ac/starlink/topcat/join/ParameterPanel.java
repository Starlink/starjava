package uk.ac.starlink.topcat.join;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.join.MatchEngine;

/**
 * Graphical component which allows editing of any matching parameters
 * associated with a match engine.
 *
 * @author   Mark Taylor (Starlink)
 * @since    20 Mar 2004
 */
public class ParameterPanel extends JPanel {

    /**
     * Construcsts a new ParameterPanel.
     *
     * @param  engine  the match engine this will work on
     */
    public ParameterPanel( MatchEngine engine ) {
        Box main = Box.createVerticalBox();
        add( main );

        /* Add one ParameterEditor for each parameter of the engine. */
        DescribedValue[] params = engine.getMatchParameters();
        for ( int i = 0; i < params.length; i++ ) {
            DescribedValue param = params[ i ];
            ValueInfo info = param.getInfo();
            Box line = Box.createHorizontalBox();
            line.add( new JLabel( info.getName() + ": " ) );
            line.add( new ParameterEditor( param ) );
            String units = info.getUnitString();
            if ( units != null && units.trim().length() > 0 ) {
                line.add( new JLabel( " (" + units + ")" ) );
            }
            main.add( line );
        }
    }
}
