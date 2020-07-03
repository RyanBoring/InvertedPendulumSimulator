package root;

import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonConfig;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Main extends Application
{
    private Pane mRoot;
    private Rectangle mPendulum;
    private Circle mAxle;

    private Button mPlayPauseButton;
    private Slider mTimeSelectSlider;

    private boolean mDrawSimulation = Constants.START_WITH_SIMULATION_RUNNING;
    private boolean mLastLoop = false;

    private HashMap<Long, Matrix> mStates = new HashMap<>(Map.of(0L, Constants.INITIAL_STATE));

    private long mStateToDrawTime = 0;
    private int mStepsToSkip = (int) (0.1 / Constants.DT_NS);

    private PythonConfig mPythonConfig = PythonConfig.pythonBinPathConfig("C:\\Program Files\\Python38\\python3.exe");

    private Matrix mAMatrix = new Matrix(new ArrayList<>(Arrays.asList(
            new ArrayList<>(Arrays.asList(0.0, (12 * Constants.PENDULUM_MASS * Constants.GRAVITY * Constants.PENDULUM_LENGTH)
                    / (Math.pow(Constants.PENDULUM_WIDTH, 2) + 4 * Math.pow(Constants.PENDULUM_LENGTH, 2)))),
            new ArrayList<>(Arrays.asList(1.0, 0.0))
    )));

    private Matrix mBMatrix = new Matrix(new ArrayList<>(Collections.singletonList(
            new ArrayList<>(Arrays.asList(0.0, 24.0 / (Math.pow(Constants.PENDULUM_WIDTH, 2) + 4.0 * Math.pow(Constants.PENDULUM_LENGTH, 2))))
    )));

    private Matrix mKMatrix = new Matrix(new ArrayList<>(Arrays.asList(
            new ArrayList<>(Collections.singletonList(((Constants.EIGENVALUE_1 * Constants.EIGENVALUE_2) * (Math.pow(Constants.PENDULUM_WIDTH, 2) + 4.0 * Math.pow(Constants.PENDULUM_LENGTH, 2))
                    + 12.0 * Constants.PENDULUM_MASS * Constants.GRAVITY * Constants.PENDULUM_LENGTH)
                    / 24.0)),
            new ArrayList<>(Collections.singletonList(((-Constants.EIGENVALUE_1 - Constants.EIGENVALUE_2) * (Math.pow(Constants.PENDULUM_WIDTH, 2) + 4.0 * Math.pow(Constants.PENDULUM_LENGTH, 2))
                    - Constants.VISCOUS_FRICTION)
                    / 24.0))
    )));

    @Override
    public void start(Stage stage)
    {
        mRoot = new Pane();
        mRoot.setPrefWidth(Constants.FRAME_WIDTH);
        mRoot.setPrefHeight(Constants.FRAME_HEIGHT);
        mRoot.setStyle("-fx-background-color: #262626;");

        mPendulum = new Rectangle();
        mPendulum.setWidth(Constants.PENDULUM_WIDTH * Constants.PIXELS_PER_METER);
        mPendulum.setHeight(Constants.PENDULUM_LENGTH * Constants.PIXELS_PER_METER);
        mPendulum.setX((mRoot.getPrefWidth() / 2) - (mPendulum.getWidth() / 2));
        mPendulum.setY(mRoot.getPrefHeight() - Constants.OFFSET_FROM_BOTTOM - mPendulum.getHeight());
        mPendulum.setFill(Paint.valueOf("#660000"));
        mPendulum.setStroke(Paint.valueOf("white"));
        mPendulum.setStrokeWidth(0.5);
        mRoot.getChildren().add(mPendulum);

        mAxle = new Circle();
        mAxle.setRadius(Constants.AXLE_RADIUS * Constants.PIXELS_PER_METER);
        mAxle.setCenterX(mPendulum.getX() + (mPendulum.getWidth() / 2));
        mAxle.setCenterY(mPendulum.getY() + mPendulum.getHeight());
        mAxle.setFill(Paint.valueOf("#999999"));
        mRoot.getChildren().add(mAxle);

        mPlayPauseButton = new Button();
        mPlayPauseButton.setText(mDrawSimulation ? "Pause" : "Play");
        mPlayPauseButton.setLayoutX(10);
        mPlayPauseButton.setLayoutY(10);
        mPlayPauseButton.setOnAction((actionEvent) ->
        {
            mDrawSimulation = !mDrawSimulation;
            mPlayPauseButton.setText(mDrawSimulation ? "Pause" : "Play");
        });
        mRoot.getChildren().add(mPlayPauseButton);

        mTimeSelectSlider = new Slider();
        mTimeSelectSlider.setMax(Constants.TIMESPAN);
        mTimeSelectSlider.setLayoutX(10);
        mTimeSelectSlider.setLayoutY(mRoot.getPrefHeight() - 40);
        mTimeSelectSlider.setPrefWidth(mRoot.getPrefWidth() - 20);
        mTimeSelectSlider.setOnMouseDragged((mouseEvent) ->
        {
            mPlayPauseButton.setText("Play");
            mStateToDrawTime = (long) mTimeSelectSlider.getValue();
            mDrawSimulation = true;
            mLastLoop = true;
        });
        mRoot.getChildren().add(mTimeSelectSlider);

        ArrayList<Double> inputTorqueValues = new ArrayList<>();

        double previousI = 0.0;
        Matrix state = Constants.INITIAL_STATE.clone();
        for (long t = 0; t < Constants.TIMESPAN; t += Constants.DT_NS)
        {
            if (!Constants.LINEARIZED)
                state.set(0, 0, getBoundedAngle(getTheta(state)));

            double inputTorque = 0.0;

            if (Constants.USE_PID)
            {
                double P = Constants.KP * getTheta(state);
                double I = Constants.KI * getTheta(state) * Constants.DT_S + previousI;
                double D = Constants.KD * getThetaDot(state);

                previousI = I;

                inputTorque = -(P + I + D);
            }

            if (Constants.USE_EIGENVALUE_PLACEMENT)
            {
                // The multiplication returns a scalar packaged in a 1x1 matrix
                inputTorque = -mKMatrix.multiply(state).get(0, 0);
            }

            if (Math.abs(inputTorque) > Constants.MAX_TORQUE_OUTPUT)
                inputTorque = Math.copySign(Constants.MAX_TORQUE_OUTPUT, inputTorque);

            if (t % Constants.SAVE_STATE_EVERY_X_NS == 0)
                inputTorqueValues.add(inputTorque);

            if (Constants.INCLUDE_PERTURBATIONS)
            {
                for (Perturbation perturbation : Constants.PERTURBATIONS)
                {
                    if (t >= perturbation.time && t <= perturbation.time + perturbation.duration)
                        inputTorque += perturbation.force;
                }
            }

            /*
            The system is time-invariant, so the only parameters for the functions should be the state of the system

            y(x) represents the system
            f(y) represents the derivative of the system

            Begin at y_0, the initial conditions of the system, which will mutate themselves during the calculations

            k_1 = f(y(now))
            k_2 = f(y(now) + step * k_1 / 2)
            k_3 = f(y(now) + step * k_2 / 2)
            k_4 = f(y(now) + step * k_3)

            y(next) = y(now) + 1/6 * step * (k_1 + 2k_2 + 2k_3 + k4)
            t(next) = t(now) + step

            Save a clone of the state
            Mutate the state
            Continue until we've elapsed enough time
            */

            Matrix k1 = getDerivativeOfSystemAt(state, inputTorque);
            Matrix k2 = getDerivativeOfSystemAt(state.add(k1.multiply(Constants.DT_S / 2.0)), inputTorque);
            Matrix k3 = getDerivativeOfSystemAt(state.add(k2.multiply(Constants.DT_S / 2.0)), inputTorque);
            Matrix k4 = getDerivativeOfSystemAt(state.add(k3.multiply(Constants.DT_S)), inputTorque);

            // dy = 1/6 * step * (k_1 + 2k_2 + 2k_3 + k4)
            Matrix deltaStateMatrix = k1.add(k2.multiply(2)).add(k3.multiply(2)).add(k4).multiply((1.0 / 6.0) * Constants.DT_S);

            state = state.add(deltaStateMatrix);

            if (t % Constants.SAVE_STATE_EVERY_X_NS == 0)
                mStates.put(t, state.clone());
        }

        final Timeline drawSimulationTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, event ->
                {
                    if (mDrawSimulation)
                    {
                        long stateToDrawTime = (int) (mStateToDrawTime / Constants.SAVE_STATE_EVERY_X_NS) * Constants.SAVE_STATE_EVERY_X_NS;

                        if (stateToDrawTime == Constants.TIMESPAN)
                            stateToDrawTime -= Constants.SAVE_STATE_EVERY_X_NS;

                        mPendulum.getTransforms().clear();
                        mPendulum.getTransforms().add(new Rotate(-Math.toDegrees(getTheta(mStates.get(stateToDrawTime))),
                                mPendulum.getX() + (mPendulum.getWidth() / 2), mPendulum.getY() + mPendulum.getHeight(), 0, Rotate.Z_AXIS));

                        mTimeSelectSlider.setValue(mStateToDrawTime);

                        if (stateToDrawTime + Constants.SAVE_STATE_EVERY_X_NS < Constants.TIMESPAN)
                            mStateToDrawTime = stateToDrawTime + Constants.SAVE_STATE_EVERY_X_NS;

                        if (mLastLoop)
                        {
                            mDrawSimulation = false;
                            mLastLoop = false;
                        }
                    }
                }),
                new KeyFrame(Duration.seconds(Constants.nanosecondsToSeconds(Constants.SAVE_STATE_EVERY_X_NS)))
        );
        drawSimulationTimeline.setCycleCount(Timeline.INDEFINITE);
        drawSimulationTimeline.play();

        HashMap<Long, Double> thetaValues = new HashMap<>();
        for (Map.Entry<Long, Matrix> pair : mStates.entrySet())
            thetaValues.put(pair.getKey(), getTheta(pair.getValue()));

        if (Constants.FIX_STUPID_PLOTS)
        {
            for (long t = Constants.SAVE_STATE_EVERY_X_NS; t < Constants.TIMESPAN; t += Constants.SAVE_STATE_EVERY_X_NS)
            {
                if (Math.abs(thetaValues.get(t) - thetaValues.get(t - Constants.SAVE_STATE_EVERY_X_NS)) > 0.5)
                {
                    thetaValues.put(t, thetaValues.get(t) + Math.copySign(2 * Math.PI, thetaValues.get(t - Constants.SAVE_STATE_EVERY_X_NS)));
                }
            }
        }

        if (Constants.SAVE_TO_CSV)
        {
            if (Constants.SAVE_THETA)
            {
                try
                {
                    FileWriter fileWriter = new FileWriter("theta.csv");

                    for (long t = 0L; t < Constants.TIMESPAN; t += Constants.SAVE_STATE_EVERY_X_NS)
                    {
                        fileWriter.append(String.valueOf(Constants.nanosecondsToSeconds(t)))
                                .append(",")
                                .append(String.valueOf(thetaValues.get(t)))
                                .append('\n');
                    }

                    fileWriter.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            if (Constants.SAVE_TORQUE)
            {
                try
                {
                    FileWriter fileWriter = new FileWriter("torque.csv");

                    for (int i = 0; i < inputTorqueValues.size(); ++i)
                    {
                        fileWriter.append(String.valueOf(i * Constants.nanosecondsToSeconds(Constants.SAVE_STATE_EVERY_X_NS)))
                                .append(",")
                                .append(String.valueOf(inputTorqueValues.get(i)))
                                .append('\n');
                    }

                    fileWriter.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        if (Constants.SHOW_PLOTS)
        {
            ArrayList<Double> newThetaValues = new ArrayList<>();

            for (long t = 0L; t < Constants.TIMESPAN; t += Constants.SAVE_STATE_EVERY_X_NS)
                newThetaValues.add(thetaValues.get(t));

            Plot thetaPlot = Plot.create(mPythonConfig);
            thetaPlot.title("Theta");
            thetaPlot.plot().add(newThetaValues);
            Plot inputTorquePlot = Plot.create(mPythonConfig);
            inputTorquePlot.title("Input Torque");
            inputTorquePlot.plot().add(inputTorqueValues);

            try
            {
                thetaPlot.show();
                inputTorquePlot.show();
            } catch (IOException | PythonExecutionException e)
            {
                e.printStackTrace();
            }
        }

        stage.setTitle("Inverted Pendulum Simulator");
        stage.setScene(new Scene(mRoot));

        if (Constants.VISUALIZE)
            stage.show();
    }

    private double getBoundedAngle(double angle)
    {
        int fullRotations = (int) (Math.abs(angle) / (2 * Math.PI));

        double newAngle = angle - (fullRotations * (2 * Math.PI));
        if (newAngle > Math.PI)
            newAngle -= (2 * Math.PI);
        else if (newAngle < -Math.PI)
            newAngle += (2 * Math.PI);

        return newAngle;
    }

    private Matrix getDerivativeOfSystemAt(Matrix state, double inputTorque)
    {
        if (Constants.LINEARIZED)
        {
            return mAMatrix.multiply(state).add(mBMatrix.multiply(inputTorque));
        } else
        {
            return new Matrix(new ArrayList<>(Collections.singletonList(new ArrayList<>(Arrays.asList(
                    getThetaDot(state),
                    12 * (Constants.PENDULUM_MASS * Constants.GRAVITY * Constants.PENDULUM_LENGTH * Math.sin(getTheta(state))
                            + 2 * inputTorque
                            - 2 * getThetaDot(state) * Constants.VISCOUS_FRICTION)
                            / (Math.pow(Constants.PENDULUM_WIDTH, 2) + 4 * Math.pow(Constants.PENDULUM_LENGTH, 2))
            )))));
        }
    }

    private double getTheta(Matrix state)
    {
        return state.get(0, 0);
    }

    private double getThetaDot(Matrix state)
    {
        return state.get(0, 1);
    }
}