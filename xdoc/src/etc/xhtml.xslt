<?xml version="1.0"?>

<!-- 
 !  This is a mostly-straightforward stylesheet for transforming from
 !  XHTML to HTML - elements are basically copied as they are found,
 !  but since the xsl:output method is "html" the output is browser-friendly
 !  HTML rather than well-formed XML.
 !
 !  There are a couple of extensions though, which will do something clever
 !  if XAlan is used as the transformation processor (as it is in Sun's
 !  J2SE 1.4 and 1.5).  If XAlan is not used, these clever bits are just
 !  ignored.
 !
 !  1. Any <img> element, if its "src" attribute corresponds to a file
 !     that can be turned into a javax.swing.ImageIcon, is given "width" 
 !     and "height" attributes which match the actual size of the image
 !     file.  Most browsers are able to make use of these attributes to
 !     give tidier loading of HTML pages with embedded images.
 !
 !  2. Any <a> element with a non-empty "reportfilesize" attribute will
 !     have the size of the file referenced by its "href" attribute 
 !     appended after the element's content.  So something like
 !
 !        <a href="archive.zip" reportfilesize="true">archive.zip</a>
 !
 !     might be transformed to
 !
 !        <a href="archive.zip">archive.zip</a> (1.5M)
 !
 !  3. The <usage class="pkg.clazz"> element will expand to the class's
 !     usage method (result of running pkg.class.main("-help")).
 !
 !  4. The <version/> element will expand to the value of the VERSION
 !     parameter if defined.
 !
 !  5. The <date/> element will expand to the value of the DATE parameter
 !     if defined, otherwise to today's date.
 !
 !-->

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:java="http://xml.apache.org/xalan/java"
                xmlns:Date="xalan://java.util.Date"
                xmlns:DateFormat="xalan://java.text.DateFormat"
                xmlns:File="xalan://java.io.File"
                xmlns:XdocUtils="xalan://uk.ac.starlink.xdoc.XdocUtils"
                exclude-result-prefixes="java Date DateFormat File XdocUtils"
                >

  <xsl:param name="BASEDIR" select="'.'"/>
  <xsl:param name="VERSION" select="'??'"/>
  <xsl:param name="DATE" select="(today)"/>

  <xsl:output method="html"
              doctype-public="-//W3C//DTD HTML 3.2//EN"/>

  <xsl:template match="*">
    <xsl:copy>
      <xsl:apply-templates select="*|@*|text()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="@*">
    <xsl:copy/>
  </xsl:template>

  <xsl:template match="img">
    <xsl:element name="img">
      <xsl:apply-templates select="@*"/>
      <xsl:if test="function-available('XdocUtils:getImageSize')">
        <xsl:variable name="src" select="string(./@src)"/>
        <xsl:variable name="srcFile" select="File:new(string($BASEDIR),$src)"/>
        <xsl:variable name="srcLoc" select="string(java:toString($srcFile))"/>
        <xsl:variable name="iconDim" select="XdocUtils:getImageSize($srcLoc)"/>
        <xsl:variable name="width" select="java:getWidth($iconDim)"/>
        <xsl:variable name="height" select="java:getHeight($iconDim)"/>
        <xsl:if test="$width&gt;=0">
          <xsl:attribute name="width">
            <xsl:value-of select="$width"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:if test="$height&gt;=0">
          <xsl:attribute name="height">
            <xsl:value-of select="$height"/>
          </xsl:attribute>
        </xsl:if>
      </xsl:if>
    </xsl:element>
  </xsl:template>

  <xsl:template match="a">
    <xsl:element name="a">
      <xsl:apply-templates select="@name|@href|text()|*"/>
    </xsl:element>
    <xsl:if test="@reportfilesize">
      <xsl:if test="function-available('XdocUtils:reportFileSize')">
        <xsl:text> (</xsl:text>
        <xsl:value-of select="XdocUtils:reportFileSize(string(@href))"/>
        <xsl:text>)</xsl:text>
      </xsl:if>
    </xsl:if>
  </xsl:template>

  <xsl:template match="usage">
    <xsl:if test="function-available('XdocUtils:classUsage')">
      <xsl:element name="blockquote">
        <xsl:element name="pre">
          <xsl:value-of select="XdocUtils:classUsage(string(@class))"/>
        </xsl:element>
      </xsl:element>
    </xsl:if>
  </xsl:template>

  <xsl:template match="version">
    <xsl:value-of select="$VERSION"/>
  </xsl:template>

  <xsl:template match="date">
    <xsl:choose>
      <xsl:when test="$DATE">
        <xsl:value-of select="$DATE"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="format" select="DateFormat:getDateInstance()"/>
        <xsl:variable name="today" select="Date:new()"/>
        <xsl:value-of select="java:format($format,$today)"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
