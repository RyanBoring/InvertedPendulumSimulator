package org.frc2851;

import java.util.ArrayList;

public class Matrix
{
    public ArrayList<ArrayList<Double>> values;

    public Matrix(int columns, int rows)
    {
        values = new ArrayList<>();

        for (int column = 0; column < columns; ++column)
        {
            ArrayList<Double> columnValues = new ArrayList<>();
            for (int row = 0; row < rows; ++row)
            {
                columnValues.add(0.0);
            }
            values.add(columnValues);
        }
    }

    public Matrix(ArrayList<ArrayList<Double>> values)
    {
        this.values = values;
    }

    public int getRows()
    {
        return values.get(0).size();
    }

    public int getColumns()
    {
        return values.size();
    }

    public Matrix add(Matrix matrix)
    {
        if (getColumns() != matrix.getColumns() || getRows() != matrix.getRows())
        {
            System.out.println("Mismatched dimensions");
            System.exit(-1);
        }

        Matrix returnMatrix = new Matrix(getColumns(), getRows());
        for (int column = 0; column < getColumns(); ++column)
        {
            for (int row = 0; row < getRows(); ++row)
            {
                returnMatrix.values.get(column).set(row, values.get(column).get(row) + matrix.values.get(column).get(row));
            }
        }

        return returnMatrix;
    }

    public Matrix multiply(Matrix matrix)
    {
        if (getColumns() != matrix.getRows())
        {
            System.out.println("Mismatched dimensions");
            System.exit(-1);
        }

        Matrix returnMatrix = new Matrix(matrix.getColumns(), getRows());

        for (int row = 0; row < getRows(); ++row)
        {
            for (int column = 0; column < matrix.getColumns(); ++column)
            {
                double dotProduct = 0;
                for (int value = 0; value < getColumns(); ++value)
                {
                    dotProduct += values.get(value).get(row) * matrix.values.get(column).get(value);
                }

                returnMatrix.values.get(column).set(row, dotProduct);
            }
        }

        return returnMatrix;
    }
}
