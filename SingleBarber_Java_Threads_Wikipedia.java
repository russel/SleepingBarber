//  This is a model of the "The Sleeping Barber" problem using Java,
//  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2011 Russel Winder
//
//  This solution follow the psuedocode of the solution set out on the Wikipedia page.  Use
//  java.util.concurrent.Semaphore as an implementation of semaphore to save having to write one or use the
//  Java basic wait/notify system.

import java.util.ArrayList ;
import java.util.concurrent.Semaphore ;

class SingleBarber_Java_Threads_Wikipedia {
  private final static class RandomCallingFunction {
    private final int scale ;
    private final int offset ;
    public RandomCallingFunction ( final int scale , final int offset ) {
      this.scale = scale ;
      this.offset = offset ;
    }
    public int call ( ) { return (int)( Math.random ( ) * scale ) + offset ; }
  }
  private final Semaphore customerSemaphore = new Semaphore ( 1 ) ;
  private final Semaphore barberSemaphore = new Semaphore ( 1 ) ;
  private final Semaphore accessSeatsSemaphore = new Semaphore ( 1 ) ;
  private int customersTurnedAway ;
  private int customersTrimmed ;
  private int numberOfFreeSeats ;
  private final class Barber extends Thread {
    private final RandomCallingFunction hairTrimTime ;
    private boolean working = true ;
    public Barber ( final RandomCallingFunction hairTrimTime ) {
      this.hairTrimTime = hairTrimTime ;
    }
    public void stopWork ( ) { working = false ; }
    @Override public void run ( ) {
      while ( working ) {
        try { customerSemaphore.acquire ( ) ; }
        catch ( InterruptedException ie ) { continue ; }
        try { accessSeatsSemaphore.acquire ( ) ; }
        catch ( InterruptedException ie ) {
          customerSemaphore.release ( ) ;
          continue ;
        }
        ++numberOfFreeSeats ;
        barberSemaphore.release ( ) ;
        accessSeatsSemaphore.release ( ) ;
        System.out.println ( "Barber : Starting Customer." ) ;
        try { Thread.sleep ( hairTrimTime.call ( ) ) ; }
        catch ( InterruptedException ie ) {  /* Intentially left blank. */ }
        System.out.println ( "Barber : Finished Customer." ) ;
      }
    }
  }
  private void runSimulation ( final int numberOfCustomers , final int numberOfWaitingSeats ,
                    final RandomCallingFunction hairTrimTime , final RandomCallingFunction nextCustomerWaitTime ) {
    customersTurnedAway = 0 ;
    customersTrimmed = 0 ;
    numberOfFreeSeats = numberOfWaitingSeats ;
    final Barber barber = new Barber ( hairTrimTime ) ;
    final Thread barberThread = new Thread ( barber ) ;
    barberThread.start ( ) ;
    final ArrayList<Thread> customerThreads = new ArrayList<Thread> ( ) ;
    for ( int i = 0 ; i < numberOfCustomers ; ++i ) {
      final int number = i ;
      System.out.println ( "World : Customer " + number + " enters the shop." ) ;
      final Thread customerThread = new Thread ( new Runnable ( ) {
          @Override public void run ( ) {
            while ( true ) {
              try { accessSeatsSemaphore.acquire ( ) ; break ; }
              catch ( InterruptedException ie ) { continue ; }
            }
            if ( numberOfFreeSeats > 0 ) {
              System.out.println ( "Shop : Customer " + number + " takes a seat. " + ( numberOfWaitingSeats - numberOfFreeSeats ) + " in use." ) ;
              --numberOfFreeSeats ;
              customerSemaphore.release ( ) ;
              accessSeatsSemaphore.release ( ) ;
              while ( true ) {
                try { barberSemaphore.acquire ( ) ; break ; }
                catch ( InterruptedException ie ) { continue ; }
              }
              System.out.println ( "Shop : Customer " + number + " leaving trimmed." ) ;
              ++customersTrimmed ;
            }
            else {
              accessSeatsSemaphore.release ( ) ;
              System.out.println ( "Shop : Customer " + number + " turned away." ) ;
              ++customersTurnedAway ;
            }
          }
        } ) ;
      customerThreads.add ( customerThread ) ;
      customerThread.start ( ) ;
      try { Thread.sleep ( nextCustomerWaitTime.call ( ) ) ; }
      catch ( InterruptedException ie ) { /* Intentially left blank. */ }
    }
    for ( Thread t : customerThreads ) {
      try { t.join ( ) ; }
      catch ( InterruptedException ie ) { /* Intentially left blank. */ }
    }
    barber.stopWork ( ) ;
    try { barberThread.join ( ) ; }
    catch ( InterruptedException ie ) { /* Intentially left blank. */ }
    System.out.println ( "\nTrimmed " + customersTrimmed + " and turned away " + customersTurnedAway + " today." ) ;
  }
  public static void main ( final String[] args ) {
    ( new SingleBarber_Java_Threads_Wikipedia ( ) ). runSimulation ( 20 , 4 ,
                                                          new RandomCallingFunction ( 60 , 10 ) ,
                                                          new RandomCallingFunction ( 20 , 10 ) ) ;
  }
}

