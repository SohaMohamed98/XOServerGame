/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xoservergame;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.PieChart.Data;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * FXML Controller class
 *
 * @author Alshaimaa
 */
public class ServerRunController implements Initializable {

    /**
     * Initializes the controller class.
     */
    @FXML
    private Label online;
    @FXML
    private Label offline;
    @FXML
    private Label active;
    @FXML
    private Button stopServer;
    @FXML
    private ListView<String> onlineList;
    @FXML
    private ListView<String> offlineList;
    @FXML
    private ListView<String> activeList;
    @FXML
    private Text onlineTotalText;
    @FXML
    private Text offlineTotalText;
    @FXML
    private Text activeTotalText;
    @FXML
    private Text ipText;
    @FXML
    private PieChart pieChart;
    volatile int onlineUser, offlineUser, activeUser;

    @FXML

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        XoServer server = XoServer.getInstance();
        ipText.setText(server.getIP());
        String style = pieChart.getStyle();
        pieChart.setMaxSize(800, 800);
        pieChart.setLabelLineLength(10);
        pieChart.setLegendSide(Side.LEFT);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    ObservableList<String> listOnline = FXCollections.observableArrayList(server.db.getOnlineUsers());
                    ObservableList<String> listOffline = FXCollections.observableArrayList(server.db.getOfflineUsers());
                    ObservableList<String> listActive = FXCollections.observableArrayList(server.db.getActiveUsers());

                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            onlineList.setItems(listOnline);
                            offlineList.setItems(listOffline);
                            activeList.setItems(listActive);
                        }
                    });

                    if (listOffline.size() != offlineUser || listOnline.size() != onlineUser || activeUser != listActive.size()) {
                        offlineUser = listOffline.size();
                        onlineUser = listOnline.size();
                        activeUser = listActive.size();
                        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                                new PieChart.Data("Offline", offlineUser),
                                new PieChart.Data("Online", onlineUser),
                                new PieChart.Data("Active", activeUser));
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                pieChart.setData(pieChartData);
                            }
                        });
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ServerRunController.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }
            }
        }).start();

        stopServer.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    stopServer.setDisable(true);
                    XoServer.closeServer();
                    Parent root = FXMLLoader.load(getClass().getResource("StartServer.fxml"));
                    Scene scene = new Scene(root);
                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    stage.setScene(scene);
                    stage.show();
                } catch (IOException ex) {
                    Logger.getLogger(ServerRunController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        // TODO
    }

}
