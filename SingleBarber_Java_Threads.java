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
  private ArrayBlockingQueue<Customer> waitingChairs ;
  private ArrayBlockingQueue<Customer> toShop ;
  private ArrayBlockingQueue<Customer> fromChair ;
  private ArrayBlockingQueue<Customer> fromShop ;
  private int customersTurnedAway ;
  private int customersTrimmed ;
  //  Must subclass thread here rather than create a Runnable, so that we can execute an interrupt when
  //  closing the shop.
  private final class Barber extends Thread {
    private boolean working = true ;
    private final RandomCallingFunction hairTrimTime ;
    public Barber( final RandomCallingFunction hairTrimTime ) { this.hairTrimTime = hairTrimTime ; }
    public void stopWork ( ) {
      working = false ;
      interrupt ( ) ;
    }
    @Override public void run ( ) {
      while ( working || ( waitingChairs.size ( ) > 0 ) ) {
        try {
          //  Use take here to simulate barber sleeping if there are no customers by blocking.  It means we
          //  have to use an interrupt when the shop gets closed.
          final Customer customer = waitingChairs.take ( ) ;
          System.out.println ( "Barber : Starting Customer " + customer.id + "." ) ;
          try { Thread.sleep ( hairTrimTime.call ( ) ) ; }
          catch ( InterruptedException iiee ) { throw new RuntimeException ( iiee ) ; }
          System.out.println ( "Barber : Finished Customer " + customer.id + "." ) ;
          try { fromChair.put ( customer ) ; }
          catch ( InterruptedException iiee ) { throw new RuntimeException ( iiee ) ; }
        }
        catch ( InterruptedException ie ) { /* Intentionally left blank */ }
      }
      System.out.println ( "Barber : Work over for the day." ) ;
    }
  }
  private final class Shop implements Runnable {
    private boolean open = true ;
    public void closeShop ( ) { open = false ; }
    @Override public void run ( ) {
      while ( open || waitingChairs.size ( ) > 0 ) {
        Customer customer = toShop.poll ( ) ;
        if ( customer != null ) {
          if ( waitingChairs.offer ( customer ) ) {
            System.out.println ( "Shop : Customer " + customer.id + " takes a seat. " + waitingChairs.size ( ) + " in use." ) ;
          }
          else {
            ++customersTurnedAway ;
            System.out.println ( "Shop : Customer " + customer.id + " turned away." ) ;
            try { fromShop.put ( customer ) ; }
            catch ( InterruptedException ie ) {  throw new RuntimeException ( ie ) ; }
          }
        }
        customer = fromChair.poll ( ) ;
        if ( customer != null ) {
          ++customersTrimmed ;
          System.out.println ( "Shop : Customer " + customer.id + " leaving trimmed." ) ;
          try { fromShop.put ( customer ) ; }
          catch ( InterruptedException ie ) {  throw new RuntimeException ( ie ) ; }
        }
      }
      System.out.println ( "Shop : Closing." ) ;
    }
  }
  private void runSimulation ( final int numberOfCustomers , final int numberOfWaitingSeats ,
                    final RandomCallingFunction hairTrimTime , final RandomCallingFunction nextCustomerWaitTime ) throws InterruptedException {
    waitingChairs = new ArrayBlockingQueue<Customer> ( numberOfWaitingSeats ) ;
    toShop = new ArrayBlockingQueue<Customer> ( numberOfCustomers ) ;
    fromChair = new ArrayBlockingQueue<Customer> ( numberOfCustomers ) ;
    fromShop = new ArrayBlockingQueue<Customer> ( numberOfCustomers ) ;
    customersTurnedAway = 0 ;
    customersTrimmed = 0 ;
    final Barber barber = new Barber ( hairTrimTime ) ;
    barber.start ( ) ;
    final Shop shop = new Shop ( ) ;
    final Thread shopThread = new Thread (shop ) ;
    shopThread.start ( ) ;
    for ( int number = 0 ; number < numberOfCustomers ; ++number ) {
      Thread.sleep ( nextCustomerWaitTime.call ( ) ) ;
      System.out.println ( "World : Customer " + number + " enters the shop." ) ;
      toShop.put ( new Customer ( number ) ) ;
    }
    for ( int number = 0 ; number < numberOfCustomers ; ++number ) {
      final Customer customer = fromShop.take ( ) ;
      System.out.println ( "World : Customer " + customer.id + " exits the shop." ) ;
    }
    System.out.println ( "World : Time to close up." ) ;
    //  If we don't get here then Sweeney Todd is the barber — we have not got as many live customers back
    //  as we put in.
    barber.stopWork ( ) ;
    barber.join ( ) ;
    shop.closeShop ( ) ;
    shopThread.join ( ) ;
    System.out.println ( "\nTrimmed " + customersTrimmed + " and turned away " + customersTurnedAway + " today." ) ;
  }
  public static void main ( final String[] args ) throws InterruptedException {
    ( new SingleBarber_Java_Threads ( ) ).runSimulation ( 20 , 4 ,
                                                          new RandomCallingFunction ( 60 , 10 ) ,
                                                          new RandomCallingFunction ( 20 , 10 ) ) ;
  }
}
