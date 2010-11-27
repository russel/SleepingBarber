#! /usr/bin/env groovy

//  This is a model of the "The Sleeping Barber" problem using Groovy (http://groovy.codehaus.org),
//  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2010 Russel Winder
//
//  This solution follow the psuedocode of the solution set out on the Wikipedia page.  Use
//  java.util.concurrent.Semaphore as an implementation of semaphore to save having to write one or use the
//  Java basic wait/notify system..

import java.util.concurrent.Semaphore

def runSimulation ( int numberOfCustomers , int numberOfWaitingSeats , Closure hairTrimTime , Closure nextCustomerWaitTime ) {
  final customerSemaphore = new Semaphore ( 1 )
  final barberSemaphore = new Semaphore ( 1 )
  final accessSeatsSemaphore = new Semaphore ( 1 )
  def customersTurnedAway = 0
  def customersTrimmed = 0
  def numberOfFreeSeats = numberOfWaitingSeats
  final barber = new Runnable ( ) {
    private working = true
    public void stopWork ( ) { working = false }
    @Override public void run ( ) {
      while ( working ) {
        customerSemaphore.acquire ( )
        accessSeatsSemaphore.acquire ( )
        ++numberOfFreeSeats
        barberSemaphore.release ( )
        accessSeatsSemaphore.release ( )
        println ( 'Barber : Starting Customer.' )
        Thread.sleep ( hairTrimTime ( ) )
        println ( 'Barber : Finished Customer.')
      }
    }
  }
  final barberThread = new Thread ( barber )
  barberThread.start ( )
  final customerThreads = [ ]
  for ( int i = 0 ; i < numberOfCustomers ; ++i ) {
    println ( 'World : Customer enters shop.' )
    final customerThread = new Thread ( new Runnable ( ) {
                                           public void run ( ) {
                                             accessSeatsSemaphore.acquire ( )
                                             if ( numberOfFreeSeats > 0 ) {
                                               println ( 'Shop : Customer takes a seat. ' + ( numberOfWaitingSeats - numberOfFreeSeats ) + ' in use.' )
                                               --numberOfFreeSeats
                                               customerSemaphore.release ( )
                                               accessSeatsSemaphore.release ( )
                                               barberSemaphore.acquire ( )
                                               println ( 'Shop : Customer leaving trimmed.' )
                                               ++customersTrimmed
                                             }
                                             else {
                                               accessSeatsSemaphore.release ( )
                                               println ( 'Shop : Customer turned away.' )
                                               ++customersTurnedAway
                                             }
                                           }
                                         } )
    customerThreads << customerThread
    customerThread.start ( )
    Thread.sleep ( nextCustomerWaitTime ( ) )
  }
  customerThreads*.join ( )
  barber.stopWork ( )
  barberThread.join ( )
  println ( '\nTrimmed ' + customersTrimmed + ' and turned away ' + customersTurnedAway + ' today.' )
}

runSimulation ( 20 , 4 , { ( Math.random ( ) * 60 + 10 ) as int }, { ( Math.random ( ) * 20 + 10 ) as int } )
