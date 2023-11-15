package uk.ac.starlink.splat.vamdc;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.vamdc.dictionary.Restrictable;
import org.vamdc.registry.client.Registry;
import org.vamdc.registry.client.RegistryFactory;

import edu.oswego.cs.dl.util.concurrent.BoundedPriorityQueue;
import jsky.util.Logger;
import net.ivoa.xml.voresource.v1.Contact;
import net.ivoa.xml.voresource.v1.Content;
import net.ivoa.xml.voresource.v1.Resource;
import shaded.parquet.it.unimi.dsi.fastutil.doubles.AbstractDouble2IntFunction;
import uk.ac.starlink.splat.data.ssldm.PhysicalQuantity;
import uk.ac.starlink.splat.data.ssldm.SpectralLine;
import uk.ac.starlink.splat.vo.SSAPRegResource;
import uk.ac.starlink.table.BeanStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.RowListStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;


public class VAMDCLib {
    
    /* 
     * This class includes several functions to communicate with VAMDC services and
     * interpret the returned data.
     * @author Margarida Castro Neves
     * 09.2016
     */
    
    public VAMDCLib() {
        // Constructor
    }
    
    
    /* 
     * Query the VAMDC Registry and get a table of databases 
     */
    
    public static StarTable queryRegistry(){
        
        ArrayList <SSAPRegResource> resources = new ArrayList<SSAPRegResource>();
        BeanStarTable bst=null;
        try {
            bst = new BeanStarTable(SSAPRegResource.class);
        } catch (IntrospectionException e) {
            System.out.println(e.getMessage());
        }

        try {
             
            Registry reg = RegistryFactory.getClient(RegistryFactory.REGISTRY_12_07);
            System.out.println("Queried Registry");
          
        
            
            for (String ivoid : reg.getIVOAIDs(Registry.Service.VAMDC_TAP)){
                try {  	
                    Resource r = reg.getResourceMetadata(ivoid);
                   
                    String title = r.getTitle();
                   
                    String description = r.getContent().getDescription();
                    String shortname = r.getShortName();
                    if (shortname == null || shortname.isEmpty() ) {
                        shortname = title;
                    }

                    String publisher = r.getCuration().getPublisher().getValue();
                    
                    List<Contact> contacts = r.getCuration().getContact();
                    String contact="";
                    if (contacts!= null)
                        contact = contacts.get(0).getName().getValue()+" "+contacts.get(0).getEmail();
                    String refURL= r.getContent().getReferenceURL();
                    
                    SSAPRegResource resource = new SSAPRegResource(shortname, title, description, reg.getVamdcTapURL(ivoid).toString());                  
                    resource.setIdentifier(r.getIdentifier());
                    resource.setPublisher(publisher);
                    resource.setContact(contact);
                    resource.setReferenceUrl(refURL);
                    
                    resources.add(resource);
                 
                  
                } catch (Exception e ) { 
                    System.out.println(e.getMessage());
                }

            }
        //    table.setModel(model);

        } catch (Exception e ) { //catch (RegistryCommuicationException e) {
            System.out.println(e.getMessage());
            
        }
        bst.setData(resources.toArray(new SSAPRegResource[0]));
        return bst;
    }
    
    /** 
     * Query a VAMDC database and return a table of results
     *  
     * @param query a string containing a VAMDC-TAP Query
     * @param inps the input string of the connected database where to send the query
     * @return a StarTable containing the parsed response
     * @throws Exception
     */
    public StarTable getResultStarTable(String query, InputStream inps ) throws Exception { 
         
       // if (  inps.available()  > 0)
       //     throw new IOException( "Empty results");
    	System.out.println("query="+query);
        ArrayList<SpectralLine> lines = new ArrayList<SpectralLine>();
        XSAMSParser  xsams = null;
        try {
            xsams = new XSAMSParser(inps); 
            
        } catch (JAXBException e) {
          //  Logger.info(this, "JAXBException: "+e.getMessage());
           // e.printStackTrace();
            inps.close();
            throw e;
        }
        catch (Exception e) {
            Logger.info(this, "Exception: "+e.getMessage());
           // e.printStackTrace();
            inps.close();
            throw e;
        }
        inps.close();
        
        if (xsams == null )
            return null;
        
        lines = xsams.getSpectralLines();
        if ( lines.isEmpty())
            return null;
        
        VoTableTranslator votable = new VoTableTranslator(lines, query);
        
        return makeStarTable(votable, lines);
        
    }

    /* 
     *  Transforms the SpectralLine objects into a StarTable
     */
    private StarTable makeStarTable(VoTableTranslator votable , ArrayList<SpectralLine> lines) {
        

      //  ArrayList<String> tableLines = votable.makeTable();
        String [] cols = votable.getColumns();
       
        
        ColumnInfo[] columns = new ColumnInfo[cols.length];
        for (int i=0;i<cols.length;i++) {
            columns[i]=new ColumnInfo(getValueInfo(lines.get(0), cols[i], votable.getUtype(i)));
        }
       
        RowListStarTable rlst = new RowListStarTable(columns);
        for (SpectralLine l :lines) {
    //        Object[] line = new Object[cols.length];
           // Row
           rlst.addRow(getRow(l, columns));
          }
        
        return (StarTable) rlst;
    }

    private ValueInfo getValueInfo(SpectralLine line, String colname, String utype) {
 
       
     
        if (colname.equals("wavelength")) 
            return  makeValueInfo(colname, line.getWavelength(), utype);
    
        if (colname.equals("initial energy")) 
            return  makeValueInfo(colname, line.getInitialLevel().getEnergy(), utype );
           
        if (colname.equals("final energy"))
            return makeValueInfo(colname, line.getFinalLevel().getEnergy(), utype);
        
        if (colname.equals("einsteinA"))
            return makeValueInfo(colname, line.getEinsteinA(), utype);
       
        if (colname.equals("air wavelength"))
            return makeValueInfo(colname, line.getAirWavelength(), utype);
        
        if (colname.equals("oscillator strength"))
            return makeValueInfo(colname, line.getOscillatorStrength(), utype );
        
        if (colname.equals("weighted oscillator strength"))
            return makeValueInfo(colname, line.getOscillatorStrength(), utype);
        
        return makeValueInfo(colname,  utype);
       
}
    private ValueInfo makeValueInfo(String colname,  String utype) {
        DefaultValueInfo vinfo = new DefaultValueInfo(colname);
        vinfo.setUtype(utype);       
        return vinfo;
        
    }

    private DefaultValueInfo makeValueInfo(String colname, PhysicalQuantity pq, String utype) {
        //System.out.println("colname "+ colname);
        if (pq==null) {   
            pq = new PhysicalQuantity();
        }
        DefaultValueInfo vinfo = new DefaultValueInfo(colname);
        vinfo.setUnitString(pq.getUnitExpression());
        vinfo.setUCD(pq.getUcd());
        vinfo.setUtype(utype);
        vinfo.setContentClass(Double.class);
       
    
        return vinfo;
    }

 
    private Object[] getRow(SpectralLine line, ColumnInfo[] cols) {
        Object[] row = new Object[cols.length];
        for (int i=0;i<cols.length;i++) {
            if (cols[i].getName().equals("title"))
                row[i]=line.getTitle();            
            if (cols[i].getName().equals("wavelength"))
                row[i]=getQuantityValue(line.getWavelength());
            if (cols[i].getName().equals("element"))
                row[i]=line.getInitialElement().getElementName();
            if (cols[i].getName().equals("ion charge"))
              //  row[i]=line.getInitialElement().getIonizationStageRoman();
            	row[i]=line.getInitialElement().getIonizationStage();
            if (cols[i].getName().equals("initial energy"))
                row[i]=getQuantityValue(line.getInitialLevel().getEnergy());
            if (cols[i].getName().equals("final energy"))
                row[i]=getQuantityValue(line.getFinalLevel().getEnergy());
            if (cols[i].getName().equals("einsteinA"))
                row[i]=getQuantityValue(line.getEinsteinA());
            if (cols[i].getName().equals("initial level"))
                row[i]=line.getInitialLevel().getConfiguration();
            if (cols[i].getName().equals("final level"))
                row[i]=line.getFinalLevel().getConfiguration();
            if (cols[i].getName().equals("air wavelength"))
                row[i]=getQuantityValue(line.getAirWavelength());
            if (cols[i].getName().equals("oscillator strength"))
                row[i]=getQuantityValue(line.getOscillatorStrength());
            if (cols[i].getName().equals("weighted oscillator strength"))
                row[i]=getQuantityValue(line.getOscillatorStrength());                
        }
        
        return row;
    }


    private Object getQuantityValue(PhysicalQuantity pq) {
        if (pq==null)
            return null;
        else return (pq.getValue());
    }

}
