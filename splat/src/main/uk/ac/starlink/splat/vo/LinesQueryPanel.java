package uk.ac.starlink.splat.vo;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.starlink.splat.iface.AbstractServerPanel;
import uk.ac.starlink.splat.iface.SpectralLinesPanel;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.vamdc.VAMDCLib;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.TableLoadPanel;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.gui.ErrorDialog;

public class LinesQueryPanel extends AbstractServerPanel implements ActionListener {
    
  
    private static int WIDTH = 400;
    private static int HEIGHT = 700;
    private static int optionsHeight = 400;
    
    private int SLAP_INDEX=0;
    private int VAMDC_INDEX=1;
    private int LINETAP_INDEX=2;

    private static String tagsFile = "linesTagsV2.xml";
    private ServerPopupTable slapServices=null;
    private ServerPopupTable vamdcServices=null;
    private LinetapPopupTable linetapServices=null;
    private JTabbedPane servTabPanel;
    private SpectralLinesPanel slPanel = null;
    private LineBrowser browser;
    private JPopupMenu speciesPopup;
    private String chosenSpecies = "";
   
    
    public LinesQueryPanel( LineBrowser browser )  {
        super(); 
        this.browser=browser;
        initUI( initOptionsPanel(), initServersPanel() );
        setVisible(true);
       // setSize(WIDTH,HEIGHT);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
     }
    
    private JScrollPane initOptionsPanel() {
        
        JPanel queryPanel = new JPanel(new GridBagLayout());
        queryPanel.setBorder(BorderFactory.createEtchedBorder() );
        GridBagConstraints gbcOptions = new GridBagConstraints();
        gbcOptions.anchor = GridBagConstraints.WEST;
        gbcOptions.fill = GridBagConstraints.NONE;
        gbcOptions.weightx=0.5;
        gbcOptions.weighty=0;
        gbcOptions.gridx=0;
        gbcOptions.gridy=0;
        queryPanel.add(browser.getPlotChoicePanel());
        
        gbcOptions.gridy=1;
        slPanel = new SpectralLinesPanel(browser, WIDTH-10);
      //  slPanel.setPreferredSize(new Dimension(slPanel.getWidth(), optionsHeight-10 ));
        
        queryPanel.add(slPanel,gbcOptions);
        gbcOptions.weighty=0;
        gbcOptions.weightx=0;
        gbcOptions.gridy=2;
        queryPanel.add(makeTagPanel(), gbcOptions);
        

        JScrollPane optionsScroller = new JScrollPane();
        optionsScroller.getViewport().add( queryPanel, null); //invOptionsPanel, null );
        optionsScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
       optionsScroller.setPreferredSize(new Dimension(WIDTH,optionsHeight));
       // optionsScroller.setMinimumSize(new Dimension(WIDTH-20,optionsHeight-20));
        optionsScroller.setMinimumSize(new Dimension(WIDTH-20,optionsHeight-20));
       // optionsScroller.setMaximumSize(new Dimension(WIDTH+20,optionsHeight+20));
        return optionsScroller;
    }
    
    private JTabbedPane initServersPanel() {
   
         servTabPanel = new JTabbedPane();
         servTabPanel.setSize(new Dimension(WIDTH, HEIGHT-optionsHeight));
         servTabPanel.setMaximumSize(new Dimension(WIDTH, HEIGHT-optionsHeight));
         servTabPanel.add( makeSlapPanel(), SLAP_INDEX);
         servTabPanel.add( makeVamdcPanel(), VAMDC_INDEX);
         servTabPanel.add( makeLinetapPanel(), LINETAP_INDEX);
         
         servTabPanel.setTitleAt(LINETAP_INDEX, "LINETAP");  
         servTabPanel.setTitleAt(SLAP_INDEX, "SLAP");
         servTabPanel.setTitleAt(VAMDC_INDEX, "VAMDC");     
          
         servTabPanel.setSelectedIndex(LINETAP_INDEX);

         setServerTable(slapServices);
         servTabPanel.addChangeListener(new ChangeListener() {
             public void stateChanged(ChangeEvent e) {
                 if (isSLAPSelected()) {
                     setServerTable(slapServices);
                     slPanel.deactivateCharge(); // does not work for SLAP (yet)
                 } else  if (isLinetapSelected()){
                	 
                	 setServerTable(linetapServices);
                	 slPanel.reloadUI(true);
                     slPanel.activateCharge();                     
                 } else {
                	 setServerTable(vamdcServices);                   
                     slPanel.activateCharge();
                //     slPanel.reloadUI(false);
                 }
             }
                 
         });
         return servTabPanel;
    }

   private JPanel makeSlapPanel() {
        
        try {
            slapServices = new ServerPopupTable(new SLAPServerList());
     //       slapServices.setComponentPopupMenu(makeServerPopup());  
        } catch (SplatException e) {
           slapServices=getSLAPServices();
        }

        return initServerPanel(slapServices);
 
    }
   private ServerPopupTable getSLAPServices() {
       return new ServerPopupTable(new SLAPServerList(querySLAPRegistry()));
   }
   
   private StarTable querySLAPRegistry() {
       
       StarTable table = null;
       try {           
    
           table =  TableLoadPanel.loadTable( this, new SSARegistryQueryDialog(SplatRegistryQuery.SLAP), new StarTableFactory() );
        }
       catch ( IOException e ) {
           ErrorDialog.showError( this, "Registry query failed", e );
           //return null;
       }
       return table;
   }
   
   private JPanel makeLinetapPanel() {
       
       try {
           linetapServices = new LinetapPopupTable(new LinetapServerList());
      } catch (SplatException e) {
          linetapServices=getLinetapServices();
      }

       return initServerPanel(linetapServices);

   }
 
  private LinetapPopupTable getLinetapServices() {
      return new LinetapPopupTable(new LinetapServerList(queryLinetapRegistry()));
  }
  
  private StarTable queryLinetapRegistry() {
      
      StarTable table = null;
     // SSARegistryQueryDialog dialog = new SSARegistryQueryDialog(LINETAP_INDEX);
      try {                
          table =  TableLoadPanel.loadTable( this, new SSARegistryQueryDialog(SplatRegistryQuery.LINETAP), new StarTableFactory() );
      }
      catch ( IOException e ) {
          ErrorDialog.showError( this, "Registry query failed", e );
          return null;
      }
      return table;
  }
  
   
   
   private JPanel makeVamdcPanel() {
       
       try {
           vamdcServices = new ServerPopupTable(new VAMDCServerList());
           setManuallyAddPossible(false);
    //       slapServices.setComponentPopupMenu(makeServerPopup());  
       } catch (SplatException e) {
           vamdcServices = getVAMDCServices();
       }
       
       JPanel vamdcPanel = initServerPanel(vamdcServices);
       addServerButton.setEnabled(false); //addServerButton.setVisible(false);
    
       return vamdcPanel;
   }
   
   private ServerPopupTable getVAMDCServices() {
           
        StarTable vamdctab =  VAMDCLib.queryRegistry(); 
        return new ServerPopupTable(new VAMDCServerList(vamdctab));
      // vamdcServices.setComponentPopupMenu(makeServerPopup());       
   }
    @Override
    public int getWidth() {
      
        return WIDTH;
    }

    @Override
    public int getHeight() {
       
        return HEIGHT;
    }

    @Override
    public String getServiceType() {
        // TODO Auto-generated method stub
        return "Spectral Lines";
    }

    @Override
    public String getTagsFilename() {
        return tagsFile;
    }
    


    @Override
    protected StarTable makeRegistryQuery() {
        if (isSLAPSelected()) {
            return querySLAPRegistry();
        } else if (isLinetapSelected()){
            return queryLinetapRegistry();
        } else {
        	return VAMDCLib.queryRegistry();
        }
    }
    // return the slap services table
    public ServerPopupTable getSlapTable() {
        return slapServices;
    }
    // return the slap services table
    public ServerPopupTable getVamdcTable() {
        return vamdcServices;
    }
    public ServerPopupTable getLinetapTable() {
		// TODO Auto-generated method stub
		return linetapServices;
	}
    
 //   public String getInChiKey(String species ) {
 //   	return slPanel.getInChiKey( species);
    	//return queryInchiKey(species );
 //   }
    
    public String getInChiKey() {
    	return slPanel.getInChiKey();
    	//return queryInchiKey(species );
    }
   
    public String getInChi() {
    	return slPanel.getInChiKey();
    	//return queryInchiKey(species );
    }
   


	public boolean isSLAPSelected() {
        // TODO Auto-generated method stub
        return (servTabPanel.getSelectedIndex() == SLAP_INDEX);
    }
    public boolean isLinetapSelected() {
        // TODO Auto-generated method stub
        return (servTabPanel.getSelectedIndex() == LINETAP_INDEX);
    }
    
    private void getServers() {
        if (isSLAPSelected()) {
            getSLAPServices();
        } else if (isLinetapSelected()) {
        	getLinetapServices();
        } else {
            getVAMDCServices();
        }
           
       }

    /* update the spectral lines panel to the current plot */
	public void updatePlot(PlotControl plotControl) {
		slPanel.updatePlot(plotControl);
		
	}
	


	@Override
	public void actionPerformed(ActionEvent e) {
		JMenuItem mi = (JMenuItem) e.getSource();
		chosenSpecies= mi.getText();
		
		
	}

	

}
