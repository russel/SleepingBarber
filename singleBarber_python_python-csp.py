#! /usr/bin/env python
# -*- mode:python; coding:utf-8; -*-

#  This is a model of the "The Sleeping Barber" problem using Python-CSP,
#  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
#
#  Copyright © 2009-10 Russel Winder

#  The barber's shop and the barber are modelled with processes.  Channels are used to pass customer objects
#  from the shop to the barber.  The current arrangement assumes there is only one barber.

#  This should work with both Python 2 and Python 3.  Use range everywhere, even where with Python 2 xrange
#  would be preferred so as to ensure it all works with both versions.

import time
import random

from csp.cspprocess import process , Channel , Par , Alt , ChannelPoison

class Customer ( object ) :
    def __init__ ( self , id ) :
        self.id = id

@process
def barber ( hairTrimTime , fromShopIn , toShopOut ) :
    while True :
        #  Barber blocks awaiting customers from the shop, this is "sleeping in his chair".
        customer = fromShopIn.read ( )
        assert type ( customer ) == Customer
        print ( 'Barber : Starting Customer ' + str ( customer.id ) )
        time.sleep ( hairTrimTime ( ) )
        print ( 'Barber : Finished Customer ' + str ( customer.id ) )
        toShopOut.write ( customer )

@process
def shopIn ( fromWorld , toBarber , toAccounts , fromShopOut ) :
    seatsTaken = 0
    try :
        while True :
            print ( 'Awaiting a customer . . .' )
            #alt =  Alt ( fromWorld , fromShopOut )
            #customer = alt.select ( )
            customer = fromWorld.read ( )
            print ( 'Got customer ' + str ( customer ) )
            assert type ( customer ) == Customer
            if seatsTaken < 4 :
                seatsTaken += 1
                print ( 'Shop : Customer ' + str ( customer.id ) + ' takes a seat. ' + str ( seatsTaken ) + ' seat(s) taken.' )
                toBarber.write ( customer )
            else :
                print ( 'Shop : Customer ' + str ( customer.id ) + ' turned away.' )
                toAccounts.write ( customer )
    except ChannelPoison :
        toAccounts.poison ( )

@process
def shopOut ( fromBarber , toAccounts , toShopIn ) :
    while True :
        customer = fromBarber.read ( )
        assert type ( customer ) == Customer
        print ( 'Shop : Customer ' + str ( customer.id ) + ' leaving trimmed.' )
        toAccounts.write ( customer )
        #toShopIn.write ( customer )

@process
def accounts ( fromShopIn , fromShopOut ) :
    rejectedCustomers = 0
    trimmedCustomers = 0
    try :
        while True :
            alt = Alt ( fromShopIn , fromShopOut )
            customer = alt.select ( )
            assert type ( customer ) == Customer
            print ( 'XXXX: alt.last_selected = ' + str ( alt.last_selected ) )
            print ( 'XXXX: customer = ' + str ( customer ) )
            print ( 'XXXX: customer.id = ' + str ( customer.id ) )
            if alt.last_selected == fromShopIn :
                rejectedCustomers += 1
            elif alt.last_selected == fromShopOut :
                trimmedCustomers += 1
            else :
                raise ValueError ( 'Incorrect return from Alt.' )
            print ( 'XXXX: rejectedCustomers = ' + str ( rejectedCustomers ) )
            print ( 'XXXX: trimmedCustomers = ' + str ( trimmedCustomers ) )
    except ChannelPoison :
        print ( 'Processed ' + str ( rejectedCustomers + trimmedCustomers ) + ' customers and rejected ' + str ( rejectedCustomers ) + ' today.' )
        print ( 'Find a sensible way of terminating all the processes.' )

@process
def world ( numberOfCustomers , customerArrivalTime , toShopIn ) :
    for i in range ( numberOfCustomers ) :
        time.sleep ( customerArrivalTime ( ) )
        toShopIn.write ( Customer ( i ) )
    toShopIn.poison ( )

def main ( numberOfCustomers , customerArrivalTime , hairTrimTime ) :
    worldToShopIn = Channel ( )
    shopOutToShopIn = Channel ( )
    toBarber = Channel ( )
    toShopOut = Channel ( )
    shopInToAccounts = Channel ( )
    shopOutToAccounts = Channel ( )
    Par (
        barber ( hairTrimTime , toBarber , toShopOut ) ,
        shopIn ( worldToShopIn , toBarber , shopInToAccounts , shopOutToShopIn ) ,
        shopOut ( toShopOut , shopOutToAccounts , shopOutToShopIn ) ,
        accounts ( shopInToAccounts , shopOutToAccounts ) ,
        world ( numberOfCustomers , customerArrivalTime , worldToShopIn ) ,
        ).start ( )
    
if __name__ == '__main__' :
    main ( 20 ,  lambda : random.random ( ) * 0.2 + 0.1 , lambda : random.random ( ) * 0.6 + 0.1 )
