package root;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Constants
{
    public static final boolean VISUALIZE = true;
    public static final boolean SHOW_PLOTS = false;
    public static final boolean START_WITH_SIMULATION_RUNNING = true;
    public static final boolean USE_PID = true;
    public static final boolean INCLUDE_PERTURBATIONS = true;

    public static final double KP = 50.0;
    public static final double KI = 0.0;
    public static final double KD = 150.0;

    // x, velocity, theta, angular velocity
    public static Matrix INITIAL_STATE = new Matrix(new ArrayList<>(Collections.singletonList(new ArrayList<>(Arrays.asList(0.0, 0.0, -10.0 * Math.PI / 180.0, 0.0)))));

    public static ArrayList<Perturbation> PERTURBATIONS = new ArrayList<>(Arrays.asList(
            new Perturbation(1.0, 5.0, 0.25),
            new Perturbation(3.0, -10.0, 0.4),
            new Perturbation(5.0, 15.0, 0.2)
    ));

    public static final double CART_WIDTH = 0.25;
    public static final double CART_HEIGHT = 0.1;
    public static final double CART_MASS = 2.0;
    public static final double PENDULUM_WIDTH = 0.05;
    public static final double PENDULUM_LENGTH = 0.8;
    public static final double PENDULUM_MASS = 0.25;
    public static final double GRAVITY = 9.81;

    public static final double FRAME_WIDTH = 1400;
    public static final double FRAME_HEIGHT = 500;

    public static final double PIXELS_PER_METER = 250;
    public static final double OFFSET_FROM_BOTTOM = 100;
    public static final double AXLE_RADIUS = 0.01;
}
