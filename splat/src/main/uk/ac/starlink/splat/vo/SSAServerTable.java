package uk.ac.starlink.splat.vo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import uk.ac.starlink.splat.iface.AbstractServerPanel;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.TableLoadPanel;
import uk.ac.starlink.util.ProxySetup;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.gui.ProxySetupFrame;




/**
 * SSAServerTable is a panel displaying the SSA services as a table. It includes
 * also selection options, as waveband and data source options, 
 * as well as user generated tags. By rightclicking on a row, 
 * a menu will appear to allos tagging the service or displaying its information.
 *
 * @author Margarida Castro Neves 
 * @version $Id: SSAServerTree.java 10350 2012-11-15 13:27:36Z mcneves $
 *
 */


public class SSAServerTable extends AbstractServerPanel  {
    
    // Logger.
    private static Logger logger =
            Logger.getLogger( "uk.ac.starlink.splat.vo.SSAServerTable" );

    
   // sizes
    
    private static int WIDTH = 340;
    private static int HEIGHT = 900;
    
    // Service type string
    private static String serviceType="SSAP";
    
    /* where to save the tags information */
    private static String tagsFile = "defaultTagsV2.xml";
    
    // options
    private JCheckBox band_rad ;
    private JCheckBox band_mm ;
    private JCheckBox band_ir;
    private JCheckBox band_opt;
    private JCheckBox band_uv ;
    private JCheckBox band_euv;
    private JCheckBox band_xr;
    private JCheckBox band_gr;
    private JCheckBox band_all;
    private JRadioButton src_theo = null;
    private JRadioButton src_obs = null;
    
    
    // user defined tags
   // @SuppressWarnings("rawtypes")
   // private JComboBox tagCombo;
    
    
    private CheckBoxListener checkBoxlistener = null;
   
    
    /** The cell table renderer */
   // static ServerTableRenderer renderer ;
    
    /** The proxy server dialog */
    protected ProxySetupFrame proxyWindow = null;

   // private static ServerPopupTable serverTable;


//    private TableRowSorter<DefaultTableModel> sorter;
    private RowFilter<DefaultTableModel, Object> obsFilter;
    private RowFilter<DefaultTableModel, Object> theoFilter;
    private RowFilter<DefaultTableModel, Object> band_GammaFilter;
    private RowFilter<DefaultTableModel, Object> band_EUVFilter;
    private RowFilter<DefaultTableModel, Object> band_XRayFilter;
    private RowFilter<DefaultTableModel, Object> band_UVFilter;
    private RowFilter<DefaultTableModel, Object> band_OpticalFilter;
    private RowFilter<DefaultTableModel, Object> band_IRFilter;
    private RowFilter<DefaultTableModel, Object> band_MmFilter;
    private RowFilter<DefaultTableModel, Object> band_RadioFilter;
      

    /** Make sure the proxy environment is setup */
    static {
        ProxySetup.getInstance().restore();
    }

    /**
     * Create an instance.
     */
    public SSAServerTable( SSAServerList list )
    {
        super(list);

      //  sortTable();
    
        initUI (initOptionsPanel(), initServerPanel());

        initFilters();
        setFilters();
        
    }  
    
  
    // initially sort the services in alphabetical order
/*    private void sortTable() {
       sorter = (TableRowSorter<DefaultTableModel>) serverTable.getRowSorter();
        List<RowSorter.SortKey> sortKeys = new ArrayList<SortKey>();
        sortKeys.add(new RowSorter.SortKey(ServerPopupTable.SHORTNAME_INDEX, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
        sorter.sort();
        
    }
*/

    private JScrollPane initOptionsPanel() {
        /************
         * The Options panel
         ************
         */
      //  optionTabs = new JTabbedPane();
       
        JScrollPane optionsScroller = new JScrollPane();
        
        JPanel optionsPanel = new JPanel(new GridBagLayout());
        optionsPanel.setBorder(BorderFactory.createTitledBorder( "Service selection options" ));
        GridBagConstraints gbcOptions = new GridBagConstraints();
     
        checkBoxlistener = new CheckBoxListener();
        
        // Options Component: BAND 
     
        JPanel bandPanel = new JPanel (new GridLayout(3,3));
        bandPanel.setBorder ( BorderFactory.createTitledBorder( "Wave Band" ) );
        band_rad = new JCheckBox( "Radio", false);
        bandPanel.add(band_rad);
        band_mm = new JCheckBox( "Millimeter",  false);
        bandPanel.add(band_mm);
        band_ir = new JCheckBox( "Infrared",  false);
        bandPanel.add(band_ir);
        band_opt = new JCheckBox( "Optical",false);
        bandPanel.add(band_opt);
        band_uv = new JCheckBox( "UV",  false);
        bandPanel.add(band_uv);
        band_euv = new JCheckBox( "EUV",  false);
        bandPanel.add(band_euv);
        band_xr = new JCheckBox( "X-ray",  false);
        bandPanel.add(band_xr);
        band_gr = new JCheckBox( "Gamma-ray", false);
        bandPanel.add(band_gr);      
        band_all = new JCheckBox( "ALL", true);
        bandPanel.add(band_all);
        
        band_rad.addItemListener(checkBoxlistener);
        band_rad.setToolTipText("<html>any wavelength > 10 mm (or frequency < 30 GHz)</html>");
        band_mm.addItemListener(checkBoxlistener);
        band_mm.setToolTipText("<html>0.1 mm <= wavelength <= 10 mm; <BR>3000 GHz >= frequency >= 30 GHz.</html>");
        band_ir.addItemListener(checkBoxlistener);
        band_ir.setToolTipText("<html>1 micron <= wavelength <= 100 microns</html>");
        band_opt.addItemListener(checkBoxlistener);
        band_opt.setToolTipText("<html>0.3 microns <= wavelength <= 1 micron; <BR>300 nm <= wavelength <= 1000 nm; <BR>3000 Angstroms <= wavelength <= 10000 Angstroms</html>");
        band_uv.addItemListener(checkBoxlistener);
        band_uv.setToolTipText("<html>0.1 micron <= wavelength <= 0.3 microns; <BR> 100 nm <= wavelength <= 300 nm;  <BR>1000 Angstroms <= wavelength <= 3000 Angstroms</html>");
        band_euv.addItemListener(checkBoxlistener);    
        band_euv.setToolTipText("<html>100 Angstroms <= wavelength <= 1000 Angstroms; <BR>12 eV <= energy <= 120 eV</html>");
        band_xr.addItemListener(checkBoxlistener);
        band_xr.setToolTipText("<html>0.1 Angstroms <= wavelength <= 100 Angstroms; <BR>0.12 keV <= energy <= 120 keV</html>");
        band_gr.addItemListener(checkBoxlistener);
        band_gr.setToolTipText("<html>energy >= 120 keV</html>");
        band_all.addItemListener(checkBoxlistener);
       
        band_rad.setName("band");
        band_mm.setName("band");
        band_opt.setName("band");
        band_ir.setName("band");
        band_uv.setName("band");
        band_euv.setName("band");
        band_xr.setName("band");
        band_gr.setName("band");
        band_all.setName("band_all");
       
        // Options component: Content (theoretical/observed)
        
        JPanel srcPanel = new JPanel (new GridLayout(1, 2));
        srcPanel.setBorder ( BorderFactory.createTitledBorder( "Data Source" ) );
        
        ButtonGroup bg = new ButtonGroup();
        
        src_obs = new JRadioButton("Observed data", true);
        src_obs.setToolTipText("<html>All observation servers.</html>");
       
        bg.add(src_obs);       
        src_obs.setName("src_obs");
        src_obs.addItemListener(checkBoxlistener);
        
        src_theo = new JRadioButton("Theoretical data", false);
        src_theo.setToolTipText("<html>All theoretical servers.</html>");
     
        bg.add(src_theo);
        src_theo.setName("src_theo");
        src_theo.addItemListener(checkBoxlistener);
        
        srcPanel.add(src_obs);
        srcPanel.add(src_theo);
  
        // Options Component: User Defined Tags
        JPanel tagPanel= makeTagPanel();
 
        // Adding all to the Options panel
            
        gbcOptions.anchor = GridBagConstraints.NORTHWEST;
        gbcOptions.fill = GridBagConstraints.HORIZONTAL;
        gbcOptions.weightx=.5;
        gbcOptions.weighty=0;
        gbcOptions.gridx=0;
        gbcOptions.gridy=0;
      
        optionsPanel.add(srcPanel,gbcOptions);
        gbcOptions.weighty=.5;
        gbcOptions.gridy=1;
        optionsPanel.add(bandPanel,gbcOptions);
        
        gbcOptions.gridy=2;
        gbcOptions.weighty=1;
        optionsPanel.add(tagPanel, gbcOptions);
        
        JPanel invOptionsPanel = new JPanel(); // invisible, just to adjust size and position of frames

        invOptionsPanel.setLayout(new BorderLayout());
        
        invOptionsPanel.add(optionsPanel, BorderLayout.LINE_START);
       
       invOptionsPanel.getAccessibleContext().setAccessibleName("Selection by data source, wave band");
       invOptionsPanel.getAccessibleContext().setAccessibleDescription("Detailed");
       
       optionsScroller.getViewport().add( invOptionsPanel, null );
       optionsScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
       optionsScroller.setMinimumSize(new Dimension(220,240));
        
       return optionsScroller;
    }
 
    /**
     * Set the SSAServerList.
     *
     * @param serverList the SSAServerList reference.
     */
    public void setSSAServerList( SSAServerList serverList )
    {
        setServerList( serverList );
    }
    
    /**
     * Inner class defining action for setting the proxy server.
     */
    @SuppressWarnings("serial")
    protected class ProxyAction
        extends AbstractAction
    {
        public ProxyAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
           showProxyDialog();
        }
    }


 
    
    /**
     * Query a registry for any new SSAP servers. New servers must have a
     * different short name.
     */
    protected StarTable makeRegistryQuery()
    {
                
        StarTable table = null;
        try {
                 
            table = TableLoadPanel.loadTable( this, new SSARegistryQueryDialog(SplatRegistryQuery.SSAP), new StarTableFactory() );
         }
        catch ( IOException e ) {
            ErrorDialog.showError( this, "Registry query failed", e );
            return null;
        }
        
        return table;
   
            
    } // makeRegistryQuery

  
 
    private void initFilters() {
        
         theoFilter = new RowFilter<DefaultTableModel, Object>() {
            
            public boolean include( RowFilter.Entry<? extends DefaultTableModel, ? extends Object> entry) {
                return (entry.getStringValue(ServerPopupTable.CONTTYPE_INDEX).toLowerCase().contains("simulation") || 
                        entry.getStringValue(ServerPopupTable.DATASOURCE_INDEX).toLowerCase().contains("theory") );
            }    
          };
          obsFilter = RowFilter.notFilter(theoFilter);    
          
          band_RadioFilter = new RowFilter<DefaultTableModel, Object>() {
              public boolean include( RowFilter.Entry<? extends DefaultTableModel, ? extends Object> entry) {
                  return ( entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("radio") );
              }  
          };
          band_MmFilter = new RowFilter<DefaultTableModel, Object>() {
              public boolean include( RowFilter.Entry<? extends DefaultTableModel, ? extends Object> entry) {
                  return ( entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("millimeter") ||  entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).equalsIgnoreCase("mm"));
              }  
          };
          band_IRFilter = new RowFilter<DefaultTableModel, Object>() {
              public boolean include( RowFilter.Entry<? extends DefaultTableModel, ? extends Object> entry) {
                  return ( entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("infrared") || entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("ir") ||
                           entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("infra-red"));
              }  
          };
          band_OpticalFilter = new RowFilter<DefaultTableModel, Object>() {
              public boolean include( RowFilter.Entry<? extends DefaultTableModel, ? extends Object> entry) {
                  return ( entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("optical") );
              }  
          };
          band_UVFilter = new RowFilter<DefaultTableModel, Object>() {
              public boolean include( RowFilter.Entry<? extends DefaultTableModel, ? extends Object> entry) {
                  return ( entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("uv") &&  !entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("euv"));
              }  
          };
          band_EUVFilter = new RowFilter<DefaultTableModel, Object>() {
              public boolean include( RowFilter.Entry<? extends DefaultTableModel, ? extends Object> entry) {
                  return ( entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("euv") );
              }  
          };
          band_XRayFilter = new RowFilter<DefaultTableModel, Object>() {
              public boolean include( RowFilter.Entry<? extends DefaultTableModel, ? extends Object> entry) {
                  return ( entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("x-ray") || entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("xray") );
              }  
          };
          band_GammaFilter = new RowFilter<DefaultTableModel, Object>() {
              public boolean include( RowFilter.Entry<? extends DefaultTableModel, ? extends Object> entry) {
                  return ( entry.getStringValue(ServerPopupTable.WAVEBAND_INDEX).toLowerCase().contains("gamma") );
              }  
          };
    }
    
    private void setFilters() {
      
        
        RowFilter<DefaultTableModel,Object> sourceFilter = src_obs.isSelected()?obsFilter:theoFilter;
       
        TableRowSorter<DefaultTableModel> sorter = getTableRowSorter();
        
        if ( band_all.isSelected()) {
            sorter.setRowFilter(sourceFilter);
            
        } 
        else {   
            
            List<RowFilter<DefaultTableModel,Object>> bandfilters = new ArrayList<RowFilter<DefaultTableModel,Object>>();   
            List<RowFilter<DefaultTableModel,Object>> filters = new ArrayList<RowFilter<DefaultTableModel,Object>>(); 
            
            String selected = "";
            
            filters.add(sourceFilter);
            
            if ( band_rad.isSelected()) {
                selected+=" radio ";
              //  composedFilter = RowFilter.andFilter(sourceFilter, band_RadioFilter);
                bandfilters.add(band_RadioFilter);
            }
            if ( band_mm.isSelected()) {
   //             composedFilter = RowFilter.andFilter(composedFilter, band_MmFilter);
                bandfilters.add(band_MmFilter);
                selected+=" millimeter ";
            }
            if ( band_ir.isSelected()) {
                bandfilters.add(band_IRFilter);
                selected+=" IR ";
            }
            if ( band_opt.isSelected()) {
                bandfilters.add(band_OpticalFilter);
            selected+=" optical ";
            }
            if ( band_uv.isSelected()) {
                bandfilters.add(band_UVFilter);
            selected+=" UV ";
            }
            if ( band_euv.isSelected()) {
                bandfilters.add(band_EUVFilter);
            selected+=" EUV ";
            }
            if ( band_xr.isSelected()) {
                bandfilters.add(band_XRayFilter);
            selected+=" xray ";
            }
            if ( band_gr.isSelected()) {
                bandfilters.add(band_GammaFilter);
            selected+=" gamma ";
            }
            
            logger.info("Selected: "+selected);
            if (filters.size() >0) {  
              RowFilter<DefaultTableModel,Object> orFilter =  RowFilter.orFilter(bandfilters);
              //filters.add(RowFilter.orFilter(bandfilters));
              filters.add(orFilter);
              sorter.setRowFilter(RowFilter.andFilter(filters));
            }
            
        } 
       setTableRowSorter(sorter);
            
    }

    /**
     * Set the proxy server and port.
     */
    protected void showProxyDialog()
    {
        if ( proxyWindow == null ) {
            ProxySetupFrame.restore( null );
            proxyWindow = new ProxySetupFrame();
        }
        proxyWindow.setVisible(true);
    }
   
  
    //Listens to the check boxes events
    class CheckBoxListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {

            JComponent source = (JComponent) e.getItemSelectable();
            String name = source.getName();


            if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED ) {  
          //      setFilters();
          //      serverTable.updateUI();

                if (name.equals("band_all")) {
                    if ( e.getStateChange() == ItemEvent.SELECTED) {                    
                        band_rad.setSelected(false);
                        band_mm.setSelected(false);
                        band_opt.setSelected(false);
                        band_ir.setSelected(false);
                        band_uv.setSelected(false);
                        band_euv.setSelected(false);
                        band_xr.setSelected(false);
                        band_gr.setSelected(false);
                        band_all.setSelected(true);
                    }  
                  
                } else if (name.startsWith("band") ) {// and not band_all 
                   
                    if ( e.getStateChange() == ItemEvent.DESELECTED) {  
                        // if all bands are deselected, then select "all"
                        if (  !band_rad.isSelected() && !band_mm.isSelected() && 
                              !band_opt.isSelected() && !band_ir.isSelected() && 
                              !band_uv.isSelected() && !band_euv.isSelected() && 
                              !band_xr.isSelected() && !band_gr.isSelected() )
                            band_all.setSelected(true);
                    } else {
                        band_all.setSelected(false);
                    }
                }
                
            } // if selected/deselected
            
            setFilters();
          //  serverTable.updateUI();
            // if view is changed, tags have to be re-selected.
        //    TagsListSelectionListener listener = (TagsListSelectionListener) tagsList.getListSelectionListeners()[0];
       //     listener.selectTaggedServers((String) tagsList.getSelectedValue());
            selectTaggedServers(getSelectedTag());

        } // itemStateChanged()  
    } // class CheckBoxListener


  @Override
  public String getServiceType() {
     
      return serviceType;
  }
  
  @Override
  public String getTagsFilename() {
      
      return tagsFile;
  }
  
@Override
public int getWidth() {
   
    return WIDTH;
}

@Override
public int getHeight() {
    return HEIGHT;
}



 
    
}
