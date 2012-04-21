//  This is a model of the "The Sleeping Barber" problem using Scala and its primitive actors,
//  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2011–2012 Russel Winder

//  The barber sleeping is modeled by the barber actor using a blocking read on its message queue.  The
//  barber seats are modeled by the barber actor message queue so the shop actor is responsible for tracking
//  the number of customers sent to the barber actor.  The world actor only captures customers leaving the
//  shop, customers are fed into the shop by the main thread, which then waits for the actors to do their
//  work.

import scala.actors.Actor
import scala.actors.Actor._
import scala.util.Random

object SingleBarber_Scala_Actors extends App {
  case class Customer ( id : Int )
  case class SuccessfulCustomer ( customer : Customer )
  case object CloseShop
  case object BarberStoppedWork
  def runSimulation ( numberOfCustomers : Int , numberOfWaitingSeats : Int , hairTrimTime : ( ) => Int , nextCustomerWaitingTime : ( ) => Int ) {
    //  Due to the cross-references of the various actor objects, the type inference system cannot resolve
    //  the types appropriately, and so the types of some of the actors must be specified, so declare the
    //  types of all of them. Moreover, the definitions have to be lazy so as to deal with the order of use
    //  versus definition.
    lazy val barber : Actor = Actor.actor {
      var customersTrimmed = 0
      var working = true
      loopWhile ( working ) {
        react {
          case customer : Customer =>
            println ( "Barber : Starting Customer " + customer.id )
            Thread.sleep ( hairTrimTime ( ) )
            println ( "Barber : Finished Customer " + customer.id )
            customersTrimmed += 1
            shop ! new SuccessfulCustomer ( customer )
          case CloseShop =>
            println ( "Barber : Work over for the day, trimmed " + customersTrimmed + "." )
            shop ! BarberStoppedWork
            working = false
        }
      }
    }
    lazy val shop : Actor = Actor.actor {
      var seatsFilled = 0
      var customersTrimmed = 0
      var customersTurnedAway = 0
      var open = true
      loopWhile ( open ) {
        react {
          case customer : Customer =>
            if ( seatsFilled <= numberOfWaitingSeats ) {
              seatsFilled += 1
              println ( "Shop : Customer " + customer.id + " takes a seat. " + seatsFilled + " in use." )
              barber ! customer
            }
            else {
              customersTurnedAway += 1
              println ( "Shop : Customer " + customer.id + " turned away." )
              world ! customer
            }
          case successfulCustomer : SuccessfulCustomer =>
            seatsFilled -= 1
            customersTrimmed += 1
            println ( "Shop : Customer " + successfulCustomer.customer.id + " leaving trimmed." )
            world ! successfulCustomer
          case CloseShop =>
            barber ! CloseShop
          case BarberStoppedWork =>
            println ( "Shop : Closing — " + customersTrimmed + " trimmed and " + customersTurnedAway + " turned away." )
            open = false
            world ! CloseShop
        }
      }
    }
    lazy val world : Actor = Actor.actor {
      //  We know that the Vogon constructor fleet is coming to destroy the world to make way for an
      //  hyperspace by-pass.
      var notVogoned = true
      var customersTurnedAway = 0
      var customersTrimmed = 0
      while ( notVogoned ) {
        var id = 0
        def message ( id : Int ) = { println ( "World : Customer " + id + " exits the shop." ) }
        receive {
          case customer : Customer =>
            customersTurnedAway += 1
            message ( customer.id )
          case successfulCustomer : SuccessfulCustomer =>
            customersTrimmed += 1
            message ( successfulCustomer.customer.id )
          case CloseShop =>
            println ( "\nTrimmed " + customersTrimmed + " and turned away " + customersTurnedAway + " today." )
            notVogoned = false
        }
      }
      println ( "World : The Vogons have arrived. Time to explode." )
    }
    barber.start ( )
    shop.start ( )
    world.start ( )
    for ( number <- 0 to numberOfCustomers ) {
      Thread.sleep ( nextCustomerWaitingTime ( ) )
      println ( "World : Customer " + number + " enters the shop." )
      shop ! new Customer ( number )
    }
    shop ! CloseShop
  }
  val r = new Random ( )
  runSimulation ( 20 , 4 , ( ) => r.nextInt ( 6 ) + 1 , ( ) => r.nextInt ( 2 ) + 1 )
}
  
