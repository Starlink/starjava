package uk.ac.starlink.topcat.activate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.gui.LocationTableLoadDialog;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.Safety;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Activation type for loading a table into the TOPCAT application.
 *
 * @author   Mark Taylor
 * @since    30 Jan 2018
 */
public class LoadTableActivationType implements ActivationType {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.activate" );

    public String getName() {
        return "Load Table";
    }

    public String getDescription() {
        return "Load the data in a file or URL column as a table"
             + " into TOPCAT";
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        return new TableColumnConfigurator( tinfo );
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return tinfo.getUrlSuitability();
    }

    /**
     * Configurator for loading a table into the TOPCAT application.
     */
    private static class TableColumnConfigurator extends UrlColumnConfigurator {
        private final ControlWindow controlWindow_;
        private final JComboBox formatSelector_;
        private final JCheckBox multipleSelector_;
        private final JCheckBox paramsSelector_;
        private final JCheckBox allowsysSelector_;
        private static final String FORMAT_KEY = "format";
        private static final String MULTIPLE_KEY = "multiple";
        private static final String IMPORTPARAMS_KEY = "importParams";
        private static final String ALLOWSYS_KEY = "allowSystem";

        /**
         * Constructor.
         *
         * @param  tinfo   topcat model information
         */
        TableColumnConfigurator( TopcatModelInfo tinfo ) {
            super( tinfo, "Table", new ColFlag[] { ColFlag.URL, } );
            controlWindow_ = ControlWindow.getInstance();
            JComponent queryPanel = getQueryPanel();
            ActionForwarder forwarder = getActionForwarder();
            LocationTableLoadDialog locTld = new LocationTableLoadDialog();
            locTld.configure( controlWindow_.getTableFactory(), null );
            formatSelector_ = locTld.createFormatSelector();
            formatSelector_.addActionListener( forwarder );
            multipleSelector_ = new JCheckBox();
            multipleSelector_.addActionListener( forwarder );
            paramsSelector_ = new JCheckBox();
            paramsSelector_.setSelected( true );
            paramsSelector_.addActionListener( forwarder );
            allowsysSelector_ = new JCheckBox();
            allowsysSelector_.setSelected( false );
            allowsysSelector_.addActionListener( forwarder );
            Box formatBox = Box.createHorizontalBox();
            formatBox.add( new JLabel( "Table Format: " ) );
            formatBox.add( new ShrinkWrapper( formatSelector_ ) );
            formatBox.add( Box.createHorizontalGlue() );
            Box multiBox = Box.createHorizontalBox();
            multiBox.add( new JLabel( "Multiple Tables" ) );
            multiBox.add( multipleSelector_ );
            multiBox.add( Box.createHorizontalGlue() );
            Box paramsBox = Box.createHorizontalBox();
            paramsBox.add( new JLabel( "Import Parameters" ) );
            paramsBox.add( paramsSelector_ );
            paramsBox.add( Box.createHorizontalGlue() );
            Box allowsysBox = Box.createHorizontalBox();
            allowsysBox.add( new JLabel( "Allow Preprocessing" ) );
            allowsysBox.add( allowsysSelector_ );
            allowsysBox.add( Box.createHorizontalGlue() );
            queryPanel.add( formatBox );
            queryPanel.add( Box.createVerticalStrut( 5 ) );
            queryPanel.add( multiBox );
            queryPanel.add( Box.createVerticalStrut( 5 ) );
            queryPanel.add( paramsBox );
            queryPanel.add( Box.createVerticalStrut( 5 ) );
            queryPanel.add( allowsysBox );
        }

        protected Activator createActivator( ColumnData cdata ) {
            final StarTableFactory tfact = controlWindow_.getTableFactory();
            final String format = (String) formatSelector_.getSelectedItem();
            final boolean isSelect = false;
            final boolean isMultiple = multipleSelector_.isSelected();
            final boolean importParams = paramsSelector_.isSelected();
            final boolean allowSystem = allowsysSelector_.isSelected();
            return new LocationColumnActivator( cdata, false ) {
                final TopcatModel parentTable = getTopcatModel();
                protected Outcome activateLocation( final String loc,
                                                    long lrow ) {
                    final List<StarTable> tables = new ArrayList<StarTable>();
                    try {
                        DataSource datsrc =
                            DataSource.makeDataSource( loc, allowSystem );
                        if ( isMultiple ) {
                            TableSequence tseq =
                                tfact.makeStarTables( datsrc, format );
                            StarTable t;
                            while ( ( t = tseq.nextTable() ) != null ) {
                                tables.add( t );
                            }
                        }
                        else {
                            tables.add( tfact.makeStarTable( datsrc, format ) );
                        }
                    }
                    catch ( IOException e ) {
                        return Outcome.failure( e );
                    }
                    if ( importParams ) {
                        List<DescribedValue> params =
                            TopcatUtils.getRowAsParameters( parentTable, lrow );
                        try {
                            for ( StarTable table : tables ) {
                                table.getParameters().addAll( params );
                            }
                        }
                        catch ( UnsupportedOperationException e ) {
                            logger_.log( Level.WARNING, "Can't add params", e );
                        }
                    }
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            int it = 0;
                            for ( StarTable table : tables ) {
                                String loc1 = tables.size() == 1
                                            ? loc
                                            : loc + "#" + ( it + 1 );
                                controlWindow_.addTable( table, loc1,
                                                         isSelect );
                                it++;
                            }
                        }
                    } );
                    final String quant;
                    int nt = tables.size();
                    switch ( nt ) {
                        case 0:
                            quant = "No table";
                            break;
                        case 1:
                            quant = "Table";
                            break;
                        default:
                            quant = Integer.toString( nt ) + " tables";
                    }
                    return Outcome.success( quant + " loaded from " + loc );
                }
            };
        }

        protected String getConfigMessage( ColumnData cdata ) {
            return null;
        }

        public Safety getSafety() {
            return allowsysSelector_.isSelected() ? Safety.UNSAFE : Safety.SAFE;
        }

        public ConfigState getState() {
            ConfigState state = getUrlState();
            state.saveSelection( FORMAT_KEY, formatSelector_ );
            state.saveFlag( MULTIPLE_KEY, multipleSelector_.getModel() );
            state.saveFlag( IMPORTPARAMS_KEY, paramsSelector_.getModel() );
            state.saveFlag( ALLOWSYS_KEY, allowsysSelector_.getModel() );
            return state;
        }

        public void setState( ConfigState state ) {
            setUrlState( state );
            state.restoreSelection( FORMAT_KEY, formatSelector_ );
            state.restoreFlag( MULTIPLE_KEY, multipleSelector_.getModel() );
            state.restoreFlag( IMPORTPARAMS_KEY, paramsSelector_.getModel() );
            state.restoreFlag( ALLOWSYS_KEY, allowsysSelector_.getModel() );
        }
    }
}
