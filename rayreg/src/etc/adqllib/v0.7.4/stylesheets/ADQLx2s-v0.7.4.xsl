<?xml version="1.0"?>

<!-- 
  - Stylesheet to convert ADQL version 0.7.4 to an SQL String 
  - Version 1.1 
  -   updated by Ray Plante (NCSA) updated for ADQLlib
  - Based on v1.0 by Ramon Williamson, NCSA (April 1, 2004)
  - Based on the schema: http://www.ivoa.net/xml/ADQL/v0.7.4
 -->
<xsl:stylesheet xmlns="http://www.ivoa.net/xml/ADQL/v0.7.4" 
                xmlns:ad="http://www.ivoa.net/xml/ADQL/v0.7.4"
                xmlns:r="urn:nvo-region" 
                xmlns:c="urn:nvo-coords" 
                xmlns:q1="urn:nvo-region" 
                xmlns:q2="urn:nvo-coords" 
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
                version="1.0">

   <xsl:output method="text"/>

   <!--
     -  xsitype:  a utility template that extracts the local type name 
     -             (i.e., without the namespace prefix) of the value of 
     -             the @xsi:type for the matched element
     -->
   <xsl:template match="*" mode="xsitype">
      <xsl:for-each select="@xsi:type">
         <xsl:choose>
            <xsl:when test="contains(.,':')">
               <xsl:value-of select="substring-after(.,':')"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:value-of select="."/>
            </xsl:otherwise>
         </xsl:choose>
      </xsl:for-each>
   </xsl:template>

   <xsl:template match="/">
      <xsl:apply-templates select="/*"/>
   </xsl:template>

   <xsl:template match="/*">
      <xsl:text>SELECT </xsl:text>
      <xsl:apply-templates select="ad:Allow"/>
      <xsl:apply-templates select="ad:Restrict"/>
      <xsl:apply-templates select="ad:SelectionList"/>
      <xsl:text> FROM </xsl:text>
      <xsl:apply-templates select="ad:From"/>
      <xsl:apply-templates select="ad:Where"/>
      <xsl:apply-templates select="ad:GroupBy"/>
      <xsl:apply-templates select="ad:Having"/>
      <xsl:apply-templates select="ad:OrderBy"/>
   </xsl:template>

   <!-- 
     -  Allow Template 
     -->
   <xsl:template match="ad:Allow">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:value-of select="@Option"/>
         <xsl:text> </xsl:text>
      </xsl:if>
   </xsl:template>

   <!-- 
     -  Restrict Template 
     -->
   <xsl:template match="ad:Restrict">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:text>TOP </xsl:text>
         <xsl:value-of select="@Top"/>
         <xsl:text> </xsl:text>
      </xsl:if>
   </xsl:template>

   <!-- 
     -  OrderBy Template 
     -->
   <xsl:template match="ad:OrderBy">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:text> ORDER BY </xsl:text>

         <xsl:for-each select="ad:Item">
            <xsl:apply-templates select="ad:Expression"/>

            <xsl:if test="ad:Order">
               <xsl:text> </xsl:text>
               <xsl:value-of select="ad:Order/@Direction"/>
            </xsl:if>

            <xsl:if test="position()!=last()">, </xsl:if>
         </xsl:for-each>
      </xsl:if>
   </xsl:template>

   <!-- 
     -  SelectionList Template 
     -->
   <xsl:template match="ad:SelectionList">

      <xsl:if test="not(@xsi:nil='true')">
         <xsl:for-each select="ad:Item">
            <xsl:apply-templates select="."/>
            <xsl:if test="position()!=last()">, </xsl:if>
         </xsl:for-each>
      </xsl:if>
   </xsl:template>

   <!--
     -  SelectionList/Item when xsi:type = 'allSelectionItemType'
     -->
   <xsl:template match="*[@xsi:type='allSelectionItemType'] | 
                      *[substring-after(@xsi:type,':')='allSelectionItemType']">
      <xsl:text>*</xsl:text>
   </xsl:template>

   <!--
     -  SelectionList/Item when xsi:type = 'aliasSelectionItemType'
     -->
   <xsl:template match="*[@xsi:type='aliasSelectionItemType'] | 
                    *[substring-after(@xsi:type,':')='aliasSelectionItemType']">
      <xsl:apply-templates select="*"/>
      <xsl:text> AS </xsl:text>
      <xsl:value-of select="@As"/>
   </xsl:template>

   <!-- 
     -  From Template 
     -->
   <xsl:template match="ad:From">
      <xsl:if test="not(@xsi:nil='true')">

         <xsl:for-each select="ad:Table">
            <xsl:apply-templates select="." />

            <xsl:if test="position()!=last()">, </xsl:if>
         </xsl:for-each>
      </xsl:if>
   </xsl:template>

   <!-- Table types -->

   <xsl:template match="*[@xsi:type='tableType'] | 
                        *[substring-after(@xsi:type,':')='tableType']">
      <xsl:value-of select="@Name"/>
      <xsl:text> </xsl:text>
      <xsl:value-of select="@Alias"/>
   </xsl:template>

   <xsl:template match="*[@xsi:type='tableType'] | 
                        *[substring-after(@xsi:type,':')='tableType']">
      <xsl:value-of select="@Name"/>
      <xsl:text> </xsl:text>
      <xsl:value-of select="@Alias"/>
   </xsl:template>

   <xsl:template match="*[@xsi:type='archiveTableType'] | 
                        *[substring-after(@xsi:type,':')='archiveTableType']">
      <xsl:value-of select="@Archive"/>
      <xsl:text>:</xsl:text>
      <xsl:value-of select="@Name"/>
      <xsl:text> </xsl:text>
      <xsl:value-of select="@Alias"/>
   </xsl:template>

   <!-- Search Types -->

   <!--
     -  Intersection Search:  a AND b
     -->
   <xsl:template match="*[@xsi:type='intersectionSearchType'] | 
                    *[substring-after(@xsi:type,':')='intersectionSearchType']">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:apply-templates select="*[1]"/>
         <xsl:text> AND </xsl:text>
         <xsl:apply-templates select="*[2]"/>
      </xsl:if>
   </xsl:template>

   <!-- 
     -  Union: a OR b
     -->
   <xsl:template match="*[@xsi:type='unionSearchType'] | 
                        *[substring-after(@xsi:type,':')='unionSearchType']">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:apply-templates select="*[1]"/>
         <xsl:text> OR </xsl:text>
         <xsl:apply-templates select="*[2]"/>
      </xsl:if>
   </xsl:template>

   <!--
     -  region
     -->
   <xsl:template match="*[@xsi:type='regionSearchType'] | 
                        *[substring-after(@xsi:type,':')='regionSearchType']">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:text>REGION('</xsl:text>
         <xsl:apply-templates select="ad:Region" />
         <xsl:text>')</xsl:text>
      </xsl:if>
   </xsl:template>

   <!--
     -  Circular region
     -->
   <xsl:template match="*[@xsi:type='circleType'] | 
                        *[substring-after(@xsi:type,':')='circleType']">
     <xsl:choose>
        <xsl:when test="r:Center/c:Pos2Vector">
           <xsl:text>Circle</xsl:text>
        </xsl:when>
        <xsl:when test="r:Center/c:Pos3Vector">
           <xsl:text>Cartesian</xsl:text>
        </xsl:when>
     </xsl:choose>
     <xsl:text> J2000 </xsl:text>
     <xsl:for-each select="r:Center/c:Pos2Vector/c:CoordValue/c:Value/c:double">
        <xsl:value-of select="."/>
        <xsl:text> </xsl:text>
     </xsl:for-each>
     <xsl:value-of select="r:Radius"/>
   </xsl:template>

   <!--
     -  XMatch
     -->
   <xsl:template match="*[@xsi:type='xMatchType'] | 
                        *[substring-after(@xsi:type,':')='xMatchType']">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:text>XMATCH(</xsl:text>

         <xsl:for-each select="ad:Table">
            <xsl:apply-templates select="." />
            <xsl:if test="position()!=last()">, </xsl:if>
         </xsl:for-each>

         <xsl:text>)</xsl:text>
         <xsl:text> </xsl:text>
         <xsl:value-of select="ad:Nature"/>
         <xsl:text> </xsl:text>
         <xsl:value-of select="ad:Sigma/@Value"/>
      </xsl:if>
   </xsl:template>

   <xsl:template match="*[@xsi:type='includeTableType'] | 
                        *[substring-after(@xsi:type,':')='includeTableType']">
      <xsl:value-of select="@Name"/>
   </xsl:template>

   <xsl:template match="*[@xsi:type='dropTableType'] | 
                        *[substring-after(@xsi:type,':')='dropTableType']">
      <xsl:text>!</xsl:text>
      <xsl:value-of select="@Name"/>
   </xsl:template>

   <!--
     -  Simple binary operator comparison:  a op b
     -->
   <xsl:template match="*[@xsi:type='comparisonPredType'] | 
                        *[substring-after(@xsi:type,':')='comparisonPredType']">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:apply-templates select="ad:Arg[1]"/>
         <xsl:text> </xsl:text>
         <xsl:value-of select="@Comparison"/>
         <xsl:text> </xsl:text>
         <xsl:apply-templates select="ad:Arg[2]"/>
      </xsl:if>
   </xsl:template>

   <!--
     -  Negates comparisons below:  a NOT comp b
     -->
   <xsl:template match="*[@xsi:type='inverseSearchType'] | 
                        *[substring-after(@xsi:type,':')='inverseSearchType']">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:text>NOT </xsl:text>
         <xsl:apply-templates select="*"/>
      </xsl:if>
   </xsl:template>

   <!--
     -  Like comparison:  a LIKE b
     -->
   <xsl:template match="*[@xsi:type='likePredType'] | 
                        *[substring-after(@xsi:type,':')='likePredType']">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:apply-templates select="ad:Arg"/>
         <xsl:text> LIKE </xsl:text>
         <xsl:apply-templates select="ad:Pattern/ad:Literal"/>
      </xsl:if>
   </xsl:template>

   <!--
     -  NotLike comparison:  a NOT LIKE b
     -->
   <xsl:template match="*[@xsi:type='notLikePredType'] | 
                        *[substring-after(@xsi:type,':')='notLikePredType']">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:apply-templates select="ad:Arg"/>
         <xsl:text> NOT LIKE </xsl:text>
         <xsl:apply-templates select="ad:Pattern/ad:Literal"/>
      </xsl:if>
   </xsl:template>

   <!--
     -  Between comparison:  
        a BETWEEN b AND c, 
     -->
   <xsl:template match="*[@xsi:type='betweenPredType'] | 
                        *[substring-after(@xsi:type,':')='betweenPredType']">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:apply-templates select="*[1]"/>
         <xsl:text> BETWEEN </xsl:text>
         <xsl:apply-templates select="*[2]"/>
         <xsl:text> AND </xsl:text>
         <xsl:apply-templates select="*[3]"/>
      </xsl:if>
   </xsl:template>

   <!--
     -  NotBetween comparison:  
          a NOT BETWEEN b AND c, 
     -->
   <xsl:template match="*[@xsi:type='notBetweenPredType'] | 
                        *[substring-after(@xsi:type,':')='notBetweenPredType']">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:apply-templates select="*[1]"/>
         <xsl:text> NOT BETWEEN </xsl:text>
         <xsl:apply-templates select="*[2]"/>
         <xsl:text> AND </xsl:text>
         <xsl:apply-templates select="*[3]"/>
      </xsl:if>
   </xsl:template>

   <!--
     -  Closed (a)
     -->
   <xsl:template match="*[@xsi:type='closedSearchType'] | 
                        *[substring-after(@xsi:type,':')='closedSearchType']">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:text>(</xsl:text>
         <xsl:apply-templates select="*"/>
         <xsl:text>)</xsl:text>
      </xsl:if>
   </xsl:template>

   <!-- Where Template -->
   <xsl:template match="ad:Where">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:text> WHERE </xsl:text>
         <xsl:apply-templates select="ad:Condition"/>
      </xsl:if>
   </xsl:template>

   <!-- GroupBy Template -->
   <xsl:template match="ad:GroupBy">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:text> GROUP BY </xsl:text>

         <xsl:for-each select="ad:Column">
            <xsl:apply-templates select="."/>
            <xsl:if test="position()!=last()">, </xsl:if>
         </xsl:for-each>

      </xsl:if>
   </xsl:template>

   <!-- Having Template -->
   <xsl:template match="ad:Having">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:text> HAVING </xsl:text>
         <xsl:apply-templates select="*"/>
      </xsl:if>
   </xsl:template>

   <xsl:template match="ad:Column">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:value-of select="@Table"/>
         <xsl:text>.</xsl:text>
         <xsl:value-of select="@Name"/>
      </xsl:if>
   </xsl:template>

   <!-- scalarExpressionTypes -->

   <!--
     -  Table Columns (columnReferenceType)
     -->
   <xsl:template match="*[@xsi:type='columnReferenceType'] |
                       *[substring-after(@xsi:type,':')='columnReferenceType']">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:value-of select="@Table"/>
         <xsl:text>.</xsl:text>
         <xsl:value-of select="@Name"/>
      </xsl:if>
   </xsl:template>

   <!-- 
     -  Unary Operation
     -->
   <xsl:template match="*[@xsi:type='unaryExprType'] | 
                        *[substring-after(@xsi:type,':')='unaryExprType']">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:apply-templates select="ad:Arg"/>
         <xsl:text> </xsl:text>
         <xsl:value-of select="@Oper"/>
         <xsl:text> </xsl:text>
      </xsl:if>
   </xsl:template>

   <!--
     -  Binary Operation
     -->
   <xsl:template match="*[@xsi:type='binaryExprType'] | 
                        *[substring-after(@xsi:type,':')='binaryExprType']">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:apply-templates select="ad:Arg[1]"/>
         <xsl:text> </xsl:text>
         <xsl:value-of select="@Oper"/>
         <xsl:text> </xsl:text>
         <xsl:apply-templates select="ad:Arg[2]"/>
      </xsl:if>
   </xsl:template>

   <!--
     -  Atom Expression
     -->
   <xsl:template match="*[@xsi:type='atomType'] | 
                        *[substring-after(@xsi:type,':')='atomType']">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:apply-templates select="*"/>
      </xsl:if>
   </xsl:template>

   <!--
     -  Closed (a)
     -->
   <xsl:template match="*[@xsi:type='closedExprType'] | 
                        *[substring-after(@xsi:type,':')='closedExprType']">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:text>(</xsl:text>
         <xsl:apply-templates select="*"/>
         <xsl:text>)</xsl:text>
      </xsl:if>
   </xsl:template>

   <!--
     -  Function Expression
     -->
   <xsl:template match="ad:Function">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:value-of select="*[1]"/>
         <xsl:text>(</xsl:text>
         <xsl:choose>
            <xsl:when test="ad:Allow[position()=2]">
               <xsl:apply-templates select="*[2]/@Option"/>
               <xsl:text> </xsl:text>
               <xsl:apply-templates select="*[3]"/>
            </xsl:when>
            <xsl:otherwise>
               <xsl:apply-templates select="*[2]"/>
            </xsl:otherwise>
         </xsl:choose>
         <xsl:text>)</xsl:text>
      </xsl:if>
   </xsl:template>

   <!--
     -  Trigonometric Function Expression
     -->
   <xsl:template match="*[@xsi:type = 'trigonometricFunctionType'] | 
                 *[substring-after(@xsi:type,':')='trigonometricFunctionType']">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:value-of select="@Name"/>
         <xsl:text>(</xsl:text>
         <xsl:apply-templates select="*"/>
         <xsl:text>)</xsl:text>
      </xsl:if>
   </xsl:template>

   <!--
     -  Math Function Expression
     -->
   <xsl:template match="*[@xsi:type = 'mathFunctionType'] | 
                        *[substring-after(@xsi:type,':')='mathFunctionType']">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:value-of select="@Name"/>
         <xsl:text>(</xsl:text>
         <xsl:apply-templates select="*"/>
         <xsl:text>)</xsl:text>
      </xsl:if>
   </xsl:template>

   <!--
     - Aggregate Function Expression
     -->
   <xsl:template match="*[@xsi:type = 'aggregateFunctionType'] | 
                     *[substring-after(@xsi:type,':')='aggregateFunctionType']">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:value-of select="@Name"/>
         <xsl:text>(</xsl:text>
         <xsl:apply-templates select="*"/>
         <xsl:text>)</xsl:text>
      </xsl:if>
   </xsl:template>

   <!--
     -    Literal Values
     -->
   <xsl:template match="*[@xsi:type='integerType'] | 
                        *[substring-after(@xsi:type,':')='integerType']">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:value-of select="@Value"/>
      </xsl:if>
   </xsl:template>

   <xsl:template match="*[@xsi:type='realType'] | 
                        *[substring-after(@xsi:type,':')='realType']">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:value-of select="@Value"/>
      </xsl:if>
   </xsl:template>

   <xsl:template match="*[@xsi:type='stringType'] | 
                        *[substring-after(@xsi:type,':')='stringType']">
      <xsl:if test="not(@xsi:nil='true')">
         <xsl:text>'</xsl:text>
         <xsl:value-of select="@Value"/>
         <xsl:text>'</xsl:text>
      </xsl:if>
   </xsl:template>

</xsl:stylesheet>
