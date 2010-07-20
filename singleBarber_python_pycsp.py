#! /usr/bin/env python
# -*- mode:python; coding:utf-8; -*-

#  This is a model of the "The Sleeping Barber" problem using PyCSP,
#  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
#
#  Copyright © 2009-10 Russel Winder

#  The barber's shop and the barber are modelled with processes.  Channels are used to pass customer objects
#  from the shop to the barber.  The current arrangement assumes there is only one barber.

#  This should work with both Python 2 and Python 3.  Use range everywhere, even where with Python 2 xrange
#  would be preferred so as to ensure it all works with both versions.

import time
import random

from pycsp.processes import ( process , retire , poison , Channel , Parallel , AltSelect ,
                              InputGuard , OutputGuard , TimeoutGuard , ChannelPoisonException )

class Customer ( object ) :
    def __init__ ( self , id ) :
        self.id = id

@process
def barber ( hairTrimTime , fromShopIn , toShopOut ) :
    while True :
        #  Barber blocks awaiting customers from the shop, this is "sleeping in his chair".
        customer = fromShopIn ( )
        assert type ( customer ) == Customer
        print ( 'Barber : Starting Customer ' + str ( customer.id ) )
        time.sleep ( hairTrimTime ( ) )
        print ( 'Barber : Finished Customer ' + str ( customer.id ) )
        toShopOut ( customer )

@process
def shopIn ( fromWorld , toBarber , toAccounts ) :
    seats = [ ]
    def trySendingToBarber ( customer ) :
        channel , message = AltSelect ( OutputGuard ( toBarber , msg = seats[0] ) , TimeoutGuard ( seconds = 0.1 ) )
        if channel == toBarber : del customer
    try :
        while True :
            customer = fromWorld ( )
            assert type ( customer ) == Customer
            if len ( seats ) < 4 :
                seats.append ( customer )
                print ( 'Shop : Customer ' + str ( customer.id ) + ' takes a seat. ' + str ( len ( seats ) ) + ' seat(s) taken.' )
                channel , message = AltSelect ( OutputGuard ( toBarber , msg = seats[0] ) , TimeoutGuard ( seconds = 0.1 ) )
                if channel == toBarber : seats = seats[1:]
            else :
                if len ( seats ) > 0 :
                    channel , message = AltSelect ( OutputGuard ( toBarber , msg = seats[0] ) , TimeoutGuard ( seconds = 0.1 ) )
                    if channel == toBarber : seats = seats[1:]
                print ( 'Shop : Customer ' + str ( customer.id ) + ' turned away.' )
                toAccounts ( customer )
    except ChannelPoisonException :
        while ( len ( seats ) > 0 ) :
            toBarber ( seats[0] )
            seats = seats[1:]
        poison ( toBarber )

@process
def shopOut ( fromBarber , toAccounts ) :
    while True :
        customer = fromBarber ( )
        assert type ( customer ) == Customer
        print ( 'Shop : Customer ' + str ( customer.id ) + ' leaving trimmed.' )
        toAccounts ( customer )

@process
def accounts ( fromShopIn , fromShopOut ) :
    rejectedCustomers = 0
    trimmedCustomers = 0
    try :
        while True :
            channel , customer = AltSelect ( InputGuard ( fromShopIn ) , InputGuard ( fromShopOut ) )
            if channel == fromShopIn :
                rejectedCustomers += 1
            elif channel == fromShopOut :
                trimmedCustomers += 1
            else :
                raise ValueError ( 'Incorrect return from AltSelect.' )
    except ChannelPoisonException :
        print ( 'Processed ' + str ( rejectedCustomers + trimmedCustomers ) + ' customers and rejected ' + str ( rejectedCustomers ) + ' today.' )

@process
def world ( numberOfCustomers , customerArrivalTime , toShopIn ) :
    for i in range ( numberOfCustomers ) :
        time.sleep ( customerArrivalTime ( ) )
        toShopIn ( Customer ( i ) )
    poison ( toShopIn )

def main ( numberOfCustomers , customerArrivalTime , hairTrimTime ) :
    worldToShopIn = Channel ( )
    shopOutToShopIn = Channel ( )
    toBarber = Channel ( )
    toShopOut = Channel ( )
    shopInToAccounts = Channel ( )
    shopOutToAccounts = Channel ( )
    Parallel (
        barber ( hairTrimTime , toBarber.reader ( ) , toShopOut.writer ( ) ) ,
        shopIn ( worldToShopIn.reader ( ) , toBarber.writer ( ) , shopInToAccounts.writer ( ) ) ,
        shopOut ( toShopOut.reader ( ) , shopOutToAccounts.writer ( ) ) ,
        accounts ( shopInToAccounts.reader ( ) , shopOutToAccounts.reader ( ) ) ,
        world ( numberOfCustomers , customerArrivalTime , worldToShopIn.writer ( ) ) ,
        )
    
if __name__ == '__main__' :
    main ( 20 ,  lambda : random.random ( ) * 0.2 + 0.1 , lambda : random.random ( ) * 0.6 + 0.1 )
