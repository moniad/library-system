package pl.wietwioorki.to22019.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import org.springframework.stereotype.Controller;
import pl.wietwioorki.to22019.model.ReservationStatus;

@Controller
public class NotificationSettingsController extends AbstractWindowController {

    @FXML
    public CheckBox readyBook;

    @FXML
    public CheckBox overdueBook;

    @FXML
    public CheckBox newReservation;

    @FXML
    public CheckBox borrowedBook;

    @FXML
    public CheckBox returnedBook;

    @FXML
    private void initialize() {
        readyBook.setSelected(sessionConstants.getCurrentUser().getNotificationSettings().get(ReservationStatus.READY));
        overdueBook.setSelected(sessionConstants.getCurrentUser().getNotificationSettings().get(ReservationStatus.OVERDUE));
        newReservation.setSelected(sessionConstants.getCurrentUser().getNotificationSettings().get(ReservationStatus.PENDING));
        borrowedBook.setSelected(sessionConstants.getCurrentUser().getNotificationSettings().get(ReservationStatus.ACTIVE));
        returnedBook.setSelected(sessionConstants.getCurrentUser().getNotificationSettings().get(ReservationStatus.RETURNED));
    }

    @FXML
    public void handleReadyBook(ActionEvent actionEvent) {
        sessionConstants.getCurrentUser().changeReadyBook();
        sessionConstants.getUserRepository().save(sessionConstants.getCurrentUser());
    }

    @FXML
    public void handleOverdueBook(ActionEvent actionEvent) {
        sessionConstants.getCurrentUser().changeOverdueBook();
        sessionConstants.getUserRepository().save(sessionConstants.getCurrentUser());
    }

    @FXML
    public void handleNewReservation(ActionEvent actionEvent) {
        sessionConstants.getCurrentUser().changeNewReservation();
        sessionConstants.getUserRepository().save(sessionConstants.getCurrentUser());
    }

    @FXML
    public void handleBorrowedBook(ActionEvent actionEvent) {
        sessionConstants.getCurrentUser().changeBorrowedBook();
        sessionConstants.getUserRepository().save(sessionConstants.getCurrentUser());
    }

    @FXML
    public void handleReturnedBook(ActionEvent actionEvent) {
        sessionConstants.getCurrentUser().changeReturnedBook();
        sessionConstants.getUserRepository().save(sessionConstants.getCurrentUser());
    }
}
