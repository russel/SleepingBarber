#! /usr/bin/env groovy

//  This is a model of the "The Sleeping Barber" problem using Groovy (http://groovy.codehaus.org) and GPars
//  (http://gpars.codehaus.org) dataflow, cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2010 Russel Winder

@Grab ( 'org.codehaus.gpars:gpars:0.11-beta-2' )

import groovy.transform.Immutable

import groovyx.gpars.dataflow.DataFlow
import groovyx.gpars.dataflow.DataFlowQueue

@Immutable class Customer { Integer id }

def runSimulation ( final int numberOfCustomers , final int numberOfWaitingSeats ,
                    final Closure hairTrimTime , final Closure nextCustomerWaitTime ) {
  def worldToShop = new DataFlowQueue ( )
  def shopToBarber = new DataFlowQueue ( )
  def barberToShop = new DataFlowQueue ( )
  final barber = DataFlow.task {
    while ( true ) {
      def customer = shopToBarber.val
      if ( customer == '' ) { break }
      assert customer instanceof Customer
      println ( "Barber : Starting Customer ${customer.id}." )
      Thread.sleep ( hairTrimTime ( ) )
      println ( "Barber : Finished Customer ${customer.id}." )
      barberToShop << customer
    }
  }
  final shop = DataFlow.task {
    def seatsTaken = 0
    def customersTurnedAway = 0
    def customersTrimmed = 0
    def isOpen = true
   mainloop:
    while ( true ) {
      def selector = DataFlow.select ( barberToShop , worldToShop )
      def item = selector.select ( )
      switch ( item.index ) {
       case 0 : //////// From the Barber ////////
         assert item.value instanceof Customer
         --seatsTaken
         ++customersTrimmed
         println ( "Shop : Customer ${item.value.id} leaving trimmed." )
         if ( ! isOpen && ( seatsTaken == 0 ) ) {
           println ( "\nTrimmed ${customersTrimmed} and turned away ${customersTurnedAway} today." )
           shopToBarber << ''
           break mainloop
         }
         break
       case 1 : //////// From the World ////////
         if ( item.value == '' ) { isOpen = false }
         else {
           assert item.value instanceof Customer
           if ( seatsTaken < numberOfWaitingSeats ) {
             ++seatsTaken
             println ( "Shop : Customer ${item.value.id} takes a seat. ${seatsTaken} in use." )
             shopToBarber << item.value
           }
           else {
             println ( "Shop : Customer ${item.value.id} turned away." )
             ++customersTurnedAway
           }
         }
         break
       default :
         throw new RuntimeException ( 'Shop : Selected an non-existent queue.' )
      }
    }
  }
  for ( number in 0 ..< numberOfCustomers ) {
    Thread.sleep ( nextCustomerWaitTime ( ) )
    println ( "World : Customer ${number} enters the shop." )
    worldToShop << new Customer ( number )
  }
  worldToShop << ''
  //  Make sure all computation is over before terminating.
  [ barber , shop ]*.join ( )
}

runSimulation ( 20 , 4 , { ( Math.random ( ) * 60 + 10 ) as int }, { ( Math.random ( ) * 20 + 10 ) as int } )
