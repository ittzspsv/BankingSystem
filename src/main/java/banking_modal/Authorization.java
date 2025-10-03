package banking_modal;

import database_modal.TableConnector;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

class CreateCustomerAccount 
{
    private int customerID;
    private String name;
    private String passwordHash;
    private String gender;
    private String dob;
    private String email;
    private String phoneNumber;

    public CreateCustomerAccount(String name, String password, String gender, String dob, String email, String phoneNumber) 
    {
        this.customerID = generateCustomerID();
        this.name = name;
        this.passwordHash = hashPassword(password);
        this.gender = gender;
        this.dob = dob;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    private int generateCustomerID() 
    {
        Random random = new Random();
        return random.nextInt(999999999);
    }

    private String hashPassword(String plainPassword) 
    {
        try 
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(plainPassword.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) 
            {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } 
        catch (NoSuchAlgorithmException e) 
        {
            throw new RuntimeException(e);
        }
    }

    public boolean PushToDatabase() 
    {
        TableConnector conn = TableConnector.getInstance();
        try 
        {
            String sql = "INSERT INTO Customers " +
                         "(ID, Name, Password, DateOfBirth, Email, PhoneNumber, Gender, CreatedAt) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP);";

            boolean ok = conn.execute(
                sql,
                customerID,
                name,
                passwordHash,
                dob,
                email,
                phoneNumber,
                gender
            );

            if (ok) conn.commit();
            return ok;
        } 
        catch (Exception e) 
        {
            conn.rollback();
            e.printStackTrace();
            return false;
        }
    }
}

public class Authorization 
{
    public String LoginAccount(String AccountId, String password) 
    {
        TableConnector conn = TableConnector.getInstance();
        try 
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) 
            {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            String hashedPassword = hexString.toString();

            ResultSet rs = conn.executeQuery("SELECT ID, Password FROM Customers WHERE ID = ?", AccountId);

            if (rs != null && rs.next()) 
            {
                String storedHash = rs.getString("Password");
                String customerID = rs.getString("ID");

                if (storedHash.equals(hashedPassword)) 
                {
                    String sessionId = UUID.randomUUID().toString();
                    Timestamp expireAt = Timestamp.from(Instant.now().plusSeconds(60 * 60 * 24));

                    conn.execute("INSERT INTO Sessions (session_id, customer_id, expire_at) VALUES (?, ?, ?)", 
                                 sessionId, customerID, expireAt);

                    conn.commit();
                    return sessionId;
                } 
                else 
                {
                    return "0";
                }
            } 
            else 
            {
                return "0";
            }
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            conn.rollback();
            return "0";
        }
    }

    public String CreateCustomerAccount(String name,String dob,String email,String phoneNumber,String gender,String password) 
    {
        TableConnector conn = TableConnector.getInstance();
        try 
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            String hashedPassword = hexString.toString();

            conn.execute(
                "INSERT INTO Customers (Name, DateOfBirth, Email, PhoneNumber, Gender, Password) VALUES (?, ?, ?, ?, ?, ?)",
                name, dob, email, phoneNumber, gender, hashedPassword
            );

            conn.commit();

            int customerId = conn.getLastInsertId();

            return String.valueOf(customerId);

        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            conn.rollback();
            return "0";
        }
    }

    public boolean Logout(String session_id)
    {
        TableConnector conn = TableConnector.getInstance();
        try 
        {
            boolean ok = conn.execute("UPDATE Sessions SET expire_at = CURRENT_TIMESTAMP WHERE session_id = ?", session_id);
            if (ok) conn.commit();
            return ok;
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            conn.rollback();
            return false;  
        }
    }

    public boolean VerifyCustomer(int customerID) 
    {
        TableConnector conn = TableConnector.getInstance();
        ResultSet rs = null;
        try 
        {
            rs = conn.executeQuery("SELECT ID FROM Customers WHERE ID = ?", customerID);
            return rs != null && rs.next();
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            return false;
        }
        finally 
        {
            try { if (rs != null) rs.close(); } catch (Exception ex) {}
        }
    }

    public int getCustomerId(String sessionId)
    {
        TableConnector conn = TableConnector.getInstance();
        ResultSet rs = null;
        try
        {
            rs = conn.executeQuery("SELECT customer_id FROM Sessions WHERE session_id = ?", sessionId);
            if(rs != null && rs.next())
            {
                return rs.getInt("customer_id");
            }
        }
        catch(Exception e) 
        {
            e.printStackTrace();
        }
        finally
        {
            try { if (rs != null) rs.close(); } catch (Exception ex) {}
        }
        return 0; 
    }
}
