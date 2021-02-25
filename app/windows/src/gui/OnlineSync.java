/*

	OnlineSync.java
	-----------------

	This is a Basic Class to Test the Functionality of the Online Datbase

	It Will Initially Just Have Read / Write / Edit Capabilities

	It Will Be Expanded to Sync With the Offline Database

	*** USE THE COMMAND BELOW TO RUN THE CLASS WHEN COMPILED ***

	java -cp ../lib/mysql-connector-java-8.0.19.jar;. OnlineSync

	*** USE THE COMMAND ABOVE TO RUN THE CLASS WHEN COMPILED ***

*/

package gui;

//Import Statements For JDBC and Etc.

import java.io.IOException;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.util.Random;
import javax.mail.*;
import javax.mail.internet.*;

public class OnlineSync
{

	/*
	 * password recovery system
	 */

	//sends an email for password recovery to a given email
	public static void sendMail(Connection studConnect, String recipient) throws Exception
	{

		Properties properties = new Properties();
		System.out.println("preparing to send mail");

		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.starttls.enable", "true");
		properties.put("mail.smtp.host", "smtp.gmail.com");
		properties.put("mail.mime.address.strict","false");
		properties.put("mail.smtp.port","587");

		String myAccountEmail = "studioruumreset@gmail.com";
		String password = "zitozito";

		Session session = Session.getInstance(properties, new Authenticator()
		{

			@Override
			protected PasswordAuthentication getPasswordAuthentication()
			{

				return new PasswordAuthentication(myAccountEmail ,password);

			}

		});

		Random rand = new Random();
		int code = rand.nextInt(100000);
		String otp=String.format("%05d",code);

		Message message = prepareMessage(session, myAccountEmail, recipient, otp);


		Transport.send(message);
		LocalDateTime time = LocalDateTime.now();
		addToResetTable(studConnect,recipient,otp,time);
		System.out.println("Message sent successfully");

	}

	//prepares the password recovery email
	private static Message prepareMessage(Session session, String myAccountEmail, String recipient, String otp)
	{

		String forgot="password";
		try
		{

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(myAccountEmail));
			InternetAddress address = new InternetAddress(recipient, false);
			message.setRecipient(Message.RecipientType.TO, address);

			if(forgot.equals("password"))
			{

				message.setSubject("Studioruum Password Reset");
				message.setText("Your OTP for reset is "+otp);
				//add to database(Connection studConnect, email, random_num, current_time)

			}

			else if(forgot.equals("username"))
			{

				message.setSubject("Studioruum Account Linked to this email");
				message.setText("The username for the account linked to this email is");

			}

			return message;

		}

		catch(Exception ex)
		{

			System.out.println(ex);

		}

		return null;

	}

	//adds the details of the password recovery to the reset_password table
	private static void addToResetTable(Connection studConnect, String email, String otp, LocalDateTime currentTime)throws SQLException
	{

		PreparedStatement addToReset = null;

		try
		{

			addToReset = studConnect.prepareStatement("INSERT INTO reset_password VALUES(?,?,?);");
			addToReset.setString(1, email);
			addToReset.setString(2, otp);
			currentTime.plusHours(2);
			Timestamp expiry = Timestamp.valueOf(currentTime);
			addToReset.setTimestamp(3,expiry);
			addToReset.executeUpdate();
			addToReset.close();

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting to Online DB: " + ex);

		}
		finally
		{

			try
			{

				addToReset.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

				addToReset = null;


			}

		}

	}

	public Boolean email_exist(Connection studConnect, String email) throws SQLException
	{

		PreparedStatement statement = studConnect.prepareStatement("SELECT email FROM users WHERE email=?;");

		statement.setString(1,email);
		ResultSet rs = statement.executeQuery();
		rs.next();
		String value=rs.getString(1);
		rs.close();
		statement.close();

		if(value.equals(email))
		{

			statement.close();
			return true;

		}

		else
		{

			statement.close();
			return false;

		}

	}

	public String find_email(Connection studConnect, String username) throws SQLException
	{

		PreparedStatement statement = studConnect.prepareStatement("SELECT email FROM users WHERE username=?;");
		statement.setString(1,username);
		ResultSet rs = statement.executeQuery();
		rs.next();

		String value=rs.getString(1);
		rs.close();
		statement.close();
		return value;

	}

	public Boolean username_exist(Connection studConnect, String username) throws SQLException
	{

		PreparedStatement statement;
		ResultSet rs;
		String value=null;

		if(username.contains("@"))
		{

			statement = studConnect.prepareStatement("SELECT email FROM users WHERE email=?;");
			statement.setString(1, username);
			rs = statement.executeQuery();

			if(rs.next())
			{

				value = rs.getString(1);

			}

			rs.close();
			statement.close();

			if (username.equals(value))
			{

				return true;

			}

			else
			{

				return false;

			}

		}

		else
		{

			statement = studConnect.prepareStatement("SELECT username FROM users WHERE username=?;");
			statement.setString(1, username);
			rs = statement.executeQuery();

			if(rs.next())
			{

				value = rs.getString(1);

			}

			rs.close();
			statement.close();

			if (username.equals(value))
			{

				return true;

			}

			else
			{

				return false;

			}

		}

	}

	public void update_password(Connection studConnect, String email, String password, byte[] salt)throws SQLException
	{

		PreparedStatement statement = studConnect.prepareStatement("UPDATE users SET password=?, salt=? WHERE email=?");
		statement.setString(1, password);
		statement.setBytes(2, salt);
		statement.setString(3, email);
		statement.executeUpdate();
		statement.close();

	}

	//deletes the one time passcode record from the database
	public void deleteOTP(Connection studConnect, String email, Boolean used)throws SQLException
	{

		PreparedStatement getExpiry = studConnect.prepareStatement("SELECT expiry FROM reset_password WHERE email=?;");
		getExpiry.setString(1,email);
		ResultSet rs = getExpiry.executeQuery();
		Timestamp time=null;

		if(rs.next())
		{

			time = rs.getTimestamp(1);

		}

		LocalDateTime expiry=time.toLocalDateTime();
		if(LocalDateTime.now().isAfter(expiry))
		{

			PreparedStatement statement = studConnect.prepareStatement("DELETE FROM reset_password WHERE email=?");
			statement.setString(1, email);

		}
		else if(used)
		{

			PreparedStatement statement = studConnect.prepareStatement("DELETE FROM reset_password WHERE email=?");
			statement.setString(1, email);

		}

	}

	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

	//Acts as the Main Method of the Program
	public Connection Connect()
	{

		//Establishes a Connection
		Connection studConnect = null;

		//The Format of the Host Name is the JDCB Specifier, Then the Address to Connect, Before the Database Name
		String host = "jdbc:mysql://studioruum.c5iijqup9ms0.us-east-1.rds.amazonaws.com/studioruumOnline";

		//Default Master Username and Password From AWS
		String user = "group40";
		String password = "zitozito";

		//Attempting to Connect
		try
		{

			studConnect = DriverManager.getConnection(host, user, password);

			if (studConnect != null)
			{

				System.out.println("Connected to " + host + ".");

			}

		}

		catch (SQLException ex)
		{

			System.out.println("An Error Occured When Connecting to the Database.");
			ex.printStackTrace();

		}

		return studConnect;

	}

	//Close The Connection When Finished
	public void Disconnect(Connection studConnect)
	{

		if (studConnect != null)
		{

			try
			{

				studConnect.close();

			}

			catch (SQLException ex)
			{

				ex.printStackTrace();

			}

		}

	}

	//////////////////////////
	/*



		PASSWORDS



	*/
	//////////////////////////

	public static String bytesToStringHex(byte[] bytes)
	{

		char[] hexChars = new char[bytes.length * 2];

		for (int j = 0; j < bytes.length; j++)
		{

			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];

		}

		return new String(hexChars);

	}

	public byte[] generateSalt()
	{

		byte[] salt=new byte[32];
		SecureRandom rand = new SecureRandom();
		rand.nextBytes(salt);
		return salt;

	}

	public byte[] getSalt(Connection studConnect, String username)
	{

		PreparedStatement getSalt;
		byte[] salt=null;

		try
		{

			getSalt = studConnect.prepareStatement("SELECT salt FROM users WHERE username=?;");
			getSalt.setString(1, username);
			ResultSet rs = getSalt.executeQuery();
			rs.next();
			Blob blob = rs.getBlob(1);
			salt = blob.getBytes(1,(int)blob.length());
			getSalt.close();
			rs.close();

		}
		catch(SQLException ex)
		{

			System.out.println(ex);

		}

		return salt;

	}

	public String generateHash(byte[] salt, String password)throws NoSuchAlgorithmException
	{

		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.reset();
		digest.update(salt);
		byte[] hash = digest.digest(password.getBytes());
		String hashedPassword=bytesToStringHex(hash);
		return hashedPassword;

	}

	//////////////////////////
	/*



		USERS TABLE



	*/
	//////////////////////////

	public Boolean uploadUsers(Connection studConnect, String uname, String account, String pword, byte[] salt, String account_type)
	{

		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
		LocalDateTime now;
		System.out.println("Attempting to Add a Value to the Table ~users~.");

		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		try
		{

			now=LocalDateTime.now();
			String username = uname;
			String email = account;
			String password = pword;
			String time_created = dtf.format(now).substring(10);
			String date = dtf.format(now).substring(0,10);
			PreparedStatement stmt = studConnect.prepareStatement("SELECT COUNT(username) FROM users WHERE username = ?");
			stmt.setString(1, username);
			ResultSet rs = stmt.executeQuery();
			rs.next();
			int unique = rs.getInt(1);
			rs.close();
			stmt.close();

			if(unique>0)
			{

				System.out.println("Username already exists");
				return false;

			}

			else
			{

				//Creating a Prepared Statement and Placing the Table Value In
				statement = studConnect.prepareStatement("INSERT INTO users VALUES(?, ?, ?, ?, ?,?,?);");
				statement.setString(1, username);
				statement.setString(2, email);
				statement.setString(3, password);
				statement.setString(4, time_created);
				statement.setString(5, date);
				statement.setBytes(6, salt);
				statement.setString(7, account_type);

				statement.executeUpdate();
				statement.close();
				return true;

			}

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting: " + ex);
			return false;

		}

	}

	public void downloadUsers(Connection studConnect)
	{

		System.out.println("Attempting to Show All Values in the Table ~users~.");

		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		try
		{

			//Creating a Prepared Statement
			statement = studConnect.prepareStatement("SELECT * FROM users;");

			//Gather the Results of the Select
			ResultSet rs = statement.executeQuery();

			//Print Out Each Result
			while(rs.next())
			{

				String username = rs.getString("username");
				String email = rs.getString("email");
				String password = rs.getString("password");
				String time_created = rs.getString("time_created");
				String time = rs.getString("date");
				byte[] salt = rs.getBytes("date");

				System.out.println(username + "  /  " + email + "  /  " + password + "  /  " + time_created + "  /  " + time);

			}

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

			}

		}

	}

	//////////////////////////
	/*



		SCHOLARS TABLE



	*/
	//////////////////////////

	public void uploadScholars(Connection studConnect)
	{

		System.out.println("Attempting to Add a Value to the Table ~scholars~.");

		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		try
		{

			String username = "slgjon13";
			int scholar_id = 1;

			//Creating a Prepared Statement and Placing the Table Value In
			statement = studConnect.prepareStatement("INSERT INTO scholar VALUES(?, ?);");
			statement.setInt(1, scholar_id);
			statement.setString(2, username);

			statement.executeUpdate();

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

			}

		}

	}

	public void downloadScholars(Connection studConnect)
	{

		System.out.println("Attempting to Show All Values in the Table ~scholars~.");

		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		try
		{

			//Creating a Prepared Statement
			statement = studConnect.prepareStatement("SELECT * FROM users;");

			//Gather the Results of the Select
			ResultSet rs = statement.executeQuery();

			//Print Out Each Result
			while(rs.next())
			{

				int scholar_id = rs.getInt("scholar_id");
				String username = rs.getString("username");

				System.out.println(Integer.toString(scholar_id) + "  /  " + username);

			}

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

			}

		}

	}

	public Boolean login(Connection studConnect, String username, String password)
	{

		PreparedStatement statement = null;

		try
		{

			//Creating a Prepared Statement
			statement = studConnect.prepareStatement("SELECT password FROM users WHERE username = ?;");
			statement.setString(1, username);
			ResultSet rs = statement.executeQuery();
			rs.next();

			String realPass = rs.getString(1);
			rs.close();
			statement.close();

			if(realPass.equals(password))
			{

				return true;

			}

			else
			{

				return false;

			}

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting: " + ex);
			return false;

		}

	}

	//////////////////////////
	/*



		EDUCATORS TABLE



	*/
	//////////////////////////

	public void uploadEducators(Connection studConnect)
	{

		System.out.println("Attempting to Add a Value to the Table ~educators~.");

		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		try
		{

			String username = "slgjon13";
			int educator_id = 1;

			//Creating a Prepared Statement and Placing the Table Value In
			statement = studConnect.prepareStatement("INSERT INTO educator VALUES(?, ?);");
			statement.setInt(1, educator_id);
			statement.setString(2, username);

			statement.executeUpdate();

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

			}

		}

	}

	public void downloadEducators(Connection studConnect)
	{

		System.out.println("Attempting to Show All Values in the Table ~educators~.");

		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		try
		{

			//Creating a Prepared Statement
			statement = studConnect.prepareStatement("SELECT * FROM users;");

			//Gather the Results of the Select
			ResultSet rs = statement.executeQuery();

			//Print Out Each Result
			while(rs.next())
			{

				int educator_id = rs.getInt("educator_id");
				String username = rs.getString("username");

				System.out.println(Integer.toString(educator_id) + "  /  " + username);

			}

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

			}

		}

	}

	//////////////////////////
	/*



		CLASSRUUMS TABLE



	*/
	//////////////////////////

	public void uploadClassruum(Connection studConnect, String class_name, String class_description, String username) throws SQLException
	{

		System.out.println("Attempting to Add a Value to the Table ~classruums~.");

		//The Format of the Host Name is the JDCB Specifier, Then the Address to Connect, Before the Database Name
		String host = "jdbc:mysql://studioruum.c5iijqup9ms0.us-east-1.rds.amazonaws.com/studioruumOnline";

		//Default Master Username and Password From AWS
		String user = "group40";
		String password = "zitozito";

		PreparedStatement statement = null;

		Connection connection = DriverManager.getConnection(host, user, password);

		try
		{

			// get the educators id
			statement = studConnect.prepareStatement("SELECT username FROM users WHERE username = ?");
			statement.setString(1, username);
			ResultSet rs = statement.executeQuery();

			if(rs.next())
			{

				String educator = rs.getString(1);

				//classid auto increments anyway

				//Creating a Prepared Statement and Placing the Table Value In
				statement = studConnect.prepareStatement("INSERT INTO classruums(educator_username, class_name, class_description) VALUES(?, ?, ?);");
				statement.setString(1, educator);
				statement.setString(2, class_name);
				statement.setString(3, class_description);
				statement.executeUpdate();

			}

			else
			{

				System.out.println("User does not exist");

			}

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

			}

		}

	}

	public void downloadClassruums(Connection studConnect)
	{

		System.out.println("Attempting to Show All Values in the Table ~classruums~.");

		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		try
		{

			//Creating a Prepared Statement
			statement = studConnect.prepareStatement("SELECT * FROM classruums;");

			//Gather the Results of the Select
			ResultSet rs = statement.executeQuery();

			//Print Out Each Result
			while(rs.next())
			{

				int class_id = rs.getInt("class_id");
				int educator_id = rs.getInt("educator_id");
				String class_name = rs.getString("class_name");

				System.out.println(Integer.toString(class_id) + Integer.toString(educator_id) + class_name);

			}

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

				statement = null;


			}

		}

	}

	public void inviteClassruum(Connection studConnect, String username, int class_id)throws SQLException
	{

		PreparedStatement statement = studConnect.prepareStatement("INSERT INTO class_member VALUES(?,?);");
		statement.setString(1,username);
		statement.setInt(2,class_id);
		statement.executeUpdate();

	}

	public void uploadClassruumScholars(Connection studConnect)
	{
		System.out.println("Attempting to Add a Value to the Table ~classruum_scholars~.");

		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		try
		{

			int class_id = 1;
			int scholar_id = 1;

			//Creating a Prepared Statement and Placing the Table Value In
			statement = studConnect.prepareStatement("INSERT INTO classruum_scholars VALUES(?, ?);");
			statement.setInt(1, class_id);
			statement.setInt(2, scholar_id);

			statement.executeUpdate();

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

			}

		}

	}

	public void updateResourceOwners(Connection studConnect, int resource_ID, String username) throws SQLException, IOException
	{

		//Declaring the Statement to Be Used
		PreparedStatement statement;
		PreparedStatement statement2;
		PreparedStatement statement3;
		String scholar_Username;
		int class_id=0;

		try
		{

			System.out.println("TEST");
			statement = studConnect.prepareStatement("SELECT class_id FROM classruums WHERE educator_username = ?;");
			statement.setString(1, username);
			ResultSet rs = statement.executeQuery();

			if(rs.next())
			{

				class_id = rs.getInt("class_ID");

			}

			rs.close();
			statement.close();
			// retrieve the educators class id ID

			// retrieve all the scholars in the classruum
			statement2 = studConnect.prepareStatement("SELECT member_name FROM class_member WHERE class_id = ?");
			statement2.setInt(1, class_id);
			ResultSet rs2 = statement2.executeQuery();

			// add each scholar in the classruum as a resource owner
			while(rs2.next())
			{

				scholar_Username = rs2.getString(1);
				statement3 = studConnect.prepareStatement("INSERT INTO resource_owner VALUES(?, ?);");
				statement3.setInt(1, resource_ID);
				statement3.setString(2, scholar_Username);
				statement3.executeUpdate();

			}

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting: " + ex);

		}

	}

	public void downloadClassruumScholars(Connection studConnect)
	{

		System.out.println("Attempting to Show All Values in the Table ~classruums_scholars~.");

		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		try
		{

			//Creating a Prepared Statement
			statement = studConnect.prepareStatement("SELECT * FROM classruums_scholars;");

			//Gather the Results of the Select
			ResultSet rs = statement.executeQuery();

			//Print Out Each Result
			while(rs.next())
			{

				int class_id = rs.getInt("class_id");
				int scholar_id = rs.getInt("educator_id");

				System.out.println(Integer.toString(class_id) + Integer.toString(scholar_id));

			}

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

				statement = null;


			}

		}

	}

	public void downloadClassResources(Connection studConnect, String currentUser, int educators_id) throws SQLException, IOException
	{

		try
		{

			PreparedStatement statement = studConnect.prepareStatement("SELECT resource_id FROM resource_owner WHERE resource_owner = ?");
			statement.setInt(1, educators_id);

			// get the resource's which have the educators id
			ResultSet rs = statement.executeQuery();

			//
			while(rs.next())
			{

				int resource_id = rs.getInt("resource_id");
				PreparedStatement statement2 = studConnect.prepareStatement("INSERT INTO resource_owner VALUES(?, ?);");
				statement2.setInt(1, resource_id);
				//statement2.setInt(2, username);

			}

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting: " + ex);

		}


	}

	//////////////////////////
	/*



		FORUUMS TABLE



	*/
	//////////////////////////

	public void uploadForuums(Integer forum_id, Integer class_id, String forum_title) throws SQLException
	{

		System.out.println("Attempting to Add a Value to the Table ~foruums~.");

		//The Format of the Host Name is the JDCB Specifier, Then the Address to Connect, Before the Database Name
		String host = "jdbc:mysql://studioruum.c5iijqup9ms0.us-east-1.rds.amazonaws.com/studioruumOnline";

		//Default Master Username and Password From AWS
		String user = "group40";
		String password = "zitozito";

		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		Connection connection = DriverManager.getConnection(host, user, password);

		try
		{

			//Creating a Prepared Statement and Placing the Table Value In
			statement = connection.prepareStatement("INSERT INTO classruum_scholars VALUES(?, ?, ?);");
			statement.setInt(1, forum_id);
			statement.setInt(2, class_id);
			statement.setString(3, forum_title);

			statement.executeUpdate();

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

			}

		}

	}

	public void downloadForuum() throws SQLException 
	{

		System.out.println("Attempting to Show All Values in the Table ~foruums~.");

		//The Format of the Host Name is the JDCB Specifier, Then the Address to Connect, Before the Database Name
		String host = "jdbc:mysql://studioruum.c5iijqup9ms0.us-east-1.rds.amazonaws.com/studioruumOnline";

		//Default Master Username and Password From AWS
		String user = "group40";
		String password = "zitozito";


		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		Connection connection = DriverManager.getConnection(host, user, password);


		try
		{

			//Creating a Prepared Statement
			statement = connection.prepareStatement("SELECT * FROM foruums;");
			//Gather the Results of the Select
			ResultSet rs = statement.executeQuery();

			//Print Out Each Result
			while (rs.next())
			{

				int forum_id = rs.getInt("forum_id");
				int class_id = rs.getInt("class_id");
				String forum_title = rs.getString("forum_title");

				//System.out.println("Foruums downloaded");

			}


		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

				statement = null;

			}

		}

	}

	//////////////////////////
	/*



		FORUUMS TABLE



	*/
	//////////////////////////

	public void uploadComment(Integer forum_id, String comment_content, String username, String time_updated) throws SQLException
	{

		System.out.println("Attempting to Add a Value to the Table ~comments~.");

		//The Format of the Host Name is the JDCB Specifier, Then the Address to Connect, Before the Database Name
		String host = "jdbc:mysql://studioruum.c5iijqup9ms0.us-east-1.rds.amazonaws.com/studioruumOnline";

		//Default Master Username and Password From AWS
		String user = "group40";
		String password = "zitozito";

		int comment_id = 0;

		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		Connection connection = DriverManager.getConnection(host, user, password);

		try
		{

			//Creating a Prepared Statement and Placing the Table Value In
			statement = connection.prepareStatement("INSERT INTO comments VALUES(?, ?, ?, ?, ?);");
			statement.setInt(1, comment_id);
			statement.setInt(2, forum_id);
			statement.setString(3, comment_content);
			statement.setString(4, username);
			statement.setString(5, time_updated);

			statement.executeUpdate();

		}
		catch (SQLException ex)
		{

			System.out.println("Error Connecting to Online DB: " + ex);

		}
		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

				statement = null;


			}

		}

	}

	public void downloadComment() throws SQLException
	{

		System.out.println("Attempting to Show All Values in the Table ~comments~.");

		//The Format of the Host Name is the JDCB Specifier, Then the Address to Connect, Before the Database Name
		String host = "jdbc:mysql://studioruum.c5iijqup9ms0.us-east-1.rds.amazonaws.com/studioruumOnline";

		//Default Master Username and Password From AWS
		String user = "group40";
		String password = "zitozito";


		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		Connection connection = DriverManager.getConnection(host, user, password);

		try
		{

			//Creating a Prepared Statement
			statement = connection.prepareStatement("SELECT * FROM comments;");

			//Gather the Results of the Select
			ResultSet rs = statement.executeQuery();

			//Print Out Each Result
			while(rs.next())
			{

				int comment_id = rs.getInt("comment_id");
				int forum_id = rs.getInt("forum_id");
				String comment_content = rs.getString("comment_content");
				String username = rs.getString("username");
				String time_updated = rs.getString("time_updated");

				//System.out.println("Comments downloaded");

			}

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting to Online DB: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

				statement = null;


			}

		}

	}

	// These functions are me downloading each forum_ids questions. I couldn't think of another way.

	public List downloadCommentsForum1() throws SQLException
	{

		List<String> commentForum1List = new ArrayList<>();

		System.out.println("Attempting to Show All Values in the Table ~comments~.");

		//The Format of the Host Name is the JDCB Specifier, Then the Address to Connect, Before the Database Name
		String host = "jdbc:mysql://studioruum.c5iijqup9ms0.us-east-1.rds.amazonaws.com/studioruumOnline";

		//Default Master Username and Password From AWS
		String user = "group40";
		String password = "zitozito";


		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		Connection connection = DriverManager.getConnection(host, user, password);

		try
		{

			//Creating a Prepared Statement
			statement = connection.prepareStatement("SELECT * FROM comments WHERE forum_id = 1;");

			//Gather the Results of the Select
			ResultSet rs = statement.executeQuery();

			//Print Out Each Result
			while(rs.next())
			{

				String comment_content = rs.getString("comment_content");
				String username = rs.getString("username");

				String cmt = new String(comment_content);
				String uname = new String(username);
				commentForum1List.add(cmt + ", username: " + uname);

			}

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting to Online DB: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

				statement = null;


			}

		}

		return commentForum1List;

	}

	public List downloadCommentsForum2() throws SQLException
	{

		List<String> commentForum2List = new ArrayList<>();

		System.out.println("Attempting to Show All Values in the Table ~comments~.");

		//The Format of the Host Name is the JDCB Specifier, Then the Address to Connect, Before the Database Name
		String host = "jdbc:mysql://studioruum.c5iijqup9ms0.us-east-1.rds.amazonaws.com/studioruumOnline";

		//Default Master Username and Password From AWS
		String user = "group40";
		String password = "zitozito";


		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		Connection connection = DriverManager.getConnection(host, user, password);

		try
		{

			//Creating a Prepared Statement
			statement = connection.prepareStatement("SELECT * FROM comments WHERE forum_id = 2;");

			//Gather the Results of the Select
			ResultSet rs = statement.executeQuery();

			//Print Out Each Result
			while(rs.next())
			{
				String comment_content = rs.getString("comment_content");
				String username = rs.getString("username");


				String cmt = new String(comment_content);
				String uname = new String(username);
				commentForum2List.add(cmt + ", username: " + uname);
			}

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting to Online DB: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

				statement = null;


			}

		}


		return commentForum2List;
	}

	public List downloadCommentsForum3() throws SQLException
	{

		List<String> commentForum3List = new ArrayList<>();

		System.out.println("Attempting to Show All Values in the Table ~comments~.");

		//The Format of the Host Name is the JDCB Specifier, Then the Address to Connect, Before the Database Name
		String host = "jdbc:mysql://studioruum.c5iijqup9ms0.us-east-1.rds.amazonaws.com/studioruumOnline";

		//Default Master Username and Password From AWS
		String user = "group40";
		String password = "zitozito";


		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		Connection connection = DriverManager.getConnection(host, user, password);

		try
		{

			//Creating a Prepared Statement
			statement = connection.prepareStatement("SELECT * FROM comments WHERE forum_id = 3;");

			//Gather the Results of the Select
			ResultSet rs = statement.executeQuery();

			//Print Out Each Result
			while(rs.next())
			{
				String comment_content = rs.getString("comment_content");
				String username = rs.getString("username");


				String cmt = new String(comment_content);
				String uname = new String(username);
				commentForum3List.add(cmt + ", username: " + uname);
			}

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting to Online DB: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

				statement = null;


			}

		}

		return commentForum3List;

	}

	public List downloadCommentsForum4() throws SQLException
	{

		List<String> commentForum4List = new ArrayList<>();

		System.out.println("Attempting to Show All Values in the Table ~comments~.");

		//The Format of the Host Name is the JDCB Specifier, Then the Address to Connect, Before the Database Name
		String host = "jdbc:mysql://studioruum.c5iijqup9ms0.us-east-1.rds.amazonaws.com/studioruumOnline";

		//Default Master Username and Password From AWS
		String user = "group40";
		String password = "zitozito";


		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		Connection connection = DriverManager.getConnection(host, user, password);

		try
		{

			//Creating a Prepared Statement
			statement = connection.prepareStatement("SELECT * FROM comments WHERE forum_id = 4;");

			//Gather the Results of the Select
			ResultSet rs = statement.executeQuery();

			//Print Out Each Result
			while(rs.next())
			{

				String comment_content = rs.getString("comment_content");
				String username = rs.getString("username");


				String cmt = new String(comment_content);
				String uname = new String(username);
				commentForum4List.add(cmt + ", username: " + uname);

			}

		}
		catch (SQLException ex)
		{

			System.out.println("Error Connecting to Online DB: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

				statement = null;


			}

		}

		return commentForum4List;
	}

	public List downloadCommentsForum5() throws SQLException
	{

		List<String> commentForum5List = new ArrayList<>();

		System.out.println("Attempting to Show All Values in the Table ~comments~.");

		//The Format of the Host Name is the JDCB Specifier, Then the Address to Connect, Before the Database Name
		String host = "jdbc:mysql://studioruum.c5iijqup9ms0.us-east-1.rds.amazonaws.com/studioruumOnline";

		//Default Master Username and Password From AWS
		String user = "group40";
		String password = "zitozito";


		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		Connection connection = DriverManager.getConnection(host, user, password);

		try
		{

			//Creating a Prepared Statement
			statement = connection.prepareStatement("SELECT * FROM comments WHERE forum_id = 5;");

			//Gather the Results of the Select
			ResultSet rs = statement.executeQuery();

			//Print Out Each Result
			while(rs.next())
			{
				String comment_content = rs.getString("comment_content");
				String username = rs.getString("username");


				String cmt = new String(comment_content);
				String uname = new String(username);
				commentForum5List.add(cmt + ", username: " + uname);

			}

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting to Online DB: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

				statement = null;

			}

		}

		return commentForum5List;
	}

	public List downloadCommentsForum6() throws SQLException
	{

		List<String> commentForum6List = new ArrayList<>();

		System.out.println("Attempting to Show All Values in the Table ~comments~.");

		//The Format of the Host Name is the JDCB Specifier, Then the Address to Connect, Before the Database Name
		String host = "jdbc:mysql://studioruum.c5iijqup9ms0.us-east-1.rds.amazonaws.com/studioruumOnline";

		//Default Master Username and Password From AWS
		String user = "group40";
		String password = "zitozito";


		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		Connection connection = DriverManager.getConnection(host, user, password);

		try
		{

			//Creating a Prepared Statement
			statement = connection.prepareStatement("SELECT * FROM comments WHERE forum_id = 6;");

			//Gather the Results of the Select
			ResultSet rs = statement.executeQuery();

			//Print Out Each Result
			while(rs.next())
			{

				String comment_content = rs.getString("comment_content");
				String username = rs.getString("username");


				String cmt = new String(comment_content);
				String uname = new String(username);
				commentForum6List.add(cmt + ", username: " + uname);

			}

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting to Online DB: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

				statement = null;


			}

		}

		return commentForum6List;
	}

	public List downloadCommentsForum7() throws SQLException
	{

		List<String> commentForum7List = new ArrayList<>();

		System.out.println("Attempting to Show All Values in the Table ~comments~.");

		//The Format of the Host Name is the JDCB Specifier, Then the Address to Connect, Before the Database Name
		String host = "jdbc:mysql://studioruum.c5iijqup9ms0.us-east-1.rds.amazonaws.com/studioruumOnline";

		//Default Master Username and Password From AWS
		String user = "group40";
		String password = "zitozito";


		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		Connection connection = DriverManager.getConnection(host, user, password);

		try
		{

			//Creating a Prepared Statement
			statement = connection.prepareStatement("SELECT * FROM comments WHERE forum_id = 7;");

			//Gather the Results of the Select
			ResultSet rs = statement.executeQuery();

			//Print Out Each Result
			while(rs.next())
			{

				String comment_content = rs.getString("comment_content");
				String username = rs.getString("username");


				String cmt = new String(comment_content);
				String uname = new String(username);
				commentForum7List.add(cmt + ", username: " + uname);

			}

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting to Online DB: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

				statement = null;


			}

		}

		return commentForum7List;
	}

	public List downloadCommentsForum8() throws SQLException
	{

		List<String> commentForum8List = new ArrayList<>();

		System.out.println("Attempting to Show All Values in the Table ~comments~.");

		//The Format of the Host Name is the JDCB Specifier, Then the Address to Connect, Before the Database Name
		String host = "jdbc:mysql://studioruum.c5iijqup9ms0.us-east-1.rds.amazonaws.com/studioruumOnline";

		//Default Master Username and Password From AWS
		String user = "group40";
		String password = "zitozito";


		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		Connection connection = DriverManager.getConnection(host, user, password);

		try
		{

			//Creating a Prepared Statement
			statement = connection.prepareStatement("SELECT * FROM comments WHERE forum_id = 8;");

			//Gather the Results of the Select
			ResultSet rs = statement.executeQuery();

			//Print Out Each Result
			while(rs.next())
			{

				String comment_content = rs.getString("comment_content");
				String username = rs.getString("username");


				String cmt = new String(comment_content);
				String uname = new String(username);
				commentForum8List.add(cmt + ", username: " + uname);

			}

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting to Online DB: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

				statement = null;

			}

		}

		return commentForum8List;
	}

	public List downloadCommentsForum9() throws SQLException
	{

		List<String> commentForum9List = new ArrayList<>();

		System.out.println("Attempting to Show All Values in the Table ~comments~.");

		//The Format of the Host Name is the JDCB Specifier, Then the Address to Connect, Before the Database Name
		String host = "jdbc:mysql://studioruum.c5iijqup9ms0.us-east-1.rds.amazonaws.com/studioruumOnline";

		//Default Master Username and Password From AWS
		String user = "group40";
		String password = "zitozito";


		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		Connection connection = DriverManager.getConnection(host, user, password);

		try
		{

			//Creating a Prepared Statement
			statement = connection.prepareStatement("SELECT * FROM comments WHERE forum_id = 9;");

			//Gather the Results of the Select
			ResultSet rs = statement.executeQuery();

			//Print Out Each Result
			while(rs.next())
			{

				String comment_content = rs.getString("comment_content");
				String username = rs.getString("username");


				String cmt = new String(comment_content);
				String uname = new String(username);
				commentForum9List.add(cmt + ", username: " + uname);

			}

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting to Online DB: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

				statement = null;


			}

		}

		return commentForum9List;

	}

	public List downloadCommentsForum10() throws SQLException
	{

		List<String> commentForum10List = new ArrayList<>();

		System.out.println("Attempting to Show All Values in the Table ~comments~.");

		//The Format of the Host Name is the JDCB Specifier, Then the Address to Connect, Before the Database Name
		String host = "jdbc:mysql://studioruum.c5iijqup9ms0.us-east-1.rds.amazonaws.com/studioruumOnline";

		//Default Master Username and Password From AWS
		String user = "group40";
		String password = "zitozito";


		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		Connection connection = DriverManager.getConnection(host, user, password);

		try
		{

			//Creating a Prepared Statement
			statement = connection.prepareStatement("SELECT * FROM comments WHERE forum_id = 10;");

			//Gather the Results of the Select
			ResultSet rs = statement.executeQuery();

			//Print Out Each Result
			while(rs.next())
			{

				String comment_content = rs.getString("comment_content");
				String username = rs.getString("username");


				String cmt = new String(comment_content);
				String uname = new String(username);
				commentForum10List.add(cmt + ", username: " + uname);

			}

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting to Online DB: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}

			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

				statement = null;

			}

		}

		return commentForum10List;

	}

	public List downloadCommentsForum11() throws SQLException
	{

		List<String> commentForum11List = new ArrayList<>();

		System.out.println("Attempting to Show All Values in the Table ~comments~.");

		//The Format of the Host Name is the JDCB Specifier, Then the Address to Connect, Before the Database Name
		String host = "jdbc:mysql://studioruum.c5iijqup9ms0.us-east-1.rds.amazonaws.com/studioruumOnline";

		//Default Master Username and Password From AWS
		String user = "group40";
		String password = "zitozito";

		//Declaring the Statement to Be Used
		PreparedStatement statement = null;

		Connection connection = DriverManager.getConnection(host, user, password);

		try
		{

			//Creating a Prepared Statement
			statement = connection.prepareStatement("SELECT * FROM comments WHERE forum_id = 11;");

			//Gather the Results of the Select
			ResultSet rs = statement.executeQuery();

			//Print Out Each Result
			while(rs.next())
			{
				String comment_content = rs.getString("comment_content");
				String username = rs.getString("username");


				String cmt = new String(comment_content);
				String uname = new String(username);
				commentForum11List.add(cmt + ", username: " + uname);
			}

		}

		catch (SQLException ex)
		{

			System.out.println("Error Connecting to Online DB: " + ex);

		}

		finally
		{

			try
			{

				statement.close();

			}
			catch (SQLException ex)
			{

				System.out.println("Error Closing: " + ex);

				statement = null;


			}

		}

		return commentForum11List;

	}

}