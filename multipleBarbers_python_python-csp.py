#! /usr/bin/env python
# -*- mode:python; coding:utf-8; -*-

#  This is a model of the "The Sleeping Barber" problem,
#  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
#
#  Copyright © 2009 Russel Winder

#  The barber's shop and the barber are modelled with processes.  Channels are used to pass customer objects
#  from the shop to the barber.  The current arrangement assumes there is only one barber.

#  The initial expectation for the behaviour of alt.select ( ) in:
#
#    alt = Alt ( channel_A , channel_B )
#    returnValue = alt.select ( )
#
#  is that returnValue is the channel ready to be read.  Python-CSP however merges the select and read and
#  so does the read and returnValue is the datum from the alt.select ( ) (Mount, personal communication).
#  This means we cannot select on the channel and hence cannot just ship round Customer objects, we have to
#  box the Customer object in a case class instance so that we can select on the type of the datum.  In this
#  sense Python-CSP is actually closer to Actor Model semantics that to CSP despite being an implementation
#  of CSP.

import time
import random

from csp.cspprocess import *

class Customer ( object ) :
    def __init__ ( self , id ) :
        self.id = id

class SuccessfulCustomer ( object ) :
    def __init__ ( self , customer ) :
        self.customer = customer

@process
def barber ( shopChannel , identity , hairTrimTime , _process = None ) :
    def _message ( message ) :
        print  'Barber' , identity , ':' , message
    _message ( 'Starting work.' )
    while True :
        customer = shopChannel.read ( )
        assert type ( customer ) == Customer
        _message ( 'Starting on Customer ' + str ( customer.id ) )
        time.sleep ( random.random ( ) * 0.6 + 0.1 )
        _message ( 'Finished Customer' + str ( customer.id ))
        shopChannel.write ( SuccessfulCustomer ( customer ) )

@process
def shop ( worldChannel , barberChannel , waitingSeatCount , barberCount , hairTrimTime , _process = None ) :
    seatsTaken = 0
    customerProcessed = 0
    customersRejected = 0
    isOpen = True
    while True :
        #  One might have anticipated that alt.select ( ) would return the channel that is ready to read,
        #  Python-CSP however has the return value being the datum already read.  So we need to use case
        #  classes to handle decision making.  Still this is how things are done with the Actore Model so no
        #  real problem.
        alt = Alt ( worldChannel , barberChannel )
        event = alt.select ( )
        if type ( event ) == Customer :
            if seatsTaken < 4 :
                seatsTaken += 1
                print 'Shop : Customer' , event.id , 'takes a seat.' , seatTaken , 'seats taken.'
                barberChannel.write ( event )
            else :
                customersRejected += 1
                print 'Shop : Customer' , event.id , 'turned away.'
        elif type ( event ) == SuccessfulCustomer :
            customer = event.customer
            assert type ( customer ) == Customer
            self.seatsTaken -= 1
            self.customersProcessed += 1
            print 'Shop : Customer' , customer.id , 'leaving trimmed.'
            if ( not self.isOpen ) and ( self.seatsTaken == 0 ) :
                print 'Processed' ,  self.customersProcessed , 'customers and rejected' , self.customersRejected , 'today.'
                self.barber.terminate ( )
                return
        elif type ( event ) == str : self.isOpen = False
        else : raise ValueError , 'Object of unexpected type received.'

@process
def world ( channel , _process = None ) :
    for i in range ( 20 ) :
        time.sleep ( random.random ( ) * 0.2 + 0.1 )
        channel.write ( Customer ( i ) )
    channel.write ( '' )

def main ( numberOfWaitingSeats , numberOfBarbers , numberOfCustomers , nextCustomerWaitTime , hairTrimTime ) :
    barberChannel = Channel ( )
    worldChannel = Channel ( )
    Par ( barber ( barberChannel ) , shop ( worldChannel , barberChannel ) , world ( worldChannel ) ).start ( )

if __name__ == '__main__' :
     main ( 8 , 4 , 1000 , lambda : random.random ( ) * 0.0002 + 0.0001 , lambda : random.random ( ) * 0.0008 + 0.0001 )
