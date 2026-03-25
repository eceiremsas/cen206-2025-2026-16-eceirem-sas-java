/**

@file KidsplannerApp.java
@brief This file serves as the main application file for the Kidsplanner App.
@details This file contains the entry point of the application, which is the main method. It initializes the necessary components and executes the Kidsplanner App.
*/
/**

@package com.eceirem.kidsplanner
@brief The com.eceirem.kidsplanner package contains all the classes and files related to the Kidsplanner App.
*/
package com.eceirem.kidsplanner;


/**
 *
 * @class KidsplannerApp
 * @brief This class represents the main application class for the Kidsplanner
 *        App.
 * @details Constructs {@link Kidsplanner} and runs the console loop (menu or text mode).
 * @author ugur.coruh
 */
public class KidsplannerApp {
  /**
   * JVM entry point.
   *
   * @param args unused in this build
   */
  public static void main(String[] args) {
    Kidsplanner planner = new Kidsplanner();
    planner.run();
  }
}
