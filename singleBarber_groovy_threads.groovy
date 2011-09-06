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
  final toShop = new ArrayBlockingQueue<Customer> ( numberOfCustomers )
  final fromChair = new ArrayBlockingQueue<Customer> ( numberOfCustomers )
  final fromShop = new ArrayBlockingQueue<Customer> ( numberOfCustomers )
  def customersTurnedAway = 0
  def customersTrimmed = 0
  //  Must subclass Thread here rather than create a Runnable, so that we can execute an interrupt when
  //  closing the shop.
  final barber = new Thread ( ) {
    private working = true
    public void stopWork ( ) {
      working = false
      interrupt ( )
    }
    @Override public void run ( ) {
      while ( working || ( waitingChairs.size ( ) > 0 ) ) {
        //  Use take here to simulate barber sleeping if there are no customers by blocking.  It means we
        //  have to use an interrupt when the shop gets closed.
        try {
          def customer = waitingChairs.take ( )
          assert customer instanceof Customer
          println ( "Barber : Starting Customer ${customer.id}." )
          Thread.sleep ( hairTrimTime ( ) )
          println ( "Barber : Finished Customer ${customer.id}." )
          fromChair.put ( customer )
        }
        catch ( InterruptedException ie ) { /* Intentionally left blank. */ }
      }
      println ( 'Barber : Work over for the day.' )
    }
  }
  barber.start ( )
  final shop = new Runnable ( ) {
    private open = true ;
    public void closeShop ( ) { open = false }
    @Override public void run ( ) {
      while ( open || waitingChairs.size ( ) > 0 ) {
        def customer = toShop.poll ( )
        if ( customer ) {
          if ( waitingChairs.offer ( customer ) ) {
            println ( "Shop : Customer ${customer.id} takes a seat. ${waitingChairs.size ( )} in use." )
          }
          else {
            ++customersTurnedAway
            println ( "Shop : Customer ${customer.id} turned away." )
            fromShop.put ( customer )
          }
        }
        customer = fromChair.poll ( )
        if ( customer ) {
          ++customersTrimmed
          println ( "Shop : Customer ${customer.id} leaving trimmed." )
          fromShop.put ( customer )
        }
      }
      println ( 'Shop : Closing.' )
    }
  }
  final shopThread = new Thread ( shop )
  shopThread.start ( )
  for ( number in 0 ..< numberOfCustomers ) {
    Thread.sleep ( nextCustomerWaitTime ( ) )
    println ( "World : Customer ${number} enters the shop." )
    toShop.put ( new Customer ( number ) )
  }
  for ( number in 0 ..< numberOfCustomers ) {
    def customer = fromShop.take ( )
    println ( "World : Customer ${customer.id} exits the shop." )
  }
  println ( 'World : Time to close up.' )
  //  If we don't get here then Sweeney Todd is the barber — we have not got as many live customers back as
  //  we put in.
  barber.stopWork ( )
  barber.join ( )
  shop.closeShop ( )
  shopThread.join ( )
  println ( "\nTrimmed ${customersTrimmed} and turned away ${customersTurnedAway} today." )
}

runSimulation ( 20 , 4 , { ( Math.random ( ) * 60 + 10 ) as int }, { ( Math.random ( ) * 20 + 10 ) as int } )
