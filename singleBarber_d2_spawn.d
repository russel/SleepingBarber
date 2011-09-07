//  This is a model of the "The Sleeping Barber" problem using D (http://d-programming-language.org/),
 //  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2010–2011 Russel Winder

//  D implements Actor Model when creating spawned processes, i.e. each process has a single message queue
//  on which receive or receiveOnly can be called. Customers are passed as message between the processes.
//  However as each process has only a single message queue we have to realize case classes.

//  The barber sleeping is modeled by the barber actor using a blocking read on its message queue.  The
//  barber seats are modeled by the barber actor message queue so the shop actor is responsible for tracking
//  the number of customers sent to the barber actor.  The world actor only captures customers leaving the
//  shop, customers are fed into the shop by the main thread until it sends in the "close up" message.  The
//  main thread then terminates leaving the actors to continue until each self-destructs.

import std.concurrency ;
import std.random ;
import std.stdio ;

import core.thread ;

//  There must be a way of making these value types using immutable without creating an Error -11 at run
//  time.

struct Customer { int id ; }
struct SuccessfulCustomer { Customer customer ; }
struct ShopClosing { }
struct ClockedOut { }

void barber ( immutable ( int ) function ( ) hairTrimTime , Tid shop ) {
  auto customersTrimmed = 0 ;
  for ( auto running = true ; running ; ) {
    receive (
             ( Customer customer ) {
               writeln ( "Barber : Starting Customer " , customer.id ) ;
               Thread.sleep ( hairTrimTime ( ) ) ;
               writeln ( "Barber : Finished Customer " , customer.id ) ;
               ++customersTrimmed ;
               shop.send ( SuccessfulCustomer ( customer ) ) ;
             } ,
             ( ShopClosing ) {
               writeln ( "Barber : Work over for the day, trimmed " , customersTrimmed , "." ) ;
               shop.send ( ClockedOut ( ) ) ;
               running = false ;
             } ,
             ( OwnerTerminated ) { }
             ) ;
  }
}

void shop ( immutable ( int ) numberOfSeats , immutable ( int ) function ( ) hairTrimTime , Tid world  ) {
  auto seatsFilled = 0 ;
  auto customersTrimmed = 0 ;
  auto customersTurnedAway = 0 ;
  auto barber = spawn ( & barber , hairTrimTime , thisTid ) ;
  for ( auto running = true ; running ; ) {
    receive (
             ( Customer customer ) {
               if ( seatsFilled < numberOfSeats ) {
                 ++seatsFilled ;
                 writeln ( "Shop : Customer " , customer.id , " takes a seat. " , seatsFilled , " in use." ) ;
                 barber.send ( customer ) ;
               }
               else {
                 ++customersTurnedAway ;
                 writeln ( "Shop : Customer " , customer.id , " turned away." ) ;
                 world.send ( customer ) ;
               }
             } ,
             ( SuccessfulCustomer successfulCustomer ) {
               --seatsFilled ;
               ++customersTrimmed ;
               writeln ( "Shop : Customer " , successfulCustomer.customer.id , " leaving trimmed." ) ;
               world.send ( successfulCustomer ) ;
             } ,
             ( ShopClosing message ) {
               barber.send ( message ) ;
             } ,
             ( ClockedOut message ) {
               writeln ( "Shop : Closing — " , customersTrimmed , " trimmed and " , customersTurnedAway , " turned away." ) ;
               world.send ( message ) ;
               running = false ;
             } ,
             ( OwnerTerminated ) { }
             ) ;
  }
}

void world ( immutable ( int ) numberOfCustomers ) {
  auto customersTurnedAway = 0 ;
  auto customersTrimmed = 0 ;
  auto message = function ( int id ) { writeln ( "World : Customer " , id , " exits the shop." ) ; } ;
  Tid shop ;
  for ( auto running = true ; running ; ) {
    receive (
             ( Tid t ) {
               assert ( shop == Tid.init ) ;
               shop = t ;
             } ,
             ( Customer customer ) {
               assert ( shop != Tid.init ) ;
               ++customersTurnedAway ;
               message ( customer.id ) ;
             } ,
             ( SuccessfulCustomer successfulCustomer ) {
               assert ( shop != Tid.init ) ;
               ++customersTrimmed ;
               message ( successfulCustomer.customer.id ) ;
             } ,
             ( ClockedOut ) { running = false ; } ,
             ( OwnerTerminated ) { }
             ) ;
  }
  writeln ( "\nTrimmed " , customersTrimmed , " and turned away " , customersTurnedAway , " today.\n" ) ;
}


void runSimulation ( immutable ( int ) numberOfCustomers , immutable ( int ) numberOfSeats ,
                     immutable ( int ) function ( ) hairTrimTime , immutable ( int ) function ( )  nextCustomerWaitTime ) {
  auto world = spawn ( & world , numberOfCustomers ) ;
  auto shop = spawn ( & shop , numberOfSeats , hairTrimTime , world ) ;
  world.send ( shop ) ;
  for ( auto i = 0 ; i < numberOfCustomers ; ++i ) {
    Thread.sleep ( nextCustomerWaitTime ( ) ) ;
    writeln ( "World : Customer " , i , " enters the shop." ) ;
    shop.send ( Customer ( i ) ) ;
  }
  shop.send ( ShopClosing ( ) ) ;
}

void main ( immutable string[] args ) {
  runSimulation ( 20 , 4 ,
                  function immutable ( int ) ( ) { return uniform ( 0 , 60000 ) + 10000 ; } ,
                   function immutable ( int ) ( ) { return uniform ( 0 , 20000 ) + 10000 ; }
                 ) ;
}
