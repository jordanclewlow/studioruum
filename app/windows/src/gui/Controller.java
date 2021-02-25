package gui;

//All Java Import Statements
import java.io.IOException;
import java.util.*;
import java.sql.*;
import java.sql.Connection;
import java.security.NoSuchAlgorithmException;

//All JavaFX Import Statements
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

public class Controller
{

    //// GLOBAL VARIABLES

    ////

    static String accountType="";
    static String username="";
    public String originalName = "WithCaps";
    static OnlineSync online = new OnlineSync();

    LocalDB locDB = new LocalDB();

    // DATABASE CONNECTIONS

    Connection onlineConnect = null;
    Connection offlineConnect = null;

    //The Format of the Host Name is the JDBC Specifier, Then the Address to Connect, Before the Database Name
    String host = "jdbc:mysql://studioruum.c5iijqup9ms0.us-east-1.rds.amazonaws.com/studioruumOnline";
    String user = "group40";
    String password = "zitozito";

    ////

    //// GLOBAL VARIABLES

    //sends a one time passcode to the email of the user
    public void sendOTP(ActionEvent event) throws Exception
    {

        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();
        TextField emailusername = (TextField) scene.lookup("#userinfo");
        Button send = (Button) scene.lookup("#send");
        String userinfo = emailusername.getText().toLowerCase();
        OnlineSync online = new OnlineSync();
        onlineConnect = online.Connect();

        if(userinfo.contains("@")&&online.email_exist(onlineConnect, userinfo))
        {

            online.deleteOTP(onlineConnect, userinfo, false);
            online.sendMail(onlineConnect, userinfo);
            send.setDisable(true);
            username=userinfo;

        }
        else if(online.username_exist(onlineConnect, userinfo))
        {

            String email = online.find_email(onlineConnect, userinfo);
            online.deleteOTP(onlineConnect, email, false);
            online.sendMail(onlineConnect,email);
            send.setDisable(true);
            username=email;

        }

    }

    //checks if the one time passcode is correct
    public void confirmOTP(ActionEvent event)throws Exception
    {

        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();
        TextField otp = (TextField) scene.lookup("#otp");
        TextField userinfo = (TextField) scene.lookup("#userinfo");
        String code = otp.getText();
        String email = userinfo.getText();
        OnlineSync online = new OnlineSync();
        onlineConnect = online.Connect();

        if(!email.contains("@"))
        {

            email = online.find_email(onlineConnect, email);

        }

        PreparedStatement getOTP = onlineConnect.prepareStatement("SELECT otp FROM reset_password WHERE email=?");
        getOTP.setString(1,email);
        ResultSet rs = getOTP.executeQuery();
        rs.next();
        String correct = rs.getString(1);

        if(code.equals(correct))
        {

            Parent new_password = FXMLLoader.load(getClass().getResource("new_password.fxml"));
            Scene nextScene = new Scene(new_password);
            window.setScene(nextScene);

        }

        online.Disconnect(onlineConnect);

    }

    //changes the users password to the new password they have entered
    public void changePassword(ActionEvent event) throws Exception
    {

        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();
        Label warning = (Label) scene.lookup("#warning");
        TextField psswrd = (TextField) scene.lookup("#passwordtxt");
        TextField Repsswrd = (TextField) scene.lookup("#Repasswordtxt");
        String password = psswrd.getText();
        String Repassword = Repsswrd.getText();

        if (password.equals(Repassword)&&password.length()>5)
        {

            onlineConnect = online.Connect();
            byte[] salt = online.generateSalt();

            try
            {
                password = online.generateHash(salt, password);
            }

            catch (NoSuchAlgorithmException ex)
            {

                ex.printStackTrace();

            }

            online.update_password(onlineConnect, username, password, salt);
            online.deleteOTP(onlineConnect, username, true);
            goLogin(event);

        }

        else if(!password.equals(Repassword))
        {

            warning.setText("The 2 passwords do not match: Please re-enter.");

        }
        else
        {

            warning.setText("Invalid Password: Please include at least 1 capital, 1 numeric and length 5.");

        }

    }

    //checks if the username and password combo used for sign up is valid
    public void validSignUp(ActionEvent event) throws IOException
    {

        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();
        Label warning = (Label) scene.lookup("#warning");
        TextField uname = (TextField) scene.lookup("#logintxt");
        TextField mailaccount = (TextField) scene.lookup("#email");
        TextField psswrd = (TextField) scene.lookup("#passwordtxt");
        TextField Repsswrd = (TextField) scene.lookup("#Repasswordtxt");

        username = uname.getText().toLowerCase();
        String email = mailaccount.getText().toLowerCase();
        String password = psswrd.getText();
        String Repassword = Repsswrd.getText();
        //check if username contains @

        if (!username.contains("@")&&password.equals(Repassword)&&password.length()>=5&&password.matches(".*\\d.*")&&password.matches(".*[A-Z].*")&&accountType!=""&&email.contains("@")&&email.contains("."))
        {

            onlineConnect = online.Connect();
            byte[] salt = online.generateSalt();

            try
            {

                password = online.generateHash(salt, password);

            }

            catch(NoSuchAlgorithmException ex)
            {

                ex.printStackTrace();

            }

            if(online.uploadUsers(onlineConnect, username, email, password, salt,accountType))
            {

                warning.setText("");
                loginSync(event);

                try
                {

                    goHome(event);

                }

                catch (Exception ex)
                {

                    ex.printStackTrace();

                }

                online.downloadUsers(onlineConnect);

            }

            else
            {

                System.out.println("username is taken");
                warning.setText("This username is taken: Please enter a different username.");

            }

            //create new record and add to database

        }

        else if(accountType.equals(""))
        {

            warning.setText("Account type not selected: Please select an account type");

        }

        else if(!password.equals(Repassword))
        {

            warning.setText("The 2 passwords do not match: Please re-enter.");

        }

        else if(!email.contains("@")&&!email.contains("."))
        {

            warning.setText("Invalid email");

        }

        else if(username.contains("@"))
        {

            warning.setText("Illegal character '@' in username");

        }

        else
        {

            warning.setText("Invalid Password: Please include at least 1 capital, 1 numeric and length 5.");

        }

    }

    // hyperlink on the log in page that allows user to recover their password
    public void forgotpassowrdlink(ActionEvent event) throws IOException
    {

        Parent signUp = FXMLLoader.load(getClass().getResource("reset_password.fxml"));
        Scene signUpScene = new Scene(signUp);
        //This line gets the Stage information
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        window.setScene(signUpScene);
        window.show();

    }

    // hyperlink on the log in page that allows user to register
    public void signUplink(ActionEvent event) throws IOException
    {

        Parent signUp = FXMLLoader.load(getClass().getResource("signup.fxml"));
        Scene signUpScene = new Scene(signUp);

        //This line gets the Stage information
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        window.setScene(signUpScene);
        window.show();

    }

    // validate user account info
    public void validNamePassword(ActionEvent event) throws Exception
    {

        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        onlineConnect = online.Connect();
        PreparedStatement statement = onlineConnect.prepareStatement("SELECT account_type FROM users WHERE username=?");
        Label warning = (Label) scene.lookup("#warning");

        // users input
        TextField uname = (TextField) scene.lookup("#logintxt");
        TextField psswrd = (TextField) scene.lookup("#passwordtxt");
        username = uname.getText().toLowerCase();
        statement.setString(1,username);
        ResultSet rs = statement.executeQuery();

        if(rs.next())
        {

            accountType = rs.getString(1);
            System.out.println(accountType);
            String password = psswrd.getText();
            byte[] salt = online.getSalt(onlineConnect, username);

            try
            {

                password = online.generateHash(salt, password);

            }

            catch (NoSuchAlgorithmException ex)
            {

                ex.printStackTrace();

            }

            if (online.login(onlineConnect, username, password))
            {

                loginSync(event);
                goHome(event);

            }

            else
            {

                warning.setText("Invalid username password combo");

            }

        }

        else
        {

            warning.setText("Invalid username");

        }

        onlineConnect.close();

    }

    //Used to Download All Resources Needed For a User to Access the System
    public void logoutSync(ActionEvent event) throws IOException
    {

        //System.out.println(currentUser);

        //Attempting to Connect
        try
        {

            onlineConnect = DriverManager.getConnection(host, user, password);

            if(onlineConnect != null)
            {

                //Preparing a Statement to Get All Locally Saved Resources
                PreparedStatement resourceStatement = null;

                try
                {
                    Class.forName("org.sqlite.JDBC");
                    offlineConnect = DriverManager.getConnection("jdbc:sqlite:StudioruumDB.sqlite");
                }

                catch(Exception ex)
                {
                    System.out.println("Error Connecting to Offline DB: " + ex.getMessage());
                }

                try
                {

                    //Creating a Prepared Statement
                    resourceStatement = offlineConnect.prepareStatement("SELECT * FROM resources;");

                    //Gather the Results of the Select
                    ResultSet resourceResults = resourceStatement.executeQuery();

                    //Preparing Statements to Get All Tables of Resources
                    PreparedStatement flashcardStatement = null;
                    PreparedStatement noteStatement = null;
                    PreparedStatement dictionaryStatement = null;
                    PreparedStatement quizStatement = null;

                    while(resourceResults.next())
                    {
                        int resourceID = resourceResults.getInt("resource_id");

                        //Gathering All Resources With That ID
                        flashcardStatement = offlineConnect.prepareStatement("SELECT * FROM flashcards WHERE resource_id = ?;");
                        flashcardStatement.setInt(1, resourceID);

                        noteStatement = offlineConnect.prepareStatement("SELECT * FROM notes WHERE resource_id = ?;");
                        noteStatement.setInt(1, resourceID);

                        dictionaryStatement = offlineConnect.prepareStatement("SELECT * FROM dictionaries WHERE resource_id = ?;");
                        dictionaryStatement.setInt(1, resourceID);

                        quizStatement = offlineConnect.prepareStatement("SELECT * FROM quizzes WHERE resource_id = ?;");
                        quizStatement.setInt(1, resourceID);

                        //Result Sets For All Where There is a Match
                        ResultSet flashcardResults = flashcardStatement.executeQuery();

                        /*
                        while(flashcardResults.next())
                        {

                            System.out.println(flashcardResults.getString("flashcard_id"));
                            System.out.println(flashcardResults.getString("front_content"));
                            System.out.println(flashcardResults.getString("back_content"));

                        }
                        */

                        ResultSet noteResults = noteStatement.executeQuery();
                        ResultSet dictionaryResults = dictionaryStatement.executeQuery();
                        ResultSet quizResults = quizStatement.executeQuery();

                        //Insert the Resource ID If Not Present
                        int resource_id = resourceResults.getInt("resource_id");

                        PreparedStatement pstmt = null;

                        PreparedStatement keyStatement = onlineConnect.prepareStatement("SET FOREIGN_KEY_CHECKS = 0;");
                        keyStatement.executeUpdate();
                        keyStatement.close();

                        pstmt = onlineConnect.prepareStatement("REPLACE INTO resources VALUES (?, ?, null);");

                        //THIS IS THE ERROR - EXECUTION
                        pstmt.setInt(1, resource_id);
                        pstmt.setString(2, username);
                        pstmt.executeUpdate();
                        pstmt.close();

                        keyStatement = onlineConnect.prepareStatement("SET FOREIGN_KEY_CHECKS = 1;");
                        keyStatement.executeUpdate();

                        if(dictionaryResults.next() != false)
                        {

                            do
                            {

                                PreparedStatement dpstmt = null;

                                int dictionary_id = dictionaryResults.getInt("dictionary_id");

                                String dictionary_name = dictionaryResults.getString("dictionary_name");

                                //THEN UPLOAD

                                PreparedStatement disableStatement = onlineConnect.prepareStatement("SET FOREIGN_KEY_CHECKS=0;");
                                disableStatement.executeUpdate();

                                String dctnSQL = "REPLACE INTO dictionaries VALUES (?, ?, ?)";
                                dpstmt = onlineConnect.prepareStatement(dctnSQL);
                                dpstmt.setInt(1, dictionary_id);
                                dpstmt.setInt(2, resource_id);
                                dpstmt.setString(3, dictionary_name);

                                dpstmt.executeUpdate();
                                dpstmt.close();

                                PreparedStatement enableStatement = onlineConnect.prepareStatement("SET FOREIGN_KEY_CHECKS=1;");
                                enableStatement.executeUpdate();


                            }
                            while (dictionaryResults.next());
                        }

                        if(quizResults.next() != false)
                        {
                            do
                            {

                                keyStatement = onlineConnect.prepareStatement("SET FOREIGN_KEY_CHECKS = 0;");
                                keyStatement.executeUpdate();

                                PreparedStatement qpstmt = null;

                                int quiz_id = quizResults.getInt("quiz_id");

                                String quiz_name = quizResults.getString("quiz_name");
                                String quiz_topic = quizResults.getString("quiz_topic");

                                //THEN UPLOAD

                                String quizSQL = "REPLACE INTO quizzes VALUES (?, ?, ?, ?)";
                                qpstmt = onlineConnect.prepareStatement(quizSQL);
                                qpstmt.setInt(1, quiz_id);
                                qpstmt.setInt(2, resource_id);
                                qpstmt.setString(3, quiz_name);
                                qpstmt.setString(4, quiz_topic);

                                qpstmt.executeUpdate();
                                qpstmt.close();

                                keyStatement = onlineConnect.prepareStatement("SET FOREIGN_KEY_CHECKS = 1;");
                                keyStatement.executeUpdate();
                                keyStatement.close();

                            }
                            while (quizResults.next());
                        }

                        if(flashcardResults.next() != false)
                        {
                            do
                            {

                                keyStatement = onlineConnect.prepareStatement("SET FOREIGN_KEY_CHECKS = 0;");
                                keyStatement.executeUpdate();

                                PreparedStatement fpstmt = null;

                                int flashcard_id = flashcardResults.getInt("flashcard_id");

                                int dictionary_id = flashcardResults.getInt("dictionary_id");

                                int quiz_id = flashcardResults.getInt("quiz_id");

                                String front_content = flashcardResults.getString("front_content");
                                String back_content = flashcardResults.getString("back_content");

                                //THEN UPLOAD

                                try
                                {

                                    String flshSQL = "REPLACE INTO flashcards VALUES (?, ?, ?, ?, ?, ?)";
                                    fpstmt = onlineConnect.prepareStatement(flshSQL);
                                    fpstmt.setInt(1, flashcard_id);
                                    fpstmt.setInt(2, resource_id);
                                    fpstmt.setInt(3, dictionary_id);

                                    fpstmt.setInt(4, quiz_id);

                                    fpstmt.setString(5, front_content);
                                    fpstmt.setString(6, back_content);

                                    fpstmt.executeUpdate();
                                    fpstmt.close();

                                }
                                catch(SQLIntegrityConstraintViolationException ex)
                                {

                                    String flshSQL = "REPLACE INTO flashcards (flashcard_id, resource_id, dictionary_id, front_content, back_content) VALUES (?, ?, ?, ?, ?)";
                                    fpstmt = onlineConnect.prepareStatement(flshSQL);
                                    fpstmt.setInt(1, flashcard_id);
                                    fpstmt.setInt(2, resource_id);
                                    fpstmt.setInt(3, dictionary_id);

                                    fpstmt.setString(4, front_content);
                                    fpstmt.setString(5, back_content);

                                    fpstmt.executeUpdate();
                                    fpstmt.close();

                                }

                                keyStatement = onlineConnect.prepareStatement("SET FOREIGN_KEY_CHECKS = 1;");
                                keyStatement.executeUpdate();
                                keyStatement.close();

                            }
                            while (flashcardResults.next());
                        }

                        if(noteResults.next() != false)
                        {
                            do
                            {

                                PreparedStatement npstmt = null;

                                int note_id = noteResults.getInt("note_id");

                                String note_title = noteResults.getString("note_title");
                                String note_content = noteResults.getString("note_content");

                                //THEN UPLOAD

                                String noteSQL = "REPLACE INTO notes VALUES (?, ?, ?, ?)";
                                npstmt = onlineConnect.prepareStatement(noteSQL);
                                npstmt.setInt(1, note_id);
                                npstmt.setInt(2, resource_id);
                                npstmt.setString(3, note_title);
                                npstmt.setString(4, note_content);

                                npstmt.executeUpdate();
                                npstmt.close();

                            }
                            while (noteResults.next());
                        }

                    }

                }
                catch (SQLException ex)
                {

                    ex.printStackTrace();

                    System.out.println("Error Connecting: " + ex);

                }

            }

        }
        catch (SQLException ex)
        {

            System.out.println("An Error Occurred When Connecting to the Database.");
            ex.printStackTrace();

        }
        finally
        {

            //Close The Connection When Finished
            if (onlineConnect != null)
            {

                try
                {

                    System.out.println("THIS IS JUST BEFORE THE LOCAL CLEAR");

                    onlineConnect.close();

                    //WARNING: CLEARS THE LOCAL DATABASE EACH TIME

                    offlineConnect.close();

                    try
                    {
                        Class.forName("org.sqlite.JDBC");
                        offlineConnect = DriverManager.getConnection("jdbc:sqlite:StudioruumDB.sqlite");

                        PreparedStatement rsrcClear = offlineConnect.prepareStatement("DELETE FROM resources;");
                        PreparedStatement noteClear = offlineConnect.prepareStatement("DELETE FROM notes;");
                        PreparedStatement dictClear = offlineConnect.prepareStatement("DELETE FROM dictionaries;");
                        PreparedStatement quizClear = offlineConnect.prepareStatement("DELETE FROM quizzes;");
                        PreparedStatement flshClear = offlineConnect.prepareStatement("DELETE FROM flashcards;");

                        rsrcClear.executeUpdate();
                        noteClear.executeUpdate();
                        dictClear.executeUpdate();
                        quizClear.executeUpdate();
                        flshClear.executeUpdate();

                    }
                    catch(Exception ex)
                    {
                        System.out.println("Error Connecting to Offline DB: ");
                        ex.printStackTrace();
                    }

                    System.out.println("THIS IS JUST AFTER THE LOCAL CLEAR");

                }
                catch (SQLException ex)
                {

                    ex.printStackTrace();

                }

            }

        }

    }

    //Used to Upload All Resources When the Program is Closed Via the X
    public void loginSync(ActionEvent event) throws IOException
    {

        //Upload the Resources, Flashcards, Dictionaries, Quizzes

        //Attempting to Connect
        try
        {

            onlineConnect = DriverManager.getConnection(host, user, password);

            if(onlineConnect != null)
            {

                //Preparing a Statement to Upload All Resources of a User
                PreparedStatement resourceStatement = null;

                //Establishes an OFFLINE Connection
                Connection offlineConnect = null;

                try
                {
                    Class.forName("org.sqlite.JDBC");
                    offlineConnect = DriverManager.getConnection("jdbc:sqlite:StudioruumDB.sqlite");
                }
                catch(Exception ex)
                {
                    System.out.println("Error Connecting to Offline DB: " + ex.getMessage());
                }

                try
                {

                    //Creating a Prepared Statement
                    resourceStatement = onlineConnect.prepareStatement("SELECT resource_id FROM resources WHERE username = ?;");
                    resourceStatement.setString(1, username);

                    //Gather the Results of the Select
                    ResultSet resourceResults = resourceStatement.executeQuery();

                    //Preparing Statements For All Tables of Resources
                    PreparedStatement flashcardStatement = null;
                    PreparedStatement noteStatement = null;
                    PreparedStatement dictionaryStatement = null;
                    PreparedStatement quizStatement = null;

                    while(resourceResults.next())
                    {

                        String resourceID = resourceResults.getString("resource_id");

                        //Gathering All Resources With That ID
                        flashcardStatement = onlineConnect.prepareStatement("SELECT * FROM flashcards WHERE resource_id = ?;");
                        flashcardStatement.setString(1, resourceID);

                        noteStatement = onlineConnect.prepareStatement("SELECT * FROM notes WHERE resource_id = ?;");
                        noteStatement.setString(1, resourceID);

                        dictionaryStatement = onlineConnect.prepareStatement("SELECT * FROM dictionaries WHERE resource_id = ?;");
                        dictionaryStatement.setString(1, resourceID);

                        quizStatement = onlineConnect.prepareStatement("SELECT * FROM quizzes WHERE resource_id = ?;");
                        quizStatement.setString(1, resourceID);

                        //Result Sets For All Where There is a Match
                        ResultSet flashcardResults = flashcardStatement.executeQuery();

                        /*
                        while(flashcardResults.next())
                        {

                            System.out.println(flashcardResults.getString("flashcard_id"));
                            System.out.println(flashcardResults.getString("front_content"));
                            System.out.println(flashcardResults.getString("back_content"));

                        }
                        */

                        ResultSet noteResults = noteStatement.executeQuery();
                        ResultSet dictionaryResults = dictionaryStatement.executeQuery();
                        ResultSet quizResults = quizStatement.executeQuery();

                        //Insert the Resource ID If Not Present
                        int resource_id = resourceResults.getInt("resource_id");

                        PreparedStatement pstmt = null;

                        pstmt = offlineConnect.prepareStatement("REPLACE INTO resources VALUES (?)");
                        pstmt.setInt(1, resource_id);
                        pstmt.executeUpdate();

                        //
                        //
                        //
                        //

                        if(flashcardResults.next() != false)
                        {
                            do
                            {

                                int flashcard_id = flashcardResults.getInt("flashcard_id");
                                int dictionary_id = flashcardResults.getInt("dictionary_id");
                                int quiz_id = flashcardResults.getInt("quiz_id");

                                String front_content = flashcardResults.getString("front_content");
                                String back_content = flashcardResults.getString("back_content");

                                //THEN UPLOAD

                                String flshSQL = "REPLACE INTO flashcards VALUES (?, ?, ?, ?, ?, ?)";
                                pstmt = offlineConnect.prepareStatement(flshSQL);
                                pstmt.setInt(1, flashcard_id);
                                pstmt.setInt(2, resource_id);
                                pstmt.setInt(3, dictionary_id);
                                pstmt.setInt(4, quiz_id);
                                pstmt.setString(5, front_content);
                                pstmt.setString(6, back_content);

                                pstmt.executeUpdate();

                            }
                            while (flashcardResults.next());
                        }

                        if(noteResults.next() != false)
                        {
                            do
                            {

                                int note_id = noteResults.getInt("note_id");

                                String note_title = noteResults.getString("note_title");
                                String note_content = noteResults.getString("note_content");

                                //THEN UPLOAD

                                String noteSQL = "REPLACE INTO notes VALUES (?, ?, ?, ?)";
                                pstmt = offlineConnect.prepareStatement(noteSQL);
                                pstmt.setInt(1, note_id);
                                pstmt.setInt(2, resource_id);
                                pstmt.setString(3, note_title);
                                pstmt.setString(4, note_content);

                                pstmt.executeUpdate();

                            }
                            while (noteResults.next());
                        }

                        if(dictionaryResults.next() != false)
                        {
                            do
                            {

                                int dictionary_id = dictionaryResults.getInt("dictionary_id");

                                String dictionary_name = dictionaryResults.getString("dictionary_name");

                                //THEN UPLOAD

                                String dctnSQL = "REPLACE INTO dictionaries VALUES (?, ?, ?)";
                                pstmt = offlineConnect.prepareStatement(dctnSQL);
                                pstmt.setInt(1, dictionary_id);
                                pstmt.setInt(2, resource_id);
                                pstmt.setString(3, dictionary_name);

                                pstmt.executeUpdate();

                            }
                            while (dictionaryResults.next());
                        }

                        if(quizResults.next() != false)
                        {
                            do
                            {

                                int quiz_id = quizResults.getInt("quiz_id");

                                String quiz_name = quizResults.getString("quiz_name");
                                String quiz_topic = quizResults.getString("quiz_topic");

                                //THEN UPLOAD

                                String quizSQL = "REPLACE INTO quizzes VALUES (?, ?, ?, ?)";
                                pstmt = offlineConnect.prepareStatement(quizSQL);
                                pstmt.setInt(1, quiz_id);
                                pstmt.setInt(2, resource_id);
                                pstmt.setString(3, quiz_name);
                                pstmt.setString(4, quiz_topic);

                                pstmt.executeUpdate();

                            }
                            while (quizResults.next());
                        }

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

                        resourceStatement.close();

                    }
                    catch (SQLException ex)
                    {

                        System.out.println("Error Closing: " + ex);

                    }

                }

            }

        }
        catch (SQLException ex)
        {

            System.out.println("An Error Occurred When Connecting to the Database.");
            ex.printStackTrace();

        }
        finally
        {

            //Close The Connection When Finished
            if (onlineConnect != null)
            {

                try
                {

                    onlineConnect.close();
                    onlineConnect.close();

                }
                catch (SQLException ex)
                {

                    ex.printStackTrace();

                }

            }

        }

    }

    // navigation buttons
    public void goHome(ActionEvent event) throws IOException
    {
        Parent dest = FXMLLoader.load(getClass().getResource("home.fxml"));
        Scene destScene = new Scene(dest);
        //This line gets the Stage information
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        // hide create button for scholar
        Button createButton = (Button) destScene.lookup("#createBtn");
        Rectangle rectangle = (Rectangle) destScene.lookup("#createRectangle");
        Label createLabel = (Label) destScene.lookup("#createLbl");
        if(accountType.equals("Scholar")){
            createButton.setDisable(true);
            createButton.setVisible(false);
            rectangle.setVisible(false);
            createLabel.setVisible(false);
        }
        // dont show create button if educator already has a classruum
        try {
            if(isInAClassruum()==true){
                createButton.setDisable(true);
                createButton.setVisible(false);
                rectangle.setVisible(false);
                createLabel.setVisible(false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        window.setScene(destScene);
        window.show();
        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {
            try
            {
                logoutSync(event);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        });
    }

    public void goLogin(ActionEvent event) throws IOException{
        Parent dest = FXMLLoader.load(getClass().getResource("login.fxml"));
        Scene destScene = new Scene(dest);
        //This line gets the Stage information
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        window.setScene(destScene);
        window.show();
    }

    public void goFlashcard(ActionEvent event) throws IOException
    {

        // Get Stage info and set destination Scene
        Parent dest = FXMLLoader.load(getClass().getResource("view_flashcard.fxml"));
        Scene destScene = new Scene(dest);
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        window.setScene(destScene);

        // Lookup in the Scene for ComboBox, fetch items from local DB and add them
        ComboBox dictDropDown = (ComboBox) destScene.lookup("#dictDrpDwn");
        List<Dictionary> dictionaries = locDB.allDictionaries();
        ObservableList<Dictionary> observableDicts = FXCollections.observableList(dictionaries);
        dictDropDown.setItems(observableDicts);

        // Updates item in ComboBox to show only their title instead of their full instance
        Callback<ListView<Dictionary>, ListCell<Dictionary>> factory = lv -> new ListCell<Dictionary>()
        {

            @Override
            protected void updateItem(Dictionary item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.getTitle());
            }

        };

        dictDropDown.setCellFactory(factory);
        dictDropDown.setButtonCell(factory.call(null));

        // Finally display window
        window.show();

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    // Populates flashcard combo box after user selects dictionary
    public void populateFlash(ActionEvent event) throws IOException
    {

        // Get Stage and Scene info
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        // Lookup in the Scene for dictionary ComboBox, get selected item's dictionary_id
        ComboBox dictDropDown = (ComboBox) scene.lookup("#dictDrpDwn");
        Dictionary selected = (Dictionary) dictDropDown.getSelectionModel().getSelectedItem();
        int dictId = selected.getDict();

        // Lookup in Scene for flashcard content text area
        TextArea flashContent = (TextArea) scene.lookup("#flashContent");

        // Lookup in Scene for "New Flashcard" button, make visible
        Button newFlash = (Button) scene.lookup("#newFlash");
        newFlash.setVisible(true);

        // Lookup in the Scene for flashcard ComboBox, fetch items from local DB
        ComboBox flashDropDown = (ComboBox) scene.lookup("#flashDrpDwn");
        List<Flashcard> flashcards = locDB.allFlashcards(dictId);

        // Proceed with operations if flashcards exist for current dictionary
        if (!flashcards.isEmpty())
        {

            ObservableList<Flashcard> observableFlashs = FXCollections.observableList(flashcards);
            flashDropDown.setItems(observableFlashs);
            flashDropDown.setVisible(true);

            // Updates item in ComboBox to show only their title instead of their full instance
            Callback<ListView<Flashcard>, ListCell<Flashcard>> factory = lv -> new ListCell<Flashcard>()
            {

                @Override
                protected void updateItem(Flashcard item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? "" : item.frontProperty().getValue());
                }

            };

            flashDropDown.setCellFactory(factory);
            flashDropDown.setButtonCell(factory.call(null));

            // Lookup in Scene for buttons to interact with flashcard, make visible
            Button editFlash = (Button) scene.lookup("#editFlash");
            editFlash.setVisible(true);
            Button deleteFlash = (Button) scene.lookup("#deleteFlash");
            deleteFlash.setVisible(true);

            // Set displayed flashcard to first flashcard in dictionary
            Flashcard firstFlash = flashcards.get(0);
            flashContent.setText(firstFlash.frontProperty().getValue());
            flashDropDown.getSelectionModel().selectFirst();

            // Lookup in Scene for "Next" and "Flip", make enabled
            Button nextFlash = (Button) scene.lookup("#nextFlash");
            nextFlash.setDisable(false);
            Button flipFlash = (Button) scene.lookup("#flipFlash");
            flipFlash.setDisable(false);

        }

        // If no flashcards for current dictionary, alert user
        else
        {

            flashContent.setText("");
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Flashcards");
            alert.setHeaderText("No flashcards in current dictionary...");
            alert.setContentText("There are no flashcards in the current dictionary. You can add some by clicking the 'New Flashcard' button");

            alert.showAndWait();

        }

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    // Displays flashcard front content after user selects flashcard
    public void displayFlash(ActionEvent event) throws IOException
    {

        // Get Stage and Scene info
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        // Lookup in the Scene for flashcard ComboBox, get selected item
        ComboBox flashDropDown = (ComboBox) scene.lookup("#flashDrpDwn");
        Flashcard selected = (Flashcard) flashDropDown.getSelectionModel().getSelectedItem();

        // Set displayed flashcard to selected one
        TextArea flashContent = (TextArea) scene.lookup("#flashContent");
        flashContent.setText(selected.frontProperty().getValue());

        // Lookup in Scene for "Flip" button, make enabled
        Button flipFlash = (Button) scene.lookup("#flipFlash");
        flipFlash.setDisable(false);

        // Make "Prev" button enabled if not first flashcard
        if (flashDropDown.getSelectionModel().getSelectedIndex() != 0)
        {

            Button prevFlash = (Button) scene.lookup("#prevFlash");
            prevFlash.setDisable(false);

        }
        else
        {
            Button prevFlash = (Button) scene.lookup("#prevFlash");
            prevFlash.setDisable(true);
        }

        // Make "Next" button enabled if not last flashcard
        if (flashDropDown.getSelectionModel().getSelectedIndex() != flashDropDown.getItems().size() - 1)
        {
            Button nextFlash = (Button) scene.lookup("#nextFlash");
            nextFlash.setDisable(false);
        }
        else
        {
            Button nextFlash = (Button) scene.lookup("#nextFlash");
            nextFlash.setDisable(true);
        }

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    // Flips flashcard content
    public void flipFlash(ActionEvent event) throws IOException
    {

        // Get Stage and Scene info
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        // Lookup in scene for flashcard content text area
        TextArea flashContent = (TextArea) scene.lookup("#flashContent");

        // Lookup in the Scene for flashcard ComboBox, get selected item
        ComboBox flashDropDown = (ComboBox) scene.lookup("#flashDrpDwn");
        Flashcard selected = (Flashcard) flashDropDown.getSelectionModel().getSelectedItem();

        // Flip to back content if front and vice-versa
        if (flashContent.getText().equals(selected.frontProperty().getValue()))
        {
            flashContent.setText(selected.backProperty().getValue());
        }
        else if (flashContent.getText().equals(selected.backProperty().getValue()))
        {
            flashContent.setText(selected.frontProperty().getValue());
        }

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    // Adds functionality to "Prev" button in flashcards page
    public void prevFlash(ActionEvent event) throws IOException
    {
        // Get Stage and Scene info
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        // Lookup in the Scene for flashcard ComboBox
        ComboBox flashDropDown = (ComboBox) scene.lookup("#flashDrpDwn");

        // Lookup in scene for flashcard content text area
        TextArea flashContent = (TextArea) scene.lookup("#flashContent");

        // Set selected flashcard and its content to prev flashcard if not second to first
        if (flashDropDown.getSelectionModel().getSelectedIndex() > 1)
        {

            flashDropDown.getSelectionModel().select(flashDropDown.getSelectionModel().getSelectedIndex() - 1);
            Flashcard selected = (Flashcard) flashDropDown.getSelectionModel().getSelectedItem();
            flashContent.setText(selected.frontProperty().getValue());

        }

        // Otherwise, set selected flashcard and its content to prev flashcard, disable "Prev" button
        else if (flashDropDown.getSelectionModel().getSelectedIndex() == 1)
        {

            flashDropDown.getSelectionModel().selectFirst();
            Flashcard selected = (Flashcard) flashDropDown.getSelectionModel().getSelectedItem();
            flashContent.setText(selected.frontProperty().getValue());
            Button prevFlash = (Button) scene.lookup("#prevFlash");
            prevFlash.setDisable(true);

        }

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    // Adds functionality to "Next" button in flashcards page
    public void nextFlash(ActionEvent event) throws IOException
    {

        // Get Stage and Scene info
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        // Lookup in the Scene for flashcard ComboBox
        ComboBox flashDropDown = (ComboBox) scene.lookup("#flashDrpDwn");

        // Lookup in scene for flashcard content text area
        TextArea flashContent = (TextArea) scene.lookup("#flashContent");

        // Set selected flashcard and its content to next flashcard if not second to last
        if (flashDropDown.getSelectionModel().getSelectedIndex() < flashDropDown.getItems().size() - 2)
        {

            flashDropDown.getSelectionModel().select(flashDropDown.getSelectionModel().getSelectedIndex() + 1);
            Flashcard selected = (Flashcard) flashDropDown.getSelectionModel().getSelectedItem();
            flashContent.setText(selected.frontProperty().getValue());

        }

        // Otherwise, set selected flashcard and its content to next flashcard, disable "Next" button
        else if (flashDropDown.getSelectionModel().getSelectedIndex() == flashDropDown.getItems().size() - 2)
        {

            flashDropDown.getSelectionModel().selectLast();
            Flashcard selected = (Flashcard) flashDropDown.getSelectionModel().getSelectedItem();
            flashContent.setText(selected.frontProperty().getValue());
            Button nextFlash = (Button) scene.lookup("#nextFlash");
            nextFlash.setDisable(true);

        }

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    // Deletes currently selected Flashcard from local DB
    public void deleteFlash(ActionEvent event) throws IOException
    {

        // Get Stage and Scene info
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        // Lookup in the Scene for flashcard ComboBox, get selected item
        ComboBox flashDropDown = (ComboBox) scene.lookup("#flashDrpDwn");
        Flashcard selected = (Flashcard) flashDropDown.getSelectionModel().getSelectedItem();

        // Lookup in scene for flashcard content text area
        TextArea flashContent = (TextArea) scene.lookup("#flashContent");

        // Create and show deletion alert
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Flashcard Deletion");
        alert.setHeaderText("Trying to delete a flashcard...");
        alert.setContentText("Are you sure you want to delete this flashcard? You will not be able to recover its content in case of deletion");
        Optional<ButtonType> result = alert.showAndWait();

        // Delete flashcard in case user clicks "OK", skip to next flashcard
        if (result.get() == ButtonType.OK)
        {

            // Delete flashcard from local db
            locDB.deleteFlashcard(selected.getFID());

            // Remove deleted flashcard from combo box
            int selectedIndex = flashDropDown.getSelectionModel().getSelectedIndex();
            flashDropDown.getItems().remove(selectedIndex);

            // If there are still flashcards in dictionary, skip to next and refresh items, else alert user
            if (flashDropDown.getItems().size() > 0)
            {


                // Set selected flashcard and its content to next flashcard if not second to last
                if (selectedIndex < flashDropDown.getItems().size() - 2)
                {

                    flashDropDown.getSelectionModel().select(selectedIndex + 1);
                    selected = (Flashcard) flashDropDown.getSelectionModel().getSelectedItem();
                    flashContent.setText(selected.frontProperty().getValue());

                }

                // If second to last, set selected flashcard and its content to next flashcard, disable "Next" button
                else if (selectedIndex == flashDropDown.getItems().size() - 2)
                {

                    flashDropDown.getSelectionModel().selectLast();
                    selected = (Flashcard) flashDropDown.getSelectionModel().getSelectedItem();
                    flashContent.setText(selected.frontProperty().getValue());
                    Button nextFlash = (Button) scene.lookup("#nextFlash");
                    nextFlash.setDisable(true);

                }

                // If last, set selected flashcard and its content to next flashcard, disable "Next" button
                else if (selectedIndex == flashDropDown.getItems().size() - 1)
                {

                    flashDropDown.getSelectionModel().select(selectedIndex - 1);
                    selected = (Flashcard) flashDropDown.getSelectionModel().getSelectedItem();
                    flashContent.setText(selected.frontProperty().getValue());
                    Button nextFlash = (Button) scene.lookup("#nextFlash");
                    nextFlash.setDisable(true);

                }

            }
            else
            {

                flashContent.setText("");
                flashDropDown.setVisible(false);
                Button editFlash = (Button) scene.lookup("#editFlash");
                editFlash.setVisible(false);
                Button deleteFlash = (Button) scene.lookup("#deleteFlash");
                deleteFlash.setVisible(false);
                alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("No Flashcards");
                alert.setHeaderText("No flashcards in current dictionary...");
                alert.setContentText("There are no flashcards in the current dictionary. You can add some by clicking the 'New Flashcard' button");

                alert.showAndWait();

            }
        }

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    // Takes user to create_flashcard page, specifies operation to be executed
    public void newFlash(ActionEvent event) throws IOException
    {

        // Get Stage and Scene info
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        // Lookup in the Scene for dictionary ComboBox, get selected item's dictionary_id
        ComboBox dictDropDown = (ComboBox) scene.lookup("#dictDrpDwn");
        Dictionary selected = (Dictionary) dictDropDown.getSelectionModel().getSelectedItem();
        int dictId = selected.getDict();

        // Get Stage info and set destination Scene
        Parent dest = FXMLLoader.load(getClass().getResource("create_flashcard.fxml"));
        Scene destScene = new Scene(dest);
        window.setScene(destScene);

        // Lookup in destination scene for labels and save params
        Label dictLabel = (Label) destScene.lookup("#dictId");
        dictLabel.setText(dictId + "");
        Label opLabel = (Label) destScene.lookup("#flashOp");
        opLabel.setText("new");

        // Finally display window
        window.show();

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    // Takes user to create_flashcard page, specifies operation to be executed and populates text areas
    public void alterFlash(ActionEvent event) throws IOException
    {

        // Get Stage and Scene info
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        // Lookup in the Scene for flashcard ComboBox, get selected item's flashcard_id & dictionary_id
        ComboBox flashDropDown = (ComboBox) scene.lookup("#flashDrpDwn");
        Flashcard selected = (Flashcard) flashDropDown.getSelectionModel().getSelectedItem();
        int flashId = selected.getFID();
        int dictId = selected.getDict();

        // Get Stage info and set destination Scene
        Parent dest = FXMLLoader.load(getClass().getResource("create_flashcard.fxml"));
        Scene destScene = new Scene(dest);
        window.setScene(destScene);

        // Set front and back of flashcard
        TextArea frontArea = (TextArea) destScene.lookup("#frontArea");
        frontArea.setText(selected.frontProperty().getValue());
        TextArea backArea = (TextArea) destScene.lookup("#backArea");
        backArea.setText(selected.backProperty().getValue());

        // Lookup in destination scene for labels and save params
        Label flashLabel = (Label) destScene.lookup("#flashId");
        flashLabel.setText(flashId + "");
        Label dictLabel = (Label) destScene.lookup("#dictId");
        dictLabel.setText(dictId + "");
        Label opLabel = (Label) destScene.lookup("#flashOp");
        opLabel.setText("alter");

        // Finally display window
        window.show();

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    // Adds functionality to "Save" button in create_flashcard page
    public void saveFlash(ActionEvent event) throws IOException
    {

        // Get Stage and Scene info
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        int dictId = 0;
        int flashId = 0;

        // Look for operation label, get operation
        Label opLabel = (Label) scene.lookup("#flashOp");
        String operation = opLabel.getText();

        if (operation.equals("new"))
        {

            // Get dictionary_id of flashcard to save
            Label dictLabel = (Label) scene.lookup("#dictId");
            dictId = Integer.parseInt(dictLabel.getText());

            // Get front and back of flashcard
            TextArea frontArea = (TextArea) scene.lookup("#frontArea");
            String frontContent = frontArea.getText();
            TextArea backArea = (TextArea) scene.lookup("#backArea");
            String backContent = backArea.getText();

            // Save flashcard
            locDB.saveFlashcard(dictId, frontContent, backContent);

        }
        else
        {

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Overwriting Flashcard");
            alert.setHeaderText("Trying to overwrite a flashcard...");
            alert.setContentText("Are you sure you want to overwrite this flashcard? You will not be able to recover its previous content");
            Optional<ButtonType> result = alert.showAndWait();

            // Alter flashcard in case user clicks "OK"
            if (result.get() == ButtonType.OK)
            {

                // Get flashcard_id and dictionary_id of flashcard to save
                Label flashLabel = (Label) scene.lookup("#flashId");
                flashId = Integer.parseInt(flashLabel.getText());
                Label dictLabel = (Label) scene.lookup("#dictId");
                dictId = Integer.parseInt(dictLabel.getText());

                // Get front and back of flashcard
                TextArea frontArea = (TextArea) scene.lookup("#frontArea");
                String frontContent = frontArea.getText();
                TextArea backArea = (TextArea) scene.lookup("#backArea");
                String backContent = backArea.getText();

                // Alter flashcard
                locDB.updateFlashcard(flashId, frontContent, backContent);

            }

        }

        // Return to view_flashcard page
        returnToFlashcard(event, dictId, flashId);

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    // Returns to flashcard page after user creates/edits flashcard
    public void returnToFlashcard(ActionEvent event, int dictId, int flashId) throws IOException
    {

        // Get Stage info and set destination Scene
        Parent dest = FXMLLoader.load(getClass().getResource("view_flashcard.fxml"));
        Scene destScene = new Scene(dest);
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        window.setScene(destScene);

        // Lookup in the Scene for ComboBox, fetch items from local DB and add them
        ComboBox dictDropDown = (ComboBox) destScene.lookup("#dictDrpDwn");
        List<Dictionary> dictionaries = locDB.allDictionaries();
        ObservableList<Dictionary> observableDicts = FXCollections.observableList(dictionaries);
        dictDropDown.setItems(observableDicts);

        // Updates item in ComboBox to show only their title instead of their full instance
        Callback<ListView<Dictionary>, ListCell<Dictionary>> factory = lv -> new ListCell<Dictionary>()
        {

            @Override
            protected void updateItem(Dictionary item, boolean empty)
            {
                super.updateItem(item, empty);
                setText(empty ? "" : item.getTitle());
            }

        };

        dictDropDown.setCellFactory(factory);
        dictDropDown.setButtonCell(factory.call(null));

        // Find position of dictionary to select
        int selectDictPos = 0;

        for (Dictionary dict : dictionaries)
        {

            if (dict.getDict() == dictId)
                break;
            else
                selectDictPos++;

        }

        // Select dictionary corresponding to passed param
        dictDropDown.getSelectionModel().select(selectDictPos);

        // Lookup in the Scene for flashcard ComboBox, fetch items from local DB
        ComboBox flashDropDown = (ComboBox) destScene.lookup("#flashDrpDwn");
        List<Flashcard> flashcards = locDB.allFlashcards(dictId);

        ObservableList<Flashcard> observableFlashs = FXCollections.observableList(flashcards);
        flashDropDown.setItems(observableFlashs);
        flashDropDown.setVisible(true);

        // Updates item in ComboBox to show only their title instead of their full instance
        Callback<ListView<Flashcard>, ListCell<Flashcard>> flashFactory = lv -> new ListCell<Flashcard>()
        {

            @Override
            protected void updateItem(Flashcard item, boolean empty)
            {
                super.updateItem(item, empty);
                setText(empty ? "" : item.frontProperty().getValue());
            }

        };

        flashDropDown.setCellFactory(flashFactory);
        flashDropDown.setButtonCell(flashFactory.call(null));

        // If new flashcard as param, select last item in flashcard drop down
        if (flashId == 0)
        {
            flashDropDown.getSelectionModel().selectLast();
        }

        // Select param flashcard
        else
        {
            // Find position of flashcard to select
            int selectFlashPos = 0;

            for (Flashcard flash : flashcards)
            {
                if (flash.getFID() == flashId)
                    break;
                else
                    selectFlashPos++;
            }

            // Select dictionary corresponding to passed param
            flashDropDown.getSelectionModel().select(selectFlashPos);

        }

        // Lookup in Scene for buttons to interact with flashcard, make visible
        Button editFlash = (Button) destScene.lookup("#editFlash");
        editFlash.setVisible(true);
        Button deleteFlash = (Button) destScene.lookup("#deleteFlash");
        deleteFlash.setVisible(true);

        // Set displayed flashcard to altered/new flashcard
        Flashcard selected = (Flashcard) flashDropDown.getSelectionModel().getSelectedItem();
        TextArea flashContent = (TextArea) destScene.lookup("#flashContent");
        flashContent.setText(selected.frontProperty().getValue());

        // Lookup in Scene for "Flip" button, make enabled
        Button flipFlash = (Button) destScene.lookup("#flipFlash");
        flipFlash.setDisable(false);

        // Make "Prev" button enabled if not first flashcard
        if (flashDropDown.getSelectionModel().getSelectedIndex() != 0)
        {
            Button prevFlash = (Button) destScene.lookup("#prevFlash");
            prevFlash.setDisable(false);
        }
        else
        {
            Button prevFlash = (Button) destScene.lookup("#prevFlash");
            prevFlash.setDisable(true);
        }

        // Make "Next" button enabled if not last flashcard
        if (flashDropDown.getSelectionModel().getSelectedIndex() != flashDropDown.getItems().size() - 1)
        {
            Button nextFlash = (Button) destScene.lookup("#nextFlash");
            nextFlash.setDisable(false);
        }
        else
        {
            Button nextFlash = (Button) destScene.lookup("#nextFlash");
            nextFlash.setDisable(true);
        }

        //Finally show window
        window.show();

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    // Called to prepare Dictionary page
    public void goDictionary(ActionEvent event) throws IOException
    {

        // Get Stage info and set destination Scene
        Parent dest = FXMLLoader.load(getClass().getResource("view_dictionary.fxml"));
        Scene destScene = new Scene(dest);
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        window.setScene(destScene);

        // Lookup in the Scene for ComboBox, fetch items from local DB and add them
        ComboBox dictDropDown = (ComboBox) destScene.lookup("#dictDrpDwn");
        List<Dictionary> dictionaries = locDB.allDictionaries();
        ObservableList<Dictionary> observableDicts = FXCollections.observableList(dictionaries);
        dictDropDown.setItems(observableDicts);

        // Updates item in ComboBox to show only their title instead of their full instance
        Callback<ListView<Dictionary>, ListCell<Dictionary>> factory = lv -> new ListCell<Dictionary>()
        {

            @Override
            protected void updateItem(Dictionary item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.getTitle());
            }

        };

        dictDropDown.setCellFactory(factory);
        dictDropDown.setButtonCell(factory.call(null));

        // Finally display window
        window.show();

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    // Columns for table in dictionary page (need to be global objects)
    @FXML TableColumn<Flashcard, String> frontCol;
    @FXML TableColumn<Flashcard, String> backCol;

    // Displays selected Dictionary in Table of the dictionary page
    public void displayDict(ActionEvent event) throws IOException
    {

        // Get Stage and Scene info
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        // Lookup in the Scene for ComboBox, get selected item
        ComboBox dictDropDown = (ComboBox) scene.lookup("#dictDrpDwn");
        Dictionary selected = (Dictionary) dictDropDown.getSelectionModel().getSelectedItem();

        //Fetch all flashcards contained in user selected dictionary
        List<Flashcard> flashcards = locDB.allFlashcards(selected.getDict());
        ObservableList<Flashcard> observableFlashcards = FXCollections.observableList(flashcards);

        // Lookup in Scene for TableView and set property value factory for the columns
        TableView<Flashcard> dictTable = (TableView<Flashcard>) scene.lookup("#dictTable");
        frontCol.setCellValueFactory(new PropertyValueFactory<Flashcard, String>("front"));
        backCol.setCellValueFactory(new PropertyValueFactory<Flashcard, String>("back"));

        // Finally add items
        dictTable.setItems(observableFlashcards);

        // Set buttons to alter and eliminate dict to visible
        Button updateDict = (Button) scene.lookup("#updateDict");
        updateDict.setVisible(true);
        Button deleteDict = (Button) scene.lookup("#deleteDict");
        deleteDict.setVisible(true);

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    // Deletes selected Dictionary from local db
    public void deleteDict(ActionEvent event) throws IOException
    {

        // Get Stage and Scene info
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        // Lookup in the Scene for ComboBox, get selected item
        ComboBox dictDropDown = (ComboBox) scene.lookup("#dictDrpDwn");
        Dictionary selected = (Dictionary) dictDropDown.getSelectionModel().getSelectedItem();

        // Try to delete dictionary if user selected one
        if (selected != null)
        {

            // Create and show deletion alert
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Dictionary Deletion");
            alert.setHeaderText("Trying to delete a dictionary...");
            alert.setContentText("Are you sure you want to delete this dictionary? You will not be able to recover its flashcards in case of deletion");
            Optional<ButtonType> result = alert.showAndWait();

            // Delete dictionary in case user clicks "OK", reload window
            if (result.get() == ButtonType.OK)
            {
                locDB.deleteDictionary(selected.getDict());
                goDictionary(event);
            }

        }

        // Show Error alert in case no note selected
        else
        {

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Delete Error");
            alert.setHeaderText("No note to delete!");
            alert.setContentText("You have to select a note to delete it!");

            alert.showAndWait();

        }

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    // Updates record of user select note in local db
    public void updateDict(ActionEvent event) throws IOException
    {
        // Get Stage and Scene info
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        // Lookup in the Scene for ComboBox, get selected item's dictionary_id
        ComboBox dictDropDown = (ComboBox) scene.lookup("#dictDrpDwn");
        Dictionary selected = (Dictionary) dictDropDown.getSelectionModel().getSelectedItem();
        int dictId = selected.getDict();

        // Create and show prompt to rename dictionary
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Rename Dictionary");
        dialog.setHeaderText("Trying to rename " + selected.getTitle() + "...");
        dialog.setContentText("Please enter new name for the dictionary:");
        Optional<String> result = dialog.showAndWait();

        // Rename dictionary if user input new name and reload window
        if (result.isPresent())
        {
            locDB.updateDictionary(dictId, result.get());
            goDictionary(event);
        }

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    // Saves new dictionary in local DB
    public void saveDict(ActionEvent event) throws IOException
    {

        // Get Stage and Scene info
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        // Create and show prompt to name new dictionary
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Dictionary");
        dialog.setHeaderText("Trying to create new dictionary...");
        dialog.setContentText("Please enter a name for the new dictionary:");
        Optional<String> result = dialog.showAndWait();

        // Rename dictionary if user input new name and reload window
        if (result.isPresent())
        {

            locDB.saveDictionary(result.get());

            // Create and show alert to add flashcards to new dictionary
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("New Dictionary Created!");
            alert.setHeaderText("Your new dictionary "+ result.get() +" has been created!");
            alert.setContentText("Congratulations, you created a new dictionary! Do you want to add flashcards to it?");
            Optional<ButtonType> resultButton = alert.showAndWait();

            // Go to flashcards page if user confirms
            if (resultButton.get() == ButtonType.OK)
            {

                goFlashcard(event);

            }

            // Reload page if user decides to remain
            else
            {

                goDictionary(event);

            }
        }

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    // Called to prepare Notes page
    public void goNotes(ActionEvent event) throws IOException
    {

        // Get Stage info and set destination Scene
        Parent dest = FXMLLoader.load(getClass().getResource("view_notes.fxml"));
        Scene destScene = new Scene(dest);
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        window.setScene(destScene);

        // Lookup in the Scene for ComboBox, fetch items from local DB and add them
        ComboBox noteDropDown = (ComboBox) destScene.lookup("#noteDrpDwn");
        List<Note> notes = locDB.allNotes();
        ObservableList<Note> observableNotes = FXCollections.observableList(notes);
        noteDropDown.setItems(observableNotes);

        // Updates item in ComboBox to show only their title instead of their full instance
        Callback<ListView<Note>, ListCell<Note>> factory = lv -> new ListCell<Note>()
        {

            @Override
            protected void updateItem(Note item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.getTitle());
            }

        };

        noteDropDown.setCellFactory(factory);
        noteDropDown.setButtonCell(factory.call(null));

        // Finally display window
        window.show();

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    // Displays selected Note in TextField and TextArea of the notes page
    public void displayNote(ActionEvent event) throws IOException
    {

        // Get Stage and Scene info
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        // Lookup in the Scene for ComboBox, get selected item
        ComboBox noteDropDown = (ComboBox) scene.lookup("#noteDrpDwn");
        Note selected = (Note) noteDropDown.getSelectionModel().getSelectedItem();

        // Set displayed title to selected note's title
        TextField noteTitle = (TextField) scene.lookup("#noteTitle");
        noteTitle.setText(selected.getTitle());

        // Set displayed content to selected note's content
        TextArea noteContent = (TextArea) scene.lookup("#noteContent");
        noteContent.setText(selected.getContent());

        // Set "Save" button (alter note) to visible
        Button updateNote = (Button) scene.lookup("#updateNote");
        updateNote.setVisible(true);

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    // Makes the note title and the note content editable when "Edit" button clicked
    public void editNote(ActionEvent event) throws IOException
    {

        // Get Stage and Scene info
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        // Lookup in the Scene for TextField (note title) and make it editable
        TextField noteTitle = (TextField) scene.lookup("#noteTitle");
        noteTitle.setEditable(true);

        // Lookup in the Scene for TextArea (note content) and make it editable
        TextArea noteContent = (TextArea) scene.lookup("#noteContent");
        noteContent.setEditable(true);

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    // Deletes a note from local DB (if user previously selected one
    public void deleteNote(ActionEvent event) throws IOException
    {

        // Get Stage and Scene info
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        // Lookup in the Scene for ComboBox, get selected item
        ComboBox noteDropDown = (ComboBox) scene.lookup("#noteDrpDwn");
        Note selected = (Note) noteDropDown.getSelectionModel().getSelectedItem();

        // Try to delete note if user selected one
        if (selected != null)
        {

            // Create and show deletion alert
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Note Deletion");
            alert.setHeaderText("Trying to delete a note...");
            alert.setContentText("Are you sure you want to delete this note? You will not be able to recover it in case of deletion");
            Optional<ButtonType> result = alert.showAndWait();

            // Delete note in case user clicks "OK", reload window
            if (result.get() == ButtonType.OK)
            {

                locDB.deleteNote(selected.getDict());
                goNotes(event);

            }

        }

        // Show Error alert in case no note selected
        else
        {

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Delete Error");
            alert.setHeaderText("No note to delete!");
            alert.setContentText("You have to select a note to delete it!");

            alert.showAndWait();

        }

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    // Saves the note as a new note in local DB
    public void saveNote(ActionEvent event) throws IOException
    {

        // Get Stage and Scene info
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        // Lookup in the Scene for note title and retrieve it
        TextField noteTitle = (TextField) scene.lookup("#noteTitle");
        String title = noteTitle.getText();

        // Lookup in the Scene for note content and retrieve it
        TextArea noteContent = (TextArea) scene.lookup("#noteContent");
        String content = noteContent.getText();

        // Save note and reload window
        locDB.saveNote(title, content);
        goNotes(event);

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    // Updates record of user select note in local db
    public void updateNote(ActionEvent event) throws IOException
    {

        // Get Stage and Scene info
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        // Lookup in the Scene for ComboBox, get selected item's note_id
        ComboBox noteDropDown = (ComboBox) scene.lookup("#noteDrpDwn");
        Note selected = (Note) noteDropDown.getSelectionModel().getSelectedItem();
        int noteId = selected.getDict();

        // Lookup in the Scene for note title and retrieve it
        TextField noteTitle = (TextField) scene.lookup("#noteTitle");
        String title = noteTitle.getText();

        // Lookup in the Scene for note content and retrieve it
        TextArea noteContent = (TextArea) scene.lookup("#noteContent");
        String content = noteContent.getText();

        // Create and show overwrite alert
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Overwrite");
        alert.setHeaderText("Trying to overwrite an existing note...");
        alert.setContentText("Are you sure you want to overwrite this note? Its previous content will be completely replaced");
        Optional<ButtonType> result = alert.showAndWait();

        // Delete note in case user clicks "OK", reload window
        if (result.get() == ButtonType.OK)
        {

            locDB.updateNote(noteId, title, content);
            goNotes(event);

        }

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    public void goBack(ActionEvent event) throws IOException
    {

        Parent dest = FXMLLoader.load(getClass().getResource("view_flashcard.fxml"));
        Scene destScene = new Scene(dest);
        //This line gets the Stage information
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        window.setScene(destScene);
        window.show();

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    public void goCreateFlashcard(ActionEvent event) throws IOException
    {

        Parent dest = FXMLLoader.load(getClass().getResource("create_flashcard.fxml"));
        Scene destScene = new Scene(dest);
        //This line gets the Stage information
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        window.setScene(destScene);
        window.show();

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    public boolean isInAClassruum() throws SQLException{
        int count = 0;
        onlineConnect = online.Connect();
        PreparedStatement statement;
        ResultSet rs;
        if(accountType.equals("Educator")){
            statement = onlineConnect.prepareStatement("SELECT COUNT(educator_username) FROM classruums WHERE educator_username = ?");
            statement.setString(1, username);
            rs = statement.executeQuery();
            if(rs.next()) {
                count = rs.getInt(1);
                rs.close();
                statement.close();
            }
            if(count==0){
                return false;
            }
            else{
                return true;
            }
        }
        else {
            statement = onlineConnect.prepareStatement("SELECT COUNT(member_name) FROM class_member WHERE member_name = ?");
            statement.setString(1, username);
            rs = statement.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
                rs.close();
                statement.close();
            }
            if (count == 0) {
                return false;
            } else {
                return true;
            }
        }
    }

    public void goForuum(ActionEvent event) throws IOException, SQLException {

        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();
        Parent dest = FXMLLoader.load(getClass().getResource("foruum.fxml"));
        Scene destScene = new Scene(dest);
        //This line gets the Stage information
        window.setScene(destScene);
        window.show();
        online.downloadForuum();
        online.downloadComment();

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    public void goForuumRelatedPage(ActionEvent event) throws IOException, SQLException {

        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        Label titleLabel = (Label) scene.lookup("#forumTitle");
        TextArea c_context = (TextArea) scene.lookup("#commentContext");
        Label introLabel = (Label) scene.lookup("intro");

        // Instead of setting the comment_id variable we need to make it so the DB automatically assigns it a value
        // This is allowed to be null as its not very important
        String time_updated = "";


        // Here i have initialised all of the hyperlinks so whichever one is clicked the forum_id changes to that.

        // All of these hyperlink mini functions are the same so ive only commented the first one. The hyperlink mini
        // functions represent each forum



        Hyperlink chem = (Hyperlink) scene.lookup("#chemistry");
        chem.setOnAction(new EventHandler<ActionEvent>() {
            // Below happens when the hyperlink is clicked
            @Override
            public void handle(ActionEvent event) {
                // the forum_id is set to 1 because chemistry is the first forum in the online DB
                int forum_id = 1;
                titleLabel.setText("Chemistry");


                // This is the drop down box that displays all of the comments related to the chemistry forum
                // and once a user adds a comment it is displayed in this column.
                ComboBox forum1CommentsDropDown = (ComboBox) scene.lookup("#commentsComboBox");
                List<String> comments = null;
                try {
                    comments = online.downloadCommentsForum1();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                ObservableList<String> observableComments = FXCollections.observableList(comments);
                forum1CommentsDropDown.setItems(observableComments);


                // setOpacity makes the dropdown list and the text area appear on the screen.
                forum1CommentsDropDown.setOpacity(1);
                c_context.setOpacity(1);

                // looks for the button on the screen
                Button submitComm = (Button) scene.lookup("#submitComment");

                // makes the button visible
                submitComm.setOpacity(1);

                submitComm.setOnAction(new EventHandler<ActionEvent>() {
                    // Below happens when the button is clicked
                    @Override
                    public void handle(ActionEvent event) {
                        try {
                            // gets the content of the text area, i.e. the comment
                            String comment_context = c_context.getText();
                            // sets the comment to blank afterwards so they can rewrite a comment if needed
                            c_context.setText("");

                            // uploads what the user just typed into the text box (their comment) onto the
                            // online database.
                            online.uploadComment(forum_id, comment_context, username, time_updated);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }


                    }
                });
            }
        });

        Hyperlink bio = (Hyperlink) scene.lookup("#biology");
        bio.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                int forum_id = 2;
                titleLabel.setText("Biology");

                ComboBox forum2CommentsDropDown = (ComboBox) scene.lookup("#commentsComboBox");
                List<String> comments = null;
                try {
                    comments = online.downloadCommentsForum2();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                ObservableList<String> observableComments = FXCollections.observableList(comments);
                forum2CommentsDropDown.setItems(observableComments);
                forum2CommentsDropDown.setOpacity(1);

                c_context.setOpacity(1);
                Button submitComm = (Button) scene.lookup("#submitComment");

                submitComm.setOpacity(1);

                submitComm.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        try {
                            String comment_context = c_context.getText();
                            c_context.setText("");

                            online.uploadComment(forum_id, comment_context, username, time_updated);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                    }
                });


            }


        });

        Hyperlink phys = (Hyperlink) scene.lookup("#physics");
        phys.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                int forum_id = 3;
                titleLabel.setText("Physics");
                ComboBox forum3CommentsDropDown = (ComboBox) scene.lookup("#commentsComboBox");
                List<String> comments = null;
                try {
                    comments = online.downloadCommentsForum3();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                ObservableList<String> observableComments = FXCollections.observableList(comments);
                forum3CommentsDropDown.setItems(observableComments);
                forum3CommentsDropDown.setOpacity(1);

                c_context.setOpacity(1);
                Button submitComm = (Button) scene.lookup("#submitComment");

                submitComm.setOpacity(1);

                submitComm.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        try {
                            String comment_context = c_context.getText();
                            c_context.setText("");

                            online.uploadComment(forum_id, comment_context, username, time_updated);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        });

        Hyperlink compsci = (Hyperlink) scene.lookup("#compsci");
        compsci.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                int forum_id = 4;
                titleLabel.setText("Computer Science");

                ComboBox forum4CommentsDropDown = (ComboBox) scene.lookup("#commentsComboBox");
                List<String> comments = null;
                try {
                    comments = online.downloadCommentsForum4();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                ObservableList<String> observableComments = FXCollections.observableList(comments);
                forum4CommentsDropDown.setItems(observableComments);
                forum4CommentsDropDown.setOpacity(1);

                c_context.setOpacity(1);
                Button submitComm = (Button) scene.lookup("#submitComment");

                submitComm.setOpacity(1);

                submitComm.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        try {
                            String comment_context = c_context.getText();
                            c_context.setText("");

                            online.uploadComment(forum_id, comment_context, username, time_updated);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        });

        Hyperlink maths = (Hyperlink) scene.lookup("#maths");
        maths.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                int forum_id = 5;
                titleLabel.setText("Maths");

                ComboBox forum5CommentsDropDown = (ComboBox) scene.lookup("#commentsComboBox");
                List<String> comments = null;
                try {
                    comments = online.downloadCommentsForum5();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                ObservableList<String> observableComments = FXCollections.observableList(comments);
                forum5CommentsDropDown.setItems(observableComments);
                forum5CommentsDropDown.setOpacity(1);

                c_context.setOpacity(1);
                Button submitComm = (Button) scene.lookup("#submitComment");

                submitComm.setOpacity(1);

                submitComm.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        try {
                            String comment_context = c_context.getText();
                            c_context.setText("");

                            online.uploadComment(forum_id, comment_context, username, time_updated);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        });

        Hyperlink eng = (Hyperlink) scene.lookup("#english");
        eng.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                int forum_id = 6;
                titleLabel.setText("English");

                ComboBox forum6CommentsDropDown = (ComboBox) scene.lookup("#commentsComboBox");
                List<String> comments = null;
                try {
                    comments = online.downloadCommentsForum6();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                ObservableList<String> observableComments = FXCollections.observableList(comments);
                forum6CommentsDropDown.setItems(observableComments);
                forum6CommentsDropDown.setOpacity(1);

                c_context.setOpacity(1);
                Button submitComm = (Button) scene.lookup("#submitComment");

                submitComm.setOpacity(1);

                submitComm.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        try {
                            String comment_context = c_context.getText();
                            c_context.setText("");

                            online.uploadComment(forum_id, comment_context, username, time_updated);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        });

        Hyperlink rs = (Hyperlink) scene.lookup("#rs");
        rs.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                int forum_id = 7;
                titleLabel.setText("Religious Studies");

                ComboBox forum7CommentsDropDown = (ComboBox) scene.lookup("#commentsComboBox");
                List<String> comments = null;
                try {
                    comments = online.downloadCommentsForum7();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                ObservableList<String> observableComments = FXCollections.observableList(comments);
                forum7CommentsDropDown.setItems(observableComments);
                forum7CommentsDropDown.setOpacity(1);

                c_context.setOpacity(1);
                Button submitComm = (Button) scene.lookup("#submitComment");

                submitComm.setOpacity(1);

                submitComm.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        try {
                            String comment_context = c_context.getText();
                            c_context.setText("");

                            online.uploadComment(forum_id, comment_context, username, time_updated);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        });

        Hyperlink bus = (Hyperlink) scene.lookup("#business");
        bus.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                int forum_id = 8;
                titleLabel.setText("Business");

                ComboBox forum8CommentsDropDown = (ComboBox) scene.lookup("#commentsComboBox");
                List<String> comments = null;
                try {
                    comments = online.downloadCommentsForum8();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                ObservableList<String> observableComments = FXCollections.observableList(comments);
                forum8CommentsDropDown.setItems(observableComments);
                forum8CommentsDropDown.setOpacity(1);

                c_context.setOpacity(1);
                Button submitComm = (Button) scene.lookup("#submitComment");

                submitComm.setOpacity(1);

                submitComm.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        try {
                            String comment_context = c_context.getText();
                            c_context.setText("");

                            online.uploadComment(forum_id, comment_context, username, time_updated);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        });

        Hyperlink pe = (Hyperlink) scene.lookup("#pe");
        pe.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                int forum_id = 9;
                titleLabel.setText("Physical Education");

                ComboBox forum9CommentsDropDown = (ComboBox) scene.lookup("#commentsComboBox");
                List<String> comments = null;
                try {
                    comments = online.downloadCommentsForum9();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                ObservableList<String> observableComments = FXCollections.observableList(comments);
                forum9CommentsDropDown.setItems(observableComments);
                forum9CommentsDropDown.setOpacity(1);


                c_context.setOpacity(1);
                Button submitComm = (Button) scene.lookup("#submitComment");

                submitComm.setOpacity(1);

                submitComm.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        try {
                            String comment_context = c_context.getText();
                            c_context.setText("");

                            online.uploadComment(forum_id, comment_context, username, time_updated);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        });

        Hyperlink history = (Hyperlink) scene.lookup("#history");
        history.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                int forum_id = 10;
                titleLabel.setText("History");

                ComboBox forum10CommentsDropDown = (ComboBox) scene.lookup("#commentsComboBox");
                List<String> comments = null;
                try {
                    comments = online.downloadCommentsForum10();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                ObservableList<String> observableComments = FXCollections.observableList(comments);
                forum10CommentsDropDown.setItems(observableComments);
                forum10CommentsDropDown.setOpacity(1);

                c_context.setOpacity(1);
                Button submitComm = (Button) scene.lookup("#submitComment");

                submitComm.setOpacity(1);

                submitComm.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        try {
                            String comment_context = c_context.getText();
                            c_context.setText("");

                            online.uploadComment(forum_id, comment_context, username, time_updated);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        });

        Hyperlink geo = (Hyperlink) scene.lookup("#geography");
        geo.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                int forum_id = 11;
                titleLabel.setText("Geography");

                ComboBox forum11CommentsDropDown = (ComboBox) scene.lookup("#commentsComboBox");
                List<String> comments = null;
                try {
                    comments = online.downloadCommentsForum11();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                ObservableList<String> observableComments = FXCollections.observableList(comments);
                forum11CommentsDropDown.setItems(observableComments);
                forum11CommentsDropDown.setOpacity(1);

                c_context.setOpacity(1);
                Button submitComm = (Button) scene.lookup("#submitComment");

                submitComm.setOpacity(1);

                submitComm.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        try {
                            String comment_context = c_context.getText();
                            c_context.setText("");

                            online.uploadComment(forum_id, comment_context, username, time_updated);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        });

    }

    public void goClassruum(ActionEvent event) throws Exception {
        System.out.println(username);
        System.out.println(accountType);
        if (isInAClassruum() == false) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Classruum Error");
            alert.setHeaderText("No classruum to join!");
            alert.setContentText("You have to join a classruum first");
            alert.showAndWait();
            return;
        } else {
            if (accountType.equals("Scholar")) {
                onlineConnect = online.Connect();
                Parent dest = FXMLLoader.load(getClass().getResource("classruum_scholar.fxml"));
                Scene destScene = new Scene(dest);
                //This line gets the Stage information
                Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
                window.setScene(destScene);
                getClassruums(onlineConnect, username, destScene);
                window.show();
            } else {
                Parent dest = FXMLLoader.load(getClass().getResource("classruum_educator.fxml"));
                Scene destScene = new Scene(dest);
                //This line gets the Stage information
                Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
                window.setScene(destScene);
                Label Classtitle = (Label) destScene.lookup("#classtitle");
                PreparedStatement statement = onlineConnect.prepareStatement("SELECT class_name FROM classruums WHERE educator_username=?;");
                statement.setString(1, username);
                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    String value = rs.getString(1);
                    Classtitle.setText(value);
                }
                // DISPLAY DROP DOWN TO CHOOSE A RESOURCE TO UPLOAD
                // Lookup in the Scene for ComboBox, fetch items from DB and add them
                // Lookup in the Scene for ComboBox, fetch items from local DB and add them
                ComboBox uplDictDropDown = (ComboBox) destScene.lookup("#uplDictDrpDwn");
                List<Dictionary> dictionaries = locDB.allDictionaries();
                ObservableList<Dictionary> observableDicts = FXCollections.observableList(dictionaries);
                uplDictDropDown.setItems(observableDicts);
                // Updates item in ComboBox to show only their title instead of their full instance
                Callback<ListView<Dictionary>, ListCell<Dictionary>> factory = lv -> new ListCell<Dictionary>() {
                    @Override
                    protected void updateItem(Dictionary item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? "" : item.getTitle());
                    }
                };
                uplDictDropDown.setCellFactory(factory);
                uplDictDropDown.setButtonCell(factory.call(null));
                // Lookup in the Scene for ComboBox, fetch items from local DB and add them
                ComboBox uplNoteDropDown = (ComboBox) destScene.lookup("#uplNoteDrpDwn");
                List<Note> notes = locDB.allNotes();
                ObservableList<Note> observableNotes = FXCollections.observableList(notes);
                uplNoteDropDown.setItems(observableNotes);
                // Updates item in ComboBox to show only their title instead of their full instance
                Callback<ListView<Note>, ListCell<Note>> factory2 = lv -> new ListCell<Note>() {
                    @Override
                    protected void updateItem(Note item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? "" : item.getTitle());
                    }
                };
                uplNoteDropDown.setCellFactory(factory2);
                uplNoteDropDown.setButtonCell(factory2.call(null));
                window.show();
                //Upload All Resources When the File is Closed
                window.setOnCloseRequest((WindowEvent ev) ->
                {
                    try {
                        logoutSync(event);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    public void goClassruumScholar(ActionEvent event) throws IOException
    {

        Parent dest = FXMLLoader.load(getClass().getResource("classruum_scholar.fxml"));
        Scene destScene = new Scene(dest);
        //This line gets the Stage information
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        window.setScene(destScene);
        window.show();

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    public void goClassruumEducator(ActionEvent event) throws IOException
    {

        Parent dest = FXMLLoader.load(getClass().getResource("classruum_educator.fxml"));
        Scene destScene = new Scene(dest);
        //This line gets the Stage information
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        window.setScene(destScene);
        window.show();

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {

            try
            {

                logoutSync(event);

            }
            catch (IOException e)
            {

                e.printStackTrace();

            }

        });

    }

    public void Scholarselected(MouseEvent event) throws IOException
    {

        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        ImageView scholar = (ImageView) scene.lookup("#scholarunselect");
        ImageView educator = (ImageView) scene.lookup("#educatorunselect");
        Label scholartxt = (Label) scene.lookup("#scholartxt");
        Label educatortxt = (Label) scene.lookup("#educatortxt");

        if(scholar.getOpacity()==0)
        {

            scholar.setOpacity(1);
            scholartxt.setStyle("-fx-font-weight: normal");
            accountType="";

        }
        else
        {

            scholar.setOpacity(0);
            scholartxt.setStyle("-fx-font-weight: bold");
            accountType="Scholar";
            if (educator.getOpacity()==0)
            {

                educator.setOpacity(1);
                educatortxt.setStyle("-fx-font-weight: normal");

            }

        }

    }

    public void Educatorselected(MouseEvent event) throws IOException
    {

        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        ImageView educator = (ImageView) scene.lookup("#educatorunselect");
        ImageView scholar = (ImageView) scene.lookup("#scholarunselect");
        Label educatortxt = (Label) scene.lookup("#educatortxt");
        Label scholartxt = (Label) scene.lookup("#scholartxt");

        if (educator.getOpacity()==0)
        {

            educator.setOpacity(1);
            educatortxt.setStyle("-fx-font-weight: normal");
            accountType="";

        }
        else
        {

            educator.setOpacity(0);
            educatortxt.setStyle("-fx-font-weight: bold");
            accountType="Educator";
            if(scholar.getOpacity()==0)
            {

                scholar.setOpacity(1);
                scholartxt.setStyle("-fx-font-weight: normal");

            }

        }

    }

    public void ClassInviteButton(ActionEvent event)throws Exception{
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();
        OnlineSync online = new OnlineSync();
        Connection onlineConnect = online.Connect();
        PreparedStatement statement = onlineConnect.prepareStatement("SELECT class_id FROM classruums WHERE educator_username=?;");
        statement.setString(1,username);
        ResultSet rs = statement.executeQuery();
        rs.next();
        int class_id=rs.getInt(1);
        TextField username = (TextField) scene.lookup("#uname");
        Label warning = (Label) scene.lookup("#warning");
        Label sent = (Label) scene.lookup("#sent");
        sent.setVisible(false);
        warning.setVisible(false);
        String invitee = username.getText();
        if(online.username_exist(onlineConnect, invitee)){
            online.inviteClassruum(onlineConnect, invitee, class_id);
            sent.setVisible(true);
        }
        else{
            warning.setVisible(true);
        }
    }

    public void createClassruum(ActionEvent event) throws SQLException, IOException {
        onlineConnect = online.Connect();
        TextInputDialog titleDialog = new TextInputDialog();
        titleDialog.setTitle("Enter the Classruum title");
        titleDialog.setHeaderText("What is the Classruum title?");
        titleDialog.setContentText("Please enter the classruum name:");
        TextInputDialog descDialog = new TextInputDialog();
        descDialog.setTitle("Enter the Classruum description");
        descDialog.setHeaderText("What is the Classruum description?");
        descDialog.setContentText("Please enter the classruum description:");
        String class_title;
        String class_desc;
        Optional<String> titleResult = titleDialog.showAndWait();
        if (titleResult.isPresent()){
            class_title = titleResult.get();
            Optional<String> descResult = descDialog.showAndWait();
            if (descResult.isPresent()) {
                class_desc = descResult.get();
                online.uploadClassruum(onlineConnect, class_title, class_desc, username);
                //changes start
                Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
                Scene scene = window.getScene();
                Button createButton = (Button) scene.lookup("#createBtn");
                Label createLabel = (Label) scene.lookup("#createLbl");
                createButton.setDisable(true);
                createButton.setVisible(false);
                createLabel.setText("Created!");
                //changes end
            }
        }

        // Get Stage and Scene info
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        window.show();

        //Upload All Resources When the File is Closed
        window.setOnCloseRequest((WindowEvent ev) ->
        {
            try
            {
                logoutSync(event);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        });
    }

    // to be called when the educator clicks the upload resource button
    public void uploadResource(ActionEvent event) {
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();
        ComboBox resourceDropDown = (ComboBox) scene.lookup("#classDrpDwn");
        // check if they have selected a resource to upload
        if(resourceDropDown.getSelectionModel().isEmpty()){
            // show error
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No resource selected");
            alert.setHeaderText("No resources selected to be uploaded..");
            alert.setContentText("You haven't selected a resource to upload");
            alert.showAndWait();
        } else {
            // add scholars as resource owner to that resource
            Resource selected = (Resource) resourceDropDown.getSelectionModel().getSelectedItem();
            int resourceId = selected.getResourceID();
            try {
                online.updateResourceOwners(onlineConnect, resourceId, username);
            } catch (SQLException ex) {
                System.out.println("Error Connecting: " + ex);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void getClassruums(Connection onlineConnect, String username, Scene scene)throws SQLException{
        int class_id;
        PreparedStatement statement = onlineConnect.prepareStatement("SELECT class_id FROM class_member WHERE member_name=?;");
        statement.setString(1,username);
        ResultSet rs = statement.executeQuery();
        PreparedStatement getClassName = onlineConnect.prepareStatement("SELECT class_name FROM classruums WHERE class_id=?");
        while(rs.next()){
            class_id=rs.getInt(1);
            getClassName.setInt(1,class_id);
            ResultSet rs2 = getClassName.executeQuery();
            rs2.next();
            String className = rs2.getString(1);
            setClassruums(scene, className);
        }
    }

    public void setClassruums(Scene scene, String className){
        VBox vbox = (VBox) scene.lookup("#classbox");
        Hyperlink classLink = new Hyperlink(className);
        classLink.setOnAction(e -> {
            try {
                ClassruumScene(e);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        vbox.getChildren().add(classLink);
    }

    public void ClassruumScene(javafx.event.ActionEvent event)throws SQLException{
        OnlineSync online = new OnlineSync();
        Connection onlineConnect = online.Connect();
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();
        Hyperlink classruum = (Hyperlink)event.getSource();
        Label class_name = (Label) scene.lookup("#className");
        TextArea description = (TextArea) scene.lookup("#description");
        class_name.setText(classruum.getText());
        class_name.setVisible(true);
        PreparedStatement statement = onlineConnect.prepareStatement("SELECT educator_username FROM classruums WHERE class_name=?;");
        statement.setString(1,classruum.getText());
        ResultSet rs = statement.executeQuery();
        rs.next();
        String educator = rs.getString(1);
        rs.close();
        PreparedStatement getResources = onlineConnect.prepareStatement("SELECT class_description FROM classruums WHERE class_name=?;");
        getResources.setString(1,classruum.getText());
        rs = getResources.executeQuery();
        rs.next();
        description.setText(rs.getString(1));
    }

    public void getClassResources(ActionEvent event){
        int resourceID;
        Connection offlineConnect = null;
        PreparedStatement noteStatement = null;
        PreparedStatement dictionaryStatement = null;
        ResultSet noteResult = null;
        ResultSet dictionaryResult = null;
        String title;
        String content;
        String dictionaryName;
        String className;

        try
        {
            Class.forName("org.sqlite.JDBC");
            offlineConnect = DriverManager.getConnection("jdbc:sqlite:StudioruumDB.sqlite");
            onlineConnect = online.Connect();
            Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
            Scene scene = window.getScene();
            Label added = (Label) scene.lookup("#added");
            className = ((Label)scene.lookup("#className")).getText()+": ";
            added.setVisible(true);
            try{
                PreparedStatement statement = onlineConnect.prepareStatement("SELECT resource_id FROM resource_owner WHERE owner=?;");
                statement.setString(1,username);
                ResultSet rs = statement.executeQuery();
                while(rs.next()){
                    resourceID = rs.getInt(1);
                    noteStatement = onlineConnect.prepareStatement("SELECT * FROM notes WHERE resource_id = ?;");
                    noteStatement.setInt(1, resourceID);
                    dictionaryStatement = onlineConnect.prepareStatement("SELECT * FROM dictionaries WHERE resource_id = ?;");
                    dictionaryStatement.setInt(1, resourceID);
                    noteResult = noteStatement.executeQuery();
                    dictionaryResult = dictionaryStatement.executeQuery();
                    if(noteResult.next()) {
                        title = className+noteResult.getString("note_title");
                        content = noteResult.getString("note_content");
                        locDB.saveNote(title, content);
                    }
                    else if(dictionaryResult.next()) {
                        dictionaryName = className+dictionaryResult.getString("dictionary_name");
                        locDB.saveDictionary(dictionaryName);
                    }
                }
            }catch (SQLException ex){
                System.out.println(ex);
            }
        }
        catch(Exception ex)
        {
            System.out.println("Error Connecting to Offline DB: " + ex.getMessage());
        }
        
    }

    // to be called when the educator clicks the upload resource button
    public void uploadNote(ActionEvent event) {
        // check if they have selected a resource to upload
        onlineConnect = online.Connect();
        // Get Stage and Scene info
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        ComboBox uplNoteDropDown = (ComboBox) scene.lookup("#uplNoteDrpDwn");
        Note selected = (Note) uplNoteDropDown.getSelectionModel().getSelectedItem();
        System.out.println(selected);

        if(selected == null){
            // show error
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No resource selected");
            alert.setHeaderText("No resources selected to be uploaded..");
            alert.setContentText("You haven't selected a resource to upload");
            alert.showAndWait();
        } else {
            // add scholars as resource owner to that resource
            int resource_ID = selected.getResourceID();
            System.out.println(resource_ID);
            try {
                online.updateResourceOwners(onlineConnect, resource_ID, username);
            } catch (SQLException ex) {
                System.out.println("Error Connecting: " + ex);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // to be called when the educator clicks the upload resource button
    public void uploadDict(ActionEvent event)
    {

        // check if they have selected a resource to upload
        onlineConnect = online.Connect();
        // Get Stage and Scene info
        Stage window = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene scene = window.getScene();

        ComboBox uplDictDropDown = (ComboBox) scene.lookup("#uplDictDrpDwn");

        Dictionary selected = (Dictionary) uplDictDropDown.getSelectionModel().getSelectedItem();
        System.out.println(selected);

        if(selected == null) {
            // show error
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No resource selected");
            alert.setHeaderText("No resources selected to be uploaded..");
            alert.setContentText("You haven't selected a resource to upload");
            alert.showAndWait();
        }
        else
        {

            // add scholars as resource owner to that resource

            int resource_ID = selected.getResourceID();

            try
            {

                online.updateResourceOwners(onlineConnect, resource_ID, username);

            }

            catch (Exception ex)
            {

                System.out.println("Error Connecting to Online DB: " + ex);

            }

        }

    }

}