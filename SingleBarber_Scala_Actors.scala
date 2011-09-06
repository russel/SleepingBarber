//  This is a model of the "The Sleeping Barber" problem using Scala and its primitive actors,
//  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2011 Russel Winder

import scala.actors.Actor
import scala.actors.Actor._
import scala.util.Random

object SingleBarber_Scala_Actors extends App {
  case class Customer ( id : Int )
  case class SuccessfulCustomer ( c : Customer )
  case object CloseShop
  case object BarberStoppedWork
  def runSimulation ( numberOfCustomers : Int , numberOfWaitingSeats : Int , hairTrimTime : ( ) => Int , nextCustomerWaitingTime : ( ) => Int ) {
    //  Due to the cross-referencesof the various actor objects, the type inference system cannot resolve
    //  the types appropriately, and so the types of some of the actors must be specified, so delcare the
    //  types of all of them.  Moreover, the definitions have to be lazy so as to deal with the order of use
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
            println ( "Shop : Customer " + successfulCustomer.c.id + " leaving trimmed." )
            world ! successfulCustomer
          case CloseShop =>
            barber ! CloseShop
          case BarberStoppedWork =>
            println ( "Shop : Closing — " + customersTrimmed + " trimmed and " + customersTurnedAway + " turned away." )
            open = false
        }
      }
    }
    lazy val world : Actor = Actor.actor {
      for ( number <- 0 to numberOfCustomers ) {
        Thread.sleep ( nextCustomerWaitingTime ( ) )
        println ( "World : Customer " + number + " enters the shop." )
        shop ! new Customer ( number )
      }
      var customersTurnedAway = 0
      var customersTrimmed = 0
      for ( number <- 0 to numberOfCustomers ) {
        var id = 0
        receive {
          case customer : Customer =>
            customersTurnedAway += 1
            id = customer.id
          case successfulCustomer : SuccessfulCustomer =>
            customersTrimmed += 1
            id = successfulCustomer.c.id
        }
        println ( "World : Customer " + id + " exits the shop." )
      }
      println ( "World : Time to close up." )
      shop ! CloseShop
      println ( "\nTrimmed " + customersTrimmed + " and turned away " + customersTurnedAway + " today." )
    }
    barber.start ( )
    shop.start ( )
    world.start ( )
  }
  val r = new Random ( )
  runSimulation ( 20 , 4 , ( ) => r.nextInt ( 60 ) + 10 , ( ) => r.nextInt ( 20 ) + 10 )
}
  
