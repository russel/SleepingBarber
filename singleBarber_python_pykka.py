#! /usr/bin/env python
# -*- coding:utf-8; -*-

#  This is a model of the "The Sleeping Barber" problem using Python and the multiprocessing package,
#  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
#
#  Copyright © 2009–2011 Russel Winder

import random
import time

#from pykka.actor import ThreadingActor as PykkaActor
from pykka.gevent import GeventActor as PykkaActor

class Customer(object):
    def __init__(self, id):
        self.id = id

class SuccessfulCustomer(object):
    def __init__(self, customer):
        self.customer = customer

tag = 'customer'

class Barber(PykkaActor):
    def __init__(self, hairTrimTime):
        self.customersTrimmed = 0
        self.shop = None
        self.hairTrimTime = hairTrimTime

    def on_receive(self, message):
        customer = message[tag]
        if isinstance(customer, Customer):
            print('Barber: Starting Customer ' + str(customer.id))
            time.sleep(self.hairTrimTime())
            customersTrimmed += 1
            print('Barber: Finished Customer ' + str(customer.id))
            self.shop.send_one_way(SuccessfulCustomer(customer))
        elif isinstance(customer, str) and customer == 'closing':
            print('Barber: Work over for the day, trimmed ' + str(customersTrimmed) + ' today.')
            shop.send_one_way({tag: 'clockedOff'})
        else:
            raise ValueError('Barber got a customer of unexpected type ' + type(customer))

class Shop(PykkaActor):
    def __init__(self, numberOfWaitingSeats, barber, world):
        self.numberOfWaitingSeats = numberOfWaitingSeats
        self.barber = barber
        self.world = world
        self.seatsTaken = 0
        self.customersTrimmed = 0
        self.customersTurnedAway = 0

    def on_receive(self, message):
        customer = message[tag]
        if isinstance(customer, Customer):
            if self.seatsTaken < self.numberOfWaitingSeats:
                self.seatsTaken += 1
                print('Shop: Customer ' + str(event.id) + ' takes a seat. ' + str(self.seatsTaken) + ' in use.')
                self.barber.send_one_way({tag: customer})
            else:
                self.customersTurnedAway += 1
                print('Shop: Customer ' + str(event.id) + ' turned away.')
                self.world.send_one_way({tag: customer})
        elif isinstance(event, SuccessfulCustomer):
            successfulCustomer = customer.customer
            self.seatsTaken -= 1
            self.customersTrimmed += 1
            print('Shop: Customer ' + str(customer.id) + ' leaving trimmed.')
            self.world.send_one_way({tag: customer})
        elif isinstance(customer, str) and customer == 'closing':
            self.barber.send_one_way({tag: 'closing'})
        elif isinstance(customer, str) and customer == 'clockedOff':
            print('\nTrimmed ' + str(self.customersTrimmed) + ' and turned away ' + str(self.customersTurnedAway) + ' today.')
            self.world.send_one_way({tag: 'closed'})
        else:
            raise ValueError('shop got an unexpected message of type ' + type(event))

class World(PykkaActor):
    def __init__(self):
        self.customersTurnedAway = 0
        self.customersTrimmed = 0

    def on_receive(self, message):
        customer = message[tag]

        def customerExit(identifier):
            print('World: Customer ' + str(identifier) + ' exits the shop.')

        if isinstance(customer, Customer):
            self.customersTurnedAway += 1
            customerExit(customer.id)
        elif isinstance(customer, SuccessfulCustomer):
            self.customersTrimmed += 1
            customerExit(customer.customer.id)
        elif isinstance(customer, str) and customer == 'closed':
            print('\nTrimmed {0} and turned away {1} today.'.format(self.customersTrimmed, self.customersTurnedAway))
        else:
            raise ValueError('World got a message of unexpected type ' + type(customer))

def runSimulation(numberOfCustomers, numberOfWaitingSeats, nextCustomerWaitTime, hairTrimTime):
    world = World.start()
    barber = Barber.start(hairTrimTime)
    shop = Shop.start(numberOfWaitingSeats, barber, world)
    barber.shop = shop
    for i in range(numberOfCustomers):
        time.sleep(nextCustomerWaitTime())
        print('World: Customer ' + str(i) + ' enters the shop.')
        shop.send_one_way({tag: Customer(i)})
    shop.send_one_way({tag: 'closing'})

if __name__ == '__main__':
    runSimulation(20,  4, lambda: random.random() * 0.002 + 0.001, lambda: random.random() * 0.006 + 0.001)
