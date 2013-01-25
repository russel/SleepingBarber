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
  def shop // Just to have the variable so it can be used.
  def world // Just to have the variable so it can be used.
  final barber = group.actor {
    def customersTrimmed = 0
    loop {
      react {customer ->
        switch (customer) {
         case Customer:
           assert customer instanceof Customer
           println "Barber: Starting Customer ${customer.id}."
           Thread.sleep(hairTrimTime())
           println "Barber: Finished Customer ${customer.id}."
           shop << new SuccessfulCustomer(customer)
           ++customersTrimmed
           break
         case 'closing':
           println "Barber: Work over for the day, trimmed ${customersTrimmed} today."
           shop << 'clockedOff'
           terminate()
           break
         default: throw new RuntimeException("Barber got a customer of unexpected type ${customer.class}.")
        }
      }
    }
  }
  shop = group.actor {
    def seatsTaken = 0
    def customersTrimmed = 0
    def customersTurnedAway = 0
    loop {
      react {customer ->
        switch (customer) {
         case Customer:
           if (seatsTaken <= numberOfWaitingSeats) {
             ++seatsTaken
             println "Shop: Customer ${customer.id} takes a seat. ${seatsTaken} in use."
             barber << customer
           }
           else {
             ++customersTurnedAway
             println "Shop: Customer ${customer.id} turned away."
             world << customer
           }
           break
         case SuccessfulCustomer:
           --seatsTaken
           ++customersTrimmed
           println "Shop: Customer ${customer.customer.id} leaving trimmed."
           world << customer
           break
         case 'closing':
           barber << 'closing'
           break
         case 'clockedOff':
           println "Shop: Closing — ${customersTrimmed} trimmed and ${customersTurnedAway} turned away."
           world << 'closed'
           terminate()
           break
         default: throw new RuntimeException("Shop got a message of unexpected type ${customer.class}")
        }
      }
    }
  }
  world = group.actor {
    def customersTurnedAway = 0
    def customersTrimmed = 0
    def customerExit = {id -> println "World: Customer ${id} exits the shop."}
    loop {
      react {customer ->
        int id
        switch (customer) {
         case Customer:
           ++customersTurnedAway
           customerExit(customer.id)
           break
         case SuccessfulCustomer:
           ++customersTrimmed
           customerExit(customer.customer.id)
           break
         case 'closed':
           println "\nTrimmed ${customersTrimmed} and turned away ${customersTurnedAway} today."
           terminate()
         default: throw new RuntimeException("World got a message of unexpected type ${customer.class}")
        }
      }
    }
  }
  for (number in  0 ..< numberOfCustomers) {
    Thread.sleep(nextCustomerWaitTime())
    println "World: Customer ${number} enters the shop."
    shop << new Customer(number)
  }
  println 'World: No more customers, closing time.'
  shop << 'closing'
  //  Wait for the end of the world.  Note that even if Sweeney Todd is the barber and we do not have
  //  conservation of live customers, we may well see closing of the shop and termination of the world.
  world.join()
}

runSimulation(20, 4, {(Math.random() * 60 + 10) as int}, {(Math.random() * 20 + 10) as int})
