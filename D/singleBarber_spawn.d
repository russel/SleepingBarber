//  This is a model of the "The Sleeping Barber" problem using D (http://d-programming-language.org/),
 //  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2010–2012 Russel Winder

//  D implements actors when creating spawned processes, i.e. each process has a single message queue on
//  which receive or receiveOnly can be called. Here, Customers are passed as message between the processes.
//  However as each process has only a single message queue we have to realize case classes so as to know
//  which Customer came from where.

//  The barber sleeping is modeled by the barber actor using a blocking read on its message queue.  The
//  barber seats are modeled by the barber actor message queue so the shop actor is responsible for tracking
//  the number of customers sent to the barber actor.  The world actor only captures customers leaving the
//  shop, customers are fed into the shop by the main thread until it sends in the "close up" message.  The
//  main thread then terminates leaving the actors to continue until each self-destructs.

import std.concurrency ;
import std.random ;
import std.stdio ;

import core.thread ;

//  There must be a way of making these value types using immutable and have the sends work.

/*immutable*/ struct Customer { int id ; }
/*immutable*/ struct SuccessfulCustomer { Customer customer ; }
immutable struct ShopClosing { }
immutable struct ClockedOut { }

void barber ( immutable ( Duration ) function ( ) hairTrimTime , Tid shop ) {
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
             ( ShopClosing x ) {
               writeln ( "Barber : Work over for the day, trimmed " , customersTrimmed , "." ) ;
               shop.send ( ClockedOut ( ) ) ;
               running = false ;
             } ,
             ( OwnerTerminated x ) { }
             ) ;
  }
}

void shop ( immutable ( int ) numberOfSeats , immutable ( Duration ) function ( ) hairTrimTime , Tid world  ) {
  auto customersTrimmed = 0 ;
  auto customersTurnedAway = 0 ;
  auto barber = spawn ( & barber , hairTrimTime , thisTid ) ;
  setMaxMailboxSize ( barber , numberOfSeats , OnCrowding.throwException ) ;
  for ( auto running = true ; running ; ) {
    receive (
             ( Customer customer ) {
               try {
                 barber.send ( customer ) ;
                 writeln ( "Shop : Customer " , customer.id , " takes a seat. " ) ;
               }
               catch ( MailboxFull mf ) {
                 ++customersTurnedAway ;
                 writeln ( "Shop : Customer " , customer.id , " turned away." ) ;
                 world.send ( customer ) ;
               }
             } ,
             ( SuccessfulCustomer successfulCustomer ) {
               ++customersTrimmed ;
               writeln ( "Shop : Customer " , successfulCustomer.customer.id , " leaving trimmed." ) ;
               world.send ( successfulCustomer ) ;
             } ,
             ( ShopClosing message ) {
               // It is imperative we can append this metadata to the baber mailbox asynchronously and
               // without error. To protect against the mailbox already being full we extend it by one.
               // This has no effect on the algorithm since this is termination time.
               setMaxMailboxSize ( barber , numberOfSeats + 1 , OnCrowding.block ) ;
               barber.send ( message ) ;
             } ,
             ( ClockedOut message ) {
               writeln ( "Shop : Closing — " , customersTrimmed , " trimmed and " , customersTurnedAway , " turned away." ) ;
               world.send ( message ) ;
               running = false ;
             } ,
             ( OwnerTerminated x ) { }
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
             ( ClockedOut x ) { running = false ; } ,
             ( OwnerTerminated x ) { }
             ) ;
  }
  writeln ( "\nTrimmed " , customersTrimmed , " and turned away " , customersTurnedAway , " today." ) ;
}


void runSimulation (
                    immutable ( int ) numberOfCustomers ,
                    immutable ( int ) numberOfSeats ,
                    immutable ( Duration ) function ( ) hairTrimTime ,
                    immutable ( Duration ) function ( )  nextCustomerWaitTime ) {
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
                  function immutable ( Duration ) ( ) { return dur ! ( "msecs" ) ( uniform ( 0 , 6 ) + 1 ) ; } ,
                  function immutable ( Duration ) ( ) { return dur ! ( "msecs" ) ( uniform ( 0 , 2 ) + 1 ) ; }
                  ) ;
}
