/*
 * Copyright 2004-2005 Neil McKellar and Chris Dailey
 * All rights reserved.
 */
package org.intranet.elevator;

import au.edu.rmit.agtgrp.elevatorsim.ElsimSettings;
import au.edu.rmit.agtgrp.elevatorsim.LaunchOptions;
import au.edu.rmit.agtgrp.elevatorsim.SimulatorParams;
import au.edu.rmit.agtgrp.elevatorsim.utils.ClassLoader;
import org.intranet.elevator.model.operate.Building;
import org.intranet.elevator.view.BuildingView;
import org.intranet.sim.Model;
import org.intranet.sim.SimulationApplication;
import org.intranet.sim.Simulator;
import org.intranet.sim.clock.RealTimeClock;
import org.intranet.sim.runner.SimulationHeadlessRunner;
import org.intranet.sim.ui.ApplicationUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Neil McKellar and Chris Dailey
 */
public class ElevatorSimulationApplication
        implements SimulationApplication {
    public static final String COPYRIGHT =
            "Copyright 2016 Joshua Beale, Matthew McNally, Joshua Richards, Abhijeet Anand, Sebastian Sardina.\n" +
                    "Forked from Elevator Simulator (https://sourceforge.net/projects/elevatorsim/)\n" +
                    "Copyright 2004-2005 Chris Dailey & Neil McKellar";
    public static final String VERSION = "1.1";
    public static final String APPLICATION_NAME = "RMIT Elevator Simulator";
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    private Image iconImage;

    public static void main(String[] args) {
        LaunchOptions.createFromCliArgs(args);

        LOG.debug("Starting Application {}", APPLICATION_NAME);
        if (LaunchOptions.get().isHeadless()) { // headless mode selected
            Simulator loadedSimulator = null;

            if (LaunchOptions.get().hasJsonParams()) {
                try {
                    // Load and build an object of class Simulator
                    loadedSimulator = ClassLoader.instantiate(SimulatorParams.instance().getActiveSimulatorClass(), Simulator.class);
                    LOG.debug("Simulator loaded from JSON parameters");
                } catch (IllegalStateException e) {
                    LOG.error("Error loading class from params file: {}", e.getMessage());
                    LOG.error("Loading default simulator");
                }
            }

            if (loadedSimulator == null) {
                loadedSimulator = new RandomElevatorSimulator();
            }

            LOG.debug("Simulator class loaded: {}", loadedSimulator);

            // Finally run a headless version of the simulator
            SimulationHeadlessRunner runner = new SimulationHeadlessRunner();
            runner.run(loadedSimulator, LaunchOptions.get().getSpeedFactor().orElse(1));
        } else {    // GUI mode
            ElevatorSimulationApplication sc = new ElevatorSimulationApplication();
            new ApplicationUI(sc);
        }

        LOG.info("Started Application {}", APPLICATION_NAME);
    }

    public ElevatorSimulationApplication() {
        super();
    }

    // Why call it createView if it is not a generic view creator?
    public JComponent createView(Model m) {
        return new BuildingView((Building) m);
    }

    public List<Simulator> getSimulations() {
        List<Simulator> simulations = new ArrayList<Simulator>();
        simulations.add(new RandomElevatorSimulator());
        simulations.add(new MorningTrafficElevatorSimulator());
        simulations.add(new EveningTrafficElevatorSimulator());
        if (ElsimSettings.get().getEnableHiddenSimulators()) {
            simulations.add(new ThreePersonBugSimulator());
            simulations.add(new ThreePersonElevatorSimulator());
            simulations.add(new UpToFourThenDownSimulator());
            simulations.add(new NoIdleElevatorCarSimulator());
            simulations.add(new ThreePersonTwoElevatorSimulator());
        }
        return simulations;
    }

    public String getApplicationName() {
        return APPLICATION_NAME;
    }

    public String getCopyright() {
        return COPYRIGHT;
    }

    public String getVersion() {
        return VERSION;
    }

    public Image getImageIcon() {
        if (iconImage == null) {
            URL iconImageURL = ElevatorSimulationApplication.class.getResource("icon.gif");
            iconImage = Toolkit.getDefaultToolkit().createImage(iconImageURL);
        }
        return iconImage;
    }
}
