//  This is a model of the "The Sleeping Barber" problem using Java threads only,
//  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2011–2013  Russel Winder

package uk.org.winder.sleepingbarber;

//  The barber is modeled with a thread, with the sleeping being modeled by a blocking read on a queue.  The
//  queue represents the cutting chairs and the waiting chairs.  Customers are injected into the shop by the
//  main thread via a queue.  Having either been to the barber and back again, or rejected due to no space
//  in the queue representing the chairs, the customers is released into the world.  Each thread keeps a
//  count of the number of customers of each sort.

import java.util.concurrent.ArrayBlockingQueue;

public class SingleBarber_Threads {
  private final static class Customer {
    public final int id;
    public Customer(final int id) { this.id = id; }
  }
  private final static class SuccessfulCustomer {
    public final Customer customer;
    public SuccessfulCustomer(final Customer customer) { this.customer = customer; }
  }
  private final static class RandomCallingFunction {
    private final int scale;
    private final int offset;
    public RandomCallingFunction(final int scale, final int offset) {
      this.scale = scale;
      this.offset = offset;
    }
    public int call() { return (int)( Math.random() * scale) + offset; }
  }
  private ArrayBlockingQueue<Customer> waitingChairs;
  private ArrayBlockingQueue<Customer> toShop;
  private ArrayBlockingQueue<SuccessfulCustomer> fromChair;
  private ArrayBlockingQueue<Object> fromShop;
  //  Must subclass thread here rather than create a Runnable, so that we can execute an interrupt when
  //  closing the shop.
  private final class Barber extends Thread {
    private boolean working = true;
    private final RandomCallingFunction hairTrimTime;
    public Barber( final RandomCallingFunction hairTrimTime) { this.hairTrimTime = hairTrimTime; }
    public void stopWork() {
      working = false;
      interrupt();
    }
    @Override public void run() {
      int customersTrimmed = 0;
      while ( working || (waitingChairs.size() > 0)) {
        try {
          //  Use take here to simulate barber sleeping if there are no customers by blocking.  It means we
          //  have to use an interrupt when the shop gets closed.
          final Customer customer = waitingChairs.take();
          System.out.println("Barber : Starting Customer " + customer.id + ".");
          //  If we don't deal with InterruptedException here then (because we ignore the interrupt in an
          //  outer scope) we get the "Sweeney Todd" effect, one customer (or more) simply disappears from
          //  the universe.
          try { Thread.sleep(hairTrimTime.call()); }
          catch (InterruptedException iiee) { /* Intentionally left blank. */ }
          ++customersTrimmed;
          System.out.println("Barber : Finished Customer " + customer.id + ".");
          try { fromChair.put(new SuccessfulCustomer(customer)); }
          catch (InterruptedException iiee) { throw new RuntimeException(iiee); }
        }
        catch (InterruptedException ie) { /* Intentionally left blank */ }
      }
      System.out.println("Barber : Work over for the day, trimmed " + customersTrimmed + " customers.");
    }
  }
  private final class Shop implements Runnable {
    private boolean open = true;
    public void closeShop() { open = false; }
    @Override public void run() {
      int customersTurnedAway = 0;
      int customersTrimmed = 0;
      while (open) {
        final Customer customer = toShop.poll();
        if (customer != null) {
          if (waitingChairs.offer(customer)) {
            System.out.println("Shop : Customer " + customer.id + " takes a seat. " + waitingChairs.size() + " in use.");
          } else {
            ++customersTurnedAway;
            System.out.println("Shop : Customer " + customer.id + " turned away.");
            try { fromShop.put(customer); }
            catch (InterruptedException ie) {  throw new RuntimeException(ie); }
          }
        }
        final SuccessfulCustomer successfulCustomer = fromChair.poll();
        if (successfulCustomer != null) {
          ++customersTrimmed;
          System.out.println("Shop : Customer " + successfulCustomer.customer.id + " leaving trimmed.");
          try { fromShop.put(successfulCustomer); }
          catch (InterruptedException ie) {  throw new RuntimeException(ie); }
        }
      }
      System.out.println("Shop : Closing — Trimmed " + customersTrimmed + " and turned away " + customersTurnedAway + " customers.");
    }
  }
  private final class World extends Thread {
    private boolean existing = true;
    public void destroyWorld() {
      existing = false;
      interrupt();
    }
    private final void message(final int id) { System.out.println("World : Customer " + id + " exits the shop."); }
    @Override public void run() {
      int customersTurnedAway = 0;
      int customersTrimmed = 0;
      while (existing) {
        try {
          final Object customer = fromShop.take();
          if (customer instanceof Customer) {
            ++customersTurnedAway;
            message(( (Customer) customer).id);
          } else if (customer instanceof SuccessfulCustomer) {
            ++customersTrimmed;
            message(( (SuccessfulCustomer) customer).customer.id);
          }
        }
        catch (InterruptedException ie) { /*Intentionally left blank. */ }
      }
      System.out.println("\nTrimmed " + customersTrimmed + " and turned away " + customersTurnedAway + " today.");
      System.out.println("World : Non-existent.");
    }
  }
  private void runSimulation(final int numberOfCustomers, final int numberOfWaitingSeats,
                    final RandomCallingFunction hairTrimTime, final RandomCallingFunction nextCustomerWaitTime)
    throws InterruptedException {
    waitingChairs = new ArrayBlockingQueue<>(numberOfWaitingSeats);
    toShop = new ArrayBlockingQueue<>(numberOfCustomers);
    fromChair = new ArrayBlockingQueue<>(numberOfCustomers);
    fromShop = new ArrayBlockingQueue<>(numberOfCustomers);
    final Barber barber = new Barber(hairTrimTime);
    final Shop shop = new Shop();
    final Thread shopThread = new Thread(shop);
    final World world = new World();
    barber.start();
    shopThread.start();
    world.start();
    for(int number = 0; number < numberOfCustomers; ++number) {
      Thread.sleep(nextCustomerWaitTime.call());
      System.out.println("World : Customer " + number + " enters the shop.");
      toShop.put(new Customer(number));
    }
    barber.stopWork();
    barber.join();
    shop.closeShop();
    shopThread.join();
    world.destroyWorld();
    world.join();
  }
  public static void main(final String[] args) throws InterruptedException {
    new SingleBarber_Threads().runSimulation(20, 4,
                                             new RandomCallingFunction(6, 1),
                                             new RandomCallingFunction(2, 1));
  }
}
