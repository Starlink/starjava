<?xml version="1.0"?>

<!-- Transforms from XHTML-like XML to SUN-friendly XML.
 !   This is pretty scrappy and very incomplete, but it does what's required
 !   for the documentation currently going into the SUN/256 build.
 !   It doesn't do much, just changes some element names.
 !   If more diverse XHTML needs to be dealt with in the future,
 !   it will have to be improved.
 !-->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                >

  <!-- Write XML, but not an XML declaration, since the output is typically
   !   embedded in a larger XML document. -->
  <xsl:output method="xml"
              omit-xml-declaration="yes"/>

  <xsl:template match="@*">
    <xsl:copy/>
  </xsl:template>

  <xsl:template match="/*">
    <xsl:apply-templates/>
  </xsl:template>

  <!-- Transform uppercase element names (permitted in XHTML but not in SUNs)
   !   into their lowercase equivalents. -->
  <xsl:variable name="lowercase" select="'abcdefghijklmnopqrstuvwxyz'" />
  <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />
  <xsl:template match="*">
    <xsl:element name="{translate(name(.), $uppercase, $lowercase)}">
       <xsl:apply-templates select="*|@*|text()"/>
    </xsl:element>
  </xsl:template>

  <!-- Convert some elements into their differently-named equivalents. -->
  <xsl:template match="i|I">
    <em><xsl:apply-templates/></em>
  </xsl:template>

  <xsl:template match="b|B">
    <strong><xsl:apply-templates/></strong>
  </xsl:template>

  <xsl:template match="tt|TT">
    <code><xsl:apply-templates/></code>
  </xsl:template>

  <xsl:template match="pre|PRE">
    <verbatim><xsl:apply-templates/></verbatim>
  </xsl:template>

  <xsl:template match="a|A">
    <webref url="{@href}">
      <xsl:apply-templates/>
    </webref>
  </xsl:template>

  <!-- SUNs require P content of DD elements, which is not usual in XHTML.
   !   Insert missing Ps as required. -->
  <xsl:template match="dd[not(child::p or child::P)] |
                       DD[not(child::p or child::P)]">
    <dd><p><xsl:apply-templates/></p></dd>
  </xsl:template>

  <xsl:template match="h3|H3">
    <p><strong><xsl:apply-templates/></strong></p>
  </xsl:template>

</xsl:stylesheet>

