//  This is a model of the "The Sleeping Barber" problem using D (http://d-programming-language.org/),
 //  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2010--2011 Russel Winder

//  D implements Actor Model when creating spawned processes, i.e. each process has a single message queue
//  on which receive or receiveOnly can be called. Customers are passed as message between the processes.
//  However as each process has only a single message queue we have to realize case classes.

import std.concurrency ;
import std.random ;
import std.stdio ;

import core.thread ;

/*immutable*/ struct Customer { int id ; }
/*immutable*/ struct SuccessfulCustomer { Customer customer ; }

void barber ( immutable ( int ) function ( ) hairTrimTime , Tid shop ) {
  auto running = true ;
  while ( running ) {
    receive (
             ( Customer customer ) {
               writeln ( "Barber : Starting Customer " , customer.id ) ;
               Thread.sleep ( hairTrimTime ( ) ) ;
               writeln ( "Barber : Finished Customer " , customer.id ) ;
               shop.send ( SuccessfulCustomer ( customer ) ) ;
             } ,
             ( OwnerTerminated ) { running = false ; }
             ) ;
  }
}

void shop ( immutable ( int ) numberOfSeats , immutable ( int ) function ( ) hairTrimTime , Tid world  ) {
  auto seatsFilled = 0 ;
  auto running = true ;
  auto barber = spawn ( & barber , hairTrimTime , thisTid ) ;
  while ( running ) {
    receive (
             ( Customer customer ) {
               if ( seatsFilled < numberOfSeats ) {
                 ++seatsFilled ;
                 writeln ( "Shop : Customer " , customer.id , " takes a seat. " , seatsFilled , " in use." ) ;
                 barber.send ( customer ) ;
               }
               else {
                 writeln ( "Shop : Customer " , customer.id , " turned away." ) ;
                 world.send ( customer ) ;
               }
             } ,
             ( SuccessfulCustomer customer ) {
               --seatsFilled ;
               writeln ( "Shop : Customer " , customer.customer.id , " leaving trimmed." ) ;
               world.send ( customer ) ;
             } ,
             ( OwnerTerminated ) { running = false ; }
             ) ;
  }
}

void world ( immutable ( int ) numberOfCustomers , immutable ( int ) numberOfSeats ,
             immutable ( int ) function ( )  nextCustomerWaitTime , immutable ( int ) function ( ) hairTrimTime ) {
  auto shop = spawn ( & shop , numberOfSeats , hairTrimTime , thisTid ) ;
  for ( auto i = 0 ; i < numberOfCustomers ; ++i ) {
    Thread.sleep ( nextCustomerWaitTime ( ) ) ;
    writeln ( "World : Customer " , i , " enters the shop." ) ;
    shop.send ( Customer ( i ) ) ;
  }
  auto customersTurnedAway = 0 ;
  auto customersTrimmed = 0 ;
  while ( customersTrimmed + customersTurnedAway < numberOfCustomers ) {
    receive (
             ( Customer customer ) { ++customersTurnedAway ; } ,
             ( SuccessfulCustomer customer ) { ++customersTrimmed ; } ,
             ( OwnerTerminated ) { writeln ( "Call the police, the barber is Sweeney Todd." ) ; }
             ) ;
  }
  writeln ( "\nTrimmed " , customersTrimmed , " and turned away " , customersTurnedAway , " today.\n" ) ;
}

void main ( immutable string[] args ) {
  world ( 20 , 4 ,
          function immutable ( int ) ( ) { return uniform ( 0 , 20000 ) + 10000 ; } ,
          function immutable ( int ) ( ) { return uniform ( 0 , 60000 ) + 10000 ; }
          ) ;
}
