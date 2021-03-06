package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.apache.commons.io.input.ReversedLinesFileReader;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    private ListView<String> clientList;
    @FXML
    private TextArea textArea;
    @FXML
    private TextField textField;
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private HBox authPanel;
    @FXML
    private HBox msgPanel;

    private Socket socket;
    private final int PORT = 8189;
    private final String IP_ADDRESS = "localhost";
    private DataInputStream in;
    private DataOutputStream out;

    private boolean authenticated;
    private String nickname;
    private String login;
    private Stage stage;
    private Stage regStage;
    private Stage renameStage;
    private RegController regController;
    private RenameController renameController;
    private File history;
    private PrintWriter writer;
    private String tempNickName;

    public void setTempNickName(String tempNickName) {
        this.tempNickName = tempNickName;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
        msgPanel.setVisible(authenticated);
        msgPanel.setManaged(authenticated);
        clientList.setVisible(authenticated);
        clientList.setManaged(authenticated);

        if (!authenticated) {
            nickname = "";
        }
        setTitle(nickname);
        textArea.clear();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            stage = (Stage) textField.getScene().getWindow();
            stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    System.out.println("bye");
                    if (socket != null && !socket.isClosed()) {
                        try {
                            out.writeUTF("/end");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        });
        setAuthenticated(false);
    }

    private void connect() {
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    // ???????? ????????????????????????????
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                break;
                            }
                            if (str.startsWith("/authok")) {
                                nickname = str.split("\\s")[1];
                                setAuthenticated(true);
                                login = str.split("\\s")[2];
                                history = new File("history_" + login + ".txt");
                                if (!history.exists()) {
                                    history.createNewFile();
                                } else {
                                    ReversedLinesFileReader reader = new ReversedLinesFileReader(history, StandardCharsets.UTF_8);
                                    List<String> historyLastMessagesList = new ArrayList<>();
                                    String line = "";
                                    while ((line = reader.readLine()) != null && historyLastMessagesList.size() < 200) {
                                        historyLastMessagesList.add(line);
                                    }
                                    for (int i = historyLastMessagesList.size() - 1; i >= 0; i--) {
                                        textArea.appendText(historyLastMessagesList.get(i) + "\n");
                                    }
                                }

                                writer = new PrintWriter(new FileOutputStream(history, true), true);
                                break;
                            }
                            if (str.equals("/regok")) {
                                regController.regResult("?????????????????????? ???????????? ??????????????");
                            }
                            if (str.equals("/regno")) {
                                regController.regResult("?????????? ?????? ?????????????? ?????? ????????????");
                            }



                        } else {
                            textArea.appendText(str + "\n");
                        }
                    }
                    // ???????? ????????????
                    while (authenticated) {
                        String str = in.readUTF();
                        if (str.startsWith("/")) {

                            if (str.equals("/renameok")) {
                                renameController.renameResult("?????????? ???????????????? ???????????? ??????????????");
                                setTitle(tempNickName);
                            }
                            if (str.equals("/renameno")) {
                                renameController.renameResult("???? ?????????????? ???????????????? ??????????????");
                            }

                            if (str.equals("/end")) {
                                writer.close();
                                break;
                            }
                            if (str.startsWith("/clientlist ")) {
                                String[] token = str.split("\\s+");
                                Platform.runLater(() -> {
                                    clientList.getItems().clear();
                                    for (int i = 1; i < token.length; i++) {
                                        clientList.getItems().add(token[i]);
                                    }
                                });
                            }
                        } else {
                            textArea.appendText(str + "\n");
                            writer.println(str + "\n");

                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("disconnected");
                    setAuthenticated(false);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void sendMsg(ActionEvent actionEvent) {
        try {
            out.writeUTF(textField.getText());
            textField.clear();
            textField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void tryToAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) {
            connect();
        }

        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        String msg = String.format("/auth %s %s", login, password);

        try {
            out.writeUTF(msg);
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setTitle(String nickname) {
        Platform.runLater(() -> {
            if (!nickname.equals("")) {
                stage.setTitle(String.format("Home Chat[ %s ]", nickname));
            } else {
                stage.setTitle("Home Chat");
            }
        });
    }

    public void clientListClick(MouseEvent mouseEvent) {
        String receiver = clientList.getSelectionModel().getSelectedItem();
        textField.setText(String.format("/w %s ", receiver));
    }

    private void createRegWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/reg.fxml"));
            Parent root = fxmlLoader.load();
            regStage = new Stage();
            regStage.setTitle("Home Chat registration");
            regStage.setScene(new Scene(root, 600, 400));
            regController = fxmlLoader.getController();
            regController.setController(this);

            regStage.initStyle(StageStyle.UTILITY);
            regStage.initModality(Modality.APPLICATION_MODAL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showRegWindow(ActionEvent actionEvent) {
        if (regStage == null) {
            createRegWindow();
        }
        regStage.show();
    }

    private void createRenameWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/rename.fxml"));
            Parent root = fxmlLoader.load();
            renameStage = new Stage();
            renameStage.setTitle("Rename Nick Name Window");
            renameStage.setScene(new Scene(root, 600, 400));
            renameController = fxmlLoader.getController();
            renameController.setController(this);

            renameStage.initStyle(StageStyle.UTILITY);
            renameStage.initModality(Modality.APPLICATION_MODAL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void showRenameNickNameWindow(ActionEvent actionEvent) {
        if (renameStage == null) {
            createRenameWindow();
        }
        renameStage.show();
    }

    public void registration(String login, String password, String nickname) {
        String msg = String.format("/reg %s %s %s", login, password, nickname);

        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void renameNickName(String nickname) {
        String msg = String.format("/rename %s", nickname);

        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
