#! /usr/bin/env groovy

//  This is a model of the "The Sleeping Barber" problem using Groovy (http://groovy.codehaus.org) and GPars
//  (http://gpars.codehaus.org) dataflow, cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2010--2011 Russel Winder

@Grab ( 'org.codehaus.gpars:gpars:0.12-beta-1-SNAPSHOT' )

import groovy.transform.Immutable

import groovyx.gpars.dataflow.DataFlow
import groovyx.gpars.dataflow.DataFlowQueue

@Immutable class Customer { Integer id }
@Immutable class SuccessfulCustomer { Customer customer }

def runSimulation ( final int numberOfCustomers , final int numberOfWaitingSeats ,
                    final Closure hairTrimTime , final Closure nextCustomerWaitTime ) {
  def worldToShop = new DataFlowQueue ( )
  def shopToBarber = new DataFlowQueue ( )
  def barberToShop = new DataFlowQueue ( )
  def shopToWorld = new DataFlowQueue ( )
  final barber = DataFlow.task {
    while ( true ) {
      def customer = shopToBarber.val
      assert customer instanceof Customer
      println ( "Barber : Starting Customer ${customer.id}." )
      Thread.sleep ( hairTrimTime ( ) )
      println ( "Barber : Finished Customer ${customer.id}." )
      barberToShop << customer
    }
  }
  final shop = DataFlow.task {
    def seatsTaken = 0
    while ( true ) {
      def selector = DataFlow.select ( barberToShop , worldToShop )
      def item = selector.select ( )
      switch ( item.index ) {
       case 0 : //////// From the Barber ////////
         assert item.value instanceof Customer
         --seatsTaken
         shopToWorld << new SuccessfulCustomer ( item.value )
         break
       case 1 : //////// From the World ////////
         assert item.value instanceof Customer
         if ( seatsTaken < numberOfWaitingSeats ) {
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
    if ( customer instanceof SuccessfulCustomer ) { ++customersTrimmed }
    else {
      assert customer instanceof Customer
      ++customersTurnedAway
    }
  }
  println ( "\nTrimmed ${customersTrimmed} and turned away ${customersTurnedAway} today." )
}

runSimulation ( 20 , 4 , { ( Math.random ( ) * 60 + 10 ) as int }, { ( Math.random ( ) * 20 + 10 ) as int } )
