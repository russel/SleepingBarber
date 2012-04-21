/*
 *  This is a model of the "The Sleeping Barber" problem using C++ and Anthoy Williams' Just::Thread Pro
 *  (http://www.stdthread.co.uk/pro/) actor implementation over C++11 threads,
 *  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
 *
 *  Copyright © 2011–2012 Russel Winder
 */

#include <iostream>

#include <jss/actor.hpp>

struct customer {
    const unsigned id ;
    explicit customer ( unsigned idI ) : id ( idI ) { }
} ;

struct successfulCustomer {
  const customer c ;
  explicit successfulCustomer ( customer cI ) : c ( cI ) { }
} ;

int runSimulation ( const int numberOfCustomers , const int numberOfWaitingSeats ,
                    std::chrono::milliseconds hairTrimTime ( ) ,
                    std::chrono::milliseconds customerDelayTime ( ) ) {
  jss::actor * world ;
  jss::actor * shop ;
  jss::actor * barber =
   new jss::actor (
                   [ & ] ( ) {
                     bool running = true ;
                     unsigned customersTrimmed = 0 ;
                     while ( running ) {
                       jss::actor::receive ( )
                        .match<customer> (
                                          [ & ] ( customer c ) {
                                            std::cout << "Barber : Starting Customer " << c.id << std::endl ;
                                            std::this_thread::sleep_for ( hairTrimTime ( ) ) ;
                                            ++customersTrimmed ;
                                            std::cout << "Barber : Finished Customer " << c.id << std::endl ;
                                            shop->send ( successfulCustomer ( c ) ) ;
                                          } )
                        .match<std::string> (
                                             [ & ] ( std::string s ) {
                                               //  How to assert s == "closing" ?
                                               std::cout << "Barber : Knocking off time, " << customersTrimmed << " customers trimmed." << std::endl ;
                                               shop->send ( std::string ( "clockedOff" ) ) ;
                                               running = false ;
                                             }
                                             ) ;
                     } } ) ;

  shop =
   new jss::actor ( 
                   [ & ] ( ) {
                     unsigned seatsFilled = 0 ;
                     unsigned customersTrimmed = 0 ;
                     unsigned customersTurnedAway = 0 ;
                     bool running = true ;
                     while ( running ) {
                       jss::actor::receive ( )
                        .match<customer> (
                                          [ & ] ( customer c ) {
                                            if ( seatsFilled >= numberOfWaitingSeats ) {
                                              ++customersTurnedAway ;
                                              std::cout << "Shop : Customer " << c.id << " turned away." << std::endl ;
                                              world->send ( c ) ;
                                            }
                                            else {
                                              ++seatsFilled ;
                                              std::cout << "Shop : Customer " << c.id << " takes a seat. " << seatsFilled << " in use." << std::endl ;
                                              barber->send ( c ) ;
                                            }
                                          } )
                        .match<successfulCustomer> (
                                                    [ & ] ( successfulCustomer s ) {
                                                      --seatsFilled ;
                                                      ++customersTrimmed ;
                                                      std::cout << "Shop : Customer " << s.c.id << " leaving trimmed." << std::endl ;
                                                      world->send ( s ) ;
                                                    } )
                        .match<std::string> (
                                             [ & ] ( std::string s ) {
                                               if ( s == std::string ( "closing" ) ) {
                                                 barber->send ( std::string ( "closing" ) ) ;
                                               }
                                               else if ( s == std::string ( "clockedOff" ) ) {
                                                 std::cout << "Shop : Closing, " <<  customersTrimmed << " trimmed and " << customersTurnedAway << " turned away." << std::endl ;
                                                 world->send ( std::string ( "closed" ) ) ;
                                                 running = false ;
                                               }
                                             } ) ; 
                     }
                   } ) ;
  
  world =
   new jss::actor (
                   [ & ] ( ) {
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
                     }
                   } ) ;
  
  
  for ( unsigned i = 0 ; i < numberOfCustomers ; ++i ) {
    std::this_thread::sleep_for ( customerDelayTime ( ) ) ;
    std::cout << "World : Customer " << i << " enters the shop." << std::endl ;
    shop->send ( customer ( i ) ) ;
  }
  std::cout << "World : No more customers, time to close the shop." << std::endl ;
  shop->send ( std::string ( "closing" ) ) ;
  // TODO : How do we know when the world has ended rather than just waiting a while and hoping?
  std::this_thread::sleep_for ( std::chrono::milliseconds ( 50 ) ) ;
}

int main ( ) {
  runSimulation ( 20 , 4 ,
                  [ ] ( ) { return std::chrono::milliseconds ( 1 + rand ( ) % 6 ) ; } ,
                  [ ] ( ) { return std::chrono::milliseconds ( 1 + rand ( ) % 2 ) ; }
                  ) ;                  
  return 0 ;  
}

                      
