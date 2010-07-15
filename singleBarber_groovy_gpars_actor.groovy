#! /usr/bin/env groovy

//  This is a model of the "The Sleeping Barber" problem,
//  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2009-10 Russel Winder
//
//  The barber sleeping is modelled by the barber actor blocking awaiting a message.  The barbers' chairs
//  are modelled by the message queue between the barbersShop actor and the barber actor.  As the queue is
//  an arbitrary length list, the barbersShop object has to control how many customers are allowed into the
//  queue.
//
//  This is only one of a potentially infinite number of correct versions.

@Grab ( group = 'org.codehaus.gpars' , module = 'gpars' , version = '0.10' )

import groovyx.gpars.group.DeafultPGroup

class Customer {
  final Integer id
  public Customer ( final int i ) { id = i }
}

class PendingCustomer {
  final Customer customer
  public PendingCustomer ( final Customer c ) { customer = c }
}

class SuccessfulCustomer {
  final Customer customer
  public SuccessfulCustomer ( final Customer c ) { customer = c }
}

def group = new DefaultPGroup ( )
def barbersShop
def barber = group.reactor { message ->
  assert message instanceof PendingCustomer
  println ( 'Barber : Starting with Customer ' + message.customer.id )
  Thread.sleep ( ( Math.random ( ) * 600 + 100 ) as int )
  println ( 'Barber : Finished with Customer ' + message.customer.id )
  new SuccessfulCustomer ( message.customer )
}
barbersShop = group.actor {
  def seatsTaken = 0
  def isOpen = true
  def customersRejected = 0
  def customersProcessed = 0
  loop {
    react { message ->
      switch ( message ) {
        case Customer :
          if ( seatsTaken < 4 ) {
            ++seatsTaken
            println ( 'Shop : Customer ' + message.id + ' takes a seat. ' + seatsTaken + ' in use.' )
            barber.send ( new PendingCustomer ( message ) )
          }
          else {
            println ( 'Shop : Customer ' + message.id + ' turned away.' )
            ++customersRejected
          }
          break
        case SuccessfulCustomer :
          --seatsTaken
          ++customersProcessed
          println ( 'Shop : Customer ' + message.customer.id + ' leaving trimmed.' )
          if ( ! isOpen && ( seatsTaken == 0 ) ) {
            println ( 'Processed ' + customersProcessed + ' customers and rejected ' + customersRejected + ' today.' )
            stop ( )
          }
          break
        case '' : isOpen = false ; break
        default : throw new RuntimeException ( 'barbersShop got a message of unexpected type ' + message.class )
      }
    }
  }
}
( 0 ..< 20 ).each { number ->
  Thread.sleep ( ( Math.random ( ) * 200 + 100 ) as int )
  barbersShop.send ( new Customer ( number ) )
}
barbersShop.send ( '' )
barbersShop.join ( )
