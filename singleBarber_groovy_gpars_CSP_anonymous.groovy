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

@Grab ( group = 'org.codehaus.jcsp' , module = 'jcsp' , version = '1.1-rc5' )
@Grab ( group = 'org.codehaus.gpars' , module = 'gpars' , version = '0.11-beta-1' )

import org.jcsp.util.Buffer
import org.jcsp.lang.Channel
import org.jcsp.lang.CSProcess

import groovyx.gpars.csp.PAR
import groovyx.gpars.csp.ALT

class Customer {
  final Integer id
  public Customer ( final int i ) { id = i }
}

def runSimulation ( final int numberOfCustomers , final int numberOfWaitingSeats ,
                    final Closure hairTrimTime , final Closure nextCustomerWaitTime ) {
  final worldToShopChannel = Channel.one2one ( )
  final shopToBarberChannel = Channel.one2one ( new Buffer ( numberOfWaitingSeats ) )
  final barberToShopChannel = Channel.one2one ( )
  final barber = new CSProcess ( ) {
    @Override public void run ( ) {
      final fromShopChannel = shopToBarberChannel.in ( )
      final toShopChannel = barberToShopChannel.out ( )
      while ( true ) {
        final customer = fromShopChannel.read ( )
        if ( customer == '' ) { break }
        assert customer instanceof Customer
        println ( 'Barber : Starting Customer ' + customer.id )
        Thread.sleep ( hairTrimTime ( ) )
        println ( 'Barber : Finished Customer ' + customer.id )
        toShopChannel.write ( customer )
      }
    }
  }
  final shop = new CSProcess ( ) {
    @Override public void run ( ) {
      final fromBarberChannel = barberToShopChannel.in ( )
      final fromWorldChannel = worldToShopChannel.in ( )
      final toBarberChannel = shopToBarberChannel.out ( )
      final selector = new ALT ( [ fromBarberChannel , fromWorldChannel ] )
      def seatsTaken = 0
      def customersTurnedAway = 0
      def customersTrimmed = 0
      def isOpen = true
    mainloop: 
      while ( true ) {
        switch ( selector.select ( ) ) { 
         case 0 : //////// From the Barber ////////
           def customer = fromBarberChannel.read ( )
           assert customer instanceof Customer
           --seatsTaken
           ++customersTrimmed
           println ( 'Shop : Customer ' + customer.id + ' leaving trimmed.' )
           if ( ! isOpen && ( seatsTaken == 0 ) ) {
             println ( '\nTrimmed ' + customersTrimmed + ' and turned away ' + customersTurnedAway + ' today.' )
             toBarberChannel.write ( '' )
             break mainloop
           }
           break
         case 1 : //////// From the World ////////
           def customer = fromWorldChannel.read ( )
           if ( customer == '' ) { isOpen = false }
           else {
             assert customer instanceof Customer
             if ( seatsTaken < numberOfWaitingSeats ) {
               ++seatsTaken
               println ( 'Shop : Customer ' + customer.id + ' takes a seat. ' + seatsTaken + ' in use.' )
               toBarberChannel.write ( customer )
             }
             else {
               println ( 'Shop : Customer ' + customer.id + ' turned away.' )
               ++customersTurnedAway
             }
           }
           break
         default :
           throw new RuntimeException ( 'Shop : Selected a non-existent channel.' )
        }
      }
    }
  }
  final world = new CSProcess ( ) {
    @Override public void run ( ) {
      def toShopChannel = worldToShopChannel.out ( )
      for ( number in 0 ..< numberOfCustomers ) {
        Thread.sleep ( nextCustomerWaitTime ( ) )
        println ( 'World : Customer ' + number + ' enters the shop.' )
        toShopChannel.write ( new Customer ( number ) )
      }
      toShopChannel.write ( '' )
    }
  }
  new PAR ( [ barber , shop , world ] ).run ( )
}

runSimulation ( 20 , 4 , { ( Math.random ( ) * 60 + 10 ) as int }, { ( Math.random ( ) * 20 + 10 ) as int } )
