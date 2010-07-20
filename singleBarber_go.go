package main

import "fmt"

type Customer struct { id int }

func barber ( hairTrimTime func ( ) , shopChannel chan *Customer ) {
	for {
		customer := <- shopChannel
		fmt.Println ( "Barber : Starting Customer %d" , customer.id )
		hairTrimTime ( )
		fmt.Println ( "Barber : Finished Customer %d" , customer.id )
		shopChannel <- customer
	}
}

func shop ( worldChannel , barberChannel chan *Customer ) {
	
}

func world ( numberOfCustomers int , customerArrivalTime func ( ) , shopChannel chan *Customer ) {
	for i := 0 ; i < numberOfCustomers ; i++ {
		customer := new ( Customer )
		customer.id = i
		shopChannel <- customer
	}
}

func main ( ) {
	shopChannel := make ( chan *Customer )
	barberChannel := make ( chan *Customer )
	go barber ( func ( ) { } , barberChannel )
	go shop ( shopChannel , barberChannel )
	go world ( 20 , func ( ) { } , shopChannel )
}
