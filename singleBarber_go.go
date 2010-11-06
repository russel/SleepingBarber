//  This is a model of the "The Sleeping Barber" problem using Go (http://go-lang.org),
 //  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2010 Russel Winder

//  Go does not support Actors or active objects directly, but it has processes with message passing between
//  processes.  We therefore model the shop and barber as processes.  Customers are passed as message between
//  the processes.

package main

import (
	"fmt"
	"rand"
	"time"
)

type Customer struct { id int }

func barber ( hairTrimTime func ( ) int64 , fromShopChannel , toShopChannel chan *Customer ) {
	for {
		customer := <- fromShopChannel
		fmt.Printf ( "Barber : Starting Customer %d\n" , customer.id )
		time.Sleep ( hairTrimTime ( ) )
		fmt.Printf ( "Barber : Finished Customer %d\n" , customer.id )
		toShopChannel <- customer
	}
}

func shop ( numberOfSeats int , fromWorldChannel , toWorldChannel , fromBarberChannel , toBarberChannel chan *Customer ) {
	shopClosing := false
	seatsFilled := 0
	customersRejected := 0
	customersTrimmed := 0
	for {
		var customer *Customer
		select {
		case customer = <- fromWorldChannel :
			if customer.id == -1 {
				shopClosing = true
			} else {
				if seatsFilled <= numberOfSeats {
					seatsFilled++
					fmt.Printf ( "Shop : Customer %d takes a seat. %d in use .\n" , customer.id , seatsFilled )
					toBarberChannel <- customer
				} else {
					customersRejected++
					fmt.Printf ( "Shop : Customer %d turned away.\n" , customer.id )
				}
			}
		case customer = <- fromBarberChannel :
			customersTrimmed++
			seatsFilled--
			fmt.Printf ( "Shop : Customer %d leaving trimmed.\n" , customer.id )
			if shopClosing && ( seatsFilled == 0 ) {
				fmt.Printf ( "\nTrimmed %d customers and rejected %d today.\n" ,  customersTrimmed , customersRejected )
				toWorldChannel <- & Customer { -1 }
			}
		}
	}
}

func world ( numberOfCustomers , numberOfSeats int , nextCustomerWaitTime , hairTrimTime func ( ) int64 ) {
	worldToShopChannel := make ( chan *Customer )
	shopToWorldChannel := make ( chan *Customer )
	shopToBarberChannel := make ( chan *Customer , numberOfSeats )
	barberToShopChannel := make ( chan *Customer )
	go barber ( hairTrimTime , shopToBarberChannel, barberToShopChannel )
	go shop ( numberOfSeats , worldToShopChannel , shopToWorldChannel, barberToShopChannel , shopToBarberChannel )
	for i := 0 ; i < numberOfCustomers ; i++ {
		time.Sleep ( nextCustomerWaitTime ( ) )
		fmt.Printf ( "World : Customer %d enters the shop.\n" , i )
		worldToShopChannel <- & Customer { i }
	}
	worldToShopChannel <- & Customer { -1 }
	<-shopToWorldChannel
}

func main ( ) {
	world ( 20 , 4 ,
		func ( ) int64 { return int64 ( rand.Float ( )  * 2000000 ) + 1000000 } ,
		func ( ) int64 { return int64 ( rand.Float ( )  * 6000000 ) + 1000000 }
	)
}
