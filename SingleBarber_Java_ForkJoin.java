//  This is a model of the "The Sleeping Barber" problem using Java only, but with all the JSR166y on Java 6
//  so as to emulate the possibilities of Java 7, cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2011 Russel Winder

//  Use the default package.

import java.util.concurrent.ArrayBlockingQueue ;
import java.util.concurrent.Callable ;
import java.util.concurrent.ExecutionException ;

import jsr166y.ForkJoinPool ;
import jsr166y.ForkJoinTask ;

public class SingleBarber_Java_ForkJoin {
  private final static class Customer {
    public final int id ;
    public Customer ( final int id ) { this.id = id ; }
  }
  private final static class RandomCallingFunction {
    private final int scale ;
    private final int offset ;
    public RandomCallingFunction ( final int scale , final int offset ) {
      this.scale = scale ;
      this.offset = offset ;
    }
    public int call ( ) { return (int)( Math.random ( ) * scale ) + offset ; }
  }
  private void runSimulation ( final int numberOfCustomers , final int numberOfWaitingSeats ,
                    final RandomCallingFunction hairTrimTime , final RandomCallingFunction nextCustomerWaitTime ) {
    final ForkJoinPool pool = new ForkJoinPool ( ) ;
    final ArrayBlockingQueue<Customer> waitingChairs = new ArrayBlockingQueue<Customer> ( numberOfWaitingSeats ) ;
    final ForkJoinTask<Integer> barber = pool.submit ( new Callable<Integer> ( ) {
        @Override public Integer call ( ) {
          int customersTrimmed = 0 ;
          while ( true ) {
            try {
              final Customer customer = waitingChairs.take ( ) ;
              if ( customer.id < 0 ) break ;
              System.out.println ( "Barber : Starting Customer " + customer.id + "." ) ;
              try { Thread.sleep ( hairTrimTime.call ( ) ) ; }
              catch ( InterruptedException iiee ) { /* Intentionally left blank. */ }
              System.out.println ( "Barber : Finished Customer " + customer.id + "." ) ;
              ++customersTrimmed ;
              System.out.println ( "Shop : Customer " + customer.id + " leaving trimmed." ) ;
            }
            catch ( InterruptedException ie ) { ie.printStackTrace ( ) ; }
          }
          return customersTrimmed ;
        }
      } ) ;
    int customersTurnedAway = 0 ;
    for ( int number = 0 ; number < numberOfCustomers ; ++number ) {
      try { Thread.sleep ( nextCustomerWaitTime.call ( ) ) ; }
      catch ( InterruptedException ie ) { /* Intentionally left blank. */ }
      System.out.println ( "World : Customer " + number + " enters the shop." ) ;
      final Customer customer = new Customer ( number ) ;
      if ( waitingChairs.offer ( customer ) ) {
        System.out.println ( "Shop : Customer " + customer.id + " takes a seat. " + waitingChairs.size ( ) + " in use." ) ;
      }
      else {
        ++customersTurnedAway ;
        System.out.println ( "Shop : Customer " + customer.id + " turned away." ) ;
      }
    }
    try { waitingChairs.put ( new Customer ( -1 ) ) ; }
    catch ( InterruptedException ie ) { /* Intentionally left blank. */ }
    System.out.println ( "\nTrimmed " + barber.join ( ) + " and turned away " + customersTurnedAway + " today." ) ;
    pool.shutdown ( ) ;
   }
  public static void main ( final String[] args ) {
    ( new SingleBarber_Java_ForkJoin ( ) ).runSimulation ( 20 , 4 ,
                                                           new RandomCallingFunction ( 60 , 10 ) ,
                                                           new RandomCallingFunction ( 20 , 10 ) ) ;
  }
}


  
