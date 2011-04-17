/*
 *  This is a model of the "The Sleeping Barber" problem using C++ and Anthoy Williams' Just::Thread Pro
 *  actor implementation over C++0x threads (http://www.stdthread.co.uk/pro/),
 *  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
 *
 *  Copyright © 2011 Russel Winder
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

extern jss::actor world ;
extern jss::actor shop ;

jss::actor barber (
                   [ ] ( ) {
                     bool running = true ;
                     while ( running ) {
                       jss::actor::receive ( )
                        .match<customer> (
                                          [ ] ( customer c ) {
                                            std::cout << "Barber : Starting Customer " << c.id << std::endl ;
                                            std::this_thread::sleep_for ( std::chrono::milliseconds ( 200 + rand ( ) % 200 ) ) ;
                                            std::cout << "Barber : Finished Customer " << c.id << std::endl ;
                                            shop.send ( successfulCustomer ( c ) ) ;
                                          } )
                       .match<std::string> (
                                            [ & ] ( std::string s ) { running = false ; }
                                            ) ;
                     } } ) ;

jss::actor shop (
                 [ ] ( ) {
                   unsigned seatsFilled = 0 ;
                   bool running = true ;
                   while ( running ) {
                     jss::actor::receive ( )
                      .match<customer> (
                                        [ & ] ( customer c ) {
                                          if ( seatsFilled >= 4 ) {
                                            std::cout << "Shop : Customer " << c.id << " turned away." << std::endl ;
                                            world.send ( c ) ;
                                          }
                                          ++seatsFilled ;
                                          std::cout << "Shop : Customer " << c.id << " takes a seat. " << seatsFilled << " in use." << std::endl ;
                                          barber.send ( c ) ;
                                        } )
                      .match<successfulCustomer> (
                                                  [ & ] ( successfulCustomer s ) {
                                                    --seatsFilled ;
                                                    std::cout << "Shop : Customer " << s.c.id << " leaving trimmed." << std::endl ;
                                                    world.send ( s ) ;
                                                  } )
                      .match<std::string> (
                                           [ & ] ( std::string s ) {
                                             running = false ;
                                             barber.send ( std::string ( "" ) ) ;
                                           }
                                           ) ; 
                   }
                 } ) ;

jss::actor world (
                  [ ] ( ) {
                    unsigned numberOfCustomers = 20 ;
                    for ( unsigned i = 0 ; i < numberOfCustomers ; ++i ) {
                      std::this_thread::sleep_for ( std::chrono::milliseconds ( 50 + rand ( ) % 250 ) ) ;
                      std::cout << "World : Customer " << i << " enters the shop." << std::endl ;
                      shop.send ( customer ( i ) ) ;
                    }
                    shop.send ( std::string ( "" ) ) ;
                    unsigned customersTurnedAway = 0 ;
                    unsigned customersTrimmed = 0 ;
                    while ( customersTrimmed + customersTurnedAway < numberOfCustomers ) {
                      jss::actor::receive ( )
                       .match<customer> ( [ & ] ( customer c ) { ++customersTurnedAway ; } )
                       .match<successfulCustomer> ( [ & ] ( successfulCustomer s ) { ++customersTrimmed ; } ) ;
                    }
                    std::cout << "\nTrimmed " << customersTrimmed << " and turned away " << customersTurnedAway << " today." << std::endl ;
                  } ) ;
                    
int main ( ) {
  return 0 ;
}
                      
