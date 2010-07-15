#! /usr/bin/env python
# -*- mode:python; coding:utf-8; -*-

#  This is a model of the "The Sleeping Barber" problem,
#  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
#
#  Copyright © 2009-10 Russel Winder

#  The barber's shop and the barber are modelled using processes each with their own event queue.  In effect
#  this is an Actor Model reactor approach with each process using a blocking read on its event queue.  The
#  waiting seats are modelled by the barber's event queue the size of which is managed by the shop.  This
#  arrangement will not support multiple barbers.

import multiprocessing
import time
import random
try :
    import queue # Python 3
except :
    import Queue # Python 2

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
        print ( 'Starting work.' )
        self.start ( )
    def run ( self ) :
        while True :
            customer = self.queue.get ( )
            assert type ( customer ) == Customer
            print ( 'Barber : Starting Customer ' + str ( customer.id ) )
            time.sleep ( self.hairTrimTime ( ) )
            print ( 'Barber : Finished Customer ' + str ( customer.id ) )
            self.shop.queue.put ( SuccessfulCustomer ( customer ) )

class BarbersShop ( multiprocessing.Process ) :
    def __init__ ( self , hairTrimTime ) :
        super ( BarbersShop , self ).__init__ ( )
        self.queue = multiprocessing.Queue ( )
        self.seatsTaken = 0
        self.customersProcessed = 0
        self.customersRejected = 0
        self.isOpen = True
        self.barber = Barber ( self , hairTrimTime )
        self.start ( )
    def run ( self ) :
        while True :
            event = self.queue.get ( )
            if type ( event ) == Customer :
                if self.seatsTaken < 4 :
                    self.seatsTaken += 1
                    print ( 'Shop : Customer ' + str ( event.id ) + ' takes a seat. ' + str ( self.seatsTaken ) + ' in use.' )
                    self.barber.queue.put ( event )
                else :
                    self.customersRejected += 1
                    print ( 'Shop : Customer ' + str ( event.id ) +' turned away.' )
            elif type ( event ) == SuccessfulCustomer :
                customer = event.customer
                assert type ( customer ) == Customer
                self.seatsTaken -= 1
                self.customersProcessed += 1
                print ( 'Shop : Customer ' + str ( customer.id ) + ' leaving trimmed.' )
                if ( not self.isOpen ) and ( self.seatsTaken == 0 ) :
                    print ( 'Processed ' + str ( self.customersProcessed ) + ' customers and rejected ' + str ( self.customersRejected ) + ' today.' )
                    self.barber.terminate ( )
                    return
            elif type ( event ) == str : self.isOpen = False
            else : raise ValueError ( 'Object of unexpected type received.' )

def main ( numberOfCustomers , nextCustomerWaitTime , hairTrimTime ) :
    shop = BarbersShop ( hairTrimTime )
    #  In Python 2 would use xrange here but use range for Python 3 compatibility.
    for i in range ( numberOfCustomers ) :
        time.sleep ( nextCustomerWaitTime ( ) )
        shop.queue.put ( Customer ( i ) )
    shop.queue.put ( '' )
    shop.join ( )

if __name__ == '__main__' :
    main ( 20 ,  lambda : random.random ( ) * 0.2 + 0.1 , lambda : random.random ( ) * 0.6 + 0.1 )
