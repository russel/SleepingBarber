#! /usr/bin/env groovy

//  This is a model of the "The Sleeping Barber" problem using Groovy (http://groovy.codehaus.org) and GPars
//  (http://gpars.codehaus.org) actors, cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2009–2013  Russel Winder
//
//  The barber sleeping is modelled by the barber actor blocking awaiting a message.  The barber's chairs
//  are modelled by the barber actor message queue.  As the queue is an arbitrary length list, the shop
//  object has to control how many customers are allowed into the queue.

import groovy.transform.Immutable

import groovyx.gpars.group.DefaultPGroup

@Immutable class Customer { Integer id }
@Immutable class SuccessfulCustomer { Customer customer }

def runSimulation(final int numberOfCustomers, final int numberOfWaitingSeats,
                    final Closure hairTrimTime, final Closure nextCustomerWaitTime) {
  def group = new DefaultPGroup()
  def world // Just to have the variable so it can be used.
  def barber = group.reactor {customer ->
    assert customer instanceof Customer
    println("Barber: Starting Customer ${customer.id}.")
    Thread.sleep(hairTrimTime())
    println("Barber: Finished Customer ${customer.id}.")
    new SuccessfulCustomer(customer)
  }
  def shop = group.messageHandler {
    def seatsTaken = 0
    def isOpen = true
    def customersTurnedAway = 0
    def customersTrimmed = 0
    when {Customer customer ->
      if (seatsTaken <= numberOfWaitingSeats) {
        ++seatsTaken
        println("Shop: Customer ${customer.id} takes a seat. ${seatsTaken} in use.")
        barber << customer
      }
      else {
        println("Shop: Customer ${customer.id} turned away.")
        ++customersTurnedAway
        world << customer
      }
    }
    when {SuccessfulCustomer successfulCustomer ->
      --seatsTaken
      ++customersTrimmed
      println("Shop: Customer ${successfulCustomer.customer.id} leaving trimmed.")
      world << successfulCustomer
      if (! isOpen &&(seatsTaken == 0)) {
        println("Shop: Closing — ${customersTrimmed} trimmed and ${customersTurnedAway} turned away.")
        world << 'closed'
        terminate()
      }
    }
    when {String message -> isOpen = false}
  }
  world = group.messageHandler {
    def customersTurnedAway = 0
    def customersTrimmed = 0
    def customerExit = { id -> println("World: Customer ${id} exits the shop.") }
    when {Customer customer ->
      ++customersTurnedAway
      customerExit(customer.id)
    }
    when {SuccessfulCustomer successfulCustomer ->
      ++customersTrimmed
      customerExit(successfulCustomer.customer.id)
    }
    when {String message ->
      assert message == 'closed'
      println("\nTrimmed ${customersTrimmed} and turned away ${customersTurnedAway} today.")
      terminate()
    }
  }
  for (number in  0 ..< numberOfCustomers) {
    Thread.sleep(nextCustomerWaitTime())
    println("World: Customer ${number} enters the shop.")
    shop << new Customer(number)
  }
  shop << 'closing'
  world.join()
}

runSimulation(20, 4, {(Math.random() * 60 + 10) as int}, {(Math.random() * 20 + 10) as int})
