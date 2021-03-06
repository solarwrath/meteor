package Meteor.controllers;

import Meteor.Main;
import Meteor.core.DBHandler;
import Meteor.core.User;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;

import com.mysql.cj.exceptions.ConnectionIsClosedException;
import com.mysql.cj.jdbc.exceptions.CommunicationsException;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.Initializable;
import javafx.stage.Window;
import org.fxmisc.easybind.EasyBind;

import javafx.beans.binding.Binding;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.net.ConnectException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

import static java.util.Arrays.asList;

public class RegistrationFormController implements Initializable {

    private Scene thisScene = Main.registrationScene;

    @FXML
    protected AnchorPane registrationScreenParent;

    @FXML
    private JFXTextField loginField;

    @FXML
    private JFXPasswordField passwordField;

    @FXML
    private JFXTextField emailField;

    @FXML
    private JFXTextField fullNameField;

    @FXML
    private ToggleGroup genderField;

    @FXML
    private JFXRadioButton maleRadioButton;

    @FXML
    private JFXRadioButton femaleRadioButton;

    @FXML
    private ImageView loginImportantMarker;

    @FXML
    private Label loginImportantMessage;

    @FXML
    private Label loginImportantMessageDups;

    @FXML
    private ImageView passwordImportantMarker;

    @FXML
    private Label passwordImportantMessage;

    @FXML
    private ImageView emailImportantMarker;

    @FXML
    private Label emailImportantMessage;

    @FXML
    private Label emailImportantMessageDups;

    @FXML
    private ImageView fullNameImportantMarker;

    @FXML
    private Label fullNameImportantMessage;

    @FXML
    private JFXButton createAccountButton;

    @FXML
    private JFXButton alreadySignedUpButton;

    private Service<Void> backgroundRegistrationThread;

    private boolean executed = false;

    @FXML
    public void initialize(URL url, ResourceBundle resourceBundle) {
        createAccountButton.setOnAction(event -> {

            Binding<Boolean> bb = EasyBind.select(Main.registrationScene.getRoot().sceneProperty())
                    .select(Scene::windowProperty)
                    .selectObject(Window::showingProperty);

            bb.addListener((observable, oldValue, newValue) -> {
                try {
                    if (newValue) {
                        if (Main.registrationScene.getProperties().containsKey("displayErrorsRequired")) {
                            if ((Boolean) Main.registrationScene.getProperties().get("displayErrorsRequired")) {
                                try {
                                    displayErrors(createUserFromUI());
                                } catch (SQLException | ConnectException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } catch (NullPointerException e) {
                    System.out.println(e.getMessage() + "\n" + e.getCause());
                }
            });

            registerUser(event);
        });

        alreadySignedUpButton.setOnAction(event -> {
            Main.changeScene(Main.loginScene, (Stage) ((Node) event.getSource()).getScene().getWindow());
            registrationScreenParent.requestFocus();
        });
    }

    private void registerUser(Event event) {
        HashMap<String, Object> passedParameters = new HashMap<>();
        passedParameters.put("event", event);
        registerUser(passedParameters);
    }

    private void registerUser(HashMap<String, Object> reflectedArguments) {
        backgroundRegistrationThread = new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        Event event = (Event) reflectedArguments.get("event");

                        if (reflectedArguments.size() > 1) {
                            System.out.println("debug 2");
                            if (reflectedArguments.containsKey("user") && reflectedArguments.containsKey("lost_connection_stage")) {
                                User givenUser = (User) reflectedArguments.get("user");
                                try {
                                    if (givenUser.validateUser(givenUser).isEmpty()) {
                                        DBHandler.addUser(givenUser);
                                        Platform.runLater(() -> {
                                            Main.changeScene(thisScene, (Stage) reflectedArguments.get("lost_connection_stage"));
                                            ((Stage) reflectedArguments.get("lost_connection_stage")).requestFocus();
                                        });
                                    } else {
                                        Main.registrationScene.getProperties().put("displayErrorsRequired", true);
                                        Platform.runLater(() -> {
                                            Main.changeScene(thisScene, (Stage) reflectedArguments.get("lost_connection_stage"));
                                            ((Stage) reflectedArguments.get("lost_connection_stage")).requestFocus();
                                        });
                                    }
                                } catch (ConnectException | SQLException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            User givenUser = createUserFromUI();
                            try {
                                if (givenUser.validateUser(givenUser).isEmpty()) {
                                    DBHandler.addUser(givenUser);
                                    for (Node givenNode : new ArrayList<>(asList(loginImportantMessage, loginImportantMessageDups, loginImportantMarker, passwordImportantMarker, passwordImportantMessage, emailImportantMarker, emailImportantMessage, fullNameImportantMarker, fullNameImportantMessage))) {
                                        givenNode.setVisible(false);
                                    }
                                    for (Control givenControl : new ArrayList<Control>(asList(loginField, emailField, fullNameField))) {
                                        if (givenControl.getClass() == JFXTextField.class || givenControl.getClass() == JFXPasswordField.class) {
                                            ((JFXTextField) givenControl).setText("");
                                        }
                                    }
                                    passwordField.setText("");
                                    femaleRadioButton.setSelected(false);
                                    maleRadioButton.setSelected(true);

                                    Platform.runLater(() -> {
                                        Main.changeScene(Main.postRegistrationScene, (Stage) ((Node) event.getSource()).getScene().getWindow());
                                        registrationScreenParent.requestFocus();
                                    });

                                } else {
                                    displayErrors(givenUser);
                                }
                            } catch (CommunicationsException e) {
                                e.printStackTrace();
                                callLostConnectionScene(event, givenUser);
                            } catch (SQLException | ConnectionIsClosedException | ConnectException e) {
                                e.printStackTrace();
                            }
                        }
                        return null;
                    }
                };
            }
        };

        if (!executed) {
            backgroundRegistrationThread.start();
            executed = true;
        }
    }

    private void displayErrors(User givenUser) throws SQLException, ConnectException {


        ArrayList<String> listOfErrors = givenUser.validateUser(givenUser);

        Platform.runLater(() -> {
            switch (listOfErrors.get(0)) {
                case "username":
                case "username_already_taken":
                    loginField.requestFocus();
                    break;
                case "password":
                    passwordField.requestFocus();
                    break;
                case "email":
                case "email_already_taken":
                    emailField.requestFocus();
                    break;
                case "full_name":
                    fullNameField.requestFocus();
                    break;
            }
        });

        if (listOfErrors.contains("username")) {
            loginImportantMessage.setVisible(true);
        } else {
            loginImportantMessage.setVisible(false);
        }

        if (listOfErrors.contains("username_already_taken")) {
            loginImportantMessageDups.setVisible(true);
        } else {
            loginImportantMessageDups.setVisible(false);
        }


        if (listOfErrors.contains("username") || listOfErrors.contains("username_already_taken")) {
            loginImportantMarker.setVisible(true);
        } else {
            loginImportantMarker.setVisible(false);
        }

        if (listOfErrors.contains("password")) {
            passwordImportantMarker.setVisible(true);
            passwordImportantMessage.setVisible(true);
        } else {
            passwordImportantMarker.setVisible(false);
            passwordImportantMessage.setVisible(false);
        }

        if (listOfErrors.contains("email")) {
            emailImportantMessage.setVisible(true);
        } else {
            emailImportantMessage.setVisible(false);
        }

        if (listOfErrors.contains("email_already_taken")) {
            emailImportantMessageDups.setVisible(true);
        } else {
            emailImportantMessageDups.setVisible(false);
        }

        if (listOfErrors.contains("email") || listOfErrors.contains("email_already_taken")) {
            emailImportantMarker.setVisible(true);
        } else {
            emailImportantMarker.setVisible(false);
        }
        if (listOfErrors.contains("full_name")) {
            fullNameImportantMarker.setVisible(true);
            fullNameImportantMessage.setVisible(true);
        } else {
            fullNameImportantMarker.setVisible(false);
            fullNameImportantMessage.setVisible(false);
        }
        System.out.println("debug7");

    }

    private User createUserFromUI() {
        User.Gender givenGender;
        if (((JFXRadioButton) genderField.getSelectedToggle()).getText().equals("Male")) {
            givenGender = User.Gender.MALE;
        } else {
            givenGender = User.Gender.FEMALE;
        }
        return new User(loginField.getText().trim(), passwordField.getText().trim(), emailField.getText().trim(), fullNameField.getText().trim(), givenGender);
    }


    private void callLostConnectionScene(Event givenEvent, User givenUser) {
        Platform.runLater(() -> {
            try {
                Main.callLostConnectionScene(
                        (Stage) ((Node) givenEvent.getSource()).getScene().getWindow(),
                        getClass().getDeclaredMethod("registerUser", HashMap.class),
                        new HashMap<>() {{
                            put("event", givenEvent);
                            put("user", givenUser);
                        }},
                        this.getClass());
            } catch (NoSuchMethodException noSuchMethodException) {
                noSuchMethodException.printStackTrace();
            }
            registrationScreenParent.requestFocus();

        });
    }
}
