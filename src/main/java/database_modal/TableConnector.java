package database_modal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TableConnector
{
    private Connection connection;
    private final String url = "jdbc:sqlite:storage/banking.db";

    public TableConnector()
    {
        try
        {
            connection = DriverManager.getConnection(url);
            System.out.println("Connected to Database");
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    //Helper function to execute DDL Commands
    public boolean execute(String query)
    {
        try (Statement stmt = connection.createStatement())
        {
            stmt.execute(query);
            return true;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    //Helper function to execute DML Commands
    public ResultSet executeQuery(String query)
    {
        try
        {
            Statement stmt = connection.createStatement();
            return stmt.executeQuery(query);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public void close()
    {
        try
        {
            if (connection != null) connection.close();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
}