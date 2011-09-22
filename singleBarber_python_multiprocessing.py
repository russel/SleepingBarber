#! /usr/bin/env python3
# -*- mode:python; coding:utf-8; -*-

#  This is a model of the "The Sleeping Barber" problem using Python and the multiprocessing package,
#  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
#
#  Copyright © 2009–2011 Russel Winder

#  The barber's shop and the barber are modelled using processes each with their own event queue.  In effect
#  this is an Actor Model reactor approach with each process using a blocking read on its event queue.  The
#  waiting seats are modelled by the barber's event queue the size of which is managed by the shop.  This
#  arrangement will not support multiple barbers.

import multiprocessing
import time
import random

class Customer ( object ) :
    def __init__ ( self , id ) :
        self.id = id

class SuccessfulCustomer ( object ) :
    def __init__ ( self , customer ) :
        self.customer = customer

class Barber ( multiprocessing.Process ) :
    def __init__ ( self , shop , hairTrimTime ) :
        super ( Barber , self ).__init__ ( )
        self.queue = multiprocessing.Queue ( )
        self.shop = shop
        self.hairTrimTime = hairTrimTime
        self.start ( )
    def run ( self ) :
        while True :
            customer = self.queue.get ( )
            assert isinstance ( customer , Customer )
            print ( 'Barber : Starting Customer ' + str ( customer.id ) )
            time.sleep ( self.hairTrimTime ( ) )
            print ( 'Barber : Finished Customer ' + str ( customer.id ) )
            self.shop.queue.put ( SuccessfulCustomer ( customer ) )

class Shop ( multiprocessing.Process ) :
    def __init__ ( self , numberOfWaitingSeats , hairTrimTime ) :
        super ( Shop , self ).__init__ ( )
        self.numberOfWaitingSeats = numberOfWaitingSeats
        self.queue = multiprocessing.Queue ( )
        self.seatsTaken = 0
        self.customersTrimmed = 0
        self.customersTurnedAway = 0
        self.isOpen = True
        self.barber = Barber ( self , hairTrimTime )
        self.start ( )
    def run ( self ) :
        while True :
            event = self.queue.get ( )
            if isinstance ( event , Customer ) :
                if self.seatsTaken < self.numberOfWaitingSeats :
                    self.seatsTaken += 1
                    print ( 'Shop : Customer ' + str ( event.id ) + ' takes a seat. ' + str ( self.seatsTaken ) + ' in use.' )
                    self.barber.queue.put ( event )
                else :
                    self.customersTurnedAway += 1
                    print ( 'Shop : Customer ' + str ( event.id ) +' turned away.' )
            elif isinstance ( event , SuccessfulCustomer ) :
                customer = event.customer
                assert isinstance ( customer , Customer )
                self.seatsTaken -= 1
                self.customersTrimmed += 1
                print ( 'Shop : Customer ' + str ( customer.id ) + ' leaving trimmed.' )
                if ( not self.isOpen ) and ( self.seatsTaken == 0 ) :
                    print ( '\nTrimmed ' + str ( self.customersTrimmed ) + ' and turned away ' + str ( self.customersTurnedAway ) + ' today.' )
                    self.barber.terminate ( )
                    return
            elif isinstance ( event , str ) : self.isOpen = False
            else : raise ValueError ( 'Object of unexpected type received.' )

def world ( numberOfCustomers , numberOfWaitingSeats , nextCustomerWaitTime , hairTrimTime ) :
    shop = Shop ( numberOfWaitingSeats , hairTrimTime )
    #  In Python 2 would use xrange here but use range for Python 3 compatibility.
    for i in range ( numberOfCustomers ) :
        time.sleep ( nextCustomerWaitTime ( ) )
        print ( 'World : Customer ' + str ( i ) + ' enters the shop.' )
        shop.queue.put ( Customer ( i ) )
    shop.queue.put ( '' )
    shop.join ( )

if __name__ == '__main__' :
    world ( 20 ,  4 , lambda : random.random ( ) * 0.002 + 0.001 , lambda : random.random ( ) * 0.006 + 0.001 )
