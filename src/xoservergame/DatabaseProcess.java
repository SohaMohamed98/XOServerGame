/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xoservergame;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;


public class DatabaseProcess {

    private ResultSet rsid;
    private static Connection con;
    private static DatabaseProcess db;

    private DatabaseProcess() {
    }

    public static DatabaseProcess getInstance() {
        if (db == null) {
            return new DatabaseProcess();
        }
        return db;
    }

    public synchronized static boolean init() {
        try {
            DriverManager.registerDriver(new ClientDriver());

            con = DriverManager.getConnection("jdbc:derby://localhost:1527/UserData", "xo", "xo");

        } catch (SQLException ex) {
            return false;
        }

        return true;
    }

    public synchronized boolean getState() {
        return con != null;
    }

    public synchronized User getUserByName(String userName) {
        User user = null;
        PreparedStatement pst;
        ResultSet rs;
        try {

            pst = con.prepareStatement("select * from USERDATA where USERNAME = ?");
            pst.setString(1, userName);
            rs = pst.executeQuery();
            if (rs.next()) {
                user = new User(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4));
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        return user;
    }

    public synchronized boolean SignIn(String userName, String password) {
        PreparedStatement pst;
        ResultSet rs;
        try {

            pst = con.prepareStatement("select * from USERDATA where USERNAME = ? AND PASSWORD = ?");
            pst.setString(1, userName);
            pst.setString(2, password);
            rs = pst.executeQuery();
            if (rs.next()) {
                return true;
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public synchronized boolean SignUp(String userName, String password) {
        try {
            PreparedStatement pst;
            ResultSet rs;

            pst = con.prepareStatement("select * from USERDATA where USERNAME = ?");
            pst.setString(1, userName);
            rs = pst.executeQuery();
            if (!rs.next()) {
                pst = con.prepareStatement("Insert Into USERDATA (USERNAME, PASSWORD, STATE,SCORE,AVAILABLE) VALUES (?,?,?,?,?)");
                if (userName != null && password != null
                        && !userName.trim().isEmpty()
                        && !password.trim().isEmpty()
                        && userName.length() < 19
                        && password.length() < 19) {

                    pst.setString(1, userName);
                    pst.setString(2, password);
                    pst.setBoolean(3, true);
                    pst.setInt(4, 0);
                    pst.setBoolean(5, true);
                    pst.execute();
                }
                return true;
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public synchronized int getScore(String userName) {
        PreparedStatement pst;
        ResultSet rs;
        try {
            pst = con.prepareStatement("select SCORE from USERDATA where USERNAME = ?");
            pst.setString(1, userName);
            rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseProcess.class.getName()).log(Level.SEVERE, null, ex);
        }

        return -1;
    }

    public synchronized boolean updateScore(int newScore, String userName) {

        PreparedStatement pst;
        try {
            pst = con.prepareStatement("UPDATE USERDATA SET SCORE = ? where USERNAME=?");
            pst.setInt(1, newScore);
            pst.setString(2, userName);
            pst.execute();
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseProcess.class.getName()).log(Level.SEVERE, null, ex);

        }
        return false;
    }

    public synchronized ArrayList<String> getOnlineUsers() {
        ArrayList<String> arr = new ArrayList<>();
        ResultSet rs;
        PreparedStatement pst;
        try {
            pst = con.prepareStatement("select USERNAME from USERDATA where STATE = ?");
            pst.setBoolean(1, true);
            rs = pst.executeQuery();
            while (rs.next()) {
                arr.add((rs.getString(1)));
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        return arr;
    }

    public synchronized ArrayList<String> getOfflineUsers() {
        ArrayList<String> arr = new ArrayList<>();
        ResultSet rs;
        PreparedStatement pst;
        try {
            pst = con.prepareStatement("select USERNAME from USERDATA where STATE = ? and AVAILABLE = ?");
            pst.setBoolean(1, false);
            pst.setBoolean(2, false);
            rs = pst.executeQuery();
            while (rs.next()) {
                arr.add((rs.getString(1)));
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        return arr;
    }

    public synchronized ArrayList<String> getActiveUsers() {
        ArrayList<String> arr = new ArrayList<>();
        ResultSet rs;
        PreparedStatement pst;
        try {
            pst = con.prepareStatement("select USERNAME from USERDATA where STATE = ? and AVAILABLE = ?");
            pst.setBoolean(1, true);
            pst.setBoolean(2, true);
            rs = pst.executeQuery();
            while (rs.next()) {
                arr.add((rs.getString(1)));
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        return arr;
    }

    public synchronized boolean updateUserState(String USERNAME, boolean STATE) {

        PreparedStatement pst;
        try {
            pst = con.prepareStatement("UPDATE USERDATA SET STATE = ? where USERNAME=?");
            pst.setBoolean(1, STATE);
            pst.setString(2, USERNAME);
            pst.execute();
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseProcess.class.getName()).log(Level.SEVERE, null, ex);

        }
        return false;
    }

    public synchronized boolean updateUserAvailabelty(String USERNAME, boolean AVAILABLE) {

        PreparedStatement pst;
        try {
            pst = con.prepareStatement("UPDATE USERDATA SET AVAILABLE = ? where USERNAME=?");
            pst.setBoolean(1, AVAILABLE);
            pst.setString(2, USERNAME);
            pst.execute();
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseProcess.class.getName()).log(Level.SEVERE, null, ex);

        }
        return false;
    }

    public synchronized boolean updatePassword(String USERNAME, String Password) {

        PreparedStatement pst;
        ResultSet rs;
        try {
            pst = con.prepareStatement("select * from USERDATA where USERNAME = ?");
            pst.setString(1, USERNAME);
            rs = pst.executeQuery();
            if (rs.next()) {
                pst = con.prepareStatement("UPDATE USERDATA SET PASSWORD = ? where USERNAME=?");
                pst.setString(1, Password);
                pst.setString(2, USERNAME);
                pst.execute();
                return true;
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseProcess.class.getName()).log(Level.SEVERE, null, ex);

        }
        return false;
    }

    public synchronized boolean isAvailable(String userName) {
        PreparedStatement pst;
        ResultSet rs;
        try {
            pst = con.prepareStatement("select AVAILABLE from USERDATA where USERNAME = ?");
            pst.setString(1, userName);
            rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getBoolean(1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseProcess.class.getName()).log(Level.SEVERE, null, ex);
        }

        return false;
    }

    public synchronized boolean isOnline(String userName) {
        PreparedStatement pst;
        ResultSet rs;
        try {
            pst = con.prepareStatement("select STATE from USERDATA where USERNAME = ?");
            pst.setString(1, userName);
            rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getBoolean(1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseProcess.class.getName()).log(Level.SEVERE, null, ex);
        }

        return false;
    }

    public synchronized void setAllUserOffline() {
        PreparedStatement pst;
        try {
            pst = con.prepareStatement("UPDATE USERDATA SET AVAILABLE = ? ,STATE = ?");
            pst.setBoolean(1, false);
            pst.setBoolean(2, false);
            pst.execute();

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseProcess.class.getName()).log(Level.SEVERE, null, ex);

        }

    }

    public synchronized String getHistory(String user) {
        StringBuilder sb = new StringBuilder();
        ResultSet rs;
        PreparedStatement pst;
        try {
            pst = con.prepareStatement("select PLAYER1,PLAYER2,WINNER from HISTORY where PLAYER1 = ? OR PLAYER2 = ?");
            pst.setString(1, user);
            pst.setString(2, user);
            rs = pst.executeQuery();
            while (rs.next()) {
                sb.append(",").append(rs.getString(1)).append(" ").append(rs.getString(2)).append(" ").append(rs.getString(3));
            }
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sb.toString();
    }

    public synchronized void saveGame(String player1, String player2, String winner) {
        try {
            PreparedStatement pst;

            pst = con.prepareStatement("Insert Into HISTORY (PLAYER1, PLAYER2, WINNER) VALUES (?,?,?)");
            pst.setString(1, player1);
            pst.setString(2, player2);
            pst.setString(3, winner);
            pst.execute();
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseProcess.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public synchronized void saveRecord(String Player, String RECORD) {
        try {
            PreparedStatement pst;

            pst = con.prepareStatement("Insert Into RECORDS (USERNAME, RECORD) VALUES (?,?)");
            pst.setString(1, Player);
            pst.setString(2, RECORD);
            pst.execute();
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseProcess.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public synchronized String getRecords(String Player) {
        StringBuilder sb = new StringBuilder();
        try {
            PreparedStatement pst;
            pst = con.prepareStatement("select RECORD from RECORDS where USERNAME = ?");
            pst.setString(1, Player);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                sb.append(rs.getString(1));
                while (rs.next()) {
                    sb.append("," + rs.getString(1));
                }
            }

        } catch (SQLException ex) {
            Logger.getLogger(DatabaseProcess.class.getName()).log(Level.SEVERE, null, ex);
        }

        return sb.toString();

    }
}
