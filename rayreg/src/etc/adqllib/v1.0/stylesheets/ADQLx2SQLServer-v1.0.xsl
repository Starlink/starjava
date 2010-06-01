<?xml version="1.0"?>

<!-- 
  - Stylesheet to convert ADQL version 0.7.4 to a SQLServer-flavored SQL String 
  - Version 1.1 
  -   updated by Ray Plante (NCSA) updated for ADQLlib
  - Based on v1.0 by Ramon Williamson, NCSA (April 1, 2004)
  - Based on the schema: http://www.ivoa.net/xml/ADQL/v1.0
 -->
<xsl:stylesheet xmlns="http://www.ivoa.net/xml/ADQL/v1.0" 
                xmlns:ad="http://www.ivoa.net/xml/ADQL/v1.0" 
                xmlns:q1="urn:nvo-region" 
                xmlns:q2="urn:nvo-coords" 
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                version="1.0">

   <!-- 
     - This stylesheet requires no modifications to the standard
     - ADQL/x to ADQL/s translation.  
     -->
   <xsl:import href="ADQLx2s-v1.0.xsl"/>

   <xsl:output method="text"/>

</xsl:stylesheet>
