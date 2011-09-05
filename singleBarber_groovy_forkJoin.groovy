#! /usr/bin/env groovy

//  This is a model of the "The Sleeping Barber" problem using Groovy (http://groovy.codehaus.org) with the
//  JSR166y stuff and Java 6 to emulate Java 7, cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2011 Russel Winder

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Callable
import jsr166y.ForkJoinPool

import groovy.transform.Immutable

@Immutable class Customer { Integer id }

def runSimulation ( final int numberOfCustomers , final int numberOfWaitingSeats ,
                    final Closure hairTrimTime , final Closure nextCustomerWaitTime ) {
  final pool = new ForkJoinPool ( )
  final waitingChairs = new ArrayBlockingQueue<Customer> ( numberOfWaitingSeats )
  final barber = pool.submit ( new Callable<Integer> ( ) {
                                 private customersTrimmed = 0
                                 @Override public Integer call ( ) {
                                   while ( true ) {
                                     def customer = waitingChairs.take ( )
                                     assert customer instanceof Customer
                                     if ( customer.id < 0 ) break
                                     println ( "Barber : Starting Customer ${customer.id}." )
                                     Thread.sleep ( hairTrimTime ( ) )
                                     println ( "Barber : Finished Customer ${customer.id}." )
                                     ++customersTrimmed
                                     println ( "Shop : Customer ${customer.id} leaving trimmed." )
                                   }
                                   return customersTrimmed
                                 }
                               } ) ;
  def customersTurnedAway = 0
  for ( number in 0 ..< numberOfCustomers ) {
    Thread.sleep ( nextCustomerWaitTime ( ) )
    println ( "World : Customer ${number} enters the shop." )
    final customer = new Customer ( number )
    if ( waitingChairs.offer ( customer ) ) {
      println ( "Shop : Customer ${customer.id} takes a seat. ${waitingChairs.size ( )} in use." )
    }
    else {
      ++customersTurnedAway
      println ( "Shop : Customer ${customer.id} turned away." )
    }
  }
  waitingChairs.put ( new Customer ( -1 ) )
  println ( "\nTrimmed ${barber.join ( )} and turned away ${customersTurnedAway} today." )
  pool.shutdown ( )
}

runSimulation ( 20 , 4 , { ( Math.random ( ) * 60 + 10 ) as int }, { ( Math.random ( ) * 20 + 10 ) as int } )
