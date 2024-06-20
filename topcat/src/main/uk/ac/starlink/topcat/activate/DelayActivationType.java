package uk.ac.starlink.topcat.activate;

import java.awt.BorderLayout;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.Safety;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * ActivationType that introduces a delay.
 * This duplicates functionality that can be achieved using the
 * JEL sleep() function or the System sleep command,
 * but it's quite useful in sequences and is more discoverable this way.
 *
 * @author   Mark Taylor
 * @since    21 May 2019
 */
public class DelayActivationType implements ActivationType {

    private static final String SECONDS_KEY = "seconds";

    public String getName() {
        return "Delay";
    }

    public String getDescription() {
        return "Pauses for a chosen number of seconds";
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        return new DelayConfigurator();
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return Suitability.PRESENT;
    }

    /**
     * Activator implementation for delay.
     */
    private static class DelayActivator implements Activator {
        private final long millis_;

        /**
         * Constructor.
         *
         * @param  millis  delay interval in milliseconds
         */
        DelayActivator( long millis ) {
            millis_ = millis;
        }

        public boolean invokeOnEdt() {
            return false;
        }

        public Outcome activateRow( long lrow, ActivationMeta meta ) {
            try {
                Thread.sleep( millis_ );
                return Outcome.success();
            }
            catch ( InterruptedException e ) {
                return Outcome.failure( "interrupted" );
            }
        }
    }

    /**
     * Configurator implementation for delay.
     */
    private static class DelayConfigurator
            extends AbstractActivatorConfigurator {

        private final JTextField secField_;

        DelayConfigurator() {
            super( new JPanel( new BorderLayout() ) );
            JComponent line = Box.createHorizontalBox();
            secField_ = new JTextField( 6 );
            line.add( new ShrinkWrapper( secField_ ) );
            line.add( new JLabel( " seconds" ) );
            getPanel().add( line, BorderLayout.NORTH );
            secField_.setText( Integer.valueOf( 1 ).toString() );
            secField_.addActionListener( getActionForwarder() );
        }

        public Activator getActivator() {
            String secTxt = secField_.getText();
            final double nsec;
            try {
                nsec = Double.parseDouble( secTxt );
            }
            catch ( RuntimeException e ) {
                return null;
            }
            if ( ! ( nsec > 0 ) ) {
                return null;
            }
            return new DelayActivator( (long) ( 1000. * nsec ) );
        }

        public String getConfigMessage() {
            return getActivator() == null
                 ? "Not positive number: " + secField_.getText()
                 : null;
        }

        public Safety getSafety() {
            return Safety.SAFE;
        }

        public ConfigState getState() {
            ConfigState state = new ConfigState();
            state.saveText( SECONDS_KEY, secField_ );
            return state;
        }

        public void setState( ConfigState state ) {
            state.restoreText( SECONDS_KEY, secField_ );
        }
    }
}
