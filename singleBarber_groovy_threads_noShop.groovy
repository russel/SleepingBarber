#! /usr/bin/env groovy

//  This is a model of the "The Sleeping Barber" problem using Groovy (http://groovy.codehaus.org) only,
//  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2010–2011 Russel Winder

import java.util.concurrent.ArrayBlockingQueue

import groovy.transform.Immutable

@Immutable class Customer { Integer id }

def runSimulation ( final int numberOfCustomers , final int numberOfWaitingSeats ,
                    final Closure hairTrimTime , final Closure nextCustomerWaitTime ) {
  final waitingChairs = new ArrayBlockingQueue<Customer> ( numberOfWaitingSeats )
  def customersTurnedAway = 0
  def customersTrimmed = 0
  final barber = new Runnable ( ) {
    private working = true
    public void stopWork ( ) { working = false }
    @Override public void run ( ) {
      while ( working || ( waitingChairs.size ( ) > 0 ) ) {
        //  Use take here to simulate barber sleeping if there are no customers by blocking.
        def customer = waitingChairs.take ( )
        assert customer instanceof Customer
        println ( "Barber : Starting Customer ${customer.id}." )
        Thread.sleep ( hairTrimTime ( ) )
        println ( "Barber : Finished Customer ${customer.id}." )
        ++customersTrimmed
        println ( "Shop : Customer ${customer.id} leaving trimmed." )
      }
      println ( 'Barber : Work over for the day.' )
    }
  }
  final barberThread = new Thread ( barber )
  barberThread.start ( )
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
  barber.stopWork ( )
  barberThread.join ( )
  println ( "\nTrimmed ${customersTrimmed} and turned away ${customersTurnedAway} today." )
}

runSimulation ( 20 , 4 , { ( Math.random ( ) * 60 + 10 ) as int }, { ( Math.random ( ) * 20 + 10 ) as int } )
