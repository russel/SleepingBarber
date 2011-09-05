//  This is a model of the "The Sleeping Barber" problem using Java threads only,
//  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2011 Russel Winder

//  Use the default package.

import java.util.concurrent.ArrayBlockingQueue ;

public class SingleBarber_Java_Threads {
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
  private final static class Barber implements Runnable {
    private int customersTrimmed = 0 ;
    private boolean working = true ;
    private final ArrayBlockingQueue<Customer> waitingChairs ;
    final RandomCallingFunction hairTrimTime ;
    public Barber( final ArrayBlockingQueue<Customer> waitingChairs , final RandomCallingFunction hairTrimTime ) {
      this.waitingChairs = waitingChairs ;
      this.hairTrimTime = hairTrimTime ;
    }
    public void stopWork ( ) { working = false ; }
    @Override public void run ( ) {
      while ( working || ( waitingChairs.size ( ) > 0 ) ) {
        try {
          final Customer customer = waitingChairs.take ( ) ;
          System.out.println ( "Barber : Starting Customer " + customer.id + "." ) ;
          try { Thread.sleep ( hairTrimTime.call ( ) ) ; }
          catch ( InterruptedException iiee ) { /* Intentionally left blank. */ }
          System.out.println ( "Barber : Finished Customer " + customer.id + "." ) ;
          ++customersTrimmed ;
          System.out.println ( "Shop : Customer " + customer.id + " leaving trimmed." ) ;
        }
        catch ( InterruptedException ie ) { /* Intentionally left blank. */ }
      }
    }
    public int getCustomersTrimmed ( ) { return customersTrimmed ; }
  } 
  private void runSimulation ( final int numberOfCustomers , final int numberOfWaitingSeats ,
                    final RandomCallingFunction hairTrimTime , final RandomCallingFunction nextCustomerWaitTime ) {
    final ArrayBlockingQueue<Customer> waitingChairs = new ArrayBlockingQueue<Customer> ( numberOfWaitingSeats ) ;
    int customersTurnedAway = 0 ;
    int customersTrimmed = 0 ;
    final Barber barber = new Barber ( waitingChairs , hairTrimTime ) ;
    final Thread barberThread = new Thread ( barber ) ;
    barberThread.start ( ) ;
    for ( int number = 0 ; number < numberOfCustomers ; ++number ) {
      try { Thread.sleep ( nextCustomerWaitTime.call ( ) ) ; }
      catch ( InterruptedException iiee ) { /* Intentionally left blank. */ }     
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
    barber.stopWork ( ) ;
    try { barberThread.join ( ) ; }
    catch ( InterruptedException iiee ) { /* Intentionally left blank. */ }     
    System.out.println ( "\nTrimmed " + barber.getCustomersTrimmed ( ) + " and turned away " + customersTurnedAway + " today." ) ;
  }
  public static void main ( final String[] args ) {
    ( new SingleBarber_Java_Threads ( ) ).runSimulation ( 20 , 4 ,
                                                          new RandomCallingFunction ( 60 , 10 ) ,
                                                          new RandomCallingFunction ( 20 , 10 ) ) ;
  }
}
