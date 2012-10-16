# -*- mode:python; coding:utf-8; -*-

import os

dEnvironment = {
    'dmd': Environment(tools=['link', 'dmd'], # Why is the order crucial here?
                       DFLAGS=['-O', '-release'],
                       #DC='gdmd'
                       ),
    'gdc':  Environment(tools=['link', 'gdc'], # Why is the order crucial here?
                        DFLAGS=['-O3'],
                        ),
    'ldc': Environment(tools=['link', 'ldc'],
                       ENV = os.environ,
                       DFLAGS=['-O', '-release'],
                       ),
                       }['ldc']

for item in Glob ( '*.d' ) :
    name , extension = os.path.splitext ( item.name )
    dEnvironment.Command ( 'run_' + name , dEnvironment.Program ( item ) , './$SOURCE' )

################################################################################

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

################################################################################

javaEnvironment = Environment ( tools = [ 'javac' ] ,
                                JAVACFLAGS = [ '-encoding' , 'utf-8' ] ,
                                )
for  item in Glob ( '*.java' ) :
    className , extension = os.path.splitext ( item.name )
    javaEnvironment.Command ( 'run_' + className , javaEnvironment.Java ( '.' , item ) , 'java ' + className )

################################################################################

scalaEnvironment = Environment ( tools = [ ] , ENV = os.environ )

for item in Glob ( '*.scala' ) :
    className , extension = os.path.splitext ( item.name )
    scalaEnvironment.Command ( 'run_' + className , scalaEnvironment.Command  ( className + '.class' , item.name , 'scalac -optimise ' + item.name ) , 'scala ' + className )

#  The Java support and the above Scala commands so not put all the generated class files into the DAG and
#  so they do not get automatically removed on a clean.  So we have to hack it :-(

Clean ( '.' , Glob ( '*.class' ) )

################################################################################

goEnvironment = Environment ( tools = [ ] , ENV = os.environ )

for item in Glob ( '*.go' ) :
    name , extension = os.path.splitext ( item.name )
    goEnvironment.Command ( 'run_' + name , goEnvironment.Command ( name , item , 'go build $SOURCE' ) , './$SOURCE' )
