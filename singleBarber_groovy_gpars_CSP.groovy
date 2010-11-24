#! /usr/bin/env groovy

//  This is a model of the "The Sleeping Barber" problem using Groovy (http://groovy.codehaus.org) and
//  Groovy CSP (a part of GPars, http://gpars.codehaus.org),
//  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2010 Russel Winder
//
//  Everything here is modelled using a process.  The waiting queue is modelled by having an n-place buffer
//  in the channel between the shop and the barber.
//
//  This is only one of a potentially infinite number of correct versions.

//  As at 2010-11-07 17:48+00:00 this code sucks.  CSP channels are synchronous so we can't use the object
//  message structure we would with actors.  Currently there is "global" data and this makes things totally
//  unsafe and very un-CSP.

@Grab ( group = 'org.codehaus.jcsp' , module = 'jcsp' , version = '1.1-rc5' )
@Grab ( group = 'org.codehaus.gpars' , module = 'gpars' , version = '0.11-beta-1' )

import org.jcsp.lang.Channel
import org.jcsp.lang.CSProcess

import groovyx.gpars.csp.PAR
import groovyx.gpars.csp.ALT

class Customer {
  final Integer id
  public Customer ( final int i ) { id = i }
}

def runSimulation ( int numberOfCustomers , int numberOfWaitingSeats , Closure hairTrimTime , Closure nextCustomerWaitTime ) {
  final worldToShopInChannel = Channel.one2one ( )
  final shopInToBarberChannel = Channel.one2one ( )
  final barberToShopOutChannel = Channel.one2one ( )
  final barber = new CSProcess ( ) {
    public void run ( ) {
      def inChannel = shopInToBarberChannel.in ( )
      def outChannel = barberToShopOutChannel.out ( )
      while ( true ) {
        def customer = inChannel.read ( )
        assert customer instanceof Customer
        println ( 'Barber : Starting Customer ' + customer.id )
        Thread.sleep ( hairTrimTime ( ) )
        println ( 'Barber : Finished Customer ' + customer.id )
        outChannel.write ( customer )
      }
    }
  }
  def seatsTaken = 0
  def customersTurnedAway = 0
  def customersTrimmed = 0
  def isOpen = true
  final barbersShopIn = new CSProcess ( ) {
    public void run ( ) {
      def inChannel = worldToShopInChannel.in ( )
      def outChannel = shopInToBarberChannel.out ( )
      while ( true ) {
        def message = inChannel.read ( )
        if ( message == '' ) { isOpen = false }
        else {
          assert message instanceof Customer
          if ( seatsTaken < numberOfWaitingSeats ) {
            ++seatsTaken
            println ( 'Shop : Customer ' + message.id + ' takes a seat. ' + seatsTaken + ' in use.' )
            outChannel.write ( message )
          }
          else {
            println ( 'Shop : Customer ' + message.id + ' turned away.' )
            ++customersTurnedAway
          }
        }
      }
    }
  }
  final barbersShopOut = new CSProcess ( ) {
    public void run ( ) {
      def inChannel = barberToShopOutChannel.in ( )
      while ( true ) {
        def message = inChannel.read ( )
        assert message instanceof Customer
        --seatsTaken
        ++customersTrimmed
        println ( 'Shop : Customer ' + message.id + ' leaving trimmed.' )
        if ( ! isOpen && ( seatsTaken == 0 ) ) {
          println ( '\nTrimmed ' + customersTrimmed + ' and turned away ' + customersTurnedAway + ' today.' )
          stop ( )
        }
      }
    }
  }
  final world = new CSProcess ( ) {
    public void run ( ) {
      def toShopChannel = worldToShopInChannel.out ( )
      for ( number in 0 ..< numberOfCustomers ) {
        Thread.sleep ( nextCustomerWaitTime ( ) )
        println ( 'World : Customer ' + number + ' enters the shop.' )
        toShopChannel.write ( new Customer ( number ) )
      }
      toShopChannel.write ( '' )
    }
  }
  new PAR ( [ barber , barbersShopIn , barbersShopOut , world ] ).run ( )
}

runSimulation ( 20 , 4 , { ( Math.random ( ) * 60 + 10 ) as int }, { ( Math.random ( ) * 20 + 10 ) as int } )
