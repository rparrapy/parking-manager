package nl.tue.iot;

import org.eclipse.leshan.standalone.LeshanStandalone;

/**
 * Created by rparra on 5/1/16.
 */
public class ParkingManager extends LeshanStandalone {
  
  public static void main(String[] args) {
      new ParkingManager().start();
  }
}
