/*
*+
*  Name:
*     jniast.h

*  Purpose:
*     Declarations for JNI implementation of AST error module.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     18-SEP-2001 (MBT)
*        Original version.
*-
*/

#ifndef ERR_JNIAST_DEFINED
#define ERR_JNIAST_DEFINED

/* Public function prototypes. */
void astPutErr_( int status, const char *message );

/* Package function prototypes. */
int jniastErrInit();
void jniastClearErrMsg();
const char *jniastGetErrMsg();

#endif  /* ERR_JNIAST_DEFINED */
