//  This is a model of the "The Sleeping Barber" problem using Java executor service only,
//  cf. http://en.wikipedia.org/wiki/Sleeping_barber_problem.
//
//  Copyright © 2011–2013  Russel Winder

package uk.org.winder.sleepingbarber;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class SingleBarber_ScheduledThreadPoolExecutor {
  private final static class Customer {
    public final int id;
    public Customer(final int id) { this.id = id; }
  }
  private final static class SuccessfulCustomer {
    public final Customer c;
    public SuccessfulCustomer(final Customer c) { this.c = c; }
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
  private void runSimulation(final int numberOfCustomers, final int numberOfWaitingSeats,
                    final RandomCallingFunction hairTrimTime, final RandomCallingFunction nextCustomerWaitTime)
    throws InterruptedException, ExecutionException {
    final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors());
    final ArrayBlockingQueue<Customer> waitingChairs = new ArrayBlockingQueue<>(numberOfWaitingSeats);
    final ArrayBlockingQueue<Customer> toShop = new ArrayBlockingQueue<>(numberOfCustomers);
    final ArrayBlockingQueue<SuccessfulCustomer> fromChair = new ArrayBlockingQueue<>(numberOfCustomers);
    final ArrayBlockingQueue<Object> fromShop = new ArrayBlockingQueue<>(numberOfCustomers);
    final Future<Integer> barber = executor.submit(new Callable<Integer>() {
        @Override public Integer call() {
          int customersTrimmed = 0;
          while (true) {
            try {
              //  Use take here to simulate barber sleeping if there are no customers by blocking.
              final Customer customer = waitingChairs.take();
              if (customer.id < 0) {
                fromChair.put(new SuccessfulCustomer(customer));
                break;
              }
              System.out.println("Barber : Starting Customer " + customer.id + ".");
              try { Thread.sleep(hairTrimTime.call()); }
              catch (InterruptedException iiee) { /* Intentionally left blank. */ }
              System.out.println("Barber : Finished Customer " + customer.id + ".");
              ++customersTrimmed;
              fromChair.put(new SuccessfulCustomer(customer));
            }
            catch (InterruptedException ie) { throw new RuntimeException(ie); }
          }
          System.out.println("Barber : Work over for the day, trimmed " + customersTrimmed + ".");
          return customersTrimmed;
        }
      });
   final Future<Integer> shop = executor.submit(new Runnable() {
        @Override public void run() {
          int customersTurnedAway = 0;
          int customersTrimmed = 0;
          while (true) {
            final Customer customer = toShop.poll();
            if (customer != null) {
              if (customer.id == -1) {
                try { waitingChairs.put(customer); }
                catch (InterruptedException ie) { throw new RuntimeException(ie); }
              } else {
                if (waitingChairs.offer(customer)) {
                  System.out.println("Shop : Customer " + customer.id + " takes a seat. " + waitingChairs.size() + " in use.");
                } else {
                  ++customersTurnedAway;
                  System.out.println("Shop : Customer " + customer.id + " turned away.");
                  try { fromShop.put(customer); }
                  catch (InterruptedException ie) { throw new RuntimeException(ie); }
                }
              }
            }
            final SuccessfulCustomer successfulCustomer = fromChair.poll();
            if (successfulCustomer != null) {
              if (successfulCustomer.c.id == -1) { break; }
              else {
                ++customersTrimmed;
                System.out.println("Shop : Customer " + successfulCustomer.c.id + " leaving trimmed.");
                try { fromShop.put(successfulCustomer); }
                catch (InterruptedException ie) { throw new RuntimeException(ie); }
              }
            }
          }
          System.out.println("Shop : Closing — " + customersTrimmed + " timmed and " + customersTurnedAway + " turned away.");
        }
      }, 0);
    for(int number = 0; number < numberOfCustomers; ++number) {
      Thread.sleep(nextCustomerWaitTime.call());
      System.out.println("World : Customer " + number + " enters the shop.");
      toShop.put(new Customer(number));
    }
    waitingChairs.put(new Customer(-1));
    int customersTrimmed = 0;
    int customersTurnedAway = 0;
    for(int number = 0; number < numberOfCustomers; ++number) {
      final Object customer = fromShop.take();
      int id;
      if (customer instanceof SuccessfulCustomer) {
        ++customersTrimmed;
        id =((SuccessfulCustomer)customer).c.id;
      } else if (customer instanceof Customer) {
        ++customersTurnedAway;
        id =((Customer)customer).id;
      } else { throw new RuntimeException("Non customer exited the shop."); }
      System.out.println("World : Customer " + id + " exits the shop.");
    }
    //  If we don't get here then Sweeney Todd is the barber — we have not got as many live customers back
    //  as we put in.
    System.out.println("World : Time to close up.");
    final int barberCount =  barber.get();
    if (barberCount != customersTrimmed) {
      System.out.println("World : Barber claimed " +  barberCount + ", but the workld counted " + customersTrimmed + ".");
    }
    System.out.println("\nTrimmed " + barberCount+ " and turned away " + customersTurnedAway + " today.");
    executor.shutdown();
   }
  public static void main(final String[] args) throws InterruptedException, ExecutionException {
    new SingleBarber_ScheduledThreadPoolExecutor().runSimulation(20, 4,
                                                                 new RandomCallingFunction(6, 1),
                                                                 new RandomCallingFunction(2, 1));
  }
}
