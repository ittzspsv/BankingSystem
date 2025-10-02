package database_modal;

import java.sql.*;

public class TableConnector
{
    private static TableConnector instance;
    private Connection connection;
    private final String url = "jdbc:sqlite:storage/banking.db";

    private TableConnector()
    {
        try
        {
            connection = DriverManager.getConnection(url);
            connection.setAutoCommit(false);

            try (Statement stmt = connection.createStatement())
            {
                stmt.execute("PRAGMA journal_mode=WAL;");
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static synchronized TableConnector getInstance()
    {
        if (instance == null)
        {
            instance = new TableConnector();
        }
        return instance;
    }

    public boolean execute(String query, Object... params)
    {
        try (PreparedStatement ps = connection.prepareStatement(query))
        {
            for (int i = 0; i < params.length; i++)
            {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
            return true;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public ResultSet executeQuery(String query, Object... params)
    {
        try
        {
            PreparedStatement ps = connection.prepareStatement(query);
            for (int i = 0; i < params.length; i++)
            {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeQuery();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public void commit()
    {
        try
        {
            connection.commit();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public void rollback()
    {
        try
        {
            connection.rollback();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public int getLastInsertId()
    {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid() AS id"))
        {
            if (rs.next())
            {
                return rs.getInt("id");
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return -1;
    }

    public void close()
    {
        try
        {
            if (connection != null)
            {
                connection.close();
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
}
