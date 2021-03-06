package pl.wietwioorki.to22019.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import org.controlsfx.control.Rating;
import org.springframework.stereotype.Controller;
import pl.wietwioorki.to22019.model.*;
import pl.wietwioorki.to22019.util.AlertFactory;
import pl.wietwioorki.to22019.util.EmailUtil;

import java.text.ParseException;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static pl.wietwioorki.to22019.util.ErrorMessage.*;
import static pl.wietwioorki.to22019.util.InfoMessage.*;

@Controller
public class ReservationListController extends AbstractWindowController {

    enum FilterValue {
        BookTitle, BorrowDate, ReturnDate, Overdue, ReservationID
    }

    @FXML
    public Button cancelReservationFromReservationList;

    @FXML
    public Button borrowBookFromReservationList;

    @FXML
    public Button returnBookFromReservationList;

    @FXML
    private TableView<Reservation> reservationTable;

    @FXML
    private TableColumn<Reservation, Long> reservationId;

    @FXML
    private TableColumn<Reservation, Long> readerPesel;

    @FXML
    private TableColumn<Reservation, String> readerName;

    @FXML
    private TableColumn<Reservation, String> booksTittle;

    @FXML
    public TableColumn<Reservation, ReservationStatus> reservationStatus;

    @FXML
    private TableColumn<Reservation, Date> borrowingDate;

    @FXML
    private TableColumn<Reservation, Date> returnDate;

    @FXML
    private Text peselText;

    @FXML
    private TextField peselField;

    @FXML
    private ComboBox selectedFilter;

    @FXML
    private TextField filterField;

    @FXML
    private HBox dateFields;

    @FXML
    private DatePicker dateFrom;

    @FXML
    private DatePicker dateTo;

    @FXML
    public Rating rating;

    @FXML
    public Label msg;


    @FXML
    private void initialize() {
        reservationId.setCellValueFactory(dataValue -> dataValue.getValue().getReservationIdProperty());
        readerPesel.setCellValueFactory(dataValue -> dataValue.getValue().getReaderPeselProperty());
        readerName.setCellValueFactory(dataValue -> dataValue.getValue().getReaderNameProperty());
        booksTittle.setCellValueFactory(dataValue -> dataValue.getValue().getBooksTitleProperty());
        reservationStatus.setCellValueFactory(dataValue -> dataValue.getValue().getReservationStatusProperty());
        borrowingDate.setCellValueFactory(dataValue -> dataValue.getValue().getBorrowingDateProperty());
        returnDate.setCellValueFactory(dataValue -> dataValue.getValue().getReturnDateProperty());

        refreshWindow();

        selectedFilter.setItems(getFilterItems());

        selectedFilter.getSelectionModel().select(0);

        dateFields.setVisible(false);

        sessionConstants.events.AddListener(this);
    }

    private void refreshData() {
        reservationTable.setItems(InitializeFilters());
    }

    private boolean compareSelectedFilter(FilterValue expected) {
        return selectedFilter.getSelectionModel().getSelectedItem().equals(expected);
    }

    @FXML
    public void handleChangeSelectedFilter(ActionEvent actionEvent) {
        boolean filterFieldVisible = true;
        boolean dateFieldsVisible = false;

        if (compareSelectedFilter(FilterValue.Overdue)) {
            filterFieldVisible = false;
        }

        if (compareSelectedFilter(FilterValue.BorrowDate) || compareSelectedFilter(FilterValue.ReturnDate)) {
            dateFieldsVisible = true;
            filterFieldVisible = false;
        }

        filterField.setVisible(filterFieldVisible);
        dateFields.setVisible(dateFieldsVisible);
        refreshFilters();
        reservationTable.refresh();
    }

    @FXML
    public void handleBorrowBookFromReservationList(ActionEvent actionEvent) {
        Reservation reservation = reservationTable.getSelectionModel().getSelectedItem();
        if (reservation == null) {
            System.out.println("No reservation selected");
            AlertFactory.showAlert(Alert.AlertType.ERROR, generalErrorHeader + "borrowing book", noReservationSelectedErrorContent);
            return;
        }

        if (!reservation.getReservationStatus().equals(ReservationStatus.READY)) {
            System.out.println("Wrong reservation status: " + reservation.getReservationStatus());
            AlertFactory.showAlert(Alert.AlertType.ERROR, generalErrorHeader + "borrowing book", cannotReturnBookErrorContent);
            return;
        }

        reservation.borrowBook();
        sessionConstants.getReservationRepository().save(reservation);

        // for statistics - save counters of borrowed books
        Book borrowedBook = reservation.getBook();
        borrowedBook.incrementNoBorrows();
        sessionConstants.getBookRepository().save(borrowedBook);

        User loggedInUser = sessionConstants.getCurrentUser();
        loggedInUser.incrementNoBorrowings();
        sessionConstants.getUserRepository().save(loggedInUser);
        sessionConstants.events.dataChanged();

        if (loggedInUser.getNotificationSettings().get(ReservationStatus.ACTIVE)) {
            EmailUtil.handleEmail(sessionConstants, reservation.getReader());
        }

        AlertFactory.showAlert(Alert.AlertType.INFORMATION, successHeader, bookSuccessfullyBorrowedContent);
    }

    @FXML
    public void handleReturnBookFromReservationList(ActionEvent actionEvent) {
        Reservation reservation = reservationTable.getSelectionModel().getSelectedItem();
        if (reservation == null) {
            AlertFactory.showAlert(Alert.AlertType.ERROR, generalErrorHeader + "returning book", noReservationSelectedErrorContent);
            return;
        }
        if (!reservation.getReservationStatus().equals(ReservationStatus.ACTIVE) &&
                !reservation.getReservationStatus().equals(ReservationStatus.OVERDUE)) {
            System.out.println("RESERVATION STATUS: " + reservation.getReservationStatus());
            AlertFactory.showAlert(Alert.AlertType.ERROR, generalErrorHeader + "returning book", cannotReturnBookErrorContent);
            return;
        }
        reservation.setReservationStatus(ReservationStatus.RETURNED);

        Book book = reservation.getBook();
        double tempSum = book.getAverageRating() * book.getVotesCount() + rating.getRating();
        book.incrementVotesCount();
        book.setAverageRating(tempSum / book.getVotesCount());
        sessionConstants.getBookRepository().save(book);

        Fine fine = reservation.returnBook();
        if (fine != null) {
            sessionConstants.getFineRepository().save(fine);
            AlertFactory.showAlert(Alert.AlertType.INFORMATION, receiveFineInfoHeader, fine.getDescription() + "\nValue: " + fine.getValue());
        }
        sessionConstants.getReservationRepository().delete(reservation);
        book.checkNextReservation(sessionConstants.getReservationRepository());
        CompleteReservation completeReservation = new CompleteReservation(reservation, fine != null);
        sessionConstants.getCompleteReservationRepository().save(completeReservation);

        sessionConstants.events.dataChanged();

        if (sessionConstants.getCurrentUser().getNotificationSettings().get(ReservationStatus.RETURNED)) {
            EmailUtil.handleEmail(sessionConstants, reservation.getReader());
        }

        AlertFactory.showAlert(Alert.AlertType.INFORMATION, successHeader, bookSuccessfullyReturnedContent);
    }

    @FXML
    public void handleCancelReservationFromReservationList(ActionEvent actionEvent) {
        Reservation reservation = reservationTable.getSelectionModel().getSelectedItem();
        if (reservation == null) {
            AlertFactory.showAlert(Alert.AlertType.ERROR, generalErrorHeader + "cancelling reservation", noReservationSelectedErrorContent);
            return;
        }

        sessionConstants.getReservationRepository().delete(reservation);
        sessionConstants.events.dataChanged();

        AlertFactory.showAlert(Alert.AlertType.INFORMATION, successHeader, reservationSuccessfullyDeletedContent);
    }

    @FXML
    public void handleChangeDate(ActionEvent actionEvent) {
        refreshFilters();
    }

    private void refreshFilters() {
        String filterText = filterField.getText();
        filterField.setText(filterText + " ");
        filterField.setText(filterText);

        String peselText = peselField.getText();
        peselField.setText(peselText + " ");
        peselField.setText(peselText);
    }

    public void handleChangeUser() {
        refreshWindow();
    }

    public void handleChangeData() {
        refreshData();
    }

    private void refreshWindow() {
        refreshData();
        boolean showAdminFields = isCurrentUserAdmin();
        peselField.setVisible(showAdminFields);
        peselText.setVisible(showAdminFields);
    }

    private ObservableList getFilterItems() {
        return FXCollections.observableList(Arrays.asList(FilterValue.values()));
    }

    private FilteredList<Reservation> InitializeFilters() {
        FilteredList<Reservation> filteredDataByPesel = new FilteredList<>(getReservationsObservable(), p -> true);
        peselField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredDataByPesel.setPredicate(fine -> {
                if (isCurrentUserAdmin()) {
                    if (!(newValue == null || newValue.isEmpty() || fine.getReaderPeselProperty().getValue().toString().startsWith(newValue))) {
                        return false;
                    }
                }
                return true;
            });
        });

        FilteredList<Reservation> filteredDataByFilter = new FilteredList<>(filteredDataByPesel, p -> true);
        filterField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredDataByFilter.setPredicate(reservation -> {
                if (newValue == null || selectedFilter.getSelectionModel().getSelectedItem() == null) {
                    return true;
                } else if (compareSelectedFilter(FilterValue.Overdue)) {
                    return reservation.getReservationStatus().equals(ReservationStatus.ACTIVE) && reservation.getBorrowingDateProperty().getValue().compareTo(new Date()) > 0;
                } else if (compareSelectedFilter(FilterValue.ReservationID) && reservation.getReservationId().toString().startsWith(newValue)) {
                    return true;
                } else if (compareSelectedFilter(FilterValue.BookTitle) && reservation.getBooksTitleProperty().getValue().startsWith(newValue)) {
                    return true;
                } else if (compareSelectedFilter(FilterValue.BorrowDate)) {
                    return checkDatesBetweenDatepickers(reservation.getReservationStartDate());
                } else if (compareSelectedFilter(FilterValue.ReturnDate)) {
                    return checkDatesBetweenDatepickers(reservation.getReservationEndDate());
                } else if (newValue.isEmpty()) {
                    return true;
                } else {
                    return false;
                }
            });
        });
        return filteredDataByFilter;
    }

    private boolean checkDatesBetweenDatepickers(Date checkedDate) {
        if (checkedDate == null) {
            return false;
        }
        return checkDateAfterDatepicker(checkedDate) && checkDateBeforeDatepicker(checkedDate);
    }

    private boolean checkDateAfterDatepicker(Date checkedDate) {
        if (dateFrom.getEditor().getText().equals("")) {
            return true;
        }
        Date dateFromValue;
        try {
            dateFromValue = sessionConstants.datePickerConverter(dateFrom);
        } catch (ParseException | DateTimeException | NullPointerException e) {
            return false;
        }
        if (dateFromValue.compareTo(checkedDate) > 0) {
            return false;
        }
        return true;
    }

    private boolean checkDateBeforeDatepicker(Date checkedDate) {
        if (dateTo.getEditor().getText().equals("")) {
            return true;
        }
        Date dateToValue;
        try {
            dateToValue = sessionConstants.datePickerConverter(dateTo);
        } catch (ParseException | DateTimeException | NullPointerException e) {
            return false;
        }
        if (dateToValue.compareTo(checkedDate) < 0) {
            return false;
        }
        return true;
    }

    private ObservableList<Reservation> getReservationsObservable() {
        List<Reservation> reservations = null;
        if (isCurrentUserAdmin()) {
            reservations = sessionConstants.getReservationRepository().findAll();
        } else {
            Reader reader = sessionConstants.getCurrentReader();
            if (reader != null) {
                reservations = sessionConstants.getReservationRepository().findByReader(reader);
            } else {
                reservations = new ArrayList<>();
            }
        }
        List<Reservation> finalReservations = reservations;
        return new ObservableListBase<Reservation>() {
            @Override
            public Reservation get(int index) {
                return finalReservations.get(index);
            }

            @Override
            public int size() {
                return finalReservations.size();
            }
        };
    }
}