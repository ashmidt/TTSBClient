package socketclientfx;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import socketfx.Constants;
import socketfx.FxSocketClient;
import socketfx.SocketListener;

/**
 * FXML Controller class
 *
 * @author jtconnor
 */
public class FXMLDocumentController implements Initializable {
    @FXML
    private TextField playerOneTxtField;
    @FXML
    private TextField playerTwoTxtField;
    @FXML
    private Button connectButton;
    @FXML
    private Label connectedLabel;
    @FXML
    private Label counterLeftLabel;
    @FXML
    private Label counterRightLabel;

    private final static Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    private boolean connected, isSwitched = false;

    private static final int DEFAULT_RETRY_INTERVAL = 2000; // in milliseconds

    public enum ConnectionDisplayState {
        DISCONNECTED, ATTEMPTING, CONNECTED, AUTOCONNECTED, AUTOATTEMPTING
    }
    
    public enum Cmd {
        LUP, LDOWN, RUP, RDOWN, RESET, SWITCH, SCHANGE, NEXT, GAME11, GAME21, PLLEFT, PLRIGHT
    }

    private FxSocketClient socket;

    /*
     * Synchronized method set up to wait until there is no socket connection.
     * When notifyDisconnected() is called, waiting will cease.
     */
    private synchronized void waitForDisconnect() {
        while (connected) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
    }

    /*
     * Synchronized method responsible for notifying waitForDisconnect()
     * method that it's OK to stop waiting.
     */
    private synchronized void notifyDisconnected() {
        connected = false;
        notifyAll();
    }

    /*
     * Synchronized method to set isConnected boolean
     */
    private synchronized void setIsConnected(boolean connected) {
        this.connected = connected;
    }

    /*
     * Synchronized method to check for value of connected boolean
     */
    private synchronized boolean isConnected() {
        return (connected);
    }

    private void connect() {
        socket = new FxSocketClient(new FxSocketListener(), "localhost", Integer.valueOf("55555"), Constants.instance().DEBUG_NONE);
        socket.connect();
    }

    private void autoConnect() {
        new Thread() {
            @Override
            public void run() { 
                   if (!isConnected()) {
                        socket = new FxSocketClient(new FxSocketListener(), "localhost", 55555, Constants.instance().DEBUG_NONE);
                        socket.connect();
                    }
                    waitForDisconnect();
                    try {
                        Thread.sleep(2000 * 1000);
                    } catch (InterruptedException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                    }
            }
        }.start();
    }

    private void displayState(ConnectionDisplayState state) {
        switch (state) {
            case DISCONNECTED:
                connectButton.setDisable(false);
                connectedLabel.setText("Disconnected");
                break;
            case ATTEMPTING:
            case AUTOATTEMPTING:
                connectButton.setDisable(true);
                connectedLabel.setText("Connecting");
                break;
            case CONNECTED:
                connectButton.setDisable(true);
                connectedLabel.setText("Connected");
                break;
            case AUTOCONNECTED:
                connectButton.setDisable(true);
                connectedLabel.setText("Connected");
                break;
        }
    }

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setIsConnected(false);
        displayState(ConnectionDisplayState.DISCONNECTED);

        Runtime.getRuntime().addShutdownHook(new ShutDownThread());
    }

    class ShutDownThread extends Thread {
        @Override
        public void run() {
            if (socket != null) {
                if (socket.debugFlagIsSet(Constants.instance().DEBUG_STATUS)) {
                    LOGGER.info("ShutdownHook: Shutting down Server Socket");
                }
                socket.shutdown();
            }
        }
    }

    class FxSocketListener implements SocketListener {

        @Override
        public void onMessage(String line) {
            if (line != null && !line.equals("")) {
                //rcvdMsgsData.add(line);
                LOGGER.info(line);
                UpdateScore(line);
            }
        }

        @Override
        public void onClosedStatus(boolean isClosed) {
            if (isClosed) {
                notifyDisconnected();
                displayState(ConnectionDisplayState.DISCONNECTED);
            } else {
                setIsConnected(true);
                displayState(ConnectionDisplayState.CONNECTED);
            }
        }
    }

    private void UpdateScore(String line) {
        String[] score, msg;
        if(line != null && line.contains(";")){
            msg = line.split(";");
            
            if(!msg[0].isEmpty() && msg[0].contains(":")){
                score = msg[0].split(":");
                if(score[0].equalsIgnoreCase("LP")){
                    counterLeftLabel.setText(score[1]);
                }
            }
            
            if(!msg[1].isEmpty() && msg[1].contains(":")){
                score = msg[1].split(":");
                if(score[0].equalsIgnoreCase("RP")){
                    counterRightLabel.setText(score[1]);
                }
            }
        }
    }
    
    private void UpdateScoreByPlayer(Cmd cmd) {
        socket.sendMessage(cmd.name());
    }
    
    @FXML
    private void handleLeftUpButton(ActionEvent event) {
        //socket.sendMessage(Cmd.LUP.name());
        if(isSwitched)
            UpdateScoreByPlayer(Cmd.RUP);
        else
            UpdateScoreByPlayer(Cmd.LUP);
    }
    
    @FXML
    private void handleLeftDownButton(ActionEvent event) {
        //socket.sendMessage(Cmd.LDOWN.name());
        if(isSwitched)
            UpdateScoreByPlayer(Cmd.RDOWN);
        else
            UpdateScoreByPlayer(Cmd.LDOWN);
    }
    
    @FXML
    private void handleRightUpButton(ActionEvent event) {
        //socket.sendMessage(Cmd.RUP.name());
        if(isSwitched)
            UpdateScoreByPlayer(Cmd.LUP);
        else
            UpdateScoreByPlayer(Cmd.RUP);
    }
    
    @FXML
    private void handleRightDownButton(ActionEvent event) {
        socket.sendMessage(Cmd.RDOWN.name());
        if(isSwitched)
            UpdateScoreByPlayer(Cmd.LDOWN);
        else
            UpdateScoreByPlayer(Cmd.RDOWN);
    }
    
    @FXML
    private void handleResetButton(ActionEvent event) {
        socket.sendMessage(Cmd.RESET.name());
    }
    
    @FXML
    private void handleNextButton(ActionEvent event) {
        socket.sendMessage(Cmd.NEXT.name());
    }
    
    @FXML
    private void handleOrderButton(ActionEvent event) {
        socket.sendMessage(Cmd.SCHANGE.name());
    }
    
    @FXML
    private void handleSwitchButton(ActionEvent event) {
        socket.sendMessage(Cmd.SWITCH.name());
        isSwitched = !isSwitched;
        
        if(isSwitched){
            Label lbl = counterLeftLabel;
            counterLeftLabel = counterRightLabel;
            counterRightLabel = lbl;
        }else{
            Label lbl = counterRightLabel;
            counterRightLabel = counterLeftLabel;
            counterLeftLabel = lbl;
        }
    }
    
    @FXML
    private void handleGame11Button(ActionEvent event) {
        socket.sendMessage(Cmd.GAME11.name());
    }
    
    @FXML
    private void handleGame21Button(ActionEvent event) {
        socket.sendMessage(Cmd.GAME21.name());
    }
    
    @FXML
    private void handleConnectButton(ActionEvent event) {
        displayState(ConnectionDisplayState.ATTEMPTING);
        connect();
    }

    @FXML
    private void handleDisconnectButton(ActionEvent event) {
        socket.shutdown();
    }
    
    @FXML
    private void handleUpdatePlayer1Name(ActionEvent event) {
        String name = playerOneTxtField.getText();
        socket.sendMessage(Cmd.PLLEFT.name() + ":" + name);
    }
    
    @FXML
    private void handleUpdatePlayer2Name(ActionEvent event) {
        String name = playerTwoTxtField.getText();
        socket.sendMessage(Cmd.PLRIGHT + ":" + name);
    }
}
