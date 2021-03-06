<a href="https://codeclimate.com/github/MontiSim/simulation/maintainability"><img src="https://api.codeclimate.com/v1/badges/0ff9cfde324c783f7957/maintainability" /></a>   [![Build Status](https://travis-ci.org/MontiSim/simulation.svg?branch=master)](https://travis-ci.org/MontiSim/simulation)   [![Coverage Status](https://coveralls.io/repos/github/MontiSim/simulation/badge.svg?branch=master)](https://coveralls.io/github/MontiSim/simulation?branch=master)

# Simulation
This repository includes the classes for the management of the simulation, the physics computations, the environment, the sensors and the vehicles.

Discrete time steps are used to advance the the simulation and a rigid body simulation is used for the physics computations.

The environment is imported from OpenStreetMap data.

# Simulation modes
Several simulation modes are available in this module.

An example for the recommended simulation mode is shown in the function `setupSimulator()` in the class `SimulatorMain.java` from the application repository.
