/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     11-NOV-2004 (Peter W. Draper):
 *       Original version.
 *     23-FEB-2012 (Margarida Castro Neves, mcneves@ari.uni-heidelberg.de)
 *       Added getSize() method  
 */
package uk.ac.starlink.splat.vo;


import java.util.HashMap;

import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.table.StarTable;


/**
 * Container for a list of possible Simple Spectral Access Protocol (SSAP)
 * servers that can be used. Each server is represented by a
 * {@link RegResource}. The primary source of these should be a query to a
 * VO registry.
 *
 * @author Peter W. Draper
 * @version $Id$
 * 
 */
public class SSAServerList extends AbstractServerList
{
    private HashMap<String, SSAPRegResource> serverList = new HashMap<String, SSAPRegResource>();
   
 //   private HashMap<String, Boolean> selectionList = new HashMap<String, Boolean>();
    private static final String oldconfigFile = "SSAPServerListV3.xml";
    private  static final String configFile = "SSAPServerListV4.xml";
 //   private  static final String defaultFile = "serverlist.xml";
    

    public SSAServerList()
        throws SplatException
    {
        super();
    }
    
    public SSAServerList(StarTable table)  //throws SplatException
    {    
         super(table);      
    }
    
    @Override
    public String getConfigFile() {
        return configFile;
    }
  

}
