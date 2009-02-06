<?xml version="1.0"?>

<!-- 
  - Stylesheet to convert ADQL version 0.7.4 to a Sybase-flavored 
  -   SQL String 
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
     - This stylesheet makes minor modifications to the standard
     - ADQL/x to ADQL/s translation.  This is accomplished by
     - importing the standard stylesheet
     -->
   <xsl:import href="ADQLx2s-v1.0.xsl"/>

   <xsl:output method="text"/>

   <xsl:template match="/">
      <xsl:apply-templates select="/*"/>
   </xsl:template>

   <!-- 
     - we move the Restrict component from after Allow (rendered as "TOP") 
     - to before Select (rendered as "SET ROWCOUNT")
     -->
   <xsl:template match="/*">
      <xsl:apply-templates select="ad:Restrict"/>
      <xsl:text>SELECT </xsl:text>
      <xsl:apply-templates select="ad:Allow"/>
      <xsl:apply-templates select="ad:SelectionList"/>
      <xsl:text> FROM </xsl:text>
      <xsl:apply-templates select="ad:From"/>
      <xsl:apply-templates select="ad:Where"/>
      <xsl:apply-templates select="ad:GroupBy"/>
      <xsl:apply-templates select="ad:Having"/>
      <xsl:apply-templates select="ad:OrderBy"/>
   </xsl:template>

   <!--
     - Restrict Template 
     -->
   <xsl:template match="ad:Restrict">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:text>SET ROWCOUNT </xsl:text>
         <xsl:value-of select="@Top"/>
         <xsl:text> </xsl:text>
      </xsl:if>
   </xsl:template>

</xsl:stylesheet>
