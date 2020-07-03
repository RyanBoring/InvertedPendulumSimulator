package root;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Constants
{
    public static final boolean VISUALIZE = false;
    public static final boolean SHOW_PLOTS = false;
    public static final boolean START_WITH_SIMULATION_RUNNING = true;
    public static final boolean USE_PID = false;
    public static final boolean USE_EIGENVALUE_PLACEMENT = true;
    public static final double OFFSET = 0.0 * Math.PI / 180.0;
    public static final boolean INCLUDE_PERTURBATIONS = true;
    public static final boolean SAVE_TO_CSV = true;
    public static final boolean SAVE_THETA = true;
    public static final boolean SAVE_TORQUE = true;
    public static final boolean FIX_STUPID_PLOTS = true;
    public static final boolean LINEARIZED = false;

    // When false, PID is calculated with PD only
    public static final boolean PD_WITH_I = false;
    public static final boolean FINDING_KU = false;

    public static final double EIGENVALUE_1 = -3;
    public static final double EIGENVALUE_2 = -60;

    public static final double MAX_TORQUE_OUTPUT = 2.5;

    public static final double KU = 20.0;
    public static final double TU = 0.57;

    public static final double KP = 20;//FINDING_KU ? KU : KU * (PD_WITH_I ? 0.6 : 0.8);
    public static final double KI = FINDING_KU ? 0.0 : PD_WITH_I ? 1.2 * KU / TU : 0.0;//2 * KU / (TU * 3);
    public static final double KD = 6;//FINDING_KU ? 0.0 : PD_WITH_I ? 3 * KU * TU / 40 : KU * TU / 10;

    public static final long TIMESPAN = secondsToNanoseconds(3);
    public static final long SAVE_STATE_EVERY_X_NS = millisecondsToNanoseconds(10);
    public static final long DT_NS = millisecondsToNanoseconds(0.1);
    public static final double DT_S = nanosecondsToSeconds(DT_NS);

    // theta, angular velocity
    public static Matrix INITIAL_STATE = new Matrix(new ArrayList<>(Collections.singletonList(new ArrayList<>(Arrays.asList(OFFSET, 0.0)))));

    public static ArrayList<Perturbation> PERTURBATIONS = new ArrayList<>(Arrays.asList(
            new Perturbation(secondsToNanoseconds(0), 2.3, secondsToNanoseconds(0.5))
    ));

    public static final double PENDULUM_WIDTH = 0.05;
    public static final double PENDULUM_LENGTH = 0.8;
    public static final double PENDULUM_MASS = 1.0;
    public static final double VISCOUS_FRICTION = 0.01;
    public static final double GRAVITY = 9.81;

    public static final double FRAME_WIDTH = 500;
    public static final double FRAME_HEIGHT = 350;

    public static final double PIXELS_PER_METER = 250;
    public static final double OFFSET_FROM_BOTTOM = 100;
    public static final double AXLE_RADIUS = 0.01;

    public static long secondsToNanoseconds(double seconds)
    {
        return (long) (seconds * 1_000_000_000L);
    }

    public static long millisecondsToNanoseconds(double milliseconds)
    {
        return (long) (milliseconds * 1_000_000);
    }

    public static double nanosecondsToSeconds(double nanoseconds)
    {
        return nanoseconds / 1_000_000_000.0;
    }
}
