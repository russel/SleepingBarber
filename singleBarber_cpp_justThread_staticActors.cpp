/*
 *  This is a model of the "The Sleeping Barber" problem using C++ and Anthoy Williams' Just::Thread Pro
 *  (http://www.stdthread.co.uk/pro/) actor implementation over C++11 threads,
 *  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
 *
 *  Copyright © 2011–2012 Russel Winder
 */

#include <iostream>

#include <jss/actor.hpp>

const unsigned numberOfCustomers = 20 ;
const unsigned numberOfWaitingSeats = 4 ;

std::chrono::milliseconds hairTrimTime ( ) {
  return  std::chrono::milliseconds ( 1 + rand ( ) % 6 ) ;
}

std::chrono::milliseconds customerDelayTime ( ) {
  return  std::chrono::milliseconds ( 1 + rand ( ) % 2 ) ;
}

struct customer {
    const unsigned id ;
    explicit customer ( unsigned idI ) : id ( idI ) { }
} ;

struct successfulCustomer {
  const customer c ;
  explicit successfulCustomer ( customer cI ) : c ( cI ) { }
} ;

extern jss::actor world ;
extern jss::actor shop ;

jss::actor barber ( [ ] ( ) {
    bool running = true ;
    unsigned customersTrimmed = 0 ;
    while ( running ) {
      jss::actor::receive ( )
       .match<customer> ( [ & ] ( customer c ) {
           std::cout << "Barber : Starting Customer " << c.id << std::endl ;
           std::this_thread::sleep_for ( hairTrimTime ( ) ) ;
           ++customersTrimmed ;
           std::cout << "Barber : Finished Customer " << c.id << std::endl ;
           shop.send ( successfulCustomer ( c ) ) ;
         } )
       .match<std::string> ( [ & ] ( std::string s ) {
           //  How to assert s == "closing" ?
           std::cout << "Barber : Knocking off time, " << customersTrimmed << " customers trimmed." << std::endl ;
           shop.send ( std::string ( "clockedOff" ) ) ;
           running = false ;
         }
         ) ;
    } } ) ;

jss::actor shop ( [ ] ( ) {
    unsigned seatsFilled = 0 ;
    unsigned customersTrimmed = 0 ;
    unsigned customersTurnedAway = 0 ;
    bool running = true ;
    while ( running ) {
      jss::actor::receive ( )
       .match<customer> ( [ & ] ( customer c ) {
           if ( seatsFilled >= numberOfWaitingSeats ) {
             ++customersTurnedAway ;
             std::cout << "Shop : Customer " << c.id << " turned away." << std::endl ;
             world.send ( c ) ;
           } else {
             ++seatsFilled ;
             std::cout << "Shop : Customer " << c.id << " takes a seat. " << seatsFilled << " in use." << std::endl ;
             barber.send ( c ) ;
           }
         } )
       .match<successfulCustomer> ( [ & ] ( successfulCustomer s ) {
           --seatsFilled ;
           ++customersTrimmed ;
           std::cout << "Shop : Customer " << s.c.id << " leaving trimmed." << std::endl ;
           world.send ( s ) ;
         } )
       .match<std::string> ( [ & ] ( std::string s ) {
           if ( s == std::string ( "closing" ) ) {
             barber.send ( std::string ( "closing" ) ) ;
           } else if ( s == std::string ( "clockedOff" ) ) {
             std::cout << "Shop : Closing, " <<  customersTrimmed << " trimmed and " << customersTurnedAway << " turned away." << std::endl ;
             world.send ( std::string ( "closed" ) ) ;
             running = false ;
           }
         } ) ; 
    } } ) ;

jss::actor world ( [ ] ( ) {
    unsigned customersTurnedAway = 0 ;
    unsigned customersTrimmed = 0 ;
    bool running = true ;
    while ( running ) {
      jss::actor::receive ( )
       .match<customer> ( [ & ] ( customer c ) {
           ++customersTurnedAway ;
           std::cout << "World : Customer " << c.id << " exits the shop." << std::endl ;
         } )
       .match<successfulCustomer> ( [ & ] ( successfulCustomer s ) {
           ++customersTrimmed ;
           std::cout << "World : Customer " << s.c.id << " exits the shop." << std::endl ;
         } )
       .match<std::string> ( [ & ] ( std::string s ) {
           // How to assert s == "closed" ?
           running = false ;
           std::cout << "\nTrimmed " << customersTrimmed << " and turned away " << customersTurnedAway << " today." << std::endl ;
         } ) ;
    } } ) ;

int main ( ) {
  for ( unsigned i = 0 ; i < numberOfCustomers ; ++i ) {
    std::this_thread::sleep_for ( customerDelayTime ( ) ) ;
    std::cout << "World : Customer " << i << " enters the shop." << std::endl ;
    shop.send ( customer ( i ) ) ;
  }
  std::cout << "World : No more customers, time to close the shop." << std::endl ;
  shop.send ( std::string ( "closing" ) ) ;
  return 0 ;
}

