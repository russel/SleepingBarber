#! /usr/bin/env python
# -*- mode:python; coding:utf-8; -*-

#  This is a model of the "The Sleeping Barber" problem using Python-CSP,
#  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
#
#  Copyright © 2009–2011 Russel Winder

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
        customer = fromShopIn.read ( )
        assert isinstance ( customer , Customer )
        print ( 'Barber : Starting Customer ' + str ( customer.id ) )
        time.sleep ( hairTrimTime ( ) )
        print ( 'Barber : Finished Customer ' + str ( customer.id ) )
        toShopOut.write ( customer )

@process
def shopIn ( numberOfWaitingSeats , fromWorld , toBarber , toAccounts , fromShopOut ) :
    seatsTaken = 0
    try :
        while True :
            print ( 'XXXX: Awaiting a customer . . .' )
            #alt =  Alt ( fromWorld , fromShopOut )
            #customer = alt.select ( )
            customer = fromWorld.read ( )
            assert isinstance ( customer , Customer )
            if seatsTaken <= numberOfWaitingSeats :
                seatsTaken += 1
                print ( 'Shop : Customer ' + str ( customer.id ) + ' takes a seat. ' + str ( seatsTaken ) + ' in use.' )
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
        assert isinstance ( customer , Customer )
        print ( 'Shop : Customer ' + str ( customer.id ) + ' leaving trimmed.' )
        toAccounts.write ( customer )
        #toShopIn.write ( customer )

@process
def accounts ( fromShopIn , fromShopOut ) :
    customersTurnedAway = 0
    customersTrimmed = 0
    try :
        while True :
            alt = Alt ( fromShopIn , fromShopOut )
            customer = alt.select ( )
            assert isinstance ( customer , Customer )
            print ( 'XXXX: alt.last_selected = ' + str ( alt.last_selected ) )
            print ( 'XXXX: customer = ' + str ( customer ) )
            print ( 'XXXX: customer.id = ' + str ( customer.id ) )
            if alt.last_selected == fromShopIn :
                customersTurnedAway += 1
            elif alt.last_selected == fromShopOut :
                customersTrimmed += 1
            else :
                raise ValueError ( 'Incorrect return from Alt.' )
            print ( 'XXXX: customersTurnedAway = ' + str ( customersTurnedAway ) )
            print ( 'XXXX: customersTrimmed = ' + str ( customersTrimmed ) )
    except ChannelPoison :
        print ( '\nTrimmed ' + str ( customersTrimmed ) + ' and turned away ' + str ( customersTurnedAway ) + ' today.' )
        print ( 'Find a sensible way of terminating all the processes.' )

@process
def world ( numberOfCustomers , nextCustomerWaitTime , toShopIn ) :
    for i in range ( numberOfCustomers ) :
        time.sleep ( nextCustomerWaitTime ( ) )
        print ( 'World : Customer ' + str ( i ) + ' enters the shop.' )
        toShopIn.write ( Customer ( i ) )
    toShopIn.poison ( )

def main ( numberOfCustomers , numberOfWaitingSeats , nextCustomerWaitTime , hairTrimTime ) :
    worldToShopIn = Channel ( )
    shopOutToShopIn = Channel ( )
    toBarber = Channel ( )
    toShopOut = Channel ( )
    shopInToAccounts = Channel ( )
    shopOutToAccounts = Channel ( )
    Par (
        barber ( hairTrimTime , toBarber , toShopOut ) ,
        shopIn ( numberOfWaitingSeats , worldToShopIn , toBarber , shopInToAccounts , shopOutToShopIn ) ,
        shopOut ( toShopOut , shopOutToAccounts , shopOutToShopIn ) ,
        accounts ( shopInToAccounts , shopOutToAccounts ) ,
        world ( numberOfCustomers , nextCustomerWaitTime , worldToShopIn ) ,
        ).start ( )
    
if __name__ == '__main__' :
    main ( 20 ,  4 , lambda : random.random ( ) * 0.002 + 0.001 , lambda : random.random ( ) * 0.006 + 0.001 )
