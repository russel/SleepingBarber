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

struct Customer {
  int id ;
  this ( int id ) { this.id = id ; }
}

struct SuccessfulCustomer {
  Customer customer ;
  this ( Customer customer ) { this.customer = customer ; }
}

void barber ( long function ( ) hairTrimTime ) {
  Tid shop ;
  auto running = true ;
  while ( running ) {
    receive (
             ( Tid tid ) { shop = tid ; } ,
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

void shop ( int numberOfSeats , Tid world , Tid barber ) {
  auto isOpen = true ;
  auto seatsFilled = 0 ;
  auto customersTurnedAway = 0 ;
  auto customersTrimmed = 0 ;
  auto running = true ;
  barber.send ( thisTid ) ;
  while ( running ) {
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
               }
             } ,
             ( SuccessfulCustomer customer ) {
               ++customersTrimmed ;
               --seatsFilled ;
               writeln ( "Shop : Customer " , customer.customer.id , " leaving trimmed." ) ;
               if ( ! isOpen && ( seatsFilled == 0 ) ) {
                 writeln ( "\nTrimmed " , customersTrimmed , " and turned away " , customersTurnedAway , " today.\n" ) ;
                 world.send ( "" ) ;
               }
             } ,
             ( string s ) { isOpen = false ; } ,
             ( OwnerTerminated ) { running = false ; }
             ) ;
  }
}

void world ( int numberOfCustomers , int numberOfSeats , long function ( )  nextCustomerWaitTime , long function ( ) hairTrimTime ) {
  auto barber = spawn ( & barber , hairTrimTime ) ;
  auto shop = spawn ( & shop , numberOfSeats , thisTid , barber ) ;
  for ( auto i = 0 ; i < numberOfCustomers ; ++i ) {
    Thread.sleep ( nextCustomerWaitTime ( ) ) ;
    writeln ( "World : Customer " , i , " enters the shop." ) ;
    shop.send ( Customer ( i ) ) ;
  }
  shop.send ( "" ) ;
  receiveOnly ! ( string ) ( ) ;
}

void main ( string[] args ) {
  world ( 20 , 4 ,
          function long ( ) { return ( cast ( long ) uniform ( 0.0 , 1.0 ) * 20000 ) + 10000 ; } ,
          function long ( ) { return ( cast ( long ) uniform ( 0.0 , 1.0 ) * 60000 ) + 10000 ; }
          ) ;
}
