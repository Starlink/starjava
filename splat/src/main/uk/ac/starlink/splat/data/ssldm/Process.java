package uk.ac.starlink.splat.data.ssldm;
/*
 ********************************************
 * IVOA Simple Spectral Line Data Model V1.0
 ********************************************
 */
public class Process {

   /* 
    * Class representing the physical process responsible for the generation of the line
    * @author Margarida Castro Neves
    * 08.2016
    */
    
    String type ; //  String identifying the type of process. Possible values are: "Matter-radiation interaction", "Matter-matter interaction", "Energy shift","Broadening"
    String name ; //  String String describing the process: Example values (corresponding to the values of "type" listed above) are: "Photoionization", "Collisional excitation", "Gravitational redshift", "Natural broadening".
    // PhysicalModel model; // A theoretical model by which  a specific process might be described

    public Process() {
        // TODO Auto-generated constructor stub
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
