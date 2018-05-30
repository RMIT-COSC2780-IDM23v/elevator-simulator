/*
 * Copyright 2003 Neil McKellar and Chris Dailey
 * All rights reserved.
 */
package org.intranet.elevator;

import au.edu.rmit.agtgrp.elevatorsim.SimulatorParams;
import org.intranet.elevator.model.operate.Building;
import org.intranet.elevator.model.operate.Person;
import org.intranet.elevator.model.operate.controller.Controller;
import org.intranet.sim.Model;
import org.intranet.sim.Simulator;
import org.intranet.sim.event.Event;
import org.intranet.sim.event.EventQueue;
import org.intranet.ui.IntegerParameter;

/**
 * @author Neil McKellar and Chris Dailey
 */
public class ThreePersonTwoElevatorSimulator
        extends Simulator {
    private IntegerParameter floorsParameter;
    private IntegerParameter carsParameter;

    private Building building;

    public ThreePersonTwoElevatorSimulator() {
        super();
    }

    @Override
    protected void initialiseParameters() {
        if (SimulatorParams.instance().isParamsLoaded()) {
            SimulatorParams params = SimulatorParams.instance();
            floorsParameter = new IntegerParameter("Number of Floors", params.getParamValueInt("floors"));
            carsParameter = new IntegerParameter("Number of Elevators", params.getParamValueInt("cars"));
        } else {
            floorsParameter = new IntegerParameter("Number of Floors", 5);
            carsParameter = new IntegerParameter("Number of Elevators", 2);
        }

        parameters.add(floorsParameter);
        parameters.add(carsParameter);

        addControllerParameter();
    }

    @Override
    public void initializeModel() {
        int numFloors = floorsParameter.getIntegerValue();
        int numCars = carsParameter.getIntegerValue();

        EventQueue eQ = getEventQueue();

        Controller controller = getController();
        building = new Building(getEventQueue(), numFloors, numCars, controller, seedParameter.getLongValue());

        final Person c = building.createPerson(building.getFloor(3), 3);
        Event eventC = new CarRequestEvent(0, c, building.getFloor(3), building.getFloor(1));
        eQ.addEvent(eventC);

        final Person a = building.createPerson(building.getFloor(1), 1);
        Event eventA = new CarRequestEvent(0, a, building.getFloor(1), building.getFloor(3));
        eQ.addEvent(eventA);

        final Person b = building.createPerson(building.getFloor(2), 2);
        Event eventB = new CarRequestEvent(0, b, building.getFloor(2), building.getFloor(4));
        eQ.addEvent(eventB);
    }

    public final Model getModel() {
        return building;
    }

    public String getDescription() {
        return "Three Person Two Elevator";
    }

    public Simulator duplicate() {
        return new ThreePersonTwoElevatorSimulator();
    }
}
