<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="html"/>

  <xsl:template match="*">
    <xsl:copy-of select="."/>
  </xsl:template>

  <xsl:template match="filesection">
    <html>
      <head>
        <title>
          <xsl:value-of select="@title"/>
        </title>
       </head>
       <body>
         <xsl:apply-templates/>
       </body>
    </html>
  </xsl:template>

</xsl:stylesheet>
