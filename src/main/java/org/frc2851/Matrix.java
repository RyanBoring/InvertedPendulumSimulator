package org.frc2851;

import java.util.ArrayList;

public class Matrix
{
    private ArrayList<ArrayList<Double>> mValues;

    public Matrix(int columns, int rows)
    {
        mValues = new ArrayList<>();

        for (int column = 0; column < columns; ++column)
        {
            ArrayList<Double> columnValues = new ArrayList<>();
            for (int row = 0; row < rows; ++row)
            {
                columnValues.add(0.0);
            }
            mValues.add(columnValues);
        }
    }

    public Matrix(ArrayList<ArrayList<Double>> values)
    {
        this.mValues = values;
    }

    public ArrayList<Double> getColumn(int column)
    {
        return mValues.get(column);
    }

    public ArrayList<Double> getRow(int row)
    {
        ArrayList<Double> rowArrayList = new ArrayList<>();
        for (ArrayList<Double> column : mValues)
        {
            rowArrayList.add(column.get(row));
        }
        return rowArrayList;
    }

    public double get(int column, int row)
    {
        return mValues.get(column).get(row);
    }

    public void set(int column, int row, double value)
    {
        mValues.get(column).set(row, value);
    }

    public int getRows()
    {
        return mValues.get(0).size();
    }

    public int getColumns()
    {
        return mValues.size();
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
                returnMatrix.set(column, row, mValues.get(column).get(row) + matrix.mValues.get(column).get(row));
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
                    dotProduct += mValues.get(value).get(row) * matrix.mValues.get(column).get(value);
                }

                returnMatrix.set(column, row, dotProduct);
            }
        }

        return returnMatrix;
    }

    public Matrix multiply(double d)
    {
        Matrix returnMatrix = new Matrix(getColumns(), getRows());

        for (int column = 0; column < getColumns(); ++column)
        {
            for (int row = 0; row < getRows(); ++row)
            {
                returnMatrix.set(column, row, get(column, row) * d);
            }
        }

        return returnMatrix;
    }

    @Override
    public Matrix clone()
    {
        Matrix clone = new Matrix(getColumns(), getRows());

        for (int column = 0; column < getColumns(); ++column)
        {
            for (int row = 0; row < getRows(); ++row)
            {
                clone.set(column, row, mValues.get(column).get(row));
            }
        }

        return clone;
    }

    @Override
    public String toString()
    {
        StringBuilder stringBuilder = new StringBuilder();

        for (int row = 0; row < getRows(); row++)
        {
            for (int column = 0; column < getColumns(); column++)
            {
                stringBuilder.append(String.format("%4.2f", get(column, row)));
            }
            if (row != getRows() - 1)
                stringBuilder.append('\n');
        }

        return stringBuilder.toString();
    }
}
