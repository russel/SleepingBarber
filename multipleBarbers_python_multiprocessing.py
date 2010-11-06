#! /usr/bin/env python3
# -*- mode:python; coding:utf-8; -*-

#  This is a model of the "The Sleeping Barber" problem
#  (cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem) in Python (http://www.python.org).
#
#  Copyright © 2009-10 Russel Winder

#  The barber's shop is modelled as a process with an event queue, it receives events from the outside world
#  (new customers arriving) and from the barbers (customers with fully trimmed barnets).  The waiting chairs
#  are modelled as a common queue to which all the barbers have shared access.  Barbers are modelled with a
#  process getting clients from the queue -- a blocking read models being asleep in the cutting chair when
#  not actively processing a customer. In effect this is an Actor Model reactor approach with each process
#  using a blocking read on its event queue but with barbers sharing a queue.  Because we are using what is
#  effectively an Actor Model approach we have to use case classes so as to carry appropriate infomration in
#  the messages sent to the shop process.

#  This should work with both Python 2 and Python 3.  Use range everywhere, even where with Python 2 xrange
#  would be preferred so as to ensure it all works with both versions.

import multiprocessing
import random
import time

try :
    import queue # Python 3
except :
    import Queue as queue # Python 2

class Customer ( object ) :
    def __init__ ( self , id ) :
        self.id = id

class SuccessfulCustomer ( object ) :
    def __init__ ( self , customer ) :
        self.customer = customer

class Barber ( multiprocessing.Process ) :
    def __init__ ( self , shop , identity , hairTrimTime ) :
        super ( Barber , self ).__init__ ( )
        self.shop = shop
        self.identity = identity
        self.hairTrimTime = hairTrimTime
        self._message ( 'Starting work.' )
        self.start ( )
    def _message ( self , message ) :
        print  ( 'Barber ' + str ( self.identity ) + ' : ' + str ( message ) )
    def run ( self ) :
        while True :
            self._message ( 'Awaiting a customer.' )
            customer = self.shop.waitingSeats.get ( )
            assert type ( customer ) == Customer
            self._message ( 'Starting Customer ' + str ( customer.id ) )
            time.sleep ( self.hairTrimTime ( ) )
            self._message ( 'Finished Customer ' + str ( customer.id ) )
            self.shop.queue.put ( SuccessfulCustomer ( customer ) )

class BarbersShop ( multiprocessing.Process ) :
    def __init__ ( self , waitingSeatCount , barberCount , hairTrimTime ) :
        assert waitingSeatCount > 0 , 'Cannot have 0 or less waiting seats'
        assert barberCount > 0 , 'Must have some barbers"'
        super ( BarbersShop , self ).__init__ ( )
        self.queue = multiprocessing.Queue ( )
        self.waitingSeats = multiprocessing.Queue ( waitingSeatCount )
        self.customersArrived = 0
        self.customersProcessed = 0
        self.customersRejected = 0
        self.isOpen = True
        self.barbers = [ Barber ( self , i , hairTrimTime ) for i in range ( barberCount ) ]
        self.start ( )
    def run ( self ) :
        while True :
            event = self.queue.get ( )
            if type ( event ) == Customer :
                if not self.isOpen :
                    print ( 'Shop : Sorry we are closed. Customer' + str ( event.id ) )
                else :
                    self.customersArrived += 1
                    try :
                        self.waitingSeats.put_nowait ( event )
                        print ( 'Shop : Customer ' + str ( event.id ) + ' takes a seat. ' + str ( self.waitingSeats.qsize ( ) ) + ' in use.' )
                    except queue.Full :
                        self.customersRejected += 1
                        print ( 'Shop : Customer ' + str ( event.id ) + ' turned away.' )
            elif type ( event ) == SuccessfulCustomer :
                customer = event.customer
                assert type ( customer ) == Customer
                self.customersProcessed += 1
                print ( 'Shop : Customer ' + str ( customer.id ) + ' leaving trimmed.' )
                if ( not self.isOpen ) and ( self.customersRejected + self.customersProcessed == self.customersArrived ) :
                    assert self.queue.empty ( )
                    for barber in self.barbers : barber.terminate ( )
                    print ( 'Processed ' +  str ( self.customersProcessed ) + ' customers and rejected ' + str ( self.customersRejected ) + ' today.' )
                    return
            elif type ( event ) == str : self.isOpen = False
            else : raise ValueError ( 'Object of unexpected type received.' )

def main ( numberOfWaitingSeats , numberOfBarbers , numberOfCustomers , nextCustomerWaitTime , hairTrimTime ) :
    shop = BarbersShop ( numberOfWaitingSeats , numberOfBarbers , hairTrimTime )
    for i in range ( numberOfCustomers ) :
        time.sleep ( nextCustomerWaitTime ( ) )
        shop.queue.put ( Customer ( i ) )
    shop.queue.put ( '' )
    shop.join ( )

if __name__ == '__main__' :
    #  If waiting seat count is 0 then it all goes wrong.
    main ( 8 , 4 , 1000 , lambda : random.random ( ) * 0.0002 + 0.0001 , lambda : random.random ( ) * 0.0008 + 0.0001 )
