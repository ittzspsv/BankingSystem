package banking_modal;

import java.sql.ResultSet;
import java.util.*;
import database_modal.TableConnector;

class BuildAccountDetails
{
    private HashMap<Integer, String> AccountType = new HashMap<>();

    public BuildAccountDetails(int customerID)
    {
        AccountType.put(customerID, "Primary");
    }

    private Integer GetCustomerID(String name, String DOB, String email, String phoneNumber, String gender) 
    {
        TableConnector conn = TableConnector.getInstance();
        try
        {
            ResultSet row = conn.executeQuery(
                "SELECT ID FROM Customers WHERE Name = ? AND DateOfBirth = ? AND Email = ? AND PhoneNumber = ? AND Gender = ?",
                name, DOB, email, phoneNumber, gender
            );

            if (row != null && row.next()) 
            {
                return row.getInt("ID");
            } 
            else 
            {
                return null;
            }
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            return null;
        }
    }

    public void PushAccountDetails(String name, String DOB, String email, String phoneNumber, String gender)
    {
        Integer CustomerID = GetCustomerID(name, DOB, email, phoneNumber, gender);
        if (CustomerID != null)
        {
            AccountType.put(CustomerID, "Secondary");
        }
    }

    public HashMap<Integer, String> GetAccountBuilder()
    {
        return AccountType;
    }
}


public class Account
{
    private TableConnector conn;

    public Account()
    {
        this.conn = TableConnector.getInstance();
    }

    public void CreateAccount(BuildAccountDetails acc, String accountType) 
    {
        try 
        {
            Random rand = new Random();
            int accno = rand.nextInt(99999999);

            conn.execute(
                "INSERT INTO Accounts (AccountNumber, Balance, Type, Opened_At, Status) VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?)",
                accno, 500.0, accountType, "Active"
            );

            int accountID = conn.getLastInsertId();

            for (Map.Entry<Integer, String> entry : acc.GetAccountBuilder().entrySet()) 
            {
                conn.execute(
                    "INSERT INTO AccountHolders (AccountID, CustomerID, Role) VALUES (?, ?, ?)",
                    accountID, entry.getKey(), entry.getValue()
                );
            }

            conn.commit();
        } 
        catch (Exception e) 
        {
            conn.rollback();
            e.printStackTrace();
        }
    }

    public boolean Withdraw(String sessionId, double amount)
    {
        try
        {
            conn.execute("PRAGMA busy_timeout = 5000");
            conn.execute("BEGIN TRANSACTION");

            ResultSet rsSession = conn.executeQuery("SELECT customer_id FROM Sessions WHERE session_id = ?", sessionId);
            if (rsSession == null || !rsSession.next())
            {
                conn.execute("ROLLBACK");
                return false;
            }

            int customerId = rsSession.getInt("customer_id");

            ResultSet rsAccount = conn.executeQuery(
                "SELECT a.ID, a.Balance FROM Accounts a " +
                "JOIN AccountHolders h ON a.ID = h.AccountID " +
                "WHERE h.CustomerID = ? AND h.Role = 'Primary'",
                customerId
            );

            if (rsAccount == null || !rsAccount.next())
            {
                conn.execute("ROLLBACK");
                return false;
            }

            int accountId = rsAccount.getInt("ID");
            double balance = rsAccount.getDouble("Balance");

            if (balance < amount)
            {
                conn.execute("ROLLBACK");
                return false;
            }

            conn.execute("UPDATE Accounts SET Balance = ? WHERE ID = ?", balance - amount, accountId);
            conn.execute("INSERT INTO Transactions (AccountID, CustomerID, Type, Amount, Status) VALUES (?, ?, 'Withdraw', ?, 'Success')",
                        accountId, customerId, amount);

            conn.execute("COMMIT");
            return true;
        } 
        catch (Exception e)
        {
            try { conn.execute("ROLLBACK"); } catch (Exception ex) {}
            e.printStackTrace();
            return false;
        }
    }

    public boolean Deposit(String sessionId, double amount)
    {
        try
        {
            conn.execute("PRAGMA busy_timeout = 5000");
            conn.execute("BEGIN TRANSACTION");

            ResultSet rsSession = conn.executeQuery("SELECT customer_id FROM Sessions WHERE session_id = ?", sessionId);
            if (rsSession == null || !rsSession.next())
            {
                conn.execute("ROLLBACK");
                return false;
            }

            int customerId = rsSession.getInt("customer_id");

            ResultSet rsAccount = conn.executeQuery(
                "SELECT a.ID, a.Balance FROM Accounts a " +
                "JOIN AccountHolders h ON a.ID = h.AccountID " +
                "WHERE h.CustomerID = ? AND h.Role = 'Primary'",
                customerId
            );

            if (rsAccount == null || !rsAccount.next())
            {
                conn.execute("ROLLBACK");
                return false;
            }

            int accountId = rsAccount.getInt("ID");
            double balance = rsAccount.getDouble("Balance");

            conn.execute("UPDATE Accounts SET Balance = ? WHERE ID = ?", balance + amount, accountId);
            conn.execute("INSERT INTO Transactions (AccountID, CustomerID, Type, Amount, Status) VALUES (?, ?, 'Deposit', ?, 'Success')",
                        accountId, customerId, amount);

            conn.execute("COMMIT");
            return true;
        } 
        catch (Exception e)
        {
            try { conn.execute("ROLLBACK"); } catch (Exception ex) {}
            e.printStackTrace();
            return false;
        }
    }

    public boolean TransferMoney(String sessionId, Long sourceAccountNumber, long targetAccountNumber, double amount)
    {
        try
        {
            conn.execute("PRAGMA busy_timeout = 5000");
            conn.execute("BEGIN TRANSACTION");

            ResultSet rsSession = conn.executeQuery("SELECT customer_id FROM Sessions WHERE session_id = ?", sessionId);
            if (rsSession == null || !rsSession.next()) 
            { 
                conn.execute("ROLLBACK"); 
                return false; 
            }

            int customerId = rsSession.getInt("customer_id");

            ResultSet rsSource;
            if (sourceAccountNumber != null)
            {
                rsSource = conn.executeQuery(
                    "SELECT a.ID, a.Balance FROM Accounts a " +
                    "JOIN AccountHolders h ON a.ID = h.AccountID " +
                    "WHERE a.AccountNumber = ? AND h.CustomerID = ?",
                    sourceAccountNumber, customerId
                );
            }
            else
            {
                rsSource = conn.executeQuery(
                    "SELECT a.ID, a.Balance FROM Accounts a " +
                    "JOIN AccountHolders h ON a.ID = h.AccountID " +
                    "WHERE h.CustomerID = ? AND h.Role = 'Primary'",
                    customerId
                );
            }

            if (rsSource == null || !rsSource.next()) 
            { 
                conn.execute("ROLLBACK"); 
                return false; 
            }

            int sourceAccountID = rsSource.getInt("ID");
            double sourceBalance = rsSource.getDouble("Balance");

            if (sourceBalance < amount)
            {
                conn.execute("ROLLBACK");
                return false;
            }

            ResultSet rsTarget = conn.executeQuery("SELECT ID, Balance FROM Accounts WHERE AccountNumber = ?", targetAccountNumber);
            if (rsTarget == null || !rsTarget.next()) 
            { 
                conn.execute("ROLLBACK"); 
                return false; 
            }

            int targetAccountID = rsTarget.getInt("ID");
            double targetBalance = rsTarget.getDouble("Balance");

            conn.execute("UPDATE Accounts SET Balance = ? WHERE ID = ?", sourceBalance - amount, sourceAccountID);
            conn.execute("UPDATE Accounts SET Balance = ? WHERE ID = ?", targetBalance + amount, targetAccountID);

            conn.execute("INSERT INTO Transactions (AccountID, CustomerID, Type, Amount, Status) VALUES (?, ?, 'Transfer', ?, 'Success')",
                        sourceAccountID, customerId, amount);

            conn.execute("COMMIT");
            return true;
        }
        catch (Exception e)
        {
            try { conn.execute("ROLLBACK"); } catch (Exception ex) {}
            e.printStackTrace();
            return false;
        }
    }
}
