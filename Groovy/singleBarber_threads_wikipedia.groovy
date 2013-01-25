#! /usr/bin/env groovy

//  This is a model of the "The Sleeping Barber" problem using Groovy (http://groovy.codehaus.org),
//  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2010–2013  Russel Winder
//
//  This solution follow the psuedocode of the solution set out on the Wikipedia page.  Use
//  java.util.concurrent.Semaphore as an implementation of semaphore to save having to write one or use the
//  Java basic wait/notify system.
//
//  Ignoring InterruptedException for Semaphore acquiring could be seen as being overly cavalier and
//  introducing deadlock possibilities.  It is lucky this never seems to happen.

import java.util.concurrent.Semaphore

def runSimulation(final int numberOfCustomers, final int numberOfWaitingSeats,
                    final Closure hairTrimTime, final Closure nextCustomerWaitTime) {
  final customerSemaphore = new Semaphore(1)
  final barberSemaphore = new Semaphore(1)
  final accessSeatsSemaphore = new Semaphore(1)
  def customersTurnedAway = 0
  def customersTrimmed = 0
  def numberOfFreeSeats = numberOfWaitingSeats
  final barber = new Runnable() {
    private working = true
    public void stopWork() { working = false }
    @Override public void run() {
    while (working) {
        customerSemaphore.acquire()
        accessSeatsSemaphore.acquire()
        ++numberOfFreeSeats
        barberSemaphore.release()
        accessSeatsSemaphore.release()
        println('Barber: Starting Customer.')
        Thread.sleep(hairTrimTime())
        println('Barber: Finished Customer.')
      }
    }
  }
  final barberThread = new Thread(barber)
  barberThread.start()
  final customerThreads = []
  for (number in 0 ..< numberOfCustomers) {
    println("World: Customer ${number} enters the shop.")
    final customerThread = new Thread(new Runnable() {
                                         @Override public void run() {
                                           accessSeatsSemaphore.acquire()
                                           if (numberOfFreeSeats > 0) {
                                             println("Shop: Customer ${number} takes a seat. ${numberOfWaitingSeats - numberOfFreeSeats} in use.")
                                             --numberOfFreeSeats
                                             customerSemaphore.release()
                                             accessSeatsSemaphore.release()
                                             barberSemaphore.acquire()
                                             println("Shop: Customer ${number} leaving trimmed.")
                                             ++customersTrimmed
                                           }
                                           else {
                                             accessSeatsSemaphore.release()
                                             println("Shop: Customer ${number} turned away.")
                                             ++customersTurnedAway
                                           }
                                         }
                                       })
    customerThreads << customerThread
    customerThread.start()
    Thread.sleep(nextCustomerWaitTime())
  }
  customerThreads*.join()
  barber.stopWork()
  barberThread.join()
  println("\nTrimmed ${customersTrimmed} and turned away ${customersTurnedAway} today.")
}

runSimulation(20, 4, {(Math.random() * 60 + 10) as int}, {(Math.random() * 20 + 10) as int})
