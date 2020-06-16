package org.frc2851;

public class Perturbation
{
    public double time;
    public double force;
    public double duration;

    public Perturbation(double time, double force, double duration)
    {
        this.time = time;
        this.force = force;
        this.duration = duration;
    }
}
