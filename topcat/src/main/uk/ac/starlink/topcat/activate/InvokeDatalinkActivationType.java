package uk.ac.starlink.topcat.activate;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.vo.datalink.LinksDoc;
import uk.ac.starlink.topcat.LinkRowPanel;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.Safety;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.UrlOptions;

/**
 * ActivationType for invoking individual rows of a Datalink Links-response
 * table.
 *
 * @author   Mark Taylor
 * @since    10 Apr 2018
 */
public class InvokeDatalinkActivationType implements ActivationType {

    public String getName() {
        return "Invoke Datalink Row";
    }

    public String getDescription() {
        return "Follow the link defined by the content of a "
             + "{links}-response table row";
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return LinksDoc
              .isLinksResponse( tinfo.getTopcatModel().getDataModel(), 2 )
             ? Suitability.SUGGESTED
             : Suitability.AVAILABLE;
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        return new LinkConfigurator( tinfo.getTopcatModel() );
    }

    /**
     * Configurator implementation for invoking datalink rows.
     */
    private static class LinkConfigurator
                         extends AbstractActivatorConfigurator {

        private final ToggleButtonModel autoModel_;
        private final TopcatModel tcModel_;
        private final LinkRowPanel linkPanel_;
        private final JFrame window_;
        private static final String AUTO_KEY = "auto";

        /**
         * Constructor.
         *
         * @param  tcModel  topcat model
         */
        LinkConfigurator( TopcatModel tcModel ) {
            super( new JPanel( new BorderLayout() ) );
            tcModel_ = tcModel;

            autoModel_ =
                new ToggleButtonModel( "Invocation", null,
                                       "Whether to invoke link automatically "
                                     + "on selection, or just display GUI "
                                     + "for manual invocation" );
            autoModel_.setSelected( false );
            JRadioButton[] autoButtons =
                 autoModel_.createRadioButtons( "Manual", "Auto" );
            JComponent autoLine = Box.createHorizontalBox();
            autoLine.add( new JLabel( "Invocation: " ) );
            autoLine.add( autoButtons[ 0 ] );
            autoLine.add( Box.createHorizontalStrut( 5 ) );
            autoLine.add( autoButtons[ 1 ] );

            linkPanel_ = new LinkRowPanel( UrlOptions.createOptions( null ) );
            linkPanel_.setPreferredSize( new Dimension( 550, 300 ) );
            String title = "TOPCAT(" + tcModel.getID() + "): "
                         + "Activation - Invoke Datalink Row";
            window_ = new JFrame( title );
            window_.getContentPane().add( linkPanel_ );
            window_.pack();

            getPanel().add( autoLine, BorderLayout.NORTH );
        }

        public Activator getActivator() {
            final boolean isAuto = autoModel_.isSelected();
            final StarTable table = tcModel_.getDataModel();
            if ( LinksDoc.isLinksResponse( table, 4 ) ) {
                return new Activator() {
                    public boolean invokeOnEdt() {
                         return !isAuto;
                    }
                    public Outcome activateRow( long lrow,
                                                ActivationMeta meta ) {
                         LinksDoc linksDoc = LinksDoc.createLinksDoc( table );
                         linkPanel_.setLinksDoc( linksDoc );
                         Object[] row;
                         try {
                             row = table.getRow( lrow );
                         }
                         catch ( IOException e ) {
                             return Outcome.failure( e );
                         }
                         linkPanel_.setRow( row );
                         if ( isAuto ) {
                             return linkPanel_.invokeRow();
                         }
                         else {
                             window_.setVisible( true );
                             return Outcome
                                   .success( linkPanel_.getRowSummary() );
                         }
                    }
                };
            }
            else {
                return null;
            }
        }

        public String getConfigMessage() {
            StarTable table = tcModel_.getViewModel().getSnapshot();
            if ( ! LinksDoc.isLinksResponse( table, 4 ) ) {
                return "Not a DataLink links-response document";
            }
            else {
                return null;
            }
        }

        public Safety getSafety() {
            // There's a very slight danger if the default action is
            // displaying a web page, but ignore it here.
            return Safety.SAFE;
        }

        public ConfigState getState() {
            ConfigState state = new ConfigState();
            state.saveFlag( AUTO_KEY, autoModel_ );
            return state;
        }

        public void setState( ConfigState state ) {
            state.restoreFlag( AUTO_KEY, autoModel_ );
        }
    }
}
