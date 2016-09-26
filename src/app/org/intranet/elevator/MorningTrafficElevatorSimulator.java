/*
 * Copyright 2003 Neil McKellar and Chris Dailey
 * All rights reserved.
 */
package org.intranet.elevator;

import java.util.Random;

import org.intranet.elevator.model.Floor;
import org.intranet.elevator.model.operate.Building;
import org.intranet.elevator.model.operate.Person;
import org.intranet.elevator.model.operate.controller.Controller;
import org.intranet.elevator.model.operate.controller.MetaController;
import org.intranet.sim.Model;
import org.intranet.sim.Simulator;
import org.intranet.sim.event.Event;
import org.intranet.ui.FloatParameter;
import org.intranet.ui.IntegerParameter;
import org.intranet.ui.LongParameter;

/**
 * @author Neil McKellar and Chris Dailey
 *
 */
public class MorningTrafficElevatorSimulator
  extends Simulator
{
  private IntegerParameter floorsParameter;
  private IntegerParameter carsParameter;
  private IntegerParameter ridersParameter;
  private FloatParameter durationParameter;
  private IntegerParameter stdDeviationParameter;
  private LongParameter seedParameter;
  
  private Building building;

  public MorningTrafficElevatorSimulator()
  {
    super();
    floorsParameter = new IntegerParameter("Number of Floors",10);
    parameters.add(floorsParameter);
    carsParameter = new IntegerParameter("Number of Elevators",3);
    parameters.add(carsParameter);
    ridersParameter = new IntegerParameter("Number of People per Floor", 20);
    parameters.add(ridersParameter);
    durationParameter = new FloatParameter("Rider insertion time (hours)",1.0f);
    parameters.add(durationParameter);
    stdDeviationParameter = new IntegerParameter("Standard Deviation", 1);
    parameters.add(stdDeviationParameter);
    seedParameter = new LongParameter("Random seed", 635359);
    parameters.add(seedParameter);
  }

  @Override
  public Controller initializeModel()
  {
    int numFloors = floorsParameter.getIntegerValue();
    int numCars = carsParameter.getIntegerValue();
    int numRiders = ridersParameter.getIntegerValue();
    float duration = durationParameter.getFloatValue();
    double durationInMs = duration * 3600.0 * 1000.0;
    int stdDeviation = stdDeviationParameter.getIntegerValue();
    long seed = seedParameter.getLongValue();

    Controller controller = new MetaController();
    building = new Building(getEventQueue(), numFloors, numCars,
        controller);
    // starting floor is the ground floor
    Floor startingFloor = building.getFloor(0);
    startingFloor.setCapacity(Integer.MAX_VALUE);
    
    Random rand = new Random(seed);

    for (int i = 1; i < numFloors; i++)
    {
      final Floor destFloor = building.getFloor(i);
      for (int j = 0; j < numRiders; j++)
      {
        final Person person = building.createPerson(startingFloor, j);
        // time to insert
        // Convert a gaussian[-1, 1] to a gaussian[0, 1]
        double gaussian = (getGaussian(rand, stdDeviation) + 1) / 2;
        // Apply gaussian value to the duration (in hours)
        // and convert to milliseconds
        long insertTime = (long)(gaussian * durationInMs);
        // insertion event for destination at time
        Event event = new CarRequestEvent(insertTime, person, startingFloor, destFloor);
        getEventQueue().addEvent(event);
      }
    }
    
    return controller;
  }

  public final Model getModel()
  {
    return building;
  }
  
  public String getDescription()
  {
    return "Morning Traffic Rider Insertion";
  }

  public Simulator duplicate()
  {
    return new MorningTrafficElevatorSimulator();
  }
  
  private static double getGaussian(Random rand, int stddev)
  {
    while (true)
    {
      double gaussian = rand.nextGaussian()/stddev;
      if (gaussian >= -1.0 && gaussian <= 1.0) return gaussian;
    }
  }
}
