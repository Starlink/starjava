package uk.ac.starlink.topcat.activate;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.ResourceInfo;
import uk.ac.starlink.topcat.ResourceType;
import uk.ac.starlink.topcat.Safety;
import uk.ac.starlink.topcat.ServiceParamPanel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.UrlInvoker;
import uk.ac.starlink.topcat.UrlOptions;
import uk.ac.starlink.util.gui.RenderingComboBox;
import uk.ac.starlink.vo.datalink.LinksDoc;
import uk.ac.starlink.vo.datalink.ServiceInvoker;
import uk.ac.starlink.votable.datalink.ServiceDescriptor;
import uk.ac.starlink.votable.datalink.ServiceParam;

/**
 * Activation type for invoking a ServiceDescriptor-described service.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2018
 */
public class ServiceActivationType implements ActivationType {

    public String getName() {
        return "Invoke Service";
    }

    public String getDescription() {
        return "Invoke a service defined by a ServiceDescriptor"
             + " attached to the table";
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        return new ServiceConfigurator( tinfo.getTopcatModel() );
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        StarTable table = tinfo.getTopcatModel().getDataModel();
        if ( LinksDoc.getServiceDescriptors( table ).length > 0 ) {
            return LinksDoc.isLinksResponse( table, 2 )
                 ? Suitability.AVAILABLE
                 : Suitability.SUGGESTED;
        }
        else {
            return Suitability.DISABLED;
        }
    }

    /**
     * ActivatorConfigurator implementation for ServiceActivationType.
     */
    private static class ServiceConfigurator
                         extends AbstractActivatorConfigurator {
        private final TopcatModel tcModel_;
        private final InvokePanel invokePanel_;
        private final ServicePanel servicePanel_;
        private final JComponent paramContainer_;
        private ServiceParamPanel paramPanel_;

        private static final String SERVICE_KEY = "service";
        private static final String ACTION_KEY = "action";

        /**
         * Constructor.
         *
         * @param  tcModel  topcat model
         */
        ServiceConfigurator( TopcatModel tcModel ) {
            super( new JPanel( new BorderLayout() ) );
            tcModel_ = tcModel;
            String ctxtTitle =
                "TOPCAT(" + tcModel.getID() + "): Invoke Service";
            invokePanel_ =
                new InvokePanel( UrlOptions.createOptions( null, ctxtTitle ) );
            ServiceDescriptor[] sds =
                LinksDoc.getServiceDescriptors( tcModel.getDataModel() );
            servicePanel_ = new ServicePanel( sds );
            final ActionForwarder forwarder = getActionForwarder();
            invokePanel_.invokeSelector_.addActionListener( forwarder );
            servicePanel_.serviceSelector_
                         .addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    updateService();
                    forwarder.actionPerformed( evt );
                }
            } );
            paramContainer_ = new JPanel( new BorderLayout() );
            updateService();
            JComponent box = Box.createVerticalBox();
            box.add( invokePanel_ );
            box.add( Box.createVerticalStrut( 5 ) );
            box.add( servicePanel_ );
            box.add( Box.createVerticalStrut( 5 ) );
            box.add( paramContainer_ );
            getPanel().add( box, BorderLayout.NORTH );
        }

        public Activator getActivator() {
            final ServiceDescriptor sd = servicePanel_.getServiceDescriptor();
            final UrlInvoker urler = invokePanel_.getUrlInvoker();
            if ( sd == null || urler == null || paramPanel_ == null ) {
                return null;
            }
            else {
                final Map<ServiceParam,String> paramMap =
                    paramPanel_.getValueMap();
                final StarTable table = tcModel_.getDataModel();
                return new Activator() {
                    public boolean invokeOnEdt() {
                        return false;
                    }
                    public Outcome activateRow( long lrow,
                                                ActivationMeta meta ) {
                        ServiceInvoker si;
                        Object[] row;
                        try {
                            si = new ServiceInvoker( sd, table );
                            row = table.getRow( lrow );
                        }
                        catch ( IOException e ) {
                            return Outcome.failure( e );
                        }
                        URL url = si.getUrl( row, paramMap );
                        Outcome outcome = urler.invokeUrl( url );
                        String urlTxt = url == null ? null : url.toString();
                        return UrlColumnConfigurator
                              .decorateOutcomeWithUrl( outcome, urlTxt );
                    }
                };
            }
        }

        public String getConfigMessage() {
            if ( servicePanel_.getServiceDescriptor() == null ) {
                return "No service descriptors";
            }
            else if ( invokePanel_.getUrlInvoker() == null ) {
                return "No invocation method";
            }
            else if ( paramPanel_ == null ) {
                return "No parameter panel??";
            }
            else {
                return null;
            }
        }

        public Safety getSafety() {
            return invokePanel_.getUrlInvoker().getSafety();
        }

        public ConfigState getState() {
            ConfigState state = new ConfigState();
            state.saveSelection( SERVICE_KEY, servicePanel_.serviceSelector_ );
            state.saveSelection( ACTION_KEY, invokePanel_.invokeSelector_ );
            return state;
        }

        public void setState( ConfigState state ) {
            state.restoreSelection( SERVICE_KEY,
                                    servicePanel_.serviceSelector_ );
            state.restoreSelection( ACTION_KEY, invokePanel_.invokeSelector_ );
        }

        /**
         * Called if the service selection may have changed to
         * make sure the display of service parameters etc is up to date.
         */
        private void updateService() {
            ResourceType rtype = servicePanel_.guessResourceType();
            invokePanel_.setResourceType( rtype );
            ActionForwarder forwarder = getActionForwarder();
            if ( paramPanel_ != null ) {
                paramPanel_.removeActionListener( forwarder );
            }
            paramContainer_.removeAll();
            ServiceDescriptor sd = servicePanel_.getServiceDescriptor();
            paramPanel_ = new ServiceParamPanel( getNonRowParams( sd ) );
            ServiceInvoker si = getServiceInvoker( sd );
            if ( si != null ) {
                paramPanel_.setValueMap( si.getFixedParamMap() );
            }
            paramContainer_.add( paramPanel_, BorderLayout.NORTH );
            paramPanel_.addActionListener( forwarder );
            paramContainer_.revalidate();
            paramContainer_.repaint();
        }

        /**
         * Returns the service parameters for a given service descriptor,
         * excluding any that are set from the rows of the table.
         * This is the list of parameters that users are usually expected
         * to be able to enter.
         *
         * @param  sd  service descriptor
         * @return   user-editable parameters
         */
        private ServiceParam[] getNonRowParams( ServiceDescriptor sd ) {
            if ( sd == null ) {
                return new ServiceParam[ 0 ];
            }
            ServiceParam[] allParams = sd.getInputParams();
            ServiceInvoker si = getServiceInvoker( sd );
            if ( si == null ) {
                return allParams;
            }
            else {
                List<ServiceParam> params = new ArrayList<ServiceParam>();
                params.addAll( Arrays.asList( allParams ) );
                params.removeAll( Arrays.asList( si.getRowParams() ) );
                return params.toArray( new ServiceParam[ 0 ] );
            }
        }

        /**
         * Attempts to convert a ServiceDescriptor into a ServiceInvoker.
         *
         * @param  sd  service descriptor
         * @return  corresponding service invoker, or null if there's a problem
         */
        public ServiceInvoker getServiceInvoker( ServiceDescriptor sd ) {
            if ( sd != null ) {
                StarTable table = tcModel_.getViewModel().getSnapshot();
                try {
                    return new ServiceInvoker( sd, table );
                }
                catch ( IOException e ) {
                }
            }
            return null;
        }
    }

    /**
     * Panel for selecting a URL Invoker.
     */
    private static class InvokePanel extends LabelledComponentStack {
        private final UrlOptions urlopts_;
        final JComboBox<UrlInvoker> invokeSelector_;

        /**
         * Constructor.
         *
         * @param  invokeMap   structure containing available url invokers
         */
        InvokePanel( UrlOptions urlopts ) {
            urlopts_ = urlopts;
            UrlInvoker[] invokers = urlopts.getInvokers();
            invokeSelector_ = new JComboBox<>( invokers );
            addLine( "Action", invokeSelector_ );
        }

        /**
         * Returns the currently selected invoker.
         *
         * @return  invoker
         */
        public UrlInvoker getUrlInvoker() {
            return invokeSelector_.getItemAt( invokeSelector_
                                             .getSelectedIndex() );
        }

        /**
         * Accepts a probably resource type that URLs will contain.
         * May update the GUI accordingly.
         *
         * @param  rtype  probable resource type
         */
        public void setResourceType( ResourceType rtype ) {
            UrlInvoker urler = urlopts_.getDefaultsMap().get( rtype );
            if ( urler != null ) {
                invokeSelector_.setSelectedItem( urler );
            }
        }
    }

    /**
     * Panel for selecting, and displaying details of, a service.
     */
    private static class ServicePanel extends JPanel {
        final JComboBox<ServiceDescriptor> serviceSelector_;
        final JTextField urlField_;
        final JTextField nameField_;
        final JTextField descripField_;
        final JTextField ivoidField_;
        final JTextField stdidField_;
        final JTextField descidField_;

        /**
         * Constructor.
         *
         * @param  sds   available service descriptors
         */
        ServicePanel( ServiceDescriptor[] sds ) {
            super( new BorderLayout() );
            serviceSelector_ = new RenderingComboBox<ServiceDescriptor>( sds ) {
                @Override
                protected String getRendererText( ServiceDescriptor sd ) {
                    return getServiceLabel( sd );
                }
            };
            if ( sds.length > 0 ) {
                serviceSelector_.setSelectedItem( 0 );
            }
            serviceSelector_.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    updateState();
                }
            } );
            nameField_ = AbstractActivatorConfigurator.createDisplayField();
            descripField_ = AbstractActivatorConfigurator.createDisplayField();
            urlField_ = AbstractActivatorConfigurator.createDisplayField();
            ivoidField_ = AbstractActivatorConfigurator.createDisplayField();
            stdidField_ = AbstractActivatorConfigurator.createDisplayField();
            descidField_ = AbstractActivatorConfigurator.createDisplayField();
            LabelledComponentStack stack = new LabelledComponentStack();
            final JComponent serviceComponent;
            if ( sds.length == 0 ) {
                serviceComponent = new JLabel( "(none available)" );
            }
            else if ( sds.length == 1 ) {
                serviceComponent = new JLabel( getServiceLabel( sds[ 0 ] ) );
            }
            else {
                serviceComponent = serviceSelector_;
            }
            if ( sds.length != 1 ) {
                stack.addLine( "Service", serviceComponent );
            }
            stack.addLine( "Name", nameField_ );
            stack.addLine( "Description", descripField_ );
            stack.addLine( "Base URL", urlField_ );
            stack.addLine( "IVOID", ivoidField_ );
            stack.addLine( "Standard ID", stdidField_ );
            stack.addLine( "Descriptor ID", descidField_ );
            add( stack, BorderLayout.NORTH );
            updateState();
        }

        /**
         * Returns the currently selected service descriptor.
         */
        public ServiceDescriptor getServiceDescriptor() {
            return (ServiceDescriptor) serviceSelector_.getSelectedItem();
        }

        /**
         * Provides a best guess at the resource type that will be returned
         * by invoking the currently selected service.
         *
         * @return  best guess resource type
         */
        public ResourceType guessResourceType() {
            URL url = null;
            ServiceDescriptor sd = getServiceDescriptor();
            String ctype = sd == null ? null : sd.getContentType();
            String standardId = sd == null ? null : sd.getStandardId();
            return ResourceType.guessResourceType( new ResourceInfo() {
                public URL getUrl() {
                    return url;
                }
                public String getContentType() {
                    return ctype;
                }
                public String getContentQualifier() {
                    return null;
                }
                public String getStandardId() {
                    return standardId;
                }
            } );
        }

        /**
         * Called when the service selection may have changed to ensure
         * that the GUI state matches it.
         */
        private void updateState() {
            ServiceDescriptor sd = getServiceDescriptor();
            nameField_.setText( sd == null ? null : sd.getName() );
            descripField_.setText( sd == null ? null : sd.getDescription() );
            urlField_.setText( sd == null ? null : sd.getAccessUrl() );
            stdidField_.setText( sd == null ? null : sd.getStandardId() );
            descidField_.setText( sd == null ? null : sd.getDescriptorId() );
            ivoidField_.setText( sd == null ? null
                                            : sd.getResourceIdentifier() );
        }

        /**
         * Stringifies a ServiceDescriptor.
         *
         * @param  sd  service descriptor
         * @return  user-readable label
         */
        private static String getServiceLabel( ServiceDescriptor sd ) {
            if ( sd == null ) {
                return null;
            }
            String name = sd.getName();
            if ( name != null ) {
                return name;
            }
            String descrip = sd.getDescription();
            if ( descrip != null ) {
                descrip = descrip.trim();
                return descrip.length() < 24
                     ? descrip
                     : descrip.substring( 0, 21 ) + "...";
            }
            String id = sd.getDescriptorId();
            if ( id != null ) {
                return id;
            }
            return "(unnamed)";
        }
    }
}
