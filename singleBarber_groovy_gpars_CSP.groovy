#! /usr/bin/env groovy

//  This is a model of the "The Sleeping Barber" problem,
//  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2010 Russel Winder
//
//  Everything here is modelled using a process.  The waiting queue is modelled by having an n-place buffer
//  in the channel between the shop and the barber.
//
//  This is only one of a potentially infinite number of correct versions.

@Grab ( group = 'org.codehaus.jcsp' , module = 'jcsp' , version = '1.1-rc5-SNAPSHOT' )
@Grab ( group = 'org.codehaus.gpars' , module = 'gpars' , version = '0.10-beta-1-SNAPSHOT' )

import org.jcsp.lang.Channel
import org.jcsp.lang.CSProcess

import groovyx.gpars.csp.PAR
import groovyx.gpars.csp.ALT

class Customer {
  final Integer id
  public Customer ( final int i ) { id = i }
}

final worldToShopChannel = Channel.one2one ( )
final shopToBarberChannel = Channel.one2one ( )
final barberToShopChannel = Channel.one2one ( )

final barber = new CSProcess ( ) {
  public void run ( ) {
    while ( true ) {
      def customer = shopToBarberChannel.in ( ).read ( )
      assert customer instanceof Customer
      println ( 'Barber : Starting with Customer ' + customer.id )
      Thread.sleep ( ( Math.random ( ) * 600 + 100 ) as int )
      println ( 'Barber : Finished with Customer ' + customer.id )
      barberToShopChannel.out ( ).write ( customer )
    }
  }
}

final barbersShop = new CSProcess ( ) {
  public void run ( ) {
    def seatsTaken = 0
    def isOpen = true
    def customersRejected = 0
    def customersProcessed = 0
    def channelList =  [ worldToShopChannel , barberToShopChannel ]
    def channelSelector = new ALT ( channelList )
    while ( true ) {
      def channel = channelList [ channelSelector.fairSelect ( worldToShopChannel , barberToShopChannel ) ]
      def message = channel.in ( ).read ( )
      switch ( channel ) {
       case worldToShopChannel :
         if ( message == '' ) { isOpen = false }
         else {
           assert message instanceof Customer
           if ( seatsTaken < 4 ) {
             ++seatsTaken
             println ( 'Shop : Customer ' + message.id + ' takes a seat. ' + seatsTaken + ' in use.' )
             shopToBarberChannel.out ( ).write ( message )
           }
           else {
             println ( 'Shop : Customer ' + message.id + ' turned away.' )
             ++customersRejected
           }
         }
         break
       case barberToShopChannel :
         assert message instanceof Customer
         --seatsTaken
         ++customersProcessed
         println ( 'Shop : Customer ' + message.customer.id + ' leaving trimmed.' )
         if ( ! isOpen && ( seatsTaken == 0 ) ) {
           println ( 'Processed ' + customersProcessed + ' customers and rejected ' + customersRejected + ' today.' )
           stop ( )
         }
         break
      }
    }
  }
}

final world = new CSProcess ( ) {
  public void run ( ) {
    for ( number in 0 ..< 20 ) {
      Thread.sleep ( ( Math.random ( ) * 200 + 100 ) as int )
      worldToShopChannel.out ( ).write ( new Customer ( number ) )
    }
    worldToShopChannel.out ( ).write ( '' )
  }
}

new PAR ( [ barber , barbersShop , world ] ).run ( )
