package uk.ac.starlink.topcat.join;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Graphical component which allows editing of any matching parameters
 * associated with a match engine.
 *
 * @author   Mark Taylor (Starlink)
 * @since    20 Mar 2004
 */
public class ParameterPanel extends JPanel {

    private JComponent tuningBox_;
    private JComponent tuningContainer_;
    private ParameterEditor[] editors_;

    /**
     * Constructs a new ParameterPanel.
     *
     * @param  engine  the match engine this will work on
     */
    @SuppressWarnings("this-escape")
    public ParameterPanel( MatchEngine engine ) {
        super( new BorderLayout() );

        /* Prepare a box with a ParameterEditor for each of the match
         * parameters. */
        JComponent paramBox = Box.createVerticalBox();
        ParameterEditor[] paramEds =
            placeParameterEditors( engine.getMatchParameters(), null,
                                   paramBox );

        /* Prepare a box with a ParameterEditor for each of the tuning
         * parameters. */
        tuningBox_ = Box.createVerticalBox();
        final ParameterEditor[] tuningEds =
            placeParameterEditors( engine.getTuningParameters(), "tuning",
                                   tuningBox_ );

        /* Arrange for the tuning parameters to be updated if the match
         * parameters are changed.  The tuning parameter default values
         * may be determined by match parameter values. */
        ChangeListener paramListener = new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                for ( int i = 0; i < tuningEds.length; i++ ) {
                    tuningEds[ i ].initValue();
                }
            }
        };
        for ( int i = 0; i < paramEds.length; i++ ) {
            paramEds[ i ].addChangeListener( paramListener );
        }

        /* Note all parameter editors. */
        List<ParameterEditor> edList = new ArrayList<>();
        edList.addAll( Arrays.asList( paramEds ) );
        edList.addAll( Arrays.asList( tuningEds ) );
        editors_ = edList.toArray( new ParameterEditor[ 0 ] );

        /* Place components. */
        Box mainBox = Box.createVerticalBox();
        tuningContainer_ = Box.createVerticalBox();
        mainBox.add( paramBox );
        mainBox.add( tuningContainer_ );
        add( mainBox, BorderLayout.NORTH );
    }

    /**
     * Indicates if the tuning parameters are currently visible in the GUI.
     *
     * @return   true iff tuning parameters are visible
     */
    public boolean isTuningVisible() {
        Component[] comps = tuningContainer_.getComponents();
        if ( comps.length == 0 ) {
            return false;
        }
        else {
            assert comps.length == 1;
            assert comps[ 0 ] == tuningBox_;
            return true;
        }
    }

    /**
     * Sets whether the tuning parameters should be visible in the GUI.
     *
     * @param   tuningVisible  true iff tuning parameters should be shown
     */
    public void setTuningVisible( boolean tuningVisible ) {
        boolean wasVisible = isTuningVisible();
        if ( tuningVisible && ! wasVisible ) {
            tuningContainer_.add( tuningBox_ );
        }
        else if ( ! tuningVisible && wasVisible ) {
            tuningContainer_.remove( tuningBox_ );
        }
    }

    /**
     * Adds a listener to be notified if the state of this panel
     * may have changed.
     *
     * @param  l  listener to add
     */
    public void addChangeListener( ChangeListener l ) {
        for ( ParameterEditor ed : editors_ ) {
            ed.addChangeListener( l );
        }
    }

    /**
     * Removes state change listener.
     *
     * @param  l  listener to remove
     */
    public void removeChangeListener( ChangeListener l ) {
        for ( ParameterEditor ed : editors_ ) {
            ed.removeChangeListener( l );
        }
    }

    /**
     * Prepares and places ParameterEditor components for each of a set
     * of DescribedValues.
     *
     * @param  params   parameters for display/edit
     * @param  annotate  optional string which will annotate the parameter line
     * @param  container  container into which the editors will be 
     *                    <code>add</code>ed
     * @return  array of editors, one for each input param
     */
    private ParameterEditor[] placeParameterEditors( DescribedValue[] params,
                                                     String annotate,
                                                     JComponent container ) {
        ParameterEditor[] eds = new ParameterEditor[ params.length ];
        for ( int i = 0; i < params.length; i++ ) {
            DescribedValue param = params[ i ];
            eds[ i ] = new ParameterEditor( param );
            container.add( Box.createVerticalStrut( 5 ) );
            ValueInfo info = param.getInfo();
            Box line = Box.createHorizontalBox();
            JLabel label = new JLabel( info.getName() + ": " );
            label.setToolTipText( info.getDescription() );
            line.add( label );
            line.add( new ShrinkWrapper( eds[ i ] ) );
            if ( annotate != null ) {
                JLabel alabel = new JLabel( " (" + annotate + ")" );
                alabel.setFont( alabel.getFont().deriveFont( Font.PLAIN ) );
                line.add( alabel );
            }
            line.add( Box.createHorizontalGlue() );
            container.add( line );
        }
        return eds;
    }
}
