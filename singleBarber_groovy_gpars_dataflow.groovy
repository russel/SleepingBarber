#! /usr/bin/env groovy

//  This is a model of the "The Sleeping Barber" problem using Groovy (http://groovy.codehaus.org) and GPars
//  (http://gpars.codehaus.org) dataflow, cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2010–2011 Russel Winder

@Grab ( 'org.codehaus.gpars:gpars:0.12' )

import groovy.transform.Immutable

import groovyx.gpars.dataflow.Dataflow
import groovyx.gpars.dataflow.DataflowQueue

@Immutable class Customer { Integer id }
@Immutable class SuccessfulCustomer { Customer customer }

def runSimulation ( final int numberOfCustomers , final int numberOfWaitingSeats ,
                    final Closure hairTrimTime , final Closure nextCustomerWaitTime ) {
  def worldToShop = new DataflowQueue ( )
  def shopToBarber = new DataflowQueue ( )
  def barberToShop = new DataflowQueue ( )
  def shopToWorld = new DataflowQueue ( )
  final barber = Dataflow.task {
    while ( true ) {
      def customer = shopToBarber.val
      assert customer instanceof Customer
      println ( "Barber : Starting Customer ${customer.id}." )
      Thread.sleep ( hairTrimTime ( ) )
      println ( "Barber : Finished Customer ${customer.id}." )
      barberToShop << customer
    }
  }
  final shop = Dataflow.task {
    def seatsTaken = 0
    while ( true ) {
      def selector = Dataflow.select ( barberToShop , worldToShop )
      def item = selector.select ( )
      switch ( item.index ) {
       case 0 : //////// From the Barber ////////
         assert item.value instanceof Customer
         --seatsTaken
         println ( "Shop : Customer ${item.value.id} leaves trimmed." )
         shopToWorld << new SuccessfulCustomer ( item.value )
         break
       case 1 : //////// From the World ////////
         assert item.value instanceof Customer
         if ( seatsTaken <= numberOfWaitingSeats ) {
           ++seatsTaken
           println ( "Shop : Customer ${item.value.id} takes a seat. ${seatsTaken} in use." )
           shopToBarber << item.value
         }
         else {
           println ( "Shop : Customer ${item.value.id} turned away." )
           shopToWorld << item.value
         }
         break
       default :
         throw new RuntimeException ( 'Shop : Selected an non-existent queue.' )
      }
    }
  }
  //
  //  The world is run in the master thread so there is a driver of the dataflow.
  //
  for ( number in 0 ..< numberOfCustomers ) {
    Thread.sleep ( nextCustomerWaitTime ( ) )
    println ( "World : Customer ${number} enters the shop." )
    worldToShop << new Customer ( number )
  }
  def customersTurnedAway = 0
  def customersTrimmed = 0
  while ( customersTurnedAway + customersTrimmed < numberOfCustomers ) {
    def customer = shopToWorld.val
    if ( customer instanceof SuccessfulCustomer ) {
      ++customersTrimmed
      println ( "World : Customer ${customer.customer.id} exits shop trimmed." )
    }
    else {
      assert customer instanceof Customer
      ++customersTurnedAway
      println ( "World : Customer ${customer.id} exits shop without being trimmed." )
    }
  }
  println ( "\nTrimmed ${customersTrimmed} and turned away ${customersTurnedAway} today." )
}

runSimulation ( 20 , 4 , { ( Math.random ( ) * 60 + 10 ) as int }, { ( Math.random ( ) * 20 + 10 ) as int } )
