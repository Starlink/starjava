package uk.ac.starlink.splat.vamdc;



import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.io.IOUtils;
import org.vamdc.registry.client.Registry;
import org.vamdc.registry.client.RegistryFactory;
import org.vamdc.xsams.io.JAXBContextFactory;
import org.vamdc.xsams.schema.AtomType;
import org.vamdc.xsams.schema.AtomicIonType;
import org.vamdc.xsams.schema.AtomicStateType;
import org.vamdc.xsams.schema.DataType;
import org.vamdc.xsams.schema.IsotopeParametersType;
import org.vamdc.xsams.schema.IsotopeType;
import org.vamdc.xsams.schema.RadiativeTransitionProbabilityType;
import org.vamdc.xsams.schema.RadiativeTransitionType;
import org.vamdc.xsams.schema.WlType;
import org.vamdc.xsams.schema.XSAMSData;

import gavo.spectral.ssldm.Level;
import gavo.spectral.ssldm.PhysicalQuantity;
import gavo.spectral.ssldm.SpectralLine;
import net.ivoa.xml.voresource.v1.Contact;
import net.ivoa.xml.voresource.v1.Resource;
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
    
    public static JTable queryRegistry(){
        
        JTable table = new JTable(new DefaultTableModel(new Object[]{"short name", "title", "description", "identifier",
                "publisher", "contact", "access URL", "reference URL"}, 0));

        // from SSAP server table, how to map? "waveband", "content type", "data source", "creation type", "stantardid", "version", "subjects", "tags"
        DefaultTableModel model = (DefaultTableModel) table.getModel();
   
        try{
             
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
                    System.out.println(title);
                    System.out.println(ivoid);

                    System.out.println("URL : "+reg.getVamdcTapURL(ivoid));
                    model.addRow(new Object[]{ shortname, title, description, r.getIdentifier(), publisher, contact, reg.getVamdcTapURL(ivoid), refURL });
                } catch (Exception e ) { 
                    System.out.println(e.getMessage());
                }

            }
            table.setModel(model);

        } catch (Exception e ) { //catch (RegistryCommuicationException e) {
            System.out.println(e.getMessage());
            
        }
        return table;
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
        
         
        ArrayList<SpectralLine> lines = new ArrayList<SpectralLine>();
        XSAMSData d;
        try {
            d = (XSAMSData)JAXBContextFactory.getUnmarshaller().unmarshal(inps);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw e;
        }
        lines = transformToLines(d);
        VoTableTranslator votable = new VoTableTranslator(lines, query);
        
        return makeStarTable(votable, lines);
        
    }

    /* 
     *  Transforms the SpectralLine objects into a StarTable
     */
    private StarTable makeStarTable(VoTableTranslator votable , ArrayList<SpectralLine> lines) {
        

      //  ArrayList<String> tableLines = votable.makeTable();
        // do not create votable; translate direct from the line array - easier to parse right now
        String [] cols = votable.getColumns();
        ColumnInfo[] columns = new ColumnInfo[cols.length];
        for (int i=0;i<cols.length;i++) {
            columns[i]=new ColumnInfo(getValueInfo(lines.get(0), cols[i]));
        }
       
        RowListStarTable rlst = new RowListStarTable(columns);
        for (SpectralLine l :lines) {
            Object[] line = new Object[cols.length];
           // Row
           rlst.addRow(getRow(l, columns));
          }
        
        return rlst;
    }

    private ValueInfo getValueInfo(SpectralLine line, String colname) {
 
        if (colname.equals("title")) 
            return  makeValueInfo(colname, "ssldm:Line.title");
        if (colname.equals("wavelength")) 
            return  makeValueInfo(colname, line.getWavelength());

        if (colname.equals("initial energy")) 
            return  makeValueInfo(colname, line.getInitialLevel().getEnergy());
           
        if (colname.equals("final energy"))
            return makeValueInfo(colname, line.getFinalLevel().getEnergy());
        if (colname.equals("einsteinA"))
            return makeValueInfo(colname, line.getEinsteinA());
        if (colname.equals("initial level"))
            return makeValueInfo(colname,  "ssldm:line.initialLevel.name");
        if (colname.equals("final level"))
            return makeValueInfo(colname,  "ssldm:line.finalLevel.name");
        if (colname.equals("air wavelength"))
            return makeValueInfo(colname, line.getAirWavelength());
        if (colname.equals("oscillator strength"))
            return makeValueInfo(colname, line.getOscillatorStrength());
        if (colname.equals("weighted oscillator strength"))
            return makeValueInfo(colname, line.getOscillatorStrength());
        return null;
       
}
    private ValueInfo makeValueInfo(String colname,  String utype) {
        DefaultValueInfo vinfo = new DefaultValueInfo(colname);
        vinfo.setUtype(utype);       
        return vinfo;
        
    }

    private DefaultValueInfo makeValueInfo(String colname, PhysicalQuantity pq) {
        //System.out.println("colname "+ colname);
        DefaultValueInfo vinfo = new DefaultValueInfo(colname);
        vinfo.setUnitString(pq.getUnitExpression());
        vinfo.setUCD(pq.getUcd());
        vinfo.setUtype(pq.getUtype());
    
        return vinfo;
    }

 
    private Object[] getRow(SpectralLine line, ColumnInfo[] cols) {
        Object[] row = new Object[cols.length];
        for (int i=0;i<cols.length;i++) {
            if (cols[i].getName().equals("title"))
                row[i]=line.getTitle();            
            if (cols[i].getName().equals("wavelength"))
                row[i]=line.getWavelength().getValue(); 
            if (cols[i].getName().equals("initial energy"))
                row[i]=line.getInitialLevel().getEnergy().getValue();
            if (cols[i].getName().equals("final energy"))
                row[i]=line.getFinalLevel().getEnergy().getValue();
            if (cols[i].getName().equals("Einstein A"))
                row[i]=line.getEinsteinA().getValue();
            if (cols[i].getName().equals("initial level"))
                row[i]=line.getInitialElement();
            if (cols[i].getName().equals("initial level"))
                row[i]=line.getFinalElement();
            if (cols[i].getName().equals("air wavelength"))
                row[i]=line.getAirWavelength().getValue();
            if (cols[i].getName().equals("oscillator strength"))
                row[i]=line.getOscillatorStrength().getValue();
            if (cols[i].getName().equals("weighted oscillator strength"))
                row[i]=line.getOscillatorStrength().getValue();                
        }
        
        return row;
    }

    
    /*
     * Reads the XSAMSData returned from a VAMDC Database, and transform it to
     * SpectralLine objects
     */
    private ArrayList<SpectralLine> transformToLines(XSAMSData d) {
        
        ArrayList<SpectralLine> lines = new ArrayList<SpectralLine>();

        HashMap <String,String> elements= new HashMap<String,String>();
        
        // get the atom/species symbol and put into a hashmap
        for (AtomType atom : d.getSpecies().getAtoms().getAtoms()) {

          //  System.out.println("Atom: "+ atom.getChemicalElement().getElementSymbol() + " - ");
          //  System.out.println( "Charge: "+ atom.getChemicalElement().getNuclearCharge() + " - ");

            for (IsotopeType iso : atom.getIsotopes()) {
            //    IsotopeParametersType isop = iso.getIsotopeParameters();
                for (AtomicIonType ion:iso.getIons()) {

                    elements.put(ion.getSpeciesID(), atom.getChemicalElement().getElementSymbol().value());
                    //ions.put(ion., ion);
                 //   System.out.println("Ion: "+ ion.getIonCharge() + " - ");
                }
            }
        }

        for ( RadiativeTransitionType radtrans: d.getProcesses().getRadiative().getRadiativeTransitions() ) { 

            //SpeciesType specref = (SpeciesType) radtrans.getSpeciesRef();
            // SpeciesStateRefType spectype = (SpeciesStateRefType) radtrans.getSpeciesRef();
            if (radtrans.getLowerStateRef() != null && radtrans.getLowerStateRef().getClass().equals(AtomicStateType.class)) {
                AtomicStateType state1 = (AtomicStateType) radtrans.getLowerStateRef();                    
                //state1.getAtomicNumericalData().getIonizationEnergy();
                DataType energy1 = state1.getAtomicNumericalData().getStateEnergy();
                AtomicStateType state2 = (AtomicStateType) radtrans.getUpperStateRef();
                state2.getAtomicNumericalData().getIonizationEnergy();
                DataType energy2 = state2.getAtomicNumericalData().getStateEnergy();

                AtomicIonType ion1 =  (AtomicIonType) state1.getParent();                     
                String id1;
                if ( ion1.getIsoelectronicSequence() != null) {
                   id1 = ion1.getIsoelectronicSequence().value();
                }else id1 = elements.get(ion1.getSpeciesID())+" "+ion1.getIonCharge();

                
                AtomicIonType ion2 =  (AtomicIonType) state2.getParent();                   
                String id2;
                if ( ion2.getIsoelectronicSequence() != null) {
                    id2=ion2.getIsoelectronicSequence().value();
                } else id2 = elements.get(ion2.getSpeciesID());;

                
                String probabilities = null;

                SpectralLine line = new SpectralLine();                    

                if (id1 != null) {
                    line.setInitialElement(id1);
                    line.setTitle(elements.get(ion1.getSpeciesID()));
                } 

                if (id2 != null)
                    line.setFinalElement(id2);

                Level initial = new Level();

                if (energy1 != null) {
                    initial.setEnergy(energy1.getValue().getValue(), energy1.getValue().getUnits());                         
                } 
                try {
                    initial.setTotalStatWeight(state1.getAtomicNumericalData().getStatisticalWeight().doubleValue(), null) ; // An integer representing the total number of terms pertaining to a given level
                } catch( Exception e) {}
                //PhysicalQuantity nuclearStatWeight ; // The same as Level.totalStatWeight for nuclear spin states only
                try {
                    initial.setLandeFactor(state1.getAtomicNumericalData().getLandeFactor().getValue().getValue(), (Double) null, null); // A dimensionless factor g that accounts for the splitting of normal energy levels into uniformly spaced sublevels in the presence of a magnetic field
                }catch( Exception e) {}
                
                Level finalLevel = new Level();
                
                if (energy2 != null) {
                    finalLevel.setEnergy(energy2.getValue().getValue(), energy2.getValue().getUnits());
                } else {
                    finalLevel.setEnergy(null);
                }
                try {
                    finalLevel.setTotalStatWeight(state2.getAtomicNumericalData().getStatisticalWeight().doubleValue(), null) ; // An integer representing the total number of terms pertaining to a given level
                }catch( Exception e) {}
                //PhysicalQuantity nuclearStatWeight ; // The same as Level.totalStatWeight for nuclear spin states only
                try {
                    finalLevel.setLandeFactor(state2.getAtomicNumericalData().getLandeFactor().getValue().getValue(), (Double) null, state1.getAtomicNumericalData().getLandeFactor().getValue().getUnits()); ;// A dimensionless factor g that accounts for the splitting of normal energy levels into uniformly spaced sublevels in the presence of a magnetic field
                }catch( Exception e) {}
                //initial.setLifeTime(state1.getAtomicNumericalData().getLifeTimes());
                //PhysicalQuantity lifeTime ; // Intrinsic lifetime of a level due to its radiative decay
                //PhysicalQuantity energy  ; //The binding energy of an electron belonging to the level

                //String energyOrigin ; // Human readable string indicating the nature of the energy origin, e.g., “Ionization energy limit”, “Ground state energy” of an atom, “Dissociation limit” for  a molecule, etc
                //QuantumState quantumState ; // A representation of the level quantum state through its set of quantum numbers
                //String nuclearSpinSymmetryType ;//  A string indicating the type of nuclear spin symmetry. Possible values are: “para”,“ortho”, “meta”
                //PhysicalQuantity parity ;//  Eigenvalue of the parityoperator. Values (+1,-1)
                //String configuration ;
                line.setInitialLevel(initial);
                line.setFinalLevel(finalLevel);
                //  line.setIntensity(intensity);
                //!!! is it possible to have more than one in our case?

                try {
                    RadiativeTransitionProbabilityType prob = radtrans.getProbabilities().get(0);
                    DataType os = prob.getOscillatorStrength();;
                    if (os != null) {
                        line.setOscillatorStrength( os.getValue().getValue(), os.getValue().getUnits());
                    }
                    os = prob.getWeightedOscillatorStrength();
                    if (os != null) {
                        line.setWeightedOscillatorStrength( os.getValue().getValue(), os.getValue().getUnits());
                    }
                    os = prob.getTransitionProbabilityA();
                    if (os != null) {
                        line.setEinsteinA( os.getValue().getValue(), os.getValue().getUnits());
                    }
                } catch (Exception e) {

                }




                //   String elSymbol=null;
                //   if (id != null)
                //       elSymbol=elements.get(id);
                try {
                    WlType wl =  radtrans.getEnergyWavelength().getWavelengths().get(0);
                    if (wl.isVacuum()) { // ?!!!!!! check if it's correct
                        line.setWavelength(wl.getValue().getValue(), wl.getValue().getUnits());

                    } else {
                        line.setAirWavelength(wl.getValue().getValue(), wl.getValue().getUnits());                          
                        line.setWavelength(wl.getValue().getValue()*wl.getAirToVacuum().getValue().getValue(), wl.getValue().getUnits());
                    }
                } catch (Exception e) {

                }
                try {
                    String e1 = null;
                    String e2 = null;
                    
                    
                    if ( line.getInitialLevel().getEnergy() != null ) 
                        e1 = line.getInitialLevel().getEnergy().getString();
                    if ( line.getFinalLevel().getEnergy() != null ) 
                            e2 = line.getFinalLevel().getEnergy().getString();
                    
  //              System.out.println(line.getTitle()+" wl "+ line.getWavelength().getString()+" "+
  //                      " e1 "+ e1+" e2 "+e2+" "+
   //                     " os "+line.getOscillatorStrength().getString()+" wos "+line.getWeightedOscillatorStrength().getString());                    
                }catch(Exception e) {
                    System.out.println(">>>>>>>>>>>>>>>>>"+line.getTitle()+" wl "+ line.getWavelength().getString());
                }
                
                lines.add(line);
            } // if radtrans ...
            // System.out.println(elSymbol+" wl "+ wl.getValue().getValue() + " "+ wl.getValue().getUnits()+" os "+osValue+" "+osUnit);      
        } // for radtrans ... 

        
        return lines;
    }


}
