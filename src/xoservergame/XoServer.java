/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xoservergame;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.control.Alert;

/**
 *
 * @author ahmed
 */
public class XoServer {

    private static ServerSocket server;
    private static XoServer instance;
    public volatile static DatabaseProcess db;
    private static volatile ConcurrentHashMap<String, PrintWriter> userOut = new ConcurrentHashMap<>();
    private static volatile ConcurrentHashMap<String, BufferedReader> userIn = new ConcurrentHashMap<>();
    private static volatile ConcurrentHashMap<String, String> terminate = new ConcurrentHashMap<>();
    private static volatile boolean runing = true;

    private XoServer() {
        db = DatabaseProcess.getInstance();
        try {

            server = new ServerSocket(5005);

        } catch (IOException ex) {
            Logger.getLogger(XoServer.class.getName()).log(Level.SEVERE, null, ex);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (runing) {
                    try {

                        Socket ss = server.accept();
                        new Thread(new clientHandler(ss)).start();

                    } catch (IOException e) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("Information Dialog");
                                alert.setHeaderText("Look, an Information Dialog");
                                alert.setContentText("force close server may case problem wirh users");
                                alert.showAndWait();

                            }
                        });

                    }
                }
            }
        }).start();

    }

    public static XoServer getInstance() {
        if (instance == null) {
            return new XoServer();
        }
        return instance;
    }

    class clientHandler implements Runnable {

        Socket socket;

        public clientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {

            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                PrintWriter out = new PrintWriter(
                        socket.getOutputStream());
                String req;
                String currentUser = null;
                String password;
                String rule;
                while (runing) {

                    String request = in.readLine();
                    StringTokenizer st = new StringTokenizer(request);
                    req = st.nextToken();
                    if (st.hasMoreTokens()) {
                        currentUser = st.nextToken();
                        password = st.nextToken();
                        if (req.equals("singin")) {
                            if (db.SignIn(currentUser, password)) {
                                if (!db.isOnline(currentUser)) {
                                    out.println("true");
                                    out.flush();
                                    break;
                                } else {
                                    out.println("false Acouunt is already singed in");
                                    out.flush();
                                }

                            } else {
                                out.println("false Wrong username or password");
                                out.flush();
                            }
                        } else if (req.equals("singup")) {
                            if (db.SignUp(currentUser, password)) {
                                out.println("true");
                                out.flush();
                                break;
                            } else {
                                out.println("false");
                                out.flush();
                            }
                        } else if (req.equals("forget")) {
                            if (db.updatePassword(currentUser, password)) {
                                out.println("true");
                                out.flush();
                            } else {
                                out.println("false");
                                out.flush();
                            }

                        }
                    } else {
                        return;
                    }

                }
                userOut.put(currentUser, out);
                userIn.put(currentUser, in);
                db.updateUserAvailabelty(currentUser, true);
                db.updateUserState(currentUser, true);
                PrintWriter otherOut;
                BufferedReader otherIN;
                while (runing) {
                   
                    StringBuilder sb = new StringBuilder();
                    sb.append("(online-list) ");
                    for (String st : db.getOnlineUsers()) {
                        if (!st.equals(currentUser)) {
                            if (!db.isAvailable(st)) {
                                sb.append(st).append(",").append("Score:").append(db.getScore(st)).append(",(In-Game) ");
                            } else {
                                sb.append(st).append(",").append("Score:").append(db.getScore(st)).append(" ");
                            }
                        }
                    }
                    sb.append("score|" + currentUser + "|" + db.getScore(currentUser));
                    out.println(sb.toString());
                    out.flush();
                    if (in.ready()) {
                        rule = in.readLine();
                        if (rule == null || rule.equals("exit")) {
                            out.close();
                            out.close();
                            db.updateUserState(currentUser, false);
                            db.updateUserAvailabelty(currentUser, false);
                            return;
                        } else if (rule.equals("history")) {
                            out.println("history" + db.getHistory(currentUser));
                            out.flush();
                        } else if (rule.contains("ok")) {
                            while (true) {
                                if (terminate.containsKey(currentUser)) {
                                    String s = terminate.get(currentUser);
                                    terminate.remove(currentUser);
                                    if (s.equals("return")) {
                                        out.close();
                                        in.close();
                                        db.updateUserState(currentUser, false);
                                        db.updateUserAvailabelty(currentUser, false);
                                        return;
                                    } else {
                                        userOut.put(currentUser, out);
                                        userIn.put(currentUser, in);
                                        db.updateUserAvailabelty(currentUser, true);
                                        break;
                                    }
                                }

                            }
                            
                        } else if (rule.contains("play")) {
                            String st[] = rule.split(" ");
                            if (st.length > 1) {
                                String otherUser = st[1];
                                if (userOut.containsKey(otherUser)) {
                                    otherOut = userOut.get(otherUser);
                                    otherIN = userIn.get(otherUser);
                                    otherOut.println("play request from " + currentUser);
                                    otherOut.flush();
                                    rule = otherIN.readLine();
                                    if (rule == null || rule.equals("exit")) {
                                        userOut.remove(otherUser).close();
                                        userIn.remove(otherUser).close();
                                        db.updateUserAvailabelty(otherUser, false);
                                        db.updateUserState(otherUser, false);
                                        out.println("no");
                                        out.flush();
                                    } else if (rule.equals("ok")) {
                                        userOut.remove(otherUser);
                                        userIn.remove(otherUser);
                                        userOut.remove(currentUser);
                                        userIn.remove(currentUser);
                                        db.updateUserAvailabelty(otherUser, false);
                                        db.updateUserAvailabelty(currentUser, false);
                                        out.println("playx " + db.getScore(otherUser));
                                        otherOut.println("playo " + db.getScore(currentUser));
                                        out.flush();
                                        otherOut.flush();
                                        String userOption;
                                        while (runing) {
                                            if (in.ready()) {
                                                userOption = in.readLine();
                                                if (userOption == null || userOption.equals("exit")) {
                                                    db.updateScore(db.getScore(otherUser) + 10, otherUser);
                                                    terminate.put(otherUser, "break");
                                                    db.saveGame(currentUser, otherUser, otherUser);
                                                    otherOut.println("other player exit");
                                                    otherOut.flush();
                                                    out.println("other player exit");
                                                    out.flush();
                                                    return;
                                                } else if (userOption.equals("win")) {
                                                    db.updateScore(db.getScore(currentUser) + 10, currentUser);
                                                    terminate.put(otherUser, "break");
                                                    db.saveGame(currentUser, otherUser, currentUser);
                                                    otherOut.println("other player exit");
                                                    otherOut.flush();
                                                    out.println("other player exit");
                                                    out.flush();
                                                    break;
                                                } else if (userOption.equals("back")) {
                                                    db.updateScore(db.getScore(otherUser) + 10, otherUser);
                                                    terminate.put(otherUser, "break");
                                                    db.saveGame(currentUser, otherUser, otherUser);
                                                    otherOut.println("other player exit");
                                                    otherOut.flush();
                                                    out.println("other player exit");
                                                    out.flush();
                                                    break;
                                                } else if (userOption.equals("draw")) {
                                                    terminate.put(otherUser, "break");
                                                    db.saveGame(currentUser, otherUser, "draw");
                                                    otherOut.println("other player exit");
                                                    otherOut.flush();
                                                    out.println("other player exit");
                                                    out.flush();
                                                    break;
                                                }
                                                otherOut.println(userOption);
                                                otherOut.flush();

                                            }
                                            if (otherIN.ready()) {
                                                userOption = otherIN.readLine();
                                                if (userOption == null || userOption.equals("exit")) {
                                                    db.updateScore(db.getScore(currentUser) + 10, currentUser);
                                                    terminate.put(otherUser, "return");
                                                    db.saveGame(currentUser, otherUser, currentUser);
                                                    otherOut.println("other player exit");
                                                    otherOut.flush();
                                                    out.println("other player exit");
                                                    out.flush();
                                                    break;
                                                } else if (userOption.equals("win")) {
                                                    db.updateScore(db.getScore(otherUser) + 10, otherUser);
                                                    db.saveGame(currentUser, otherUser, otherUser);
                                                    terminate.put(otherUser, "break");
                                                    otherOut.println("other player exit");
                                                    otherOut.flush();
                                                    out.println("other player exit");
                                                    out.flush();
                                                    break;
                                                } else if (userOption.equals("back")) {
                                                    db.updateScore(db.getScore(currentUser) + 10, currentUser);
                                                    db.saveGame(currentUser, otherUser, currentUser);
                                                    terminate.put(otherUser, "break");
                                                    otherOut.println("other player exit");
                                                    otherOut.flush();
                                                    out.println("other player exit");
                                                    out.flush();
                                                    break;
                                                } else if (userOption.equals("draw")) {
                                                    terminate.put(otherUser, "break");
                                                    db.saveGame(currentUser, otherUser, "draw");
                                                    otherOut.println("other player exit");
                                                    otherOut.flush();
                                                    out.println("other player exit");
                                                    out.flush();
                                                    break;
                                                }
                                                out.println(userOption);
                                                out.flush();

                                            }
                                        }
                                        userOut.put(currentUser, out);
                                        userIn.put(currentUser, in);
                                        db.updateUserAvailabelty(currentUser, true);
                                    }

                                }
                            }
                        } else if (rule.contains("save")) {
                            System.out.println(rule);
                            db.saveRecord(currentUser, rule.substring(4));

                        } else if (rule.contains("records")) {
                            System.out.println(db.getRecords(currentUser));
                             out.println("records," + db.getRecords(currentUser));
                             out.flush();

                        } else {
                            out.println("no");
                            out.flush();

                        }
                    }

                }

            } catch (IOException e) {
                Logger.getLogger(XoServer.class.getName()).log(Level.SEVERE, null, e);
            }

        }

    }

    public String getIP() {
        String ip = null;
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ip = socket.getLocalAddress().getHostAddress();
        } catch (UnknownHostException | SocketException ex) {
            Logger.getLogger(XoServer.class.getName()).log(Level.SEVERE, null, ex);
        }

        return ip;
    }

    public static void closeServer() {

        if (server != null) {
            try {
                runing = false;
                server.close();
                for (String s : userIn.keySet()) {
                    userIn.remove(s).close();
                    userOut.remove(s).close();
                }

                db.setAllUserOffline();
            } catch (IOException ex) {
                Logger.getLogger(XoServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void startServer() {
        runing = true;
    }
}
