#! /usr/bin/env groovy

//  This is a model of the "The Sleeping Barber" problem using Groovy (http://groovy.codehaus.org) and GPars
//  (http://gpars.codehaus.org) actors, cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2009-10 Russel Winder
//
//  The barber sleeping is modelled by the barber actor blocking awaiting a message.  The barber's chairs
//  are modelled by the message queue between the shop actor and the barber actor.  As the queue is an
//  arbitrary length list, the shop object has to control how many customers are allowed into the queue.
//
//  This is only one of a potentially infinite number of correct versions.

@Grab ( group = 'org.codehaus.gpars' , module = 'gpars' , version = '0.11-beta-1' )

import groovyx.gpars.group.DefaultPGroup

class Customer {
  final Integer id
  public Customer ( final Integer id ) { this.id = id }
}

class SuccessfulCustomer {
  final Customer customer
  public SuccessfulCustomer ( final Customer customer ) { this.customer = customer }
}

def runSimulation ( final int numberOfCustomers , final int numberOfWaitingSeats ,
                    final Closure hairTrimTime , final Closure nextCustomerWaitTime ) {
  def group = new DefaultPGroup ( )
  def barber = group.reactor { customer ->
    assert customer instanceof Customer
    println ( 'Barber : Starting Customer ' + customer.id )
    Thread.sleep ( hairTrimTime ( ) )
    println ( 'Barber : Finished Customer ' + customer.id )
    new SuccessfulCustomer ( customer )
  }
  def shop = group.actor {
    def seatsTaken = 0
    def isOpen = true
    def customersTurnedAway = 0
    def customersTrimmed = 0
    loop {
      react { message ->
        switch ( message ) {
         case Customer :
           if ( seatsTaken <= numberOfWaitingSeats ) {
             ++seatsTaken
             println ( 'Shop : Customer ' + message.id + ' takes a seat. ' + seatsTaken + ' in use.' )
             barber.send ( message )
           }
           else {
             println ( 'Shop : Customer ' + message.id + ' turned away.' )
             ++customersTurnedAway
           }
           break
         case SuccessfulCustomer :
           --seatsTaken
           ++customersTrimmed
           println ( 'Shop : Customer ' + message.customer.id + ' leaving trimmed.' )
           if ( ! isOpen && ( seatsTaken == 0 ) ) {
             println ( '\nTrimmed ' + customersTrimmed + ' and turned away ' + customersTurnedAway + ' today.' )
             stop ( )
           }
           break
         case '' : isOpen = false ; break
         default : throw new RuntimeException ( 'Shop got a message of unexpected type ' + message.class )
        }
      }
    }
  }
  for ( number in  0 ..< numberOfCustomers ) {
    Thread.sleep ( nextCustomerWaitTime ( ) )
    println ( 'World : Customer ' + number + ' enters the shop.' )
    shop.send ( new Customer ( number ) )
  }
  shop.send ( '' )
  shop.join ( )
}

runSimulation ( 20 , 4 , { ( Math.random ( ) * 60 + 10 ) as int }, { ( Math.random ( ) * 20 + 10 ) as int } )
