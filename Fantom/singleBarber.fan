#! /usr/bin/env fan

/*
 * This is a model of the "The Sleeping Barber" problem using Fantom and its actors,
 *  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
 *
 *  Copyright © 2011 Russel Winder
 *
 *  The barber sleeping is modelled by the barber actor blocking awaiting a message.  The barber's chairs
 *  are modelled by the message queue between the shop actor and the barber actor.  As the queue is an
 *  arbitrary length list, the shop object has to control how many customers are allowed into the queue.
 *
 *  This is only one of a potentially infinite number of correct versions.
 */

const final class Customer {
  const Int id
  new make ( Int id ) { this.id = id }
}

const final class SuccessfulCustomer {
  const Customer customer
  new make ( Customer customer ) { this.customer = customer }
}

class Main {
  static Void runSimulation ( Int numberOfCustomers , Int numberOfWatingSeats ,
                               Func hairTrimTime ,  Func nextCustomerWaitTime ) {
    pool := concurrent::ActorPool ( )
    barber := concurrent::Actor ( pool ) | Customer customer -> SuccessfulCustomer | {
      echo ( "Barber : Starting Customer " + customer.id )
      concurrent::Actor.sleep ( sys::Duration ( hairTrimTime ( ) ) )
      echo ( "Barber : Finished Customer " + customer.id )
      return SuccessfulCustomer ( customer )
    }
    shop := concurrent::Actor ( pool ) | message -> Customer | {
      switch ( Type.of ( message ).name ( ) ) {
      case "Customer" : echo ( "Customer " + ((Customer)message).id ) ; return ((Customer)message)
      case "SuccessfulCustomer" : echo ( "SuccessfulCustomer " + ((Customer)message).id ) ; return ((SuccessfulCustomer)message).customer
      default : echo ( "default" ) ; return (Customer)null
      }
    }
    customerFutures := ( 1 .. numberOfCustomers ).map | id | {
      concurrent::Actor.sleep ( sys::Duration ( nextCustomerWaitTime ( ) ) )
      return shop.send ( Customer ( id ) )
    }
    customerFutures.each | concurrent::Future f | { f.get ( ) }
  }
  static Void main ( ) {
    runSimulation ( 20 , 4 , | -> Int | { return Int.random ( 0 .. 6000 ) + 1000 } , | -> Int | { return Int.random ( 0 .. 2000 ) + 1000 } )
  }
}
