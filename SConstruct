# -*- mode:python; coding:utf-8; -*-

# SCons build file for the Sleeping Barber code. 

import os

#goEnvironment = Environment ( tools = [ 'go' ] )
#goEnvironment.GoTarget ( os.environ['GOOS'] , os.environ['GOARCH'] ) 
#for item in Glob ( '*.go' ) :
#    name , extension = os.path.splitext ( item.name )
#    goEnvironment.Command ( 'run_' + name , goEnvironment.GoProgram ( os.path.splitext ( item.name ) [0] ,  item ) , './$SOURCE' )

#  The dmd tool fails to set up the environment correctly to do linking on Ubuntu unless there is a compiler
#  tool specified in order to determine the linker AND the dmd tool is included after the link and compiler
#  tools. Also the dmd compiler is not in the bootstrap path.

dEnvironment = Environment (
    tools = [ 'gcc' , 'gnulink' , 'dmd_new' ] ,# NB dmd must follow gcc and gnulink.
    ENV = os.environ ,# dmd is not in the standard place.
    DFLAGS = [ '-O' , '-release' , '-inline' ] ,
   )
for item in Glob ( '*.d' ) :
    name , extension = os.path.splitext ( item.name )
    dEnvironment.Command ( 'run_' + name , dEnvironment.Program ( item ) , './$SOURCE' )
    #dEnvironment.Command ( 'run_' + name , dEnvironment.Command ( name , item , 'gdc -O3 -o $TARGET $SOURCE' ) , './$SOURCE' )

justThreadPro_home = os.environ['HOME'] + '/lib.Linux.x86_64/JustThreadPro'
cppEnvironment = Environment (
    tools = [ 'g++' , 'gnulink' ] ,
    CPPPATH = [ justThreadPro_home + '/include' ] ,
    CXXFLAGS = [ '-std=c++0x' ] ,
    LINKFLAGS = [ '-static' ] ,
    LIBPATH = [ justThreadPro_home + '/libs' ] ,
    LIBS = [ 'justthread' , 'pthread' , 'rt' ] ,
    )
for item in Glob ( '*.cpp' ) :
    name , extension = os.path.splitext ( item.name )
    cppEnvironment.Command ( 'run_' + name , cppEnvironment.Program ( item ) , './$SOURCE' ) 

javaEnvironment = Environment ( tools = [ 'javac' ] ,
                                JAVACFLAGS = [ '-encoding' , 'utf-8' ] ,
                                )
for  item in Glob ( '*.java' ) :
    className , extension = os.path.splitext ( item.name )
    javaEnvironment.Command ( 'run_' + className , javaEnvironment.Java ( '.' , item ) , 'java ' + className ) 


scalaEnvironment = Environment ( tools = [ ] , ENV = os.environ )

for item in Glob ( '*.scala' ) :
    className , extension = os.path.splitext ( item.name )
    scalaEnvironment.Command ( 'run_' + className , scalaEnvironment.Command  ( className + '.class' , item.name , 'scalac -optimise ' + item.name ) , 'scala ' + className )

#  The Java support and the above Scala commands so not put all the generated class files into the DAG and
#  so they do not get automatically removed on a clean.  So we have to hack it :-(

Clean ( '.' , Glob ( '*.class' ) )


goEnvironment = Environment ( tools = [ ] , ENV = os.environ )

for item in Glob ( '*.go' ) :
    name , extension = os.path.splitext ( item.name )
    goEnvironment.Command ( 'run_' + name , goEnvironment.Command ( name , item , 'go build $SOURCE' ) , './$SOURCE' ) 
