package org.frc2851;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Main extends Application
{
    private final double mCartWidth = 0.25;
    private final double mCartHeight = 0.1;
    private final double mCartMass = 5;
    private final double mPendulumWidth = 0.05;
    private final double mPendulumLength = 0.4;
    private final double mPendulumMass = 0.5;
    private final double mAxleRadius = 0.01;
    private final double mGravity = 9.81;
    private final double mDt = 10;

    private final double mLengthMultiplier = 250;
    private final double mOffsetFromBottom = 100;

    private Pane mRoot;
    private Rectangle mTrack;
    private Rectangle mCart;
    private Rectangle mPendulum;
    private Circle mAxle;

    private Matrix mAMatrix = new Matrix(new ArrayList<>(Arrays.asList(
            new ArrayList<>(Arrays.asList(0.0, 0.0, 0.0, 0.0)),
            new ArrayList<>(Arrays.asList(1.0, 0.0, 0.0, 0.0)),
            new ArrayList<>(Arrays.asList(0.0, (mPendulumMass * mGravity) / mCartMass, 0.0, (2 * mGravity * (mCartMass + mPendulumMass)) / (mCartMass * mPendulumLength))),
            new ArrayList<>(Arrays.asList(0.0, 0.0, 1.0, 0.0))
    )));

    private double mPendulumAngularVelocity = 0.5;

    private double mCartVelocity = 0.0;

    private double mForceOnCart = 0.0;

    private Button mPlayPauseButton;
    private Button mTimestepButton;

    private boolean mIsPlaying = false;
    private boolean mLastLoop = false;

    @Override
    public void start(Stage stage)
    {
        stage.setOnCloseRequest(e ->
                System.exit(0));

        mRoot = new Pane();
        mRoot.setPrefWidth(700);
        mRoot.setPrefHeight(500);
        mRoot.setStyle("-fx-background-color: #262626;");

        mCart = new Rectangle();
        mCart.setWidth(mCartWidth * mLengthMultiplier);
        mCart.setHeight(mCartHeight * mLengthMultiplier);
        mCart.setX((mRoot.prefWidthProperty().doubleValue() - mCart.getWidth()) / 2);
        mCart.setY(mRoot.prefHeightProperty().doubleValue() - mCart.getHeight() - mOffsetFromBottom);
        mCart.setFill(Paint.valueOf("black"));
        mCart.setStroke(Paint.valueOf("white"));
        mCart.setStrokeWidth(0.5);

        mTrack = new Rectangle();
        mTrack.setWidth(2000);
        mTrack.setHeight(5);
        mTrack.setX(-1000);
        mTrack.setY(mCart.getY() + (mCart.getHeight() / 2));
        mTrack.setStroke(Paint.valueOf("#999999"));
        mTrack.setStrokeWidth(2);
        mRoot.getChildren().add(mTrack);

        mRoot.getChildren().add(mCart);

        mPendulum = new Rectangle();
        mPendulum.setWidth(mPendulumWidth * mLengthMultiplier);
        mPendulum.setHeight(mPendulumLength * mLengthMultiplier);
        mPendulum.setX((mCart.getX() + (mCart.getWidth() / 2) - (mPendulum.getWidth() / 2)));
        mPendulum.setY(mCart.getY() - mPendulum.getHeight());
        mPendulum.setFill(Paint.valueOf("#660000"));
        mPendulum.setStroke(Paint.valueOf("white"));
        mPendulum.setStrokeWidth(0.5);
        mRoot.getChildren().add(mPendulum);

        mAxle = new Circle();
        mAxle.setRadius(mAxleRadius * mLengthMultiplier);
        mAxle.setCenterX(mCart.getX() + (mCart.getWidth() / 2));
        mAxle.setCenterY(mCart.getY());
        mAxle.setFill(Paint.valueOf("#999999"));
        mRoot.getChildren().add(mAxle);

        mPlayPauseButton = new Button();
        mPlayPauseButton.setText(mIsPlaying ? "Pause" : "Play");
        mPlayPauseButton.setLayoutX(10);
        mPlayPauseButton.setLayoutY(10);
        mPlayPauseButton.setOnAction((actionEvent) ->
        {
            mIsPlaying = !mIsPlaying;
            mPlayPauseButton.setText(mIsPlaying ? "Pause" : "Play");
        });
        mRoot.getChildren().add(mPlayPauseButton);

        mTimestepButton = new Button();
        mTimestepButton.setText("Step");
        mTimestepButton.setLayoutX(10);
        mTimestepButton.setLayoutY(50);
        mTimestepButton.setOnAction((actionEvent) ->
        {
            mIsPlaying = true;
            mLastLoop = true;
        });
        mRoot.getChildren().add(mTimestepButton);

        final Timeline physicsSimulator = new Timeline(
                new KeyFrame(Duration.ZERO, event ->
                {
                    if (mIsPlaying)
                    {
                        double x = mCart.getTranslateX();

                        double angle = 0.0;
                        if (mPendulum.getTransforms().size() > 0)
                            angle = ((Rotate) mPendulum.getTransforms().get(mPendulum.getTransforms().size() - 1)).getAngle();

                        Matrix stateMatrix = new Matrix(new ArrayList<>(Collections.singletonList(new ArrayList<>(Arrays.asList(x, mCartVelocity, angle, mPendulumAngularVelocity)))));
                        Matrix stateMatrixDot = mAMatrix.multiply(stateMatrix);
                        Matrix newStateMatrix = stateMatrix.add(stateMatrixDot);

                        System.out.println("Old");
                        System.out.println("------------");
                        System.out.println("X: " + stateMatrix.values.get(0).get(0));
                        System.out.println("X_dot: " + stateMatrix.values.get(0).get(1));
                        System.out.println("Theta: " + stateMatrix.values.get(0).get(2));
                        System.out.println("Theta_dot: " + stateMatrix.values.get(0).get(3));
                        System.out.println();

                        System.out.println("New");
                        System.out.println("------------");
                        System.out.println("X: " + newStateMatrix.values.get(0).get(0));
                        System.out.println("X_dot: " + newStateMatrix.values.get(0).get(1));
                        System.out.println("Theta: " + newStateMatrix.values.get(0).get(2));
                        System.out.println("Theta_dot: " + newStateMatrix.values.get(0).get(3));
                        System.out.println();

                        //System.exit(-1);

                        mPendulumAngularVelocity = newStateMatrix.values.get(0).get(3);
                        mPendulum.getTransforms().add(new Rotate(angle + Math.toDegrees(mPendulumAngularVelocity * (mDt / 1000)),
                                mPendulum.getX() + (mPendulum.getWidth() / 2), mPendulum.getY() + mPendulum.getHeight(), 0, Rotate.Z_AXIS));

                        mCartVelocity = newStateMatrix.values.get(0).get(1);
                        mCart.setTranslateX(mCart.getTranslateX() + mCartVelocity);

                        mPendulum.setX((mCart.getX() + mCart.getTranslateX() + (mCart.getWidth() / 2) - (mPendulum.getWidth() / 2)));
                        mAxle.setCenterX(mCart.getX() + mCart.getTranslateX() + (mCart.getWidth() / 2));

                        if (mLastLoop)
                        {
                            mIsPlaying = false;
                            mLastLoop = false;
                        }
                    }
                }),
                new KeyFrame(Duration.millis(mDt))
        );
        physicsSimulator.setCycleCount(Timeline.INDEFINITE);
        physicsSimulator.play();

        stage.setTitle("Inverted Pendulum Simulator");
        stage.setScene(new Scene(mRoot));
        stage.show();
    }
}