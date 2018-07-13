package uk.ac.starlink.topcat.activate;

import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.JPanel;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.Safety;

/**
 * Activation type which does nothing.
 *
 * <p>Pointless.
 *
 * @author   Mark Taylor
 * @since    23 Jan 2018
 */
public class NopActivationType implements ActivationType {

    /** Singleton instance. */
    public static final NopActivationType INSTANCE = new NopActivationType();

    /** Unconfigurable activator instance. */
    private static final Activator NOP_ACTIVATOR = new Activator() {
        public boolean invokeOnEdt() {
            return true;
        }
        public Outcome activateRow( long lrow, ActivationMeta meta ) {
            return NOP_OUTCOME;
        }
    };  

    private static final Outcome NOP_OUTCOME = Outcome.success( "No action" );

    /**
     * Private constructor for singleton class.
     */
    private NopActivationType() {
    }

    public String getName() {
        return "No action";
    }

    public String getDescription() {
        return "Do nothing";
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return Suitability.NONE;
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        final JComponent panel = new JPanel();
        return new ActivatorConfigurator() {
            public JComponent getPanel() {
                return panel;
            }
            public Activator getActivator() {
                return NOP_ACTIVATOR;
            }
            public String getConfigMessage() {
                return "Doesn't do anything";
            }
            public Safety getSafety() {
                return Safety.SAFE;
            }
            public void addActionListener( ActionListener l ) {
            }
            public void removeActionListener( ActionListener l ) {
            }
            public ConfigState getState() {
                return new ConfigState();
            }
            public void setState( ConfigState state ) {
            }
        };
    }
}
