#! /usr/bin/env groovy

//  This is a model of the "The Sleeping Barber" problem using Groovy (http://groovy.codehaus.org) and GPars
//  (http://gpars.codehaus.org) actors, cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2009–2011 Russel Winder
//
//  The barber sleeping is modelled by the barber actor blocking awaiting a message.  The barber's chairs
//  are modelled by the message queue between the shop actor and the barber actor.  As the queue is an
//  arbitrary length list, the shop object has to control how many customers are allowed into the queue.

@Grab ( 'org.codehaus.gpars:gpars:0.12' )

import groovy.transform.Immutable

import groovyx.gpars.group.DefaultPGroup

@Immutable class Customer { Integer id }
@Immutable class SuccessfulCustomer { Customer customer }

def runSimulation ( final int numberOfCustomers , final int numberOfWaitingSeats ,
                    final Closure hairTrimTime , final Closure nextCustomerWaitTime ) {
  def group = new DefaultPGroup ( )
  def world // Just to have the variable so it can be used.
  def barber = group.reactor { customer ->
    assert customer instanceof Customer
    println ( "Barber : Starting Customer ${customer.id}." )
    Thread.sleep ( hairTrimTime ( ) )
    println ( "Barber : Finished Customer ${customer.id}." )
    new SuccessfulCustomer ( customer )
  }
  def shop = group.actor {
    def seatsTaken = 0
    def customersTrimmed = 0
    def customersTurnedAway = 0 
    while ( customersTrimmed + customersTurnedAway < numberOfCustomers ) {
      //loop {
      react { customer ->
        switch ( customer ) {
         case Customer :
           if ( seatsTaken <= numberOfWaitingSeats ) {
             ++seatsTaken
             println ( "Shop : Customer ${customer.id} takes a seat. ${seatsTaken} in use." )
             barber.send ( customer )
           }
           else {
             ++customersTurnedAway
             println ( "Shop : Customer ${customer.id} turned away." )
             world.send ( customer )
           }
           break
         case SuccessfulCustomer :
           --seatsTaken
           ++customersTrimmed
           println ( "Shop : Customer ${customer.customer.id} leaving trimmed." )
           world.send ( customer )
           break
         default : throw new RuntimeException ( "Shop got a message of unexpected type ${customer.class}" )
        }
      }
    }
    println ( "Shop : Closing — ${customersTrimmed} trimmed and ${customersTurnedAway} turned away." )
  }
  world = group.actor {
    for ( number in  0 ..< numberOfCustomers ) {
      Thread.sleep ( nextCustomerWaitTime ( ) )
      println ( "World : Customer ${number} enters the shop." )
      shop.send ( new Customer ( number ) )
    }
    def customersTurnedAway = 0
    def customersTrimmed = 0
    while ( customersTurnedAway + customersTrimmed < numberOfCustomers ) {
      react { customer ->
        int id
        switch ( customer ) {
         case Customer : ++customersTurnedAway ; id = customer.id ; break
         case SuccessfulCustomer : ++customersTrimmed ; id = customer.c.id ; break
         default : throw new RuntimeException ( "World got a message of unexpected type ${customer.class}" )
        }
        println ( "World : Customer ${id} exits the shop." )
      }
    }
    println ( "\nTrimmed ${customersTrimmed} and turned away ${customersTurnedAway} today." )
  }
  //  Delay terminating the program till the world has got back all the customers it sent into the shop.  If
  //  we get stuck here then the obvious inference is that Sweeney Todd is the barber and we do not have
  //  conservation of live customers.
  world.join ( )
}

runSimulation ( 20 , 4 , { ( Math.random ( ) * 60 + 10 ) as int }, { ( Math.random ( ) * 20 + 10 ) as int } )
