package banking_modal;

import java.util.Scanner;
import database_modal.TableConnector;

public class Service
{
    private static Scanner scanner = new Scanner(System.in);
    private static Authorization auth = new Authorization();
    private static Account account = new Account();

    public static void main(String[] args)
    {
        TableConnector conn = TableConnector.getInstance();

        try
        {
            while (true)
            {
                System.out.println("==================================");
                System.out.println("        Welcome to MyBank         ");
                System.out.println("==================================");
                System.out.println("1. Login");
                System.out.println("2. Sign Up");
                System.out.println("3. Exit");
                System.out.print("Select an option: ");
                int choice = Integer.parseInt(scanner.nextLine());

                switch (choice)
                {
                    case 1:
                        if (loginFlow())
                        {
                            accountMenu();
                        }
                        break;
                    case 2:
                        signUpFlow();
                        break;
                    case 3:
                        System.out.println("Thank you for visiting MyBank!");
                        return;
                    default:
                        System.out.println("Invalid choice! Try again.");
                }
            }
        }
        finally
        {
            conn.close();
        }
    }

    private static void signUpFlow()
    {
        System.out.println();
        System.out.println("==================================");
        System.out.println("           SIGN UP MENU           ");
        System.out.println("==================================");

        System.out.print("Enter Name: ");
        String name = scanner.nextLine();
        System.out.print("Enter Date of Birth (YYYY-MM-DD): ");
        String dob = scanner.nextLine();
        System.out.print("Enter Email: ");
        String email = scanner.nextLine();
        System.out.print("Enter Phone Number: ");
        String phone = scanner.nextLine();
        System.out.print("Enter Gender: ");
        String gender = scanner.nextLine();
        System.out.print("Enter Password: ");
        String password = scanner.nextLine();

        String customerId = auth.CreateCustomerAccount(name, dob, email, phone, gender, password);

        if (!customerId.equals("0"))
        {
            System.out.println("Account created successfully! Customer ID: " + customerId);

            // Automatically create a Primary account
            BuildAccountDetails builder = new BuildAccountDetails(Integer.parseInt(customerId));
            account.CreateAccount(builder, "Primary");
            System.out.println("Primary bank account created successfully!");
        }
        else
        {
            System.out.println("Account creation failed. Please try again.");
        }
    }

    private static boolean loginFlow()
    {
        System.out.println();
        System.out.println("==================================");
        System.out.println("           LOGIN MENU             ");
        System.out.println("==================================");
        System.out.print("Enter Account ID: ");
        String accountId = scanner.nextLine();
        System.out.print("Enter Password: ");
        String password = scanner.nextLine();

        String session = auth.LoginAccount(accountId, password);
        if (session.equals("0"))
        {
            System.out.println("Login failed! Invalid credentials.");
            return false;
        }

        SessionHolder.sessionId = session;
        SessionHolder.customerId = auth.getCustomerId(session);

        System.out.println("Login successful! Customer ID: " + SessionHolder.customerId);
        return true;
    }

    private static void accountMenu()
    {
        while (true)
        {
            System.out.println("==================================");
            System.out.println("        Account Menu              ");
            System.out.println("==================================");
            System.out.println();
            System.out.println("1. Create New Account");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. Logout");
            System.out.print("Select an option: ");
            int choice = Integer.parseInt(scanner.nextLine());

            switch (choice)
            {
                case 1:
                    createAccountFlow();
                    break;
                case 2:
                    depositFlow();
                    break;
                case 3:
                    withdrawFlow();
                    break;
                case 4:
                    transferFlow();
                    break;
                case 5:
                    auth.Logout(SessionHolder.sessionId);
                    System.out.println("Logged out successfully.");
                    return;
                default:
                    System.out.println("Invalid choice! Try again.");
            }
        }
    }

    private static void createAccountFlow()
    {
        BuildAccountDetails builder = new BuildAccountDetails(SessionHolder.customerId);
        account.CreateAccount(builder, "Primary");
        System.out.println("Primary account created successfully.");
    }

    private static void depositFlow()
    {
        System.out.print("Enter amount to deposit: ");
        double amount = Double.parseDouble(scanner.nextLine());
        boolean success = account.Deposit(SessionHolder.sessionId, amount);
        if (success)
            System.out.println("Deposit successful! Amount: " + amount);
        else
            System.out.println("Deposit failed!");
    }

    private static void withdrawFlow()
    {
        System.out.print("Enter amount to withdraw: ");
        double amount = Double.parseDouble(scanner.nextLine());
        boolean success = account.Withdraw(SessionHolder.sessionId, amount);
        if (success)
            System.out.println("Withdrawal successful! Amount: " + amount);
        else
            System.out.println("Insufficient balance or error!");
    }

    private static void transferFlow()
    {
        System.out.print("Enter target account number: ");
        long targetAccount = Long.parseLong(scanner.nextLine());
        System.out.print("Enter amount to transfer: ");
        double amount = Double.parseDouble(scanner.nextLine());

        boolean success = account.TransferMoney(SessionHolder.sessionId, null, targetAccount, amount);
        if (success)
            System.out.println("Transfer successful! Amount: " + amount);
        else
            System.out.println("Transfer failed! Check balance or account number.");
    }

    private static class SessionHolder
    {
        static String sessionId;
        static int customerId;
    }
}
