package uk.ac.starlink.splat.vo;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.starlink.splat.iface.AbstractServerPanel;
import uk.ac.starlink.splat.iface.SpectralLinesPanel;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.vamdc.VAMDCLib;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.TableLoadPanel;
import uk.ac.starlink.util.gui.ErrorDialog;

public class LinesQueryPanel extends AbstractServerPanel  {
    
   
    private static int WIDTH = 350;
    private static int HEIGHT = 750;
    private static int optionsHeight = 280;
    
    private int SLAP_INDEX=0;
    private int VAMDC_INDEX=1;

    private static String tagsFile = "linesTagsV2.xml";
    private ServerPopupTable slapServices=null;
    private ServerPopupTable vamdcServices=null;
    private JTabbedPane servTabPanel;
    private SpectralLinesPanel slPanel = null;
    private LineBrowser browser;
    private VAMDCLib vamdclib;

    
    public LinesQueryPanel( LineBrowser browser )  {
        super(); 
        this.browser=browser;
       // slPanel=slp;
        vamdclib = new VAMDCLib();
        initUI( initOptionsPanel(), initServersPanel() );
     }
    
    private JScrollPane initOptionsPanel() {
        
        JPanel queryPanel = new JPanel(new GridBagLayout());
        queryPanel.setBorder(BorderFactory.createEtchedBorder() );
        GridBagConstraints gbcOptions = new GridBagConstraints();
        gbcOptions.anchor = GridBagConstraints.NORTHWEST;
        gbcOptions.fill = GridBagConstraints.HORIZONTAL;
        gbcOptions.weightx=.5;
        gbcOptions.weighty=1;
        gbcOptions.gridx=0;
        gbcOptions.gridy=0;
	slPanel = new SpectralLinesPanel(browser);
        queryPanel.add(slPanel,gbcOptions);
        gbcOptions.weighty=0;
        gbcOptions.weightx=0;
        gbcOptions.gridy=1;
        queryPanel.add(makeTagPanel(), gbcOptions);
        

        JScrollPane optionsScroller = new JScrollPane();
        optionsScroller.getViewport().add( queryPanel, null); //invOptionsPanel, null );
        optionsScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        optionsScroller.setPreferredSize(new Dimension(WIDTH,optionsHeight));
       // optionsScroller.setMinimumSize(new Dimension(WIDTH-20,optionsHeight-20));
        optionsScroller.setMinimumSize(new Dimension(WIDTH-20,optionsHeight-20));
        return optionsScroller;
    }
    
    private JTabbedPane initServersPanel() {
         servTabPanel = new JTabbedPane();
         servTabPanel.add( makeSlapPanel(), SLAP_INDEX);
         servTabPanel.add( makeVamdcPanel(), VAMDC_INDEX);
         servTabPanel.setTitleAt(SLAP_INDEX, "SLAP Services");
         servTabPanel.setTitleAt(VAMDC_INDEX, "VAMDC Services");     
         servTabPanel.setSelectedIndex(SLAP_INDEX);
	 slPanel.deactivateStage(); // does not work for SLAP (yet)
         setServerTable(slapServices);
         servTabPanel.addChangeListener(new ChangeListener() {
             public void stateChanged(ChangeEvent e) {
                 if (isSLAPSelected()) {
                     setServerTable(slapServices);
		     slPanel.deactivateStage(); // does not work for SLAP (yet)
                 } else {
                     setServerTable(vamdcServices);
		     slPanel.activateStage();
                 }
             }
         });
         servTabPanel.setPreferredSize(new Dimension(WIDTH,HEIGHT-optionsHeight));
       

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

    public boolean isSLAPSelected() {
        // TODO Auto-generated method stub
        return (servTabPanel.getSelectedIndex() == SLAP_INDEX);
    }
    
    private void getServers() {
        if (isSLAPSelected()) {
            getSLAPServices();
        } else {
            getVAMDCServices();
        }
           
       }
    
   



}
