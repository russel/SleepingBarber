//  This is a model of the "The Sleeping Barber" problem using Go (http://go-lang.org),
 //  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2010–2012 Russel Winder

//  Go's concurrency/parallelism model is (effectively) that of CSP: sequential processes (goroutines)
//  sending synchronous messages to each other on channels.  Go allows for buffered channels in which case
//  message passing is asynchronous -- sort of, there is blocking if a send is made to a full chennel. Thus
//  goroutines can be made to work like actors if so desired.
//
//  This implementation of the solution uses synchronous messaging throughout even with the use of a
//  buffered channel to represent the waiting seats in the shop.

package main

import (
	"fmt"
	"math/rand"
	"time"
)

type Customer struct { id int ; successful bool }

func barber ( hairTrimTime func ( ) time.Duration , fromShopChannel <-chan *Customer , toShopChannel chan<- *Customer ) {
	customersTrimmed := 0
	for working := true ; working ; {
		customer := <- fromShopChannel
		if customer.id == -1 {
			working = false
			fmt.Printf ( "Barber : Knocking off time, trimed %d customers.\n" , customersTrimmed )
		} else {
			fmt.Printf ( "Barber : Starting Customer %d\n" , customer.id )
			time.Sleep ( hairTrimTime ( ) )
			customersTrimmed ++
			customer.successful = true
			fmt.Printf ( "Barber : Finished Customer %d\n" , customer.id )
		}
		toShopChannel <- customer
	}
}

func shop ( numberOfSeats int , fromWorldChannel <-chan *Customer , toBarberChannel chan<- *Customer ,
	fromBarberChannel <-chan *Customer , toWorldChannel chan<- *Customer ) {
	//  Have to manually track the number of people waiting so that there is always a space fo rthe
	//  "closing" Customer object.
	seatsFilled := 0
	customersTurnedAway := 0
	customersTrimmed := 0
	for isOpen := true ; isOpen ; {
		var customer *Customer
		select {
		case customer = <- fromWorldChannel :
			if customer.id == -1 {
				toBarberChannel <- customer
			} else {
				// Sends always blocks so pre-calculate the state of the channel.
				if seatsFilled <= numberOfSeats {
					seatsFilled++
					fmt.Printf ( "Shop : Customer %d takes a seat. %d in use.\n" , customer.id , seatsFilled )
					toBarberChannel <- customer
				} else {
					customersTurnedAway++
					fmt.Printf ( "Shop : Customer %d turned away.\n" , customer.id )
					toWorldChannel <- customer
				}
			}
		case customer = <- fromBarberChannel :
			if customer.id == -1 {
				isOpen = false
				fmt.Printf ( "Shop closing, %d trimmed and %d turned away.\n" ,  customersTrimmed , customersTurnedAway )
			} else {
				customersTrimmed++
				seatsFilled--
				fmt.Printf ( "Shop : Customer %d leaving trimmed.\n" , customer.id )
			}
			toWorldChannel <- customer
		}
	}
}

func world ( fromShopChannel <-chan *Customer , terminationChannel chan<- *Customer ) {
	customersTurnedAway := 0
	customersTrimmed := 0
	for exists := true ; exists ; {
		customer := <- fromShopChannel
		if customer.id == -1 {
			exists = false
			fmt.Printf ( "\nTrimmed %d and turned away %d today.\n" ,  customersTrimmed , customersTurnedAway )
			terminationChannel <- customer
		} else {
			if customer.successful {
				customersTrimmed++
			} else {
				customersTurnedAway++
			}
			fmt.Printf ( "World : Customer %d exits the shop.\n" , customer.id )
		}
	}
}

func runSimulation ( numberOfCustomers , numberOfSeats int , nextCustomerWaitTime , hairTrimTime func ( ) time.Duration ) {
	worldToShopChannel := make ( chan *Customer )
	shopToWorldChannel := make ( chan *Customer )
	//  Must ensure that the "closing" customer can be added to the waiting seats no matter what the
	//  state is.
	shopToBarberChannel := make ( chan *Customer , numberOfSeats + 1 )
	barberToShopChannel := make ( chan *Customer )
	terminationChannel := make ( chan *Customer ) 
	go barber ( hairTrimTime , shopToBarberChannel, barberToShopChannel )
	go shop ( numberOfSeats , worldToShopChannel , shopToBarberChannel , barberToShopChannel , shopToWorldChannel )
	go world ( shopToWorldChannel , terminationChannel )
	for i := 0 ; i < numberOfCustomers ; i++ {
		time.Sleep ( nextCustomerWaitTime ( ) )
		fmt.Printf ( "World : Customer %d enters the shop.\n" , i )
		worldToShopChannel <- & Customer { i , false }
	}
	worldToShopChannel <- & Customer { -1 , false }
	<- terminationChannel
}

func main ( ) {
	runSimulation ( 20 , 4 ,
		func ( ) time.Duration { return time.Duration ( ( rand.Float64 ( )  * 2 ) + 1 ) * time.Millisecond } ,
		func ( ) time.Duration { return time.Duration ( ( rand.Float64 ( )  * 6 ) + 1 ) * time.Millisecond } ,
	)
}
