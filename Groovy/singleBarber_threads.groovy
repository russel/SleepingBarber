#! /usr/bin/env groovy

//  This is a model of the "The Sleeping Barber" problem using Groovy (http://groovy.codehaus.org) only,
//  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2010–2013  Russel Winder

//  The barber is modeled with a thread, with the sleeping being modeled by a blocking read on a queue.  The
//  queue represents the cutting chairs and the waiting chairs.  Customers are injected into the shop by the
//  main thread via a queue.  Having either been to the barber and back again, or rejected due to no space
//  in the queue representing the chairs, the customers is released into the world.  Each thread keeps a
//  count of the number of customers of each sort.

import java.util.concurrent.ArrayBlockingQueue

import groovy.transform.Immutable

@Immutable class Customer { Integer id }
@Immutable class SuccessfulCustomer { Customer customer }

def runSimulation(final int numberOfCustomers, final int numberOfWaitingSeats,
                    final Closure hairTrimTime, final Closure nextCustomerWaitTime) {
  final waitingChairs = new ArrayBlockingQueue<Customer>(numberOfWaitingSeats)
  final toShop = new ArrayBlockingQueue<Customer>(numberOfCustomers)
  final fromChair = new ArrayBlockingQueue<SuccessfulCustomer>(numberOfCustomers)
  final fromShop = new ArrayBlockingQueue<Object>(numberOfCustomers)
  def shop // So we can use before defined.
  def world // So we can use before defined.
  //  Must subclass Thread here rather than create a Runnable, so that we can execute an interrupt when
  //  closing the shop.
  final barber = new Thread() {
    private working = true
    public void stopWork() {
      working = false
      interrupt()
    }
    @Override public void run() {
      def customersTrimmed = 0
      while (working || (waitingChairs.size() > 0)) {
        try {
          //  Use take here to simulate barber sleeping if there are no customers by blocking.  It means we
          //  have to use an interrupt when the shop gets closed.
          def customer = waitingChairs.take()
          assert customer instanceof Customer
          println("Barber: Starting Customer ${customer.id}.")
          //  If we don't deal with InterruptedException here then (because we ignore the interrupt in an
          //  outer scope) we get the "Sweeney Todd" effect, one customer (or more) simply disappears from
          //  the universe.
          try { Thread.sleep(hairTrimTime()) }
          catch (InterruptedException iiee) { /* Intentionally left blank. */ }
          ++customersTrimmed
          println("Barber: Finished Customer ${customer.id}.")
          fromChair.put(new SuccessfulCustomer(customer))
        }
        catch (InterruptedException ie) { /* Intentionally left blank. */ }
      }
      println("Barber: Work over for the day, trimmed ${customersTrimmed} customers.")
      shop.clockedOut()
    }
  }
  shop = new Runnable() {
    private open = true
    public void closeShop() { barber.stopWork() }
    public void clockedOut() { open = false }
    @Override public void run() {
      def customersTurnedAway = 0
      def customersTrimmed = 0
      while (open) {
        def customer = toShop.poll()
        if (customer) {
          assert customer.class == Customer
          if (waitingChairs.offer(customer)) {
            println("Shop: Customer ${customer.id} takes a seat. ${waitingChairs.size()} in use.")
          }
          else {
            ++customersTurnedAway
            println("Shop: Customer ${customer.id} turned away.")
            fromShop.put(customer)
          }
        }
        def successfulCustomer = fromChair.poll()
        if (successfulCustomer) {
          assert successfulCustomer.class == SuccessfulCustomer
          ++customersTrimmed
          println("Shop: Customer ${successfulCustomer.customer.id} leaving trimmed.")
          fromShop.put(successfulCustomer)
        }
      }
      println("Shop: Closing — Trimmed ${customersTrimmed} and turned away ${customersTurnedAway} customers.")
      world.shopClosed()
    }
  }
  final shopThread = new Thread(shop)
  world = new Thread() {
    private existing = true
    public void selfDestruct() { shop.closeShop() }
    public void shopClosed() { existing = false ; interrupt() }
    @Override public void run() {
      def customersTurnedAway = 0
      def customersTrimmed = 0
      def message = {  int id -> println("World: Customer ${id} exits the shop.") }
      while (existing) {
        try {
          def customer = fromShop.take()
          switch (customer) {
           case Customer:
             ++customersTurnedAway
             message(customer.id)
             break
           case SuccessfulCustomer:
             ++customersTrimmed ;
             message(customer.customer.id) ;
             break
          }
        }
        catch(InterruptedException ie) { /* Intentionally left blank. */ }
      }
      println("\nTrimmed ${customersTrimmed} and turned away ${customersTurnedAway} today.")
      println('World: Non-existent.') ;
    }
  }
  barber.start()
  shopThread.start()
  world.start()
  for (number in 0 ..< numberOfCustomers) {
    Thread.sleep(nextCustomerWaitTime())
    println("World: Customer ${number} enters the shop.")
    toShop.put(new Customer(number))
  }
  world.selfDestruct()
  world.join()
}

runSimulation(20, 4, {(Math.random() * 60 + 10) as int}, {(Math.random() * 20 + 10) as int})
