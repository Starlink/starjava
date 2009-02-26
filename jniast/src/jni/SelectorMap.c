/*
*+
*  Name:
*     SelectorMap.c

*  Purpose:
*     JNI implementations of native methods of SelectorMap class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor

*  History:
*     20-FEB-2009 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_SelectorMap.h"


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_SelectorMap_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobjectArray jRegs,   /* Region array */
   jdouble badval        /* Bad value */
) {
   AstPointer pointer;
   AstObject **astObjs = NULL;
   AstRegion **regs = NULL;
   jobject jReg;
   AstPointer regPointer;
   int i;
   int j;

   if ( jniastCheckNotNull( env, jRegs ) ) {
      int nreg = (*env)->GetArrayLength( env, jRegs );
      astObjs = jniastMalloc( env, ( nreg + 1 ) * sizeof( AstObject * ) );
      regs = jniastMalloc( env, nreg * sizeof( AstRegion * ) );
      if ( astObjs && regs ) {
         j = 0;
         for ( i = 0; i < nreg; i++ ) {
            jReg = (*env)->GetObjectArrayElement( env, jRegs, i );
            if ( jReg != NULL ) {
               regPointer = jniastGetPointerField( env, jReg );
               regs[ i ] = regPointer.Region;
               astObjs[ j++ ] = regPointer.AstObject;
            }
            else {
               regs[ i ] = NULL;
            }
         }
         astObjs[ j ] = NULL;
         THASTCALL( astObjs,
            pointer.SelectorMap =
                astSelectorMap( nreg, (void **) regs, badval, "" );
         )
         jniastInitObject( env, this, pointer );
      }
      free( regs );
   }
}
