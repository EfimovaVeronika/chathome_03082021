package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class RenameController {
    @FXML
    private TextField loginField;

    @FXML
    private TextField nicknameField;

    @FXML
    private TextArea textArea;

    private Controller controller;

    public void setController(Controller controller) {
        this.controller = controller;
    }

    @FXML
    public void tryToRename(ActionEvent actionEvent) {

        String nickname = nicknameField.getText().trim();


        if (nickname.equals("")) {
            textArea.appendText("Поле должно быть не пустым\n");
            return;
        }

        if (nickname.contains(" ")) {
            textArea.appendText("Никнейм не должн содержать пробелы\n");
            return;
        }
        controller.setTempNickName(nickname);
        controller.renameNickName (nickname);

    }

    public void renameResult(String msg) {
        textArea.appendText(msg + "\n");
    }

}

