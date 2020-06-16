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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Main extends Application
{
    private Pane mRoot;
    private Rectangle mTrack;
    private Rectangle mCart;
    private Rectangle mPendulum;
    private Circle mAxle;

    private Matrix mAMatrix = new Matrix(new ArrayList<>(Arrays.asList(
            new ArrayList<>(Arrays.asList(0.0, 0.0, 0.0, 0.0)),
            new ArrayList<>(Arrays.asList(1.0, 0.0, 0.0, 0.0)),
            new ArrayList<>(Arrays.asList(0.0, (Constants.PENDULUM_MASS * Constants.GRAVITY) / Constants.CART_MASS, 0.0, (2 * Constants.GRAVITY * (Constants.CART_MASS + Constants.PENDULUM_MASS)) / (Constants.CART_MASS * Constants.PENDULUM_LENGTH))),
            new ArrayList<>(Arrays.asList(0.0, 0.0, 1.0, 0.0))
    )));

    private Button mPlayPauseButton;
    private Button mTimestepButton;
    private Slider mTimeSelectSlider;

    private boolean mDrawSimulation = Constants.START_WITH_SIMULATION_RUNNING;
    private boolean mLastLoop = false;

    // Dividing by 1000 converts from milliseconds to seconds
    private final double mDt = 5.0 / 1000.0;

    // In seconds
    private int mTimespan = 10;

    private ArrayList<Matrix> mStates = new ArrayList<>(Collections.singletonList(Constants.INITIAL_STATE));

    private Matrix mStateToDraw = mStates.get(0);
    private int mStepsToSkip = (int) (0.1 / mDt);

    private ArrayList<Double> mForceOnCartValues = new ArrayList<>(Collections.singletonList(0.0));

    private PythonConfig mPythonConfig = PythonConfig.pythonBinPathConfig("C:\\Program Files\\Python38\\python3.exe");

    private Plot mXPlot = Plot.create(mPythonConfig);
    private Plot mXDotPlot = Plot.create(mPythonConfig);
    private Plot mThetaPlot = Plot.create(mPythonConfig);
    private Plot mThetaDotPlot = Plot.create(mPythonConfig);

    @Override
    public void start(Stage stage)
    {
        stage.setOnCloseRequest(e ->
                System.exit(0));

        mRoot = new Pane();
        mRoot.setPrefWidth(Constants.FRAME_WIDTH);
        mRoot.setPrefHeight(Constants.FRAME_HEIGHT);
        mRoot.setStyle("-fx-background-color: #262626;");

        mCart = new Rectangle();
        mCart.setWidth(Constants.CART_WIDTH * Constants.PIXELS_PER_METER);
        mCart.setHeight(Constants.CART_HEIGHT * Constants.PIXELS_PER_METER);
        mCart.setX((mRoot.prefWidthProperty().doubleValue() - mCart.getWidth()) / 2);
        mCart.setY(mRoot.prefHeightProperty().doubleValue() - mCart.getHeight() - Constants.OFFSET_FROM_BOTTOM);
        mCart.setFill(Paint.valueOf("black"));
        mCart.setStroke(Paint.valueOf("white"));
        mCart.setStrokeWidth(0.5);

        mTrack = new Rectangle();
        mTrack.setWidth(999999999);
        mTrack.setHeight(5);
        mTrack.setX(-mTrack.getWidth() / 2);
        mTrack.setY(mCart.getY() + (mCart.getHeight() / 2));
        mTrack.setStroke(Paint.valueOf("#999999"));
        mTrack.setStrokeWidth(2);
        mRoot.getChildren().add(mTrack);

        mRoot.getChildren().add(mCart);

        mPendulum = new Rectangle();
        mPendulum.setWidth(Constants.PENDULUM_WIDTH * Constants.PIXELS_PER_METER);
        mPendulum.setHeight(Constants.PENDULUM_LENGTH * Constants.PIXELS_PER_METER);
        mPendulum.setX((mCart.getX() + (mCart.getWidth() / 2) - (mPendulum.getWidth() / 2)));
        mPendulum.setY(mCart.getY() - mPendulum.getHeight() + (mCart.getHeight() / 2));
        mPendulum.setFill(Paint.valueOf("#660000"));
        mPendulum.setStroke(Paint.valueOf("white"));
        mPendulum.setStrokeWidth(0.5);
        mRoot.getChildren().add(mPendulum);

        mAxle = new Circle();
        mAxle.setRadius(Constants.AXLE_RADIUS * Constants.PIXELS_PER_METER);
        mAxle.setCenterX(mCart.getX() + (mCart.getWidth() / 2));
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

        mTimestepButton = new Button();
        mTimestepButton.setText("Step");
        mTimestepButton.setLayoutX(10);
        mTimestepButton.setLayoutY(50);
        mTimestepButton.setOnAction((actionEvent) ->
        {
            if (mStates.lastIndexOf(mStateToDraw) + mStepsToSkip < mStates.size())
                mStateToDraw = mStates.get(mStates.lastIndexOf(mStateToDraw) + mStepsToSkip);
            else
                mStateToDraw = mStates.get(mStates.size() - 1);

            mDrawSimulation = true;
            mLastLoop = true;
        });
        mRoot.getChildren().add(mTimestepButton);

        mTimeSelectSlider = new Slider();
        mTimeSelectSlider.setMax(mTimespan / mDt);
        mTimeSelectSlider.setLayoutX(10);
        mTimeSelectSlider.setLayoutY(mRoot.getPrefHeight() - 40);
        mTimeSelectSlider.setPrefWidth(mRoot.getPrefWidth() - 20);
        mTimeSelectSlider.setOnMouseDragged((mouseEvent) ->
        {
            mStateToDraw = mStates.get((int) mTimeSelectSlider.getValue());
            mDrawSimulation = true;
            mLastLoop = true;
        });
        mRoot.getChildren().add(mTimeSelectSlider);

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

        double previousI = 0.0;
        double previousError = 0.0;
        Matrix state = Constants.INITIAL_STATE.clone();
        for (double t = 0; t < mTimespan; t += mDt)
        {
            state.set(0, 2, getBoundedAngle(getTheta(state)));

            double forceOnCart = 0.0;

            if (Constants.USE_PID)
            {
                double P = Constants.KP * -getTheta(state);
                double I = Constants.KI * -getTheta(state) + previousI;
                double D = Constants.KD * (-getTheta(state) - previousError);

                previousI = I;
                previousError = -getTheta(state);

                forceOnCart = P + I + D;
            }

            if (Constants.INCLUDE_PERTURBATIONS)
            {
                for (Perturbation perturbation : Constants.PERTURBATIONS)
                {
                    if (t >= perturbation.time && t <= perturbation.time + perturbation.duration)
                        forceOnCart += perturbation.force;
                }
            }

            Matrix k1 = getDerivativeOfSystemAt(state, forceOnCart);
            Matrix k2 = getDerivativeOfSystemAt(state.add(k1.multiply(mDt / 2.0)), forceOnCart);
            Matrix k3 = getDerivativeOfSystemAt(state.add(k2.multiply(mDt / 2.0)), forceOnCart);
            Matrix k4 = getDerivativeOfSystemAt(state.add(k3.multiply(mDt)), forceOnCart);

            // dy = 1/6 * step * (k_1 + 2k_2 + 2k_3 + k4)
            Matrix deltaStateMatrix = k1.add(k2.multiply(2)).add(k3.multiply(2)).add(k4).multiply((1.0 / 6.0) * mDt);

            state = state.add(deltaStateMatrix);
            mStates.add(state.clone());
        }

        final Timeline drawSimulationTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, event ->
                {
                    if (mDrawSimulation)
                    {
                        mPendulum.getTransforms().clear();
                        mPendulum.getTransforms().add(new Rotate(-Math.toDegrees(getTheta(mStateToDraw)),
                                mPendulum.getX() + (mPendulum.getWidth() / 2), mPendulum.getY() + mPendulum.getHeight(), 0, Rotate.Z_AXIS));

                        mCart.setTranslateX(getX(mStateToDraw) * Constants.PIXELS_PER_METER);

                        mPendulum.setTranslateX(mCart.getTranslateX());
                        mAxle.setTranslateX(mCart.getTranslateX());

                        mTimeSelectSlider.setValue(mStates.lastIndexOf(mStateToDraw));

                        if (mStates.lastIndexOf(mStateToDraw) != mStates.size() - 1)
                            mStateToDraw = mStates.get(mStates.lastIndexOf(mStateToDraw) + 1);

                        if (mLastLoop)
                        {
                            mDrawSimulation = false;
                            mLastLoop = false;
                        }
                    }
                }),
                new KeyFrame(Duration.seconds(mDt))
        );
        drawSimulationTimeline.setCycleCount(Timeline.INDEFINITE);
        drawSimulationTimeline.play();

        if (Constants.SHOW_PLOTS)
        {
            mXPlot.title("X");
            mXDotPlot.title("X Dot");
            mThetaPlot.title("Theta");
            mThetaDotPlot.title("Theta Dot");

            ArrayList<Double> xValues = new ArrayList<>();
            ArrayList<Double> xDotValues = new ArrayList<>();
            ArrayList<Double> thetaValues = new ArrayList<>();
            ArrayList<Double> thetaDotValues = new ArrayList<>();

            for (Matrix matrix : mStates)
            {
                xValues.add(getX(matrix));
                xDotValues.add(getXDot(matrix));
                thetaValues.add(getTheta(matrix));
                thetaDotValues.add(getThetaDot(matrix));
            }

            mXPlot.plot().add(xValues);
            mXDotPlot.plot().add(xDotValues);
            mThetaPlot.plot().add(thetaValues);
            mThetaDotPlot.plot().add(thetaDotValues);

            try
            {
                mXPlot.show();
                mXDotPlot.show();
                mThetaPlot.show();
                mThetaDotPlot.show();
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

    private Matrix getDerivativeOfSystemAt(Matrix state, double forceOnCart)
    {
        return new Matrix(new ArrayList<>(Collections.singletonList(new ArrayList<>(Arrays.asList(
                getDerivativeOfX(state),
                getDerivativeOfXDot(state, forceOnCart),
                getDerivativeOfTheta(state),
                getDerivativeOfThetaDot(state, forceOnCart)
        )))));
    }

    private double getDerivativeOfX(Matrix state)
    {
        return getXDot(state);
    }

    private double getDerivativeOfXDot(Matrix state, double forceOnCart)
    {
        return (2 * forceOnCart
                + 2 * Constants.PENDULUM_MASS * Constants.GRAVITY * Math.sin(getTheta(state)) * Math.cos(getTheta(state))
                - Constants.PENDULUM_MASS * Constants.PENDULUM_LENGTH * Math.pow(getThetaDot(state), 2) * Math.sin(getTheta(state)))
                / (2 * (Constants.CART_MASS + Constants.PENDULUM_MASS * Math.pow(Math.sin(getTheta(state)), 2)));
    }

    private double getDerivativeOfTheta(Matrix state)
    {
        return getThetaDot(state);
    }

    private double getDerivativeOfThetaDot(Matrix state, double forceOnCart)
    {
        return (2 * forceOnCart * Math.cos(getTheta(state))
                - Constants.PENDULUM_MASS * Constants.PENDULUM_LENGTH * Math.pow(getThetaDot(state), 2) * Math.sin(getTheta(state)) * Math.cos(getTheta(state))
                + 2 * Constants.CART_MASS * Constants.GRAVITY * Math.sin(getTheta(state))
                + 2 * Constants.PENDULUM_MASS * Constants.GRAVITY * Math.sin(getTheta(state)))
                / (Constants.PENDULUM_LENGTH * (Constants.CART_MASS + Constants.PENDULUM_MASS * Math.pow(Math.sin(getTheta(state)), 2)));
    }

    private double getX(Matrix state)
    {
        return state.get(0, 0);
    }

    private double getXDot(Matrix state)
    {
        return state.get(0, 1);
    }

    private double getTheta(Matrix state)
    {
        return state.get(0, 2);
    }

    private double getThetaDot(Matrix state)
    {
        return state.get(0, 3);
    }
}