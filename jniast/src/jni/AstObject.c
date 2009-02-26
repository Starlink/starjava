/*
*+
*  Name:
*     AstObject.c

*  Purpose:
*     JNI implementations of native methods of AstObject class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     18-SEP-2001 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include <stdlib.h>
#include <string.h>
#include "jni.h"
#include "ast.h"
#include "grf.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_AstObject.h"

#define BUFLENG 256

/* Class methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_AstObject_nativeInitialize(
   JNIEnv *env,          /* Interface pointer */
   jclass class          /* Class object */
) {
   jniastInitialize( env );
}

JNIEXPORT jint JNICALL Java_uk_ac_starlink_ast_AstObject_getAstConstantI(
   JNIEnv *env,          /* Interface pointer */
   jclass class,         /* Class object */
   jstring jName         /* Name of AST constant to retrieve */
) {
   const char *name;
   jint result;
   int success = 0;
   char namcopy[ BUFLENG ]; 

   name = jniastGetUTF( env, jName );
   if ( name != NULL ) {

#define TRY_CONST( Xname ) \
      if ( strcmp( #Xname, name ) == 0 ) { \
         result = (jint) Xname; \
         success = 1; \
      }
      if ( 0 ) {}

      /* Version identifiers. */
      else TRY_CONST( AST_MAJOR_VERS )
      else TRY_CONST( AST_MINOR_VERS )
      else TRY_CONST( AST_RELEASE )
      else TRY_CONST( JNIAST_MAJOR_VERS )
      else TRY_CONST( JNIAST_MINOR_VERS )
      else TRY_CONST( JNIAST_RELEASE )

      /* Tuning special. */
      else TRY_CONST( AST__TUNULL )

      /* Interpolation schemes. */
      else TRY_CONST( AST__NEAREST )
      else TRY_CONST( AST__LINEAR )
      else TRY_CONST( AST__SINC )
      else TRY_CONST( AST__SINCSINC )
      else TRY_CONST( AST__SINCCOS )
      else TRY_CONST( AST__SINCGAUSS )
      else TRY_CONST( AST__BLOCKAVE )
      else TRY_CONST( AST__UKERN1 )
      else TRY_CONST( AST__UINTERP )
      else TRY_CONST( AST__GAUSS )
      else TRY_CONST( AST__SOMB )
      else TRY_CONST( AST__SOMBCOS )

      /* Resampling flags. */
      else TRY_CONST( AST__NOBAD )
      else TRY_CONST( AST__USEBAD )
      else TRY_CONST( AST__CONSERVEFLUX )

      /* Symbolic frame numbers. */
      else TRY_CONST( AST__BASE )
      else TRY_CONST( AST__CURRENT )
      else TRY_CONST( AST__NOFRAME )

      /* IntraMap flags. */
      else TRY_CONST( AST__NOFWD )
      else TRY_CONST( AST__NOINV )
      else TRY_CONST( AST__SIMPFI )
      else TRY_CONST( AST__SIMPIF )

      /* WcsMap projection types. */
      else TRY_CONST( AST__AZP )
      else TRY_CONST( AST__TAN )
      else TRY_CONST( AST__SIN )
      else TRY_CONST( AST__STG )
      else TRY_CONST( AST__ARC )
      else TRY_CONST( AST__ZPN )
      else TRY_CONST( AST__ZEA )
      else TRY_CONST( AST__AIR )
      else TRY_CONST( AST__CYP )
      else TRY_CONST( AST__CAR )
      else TRY_CONST( AST__MER )
      else TRY_CONST( AST__CEA )
      else TRY_CONST( AST__COP )
      else TRY_CONST( AST__COD )
      else TRY_CONST( AST__COE )
      else TRY_CONST( AST__COO )
      else TRY_CONST( AST__BON )
      else TRY_CONST( AST__PCO )
      else TRY_CONST( AST__GLS )
      else TRY_CONST( AST__SFL )
      else TRY_CONST( AST__PAR )
      else TRY_CONST( AST__AIT )
      else TRY_CONST( AST__MOL )
      else TRY_CONST( AST__CSC )
      else TRY_CONST( AST__QSC )
      else TRY_CONST( AST__NCP )
      else TRY_CONST( AST__TSC )
      else TRY_CONST( AST__TPN )
      else TRY_CONST( AST__SZP )
      else TRY_CONST( AST__WCSBAD )

      /* KeyMap entry types. */
      else TRY_CONST( AST__INTTYPE )
      else TRY_CONST( AST__DOUBLETYPE )
      else TRY_CONST( AST__STRINGTYPE )
      else TRY_CONST( AST__OBJECTTYPE )
      else TRY_CONST( AST__BADTYPE )

      /* Combination types used in CmpRegion (and elsewhere?). */
      else TRY_CONST( AST__AND )
      else TRY_CONST( AST__OR )

      /* GRF attribute types. */
      else TRY_CONST( GRF__STYLE )
      else TRY_CONST( GRF__WIDTH )
      else TRY_CONST( GRF__SIZE )
      else TRY_CONST( GRF__FONT )
      else TRY_CONST( GRF__COLOUR )

      /* GRF primitives. */
      else TRY_CONST( GRF__TEXT )
      else TRY_CONST( GRF__LINE )
      else TRY_CONST( GRF__MARK )

      /** GRF capabliities. */
      else TRY_CONST( GRF__ESC )
      else TRY_CONST( GRF__MJUST )
      else TRY_CONST( GRF__SCALES )

      /** GRF escape sequence codes. */
      else TRY_CONST( GRF__ESPER )
      else TRY_CONST( GRF__ESSUP )
      else TRY_CONST( GRF__ESSUB )
      else TRY_CONST( GRF__ESGAP )
      else TRY_CONST( GRF__ESBAC )
      else TRY_CONST( GRF__ESSIZ )
      else TRY_CONST( GRF__ESWID )
      else TRY_CONST( GRF__ESFON )
      else TRY_CONST( GRF__ESCOL )
      else TRY_CONST( GRF__ESSTY )
      else TRY_CONST( GRF__ESPOP )
      else TRY_CONST( GRF__ESPSH )

#undef TRY_CONST
   }

   if ( ! success ) {
      strncpy( namcopy, name, BUFLENG );
   }
   jniastReleaseUTF( env, jName, name );
   if ( ! success ) {
      jniastThrowIllegalArgumentException( env, 
            "There is no AST int constant called \"%s\"", namcopy );

   }
   return result;
}

JNIEXPORT jdouble JNICALL Java_uk_ac_starlink_ast_AstObject_getAstConstantD(
   JNIEnv *env,          /* Interface pointer */
   jclass class,         /* Class object */
   jstring jName         /* Name of AST constant to retrieve */
) {
   const char *name;
   jdouble result;
   int success = 0;
   char namcopy[ BUFLENG ];

   name = jniastGetUTF( env, jName );
   if ( name != NULL ) {

#define TRY_CONST( Xname ) \
      if ( strcmp( #Xname, name ) == 0 ) { \
         result = (jdouble) Xname; \
         success = 1; \
      }
      TRY_CONST( AST__BAD )

      /* SlaMap constants. */
      TRY_CONST( AST__AU )
      TRY_CONST( AST__SOLRAD )

#undef TRY_CONST
   }

   if ( ! success ) {
      strncpy( namcopy, name, BUFLENG - 1 );
   }
   jniastReleaseUTF( env, jName, name );
   if ( ! success ) {
      jniastThrowIllegalArgumentException( env, 
            "There is no AST double constant called \"%s\"", namcopy );
   }
   return result;
}

JNIEXPORT jstring JNICALL Java_uk_ac_starlink_ast_AstObject_getAstConstantC(
   JNIEnv *env,          /* Interface pointer */
   jclass class,         /* Class object */
   jstring jName         /* Name of AST constant to retrieve */
) {
   const char *name;
   jstring result = NULL;
   int success = 0;
   char namcopy[ BUFLENG ];

   name = jniastGetUTF( env, jName );
   if ( name != NULL ) {

#define TRY_CONST( Xname ) \
      if ( strcmp( #Xname, name ) == 0 ) { \
         result = (*env)->NewStringUTF( env, Xname ); \
         success = 1; \
      }
      TRY_CONST( AST__XMLNS )
      TRY_CONST( AST__STCNAME )
      TRY_CONST( AST__STCVALUE )
      TRY_CONST( AST__STCERROR )
      TRY_CONST( AST__STCRES )
      TRY_CONST( AST__STCSIZE )
      TRY_CONST( AST__STCPIXSZ )

#undef TRY_CONST
   }

   if ( ! success ) {
      strncpy( namcopy, name, BUFLENG - 1 );
   }
   jniastReleaseUTF( env, jName, name );
   if ( ! success ) {
      jniastThrowIllegalArgumentException( env,
            "There is no AST character constant called \"%s\"", namcopy );
   }
   return result;
}

JNIEXPORT jboolean JNICALL Java_uk_ac_starlink_ast_AstObject_isThreaded(
   JNIEnv *env,          /* Interface pointer */
   jclass class          /* Class object */
) {
   return JNIAST_THREADS ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_uk_ac_starlink_ast_AstObject_tune(
   JNIEnv *env,          /* Interface pointer */
   jclass class,         /* Class object */
   jstring jName,        /* Name of tuning parameter */
   jint jValue           /* Value of tuning parameter */
) {
   int oldVal;
   const char *name;

   name = jniastGetUTF( env, jName );
   ASTCALL(
      oldVal = astTune( name, (int) jValue );
   )
   jniastReleaseUTF( env, jName, name );
   return (jint) oldVal;
}


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_AstObject_annul(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   ASTCALL(
      astAnnul( pointer.AstObject );
   )
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_AstObject_delete(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );

   ASTCALL(
      astLock( pointer.AstObject, 0 );
      astDelete( pointer.AstObject );
   )
}

JNIEXPORT void JNICALL  Java_uk_ac_starlink_ast_AstObject_clear(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jAttrib       /* Attribute names to be cleared */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *attrib = jniastGetUTF( env, jAttrib );

   THASTCALL( jniastList( 1, pointer.AstObject ),
      astClear( pointer.AstObject, attrib );
   )
   jniastReleaseUTF( env, jAttrib, attrib );
}

JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_AstObject_clone(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstPointer newpointer;
   jobject newobj;

   newobj = (*env)->AllocObject( env, (*env)->GetObjectClass( env, this ) );
   THASTCALL( jniastList( 1, pointer.AstObject ),
      newpointer.AstObject = astClone( pointer.AstObject );
   )
   jniastInitObject( env, newobj, newpointer );
   return newobj;
}


JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_AstObject_copy(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstPointer newpointer;
   jobject newobj;

   newobj = (*env)->AllocObject( env, (*env)->GetObjectClass( env, this ) );
   THASTCALL( jniastList( 1, pointer.AstObject ),
      newpointer.AstObject = astCopy( pointer.AstObject );
   )
   jniastInitObject( env, newobj, newpointer );
   return newobj;
}

JNIEXPORT jstring JNICALL Java_uk_ac_starlink_ast_AstObject_getC(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jAttrib       /* Name of the character attribute */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *attrib = jniastGetUTF( env, jAttrib );
   jstring jValue = NULL;
   const char *value;

   THASTCALL( jniastList( 1, pointer.AstObject ),
      value = astGetC( pointer.AstObject, attrib );
   )
   jniastReleaseUTF( env, jAttrib, attrib );

   if ( ! (*env)->ExceptionCheck( env ) ) {
      jValue = (*env)->NewStringUTF( env, (const char *) value );
   }
   return jValue;
}

#define MAKE_ASTGETX(Xletter,Xjtype) \
JNIEXPORT Xjtype JNICALL Java_uk_ac_starlink_ast_AstObject_get##Xletter( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this,         /* Instance object */ \
   jstring jAttrib       /* Name of the attribute */ \
) { \
   AstPointer pointer = jniastGetPointerField( env, this ); \
   const char *attrib = jniastGetUTF( env, jAttrib ); \
   Xjtype value; \
 \
   THASTCALL( jniastList( 1, pointer.AstObject ), \
      value = (Xjtype) astGet##Xletter( pointer.AstObject, attrib ); \
   ) \
   jniastReleaseUTF( env, jAttrib, attrib ); \
   return value; \
}
MAKE_ASTGETX(D,jdouble)
MAKE_ASTGETX(F,jfloat)
MAKE_ASTGETX(I,jint)
MAKE_ASTGETX(L,jlong)
#undef MAKE_ASTGETX

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_AstObject_setC(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jAttrib,      /* Name of the character attribute */
   jstring jValue        /* The value to which it will be set */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *attrib = jniastGetUTF( env, jAttrib );
   const char *value = jniastGetUTF( env, jValue );

   THASTCALL( jniastList( 1, pointer.AstObject ),
      astSetC( pointer.AstObject, attrib, value );
   )
   jniastReleaseUTF( env, jAttrib, attrib );
   jniastReleaseUTF( env, jValue, value );
}

#define MAKE_ASTSETX(Xletter,Xjtype,Xtype) \
JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_AstObject_set##Xletter( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this,         /* Instance object */ \
   jstring jAttrib,      /* Name of the numeric attribute */ \
   Xjtype value          /* The value to which it will be set */ \
) { \
   AstPointer pointer = jniastGetPointerField( env, this ); \
   const char *attrib = jniastGetUTF( env, jAttrib ); \
 \
   THASTCALL( jniastList( 1, pointer.AstObject ), \
      astSet##Xletter( pointer.AstObject, attrib, (Xtype) value ); \
   ) \
   jniastReleaseUTF( env, jAttrib, attrib ); \
}
MAKE_ASTSETX(D,jdouble,double)
MAKE_ASTSETX(F,jfloat,float)
MAKE_ASTSETX(I,jint,int)
MAKE_ASTSETX(L,jlong,int)

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_AstObject_set(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jSettings     /* Name of the character attribute */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *settings;
   char *setbuf;

   if ( jniastCheckNotNull( env, jSettings ) ) {
      settings = jniastGetUTF( env, jSettings );
      setbuf = jniastEscapePercents( env, settings );
      jniastReleaseUTF( env, jSettings, settings );
      
      THASTCALL( jniastList( 1, pointer.AstObject ),
         astSet( pointer.AstObject, setbuf );
      )

      free( setbuf );
   }
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_AstObject_show(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );

   THASTCALL( jniastList( 1, pointer.AstObject ),
      astShow( pointer.AstObject );
   )
}

JNIEXPORT jboolean JNICALL Java_uk_ac_starlink_ast_AstObject_test(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jAttrib       /* Name of the attribute to be tested */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *attrib = jniastGetUTF( env, jAttrib );
   int result;

   THASTCALL( jniastList( 1, pointer.AstObject ),
      result = astTest( pointer.AstObject, attrib );
   )
   jniastReleaseUTF( env, jAttrib, attrib );
   return result ? JNI_TRUE : JNI_FALSE;
}


/* $Id$ */
