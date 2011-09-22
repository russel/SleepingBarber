//  This is a model of the "The Sleeping Barber" problem using Go (http://go-lang.org),
 //  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2010 Russel Winder

//  Go does not support Actors or active objects, its concurrency model is effectively that of CSP:
//  processes with synchronous message passing.

package main

import (
	"fmt"
	"rand"
	"time"
)

type Customer struct { id int ; successful bool }

func barber ( hairTrimTime func ( ) int64 , fromShopChannel , toShopChannel chan *Customer ) {
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

func shop ( numberOfSeats int , fromWorldChannel , toBarberChannel , fromBarberChannel , toWorldChannel chan *Customer ) {
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

func world ( fromShopChannel , terminationChannel chan *Customer ) {
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

func runSimulation ( numberOfCustomers , numberOfSeats int , nextCustomerWaitTime , hairTrimTime func ( ) int64 ) {
	worldToShopChannel := make ( chan *Customer )
	shopToWorldChannel := make ( chan *Customer )
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
		func ( ) int64 { return int64 ( rand.Float64 ( )  * 200000 ) + 100000 } ,
		func ( ) int64 { return int64 ( rand.Float64 ( )  * 600000 ) + 100000 } ,
	)
}
