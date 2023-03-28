<?xml version="1.0"?>

<!--
 ! This is an XSLT stylesheet that copies XHTML input to HTML output
 ! making only one adjustment as it goes: elements with the attribute
 ! @class="adql2.0" are only visible if the parameter ADQL_VERSION is "2.0",
 ! and similarly for @class="adql2.1" and ADQL_VERSION "2.1".
 !
 ! It's kind of annoying to have to use this, because you should be able to do
 ! the same thing more straightforwardly at HTML rendering time by tweaking
 ! the CSS stylesheet (something like '*[class="adql2.1"] {display: none;}'),
 ! but it seems that the javax.swing.text.html.StyleSheet implementation,
 ! at least at Java 8, is not sufficiently powerful to do that.
 !-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html"/>

  <xsl:param name="ADQL_VERSION" select="2.0"/>

  <xsl:template match="*[@class='adql2.0']">
    <xsl:if test="$ADQL_VERSION=2.0">
      <xsl:apply-templates/>
    </xsl:if>
  </xsl:template>

  <xsl:template match="*[@class='adql2.1']">
    <xsl:if test="$ADQL_VERSION=2.1">
      <xsl:apply-templates/>
    </xsl:if>
  </xsl:template>

  <xsl:template match="*">
    <xsl:copy>
      <xsl:apply-templates select="*|@*|text()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="@*">
    <xsl:copy/>
  </xsl:template>

</xsl:stylesheet>
