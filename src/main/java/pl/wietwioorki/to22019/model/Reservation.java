package pl.wietwioorki.to22019.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.Date;

@NoArgsConstructor
@Getter
@ToString
@Entity
@Table(name = "reservation")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    @Column(name = "reservation_id")
    private Long reservationId;

    @ManyToOne
    @JoinColumn(name = "pesel",
            referencedColumnName = "pesel")
    private Reader reader;

    @ManyToOne
    @JoinColumn(name = "book_id",
            referencedColumnName = "book_id")
    private Book book; // todo: only one book in each reservation? //SW Dont change this, what if many readers want get the same book

    @Column(name = "start_date")
    private Date reservationStartDate;

    @Column(name = "end_date")
    private Date reservationEndDate;

    @Column(name = "status")
    @Convert(converter = ReservationStatusConverter.class)
    @Setter
    private ReservationStatus reservationStatus;

    public Reservation(Reader reader, Book book, Date reservationStartDate, Date reservationEndDate, ReservationStatus reservationStatus) {
        this.reader = reader;
        this.book = book;
        this.reservationStartDate = reservationStartDate;
        this.reservationEndDate = reservationEndDate;
        this.reservationStatus = reservationStatus;
    }

    public ObjectProperty<Long> getReservationIdProperty() {
        return new SimpleObjectProperty<>(reservationId);
    }

    public ObjectProperty<Long> getReaderPeselProperty() {
        return new SimpleObjectProperty<>(reader == null ? -1L : reader.getPesel());
    }

    public StringProperty getReaderNameProperty() {
        return new SimpleStringProperty(reader == null ? "" : reader.getFullName());
    }

    public StringProperty getBooksTitleProperty() {
        return new SimpleStringProperty(book.getTitle());
    }

    public ObjectProperty<ReservationStatus> getReservationStatusProperty() {
        return new SimpleObjectProperty<>(reservationStatus);
    }

    public ObjectProperty<Date> getBorrowingDateProperty() {
        return new SimpleObjectProperty<>(reservationStartDate);
    }

    public ObjectProperty<Date> getReturnDateProperty() {
        return new SimpleObjectProperty<>(reservationEndDate);
    }

    public void borrowBook() {
        book.popReaderFromQueue();
        setReservationStartDate(new Date(System.currentTimeMillis()));
        setReservationStatus(ReservationStatus.ACTIVE);
    }

    public Fine returnBook() {
        Date returnDate = new Date(System.currentTimeMillis());
        Fine fine = null;
        int late = returnDate.compareTo(reservationEndDate);
        if (late > 0) {
            float amount = late * 1; //todo: add price to book
            String description = late + " days to late return book: " + book.getTitle() + ". Return date: " + returnDate;
            fine = new Fine(amount, description, reader);
        }
        reservationEndDate = returnDate;
        return fine;
    }

    // fixme - to discuss - how long should be borrowing time?
    public static int getBorrowingTimeInDays() {
        return 14;
    }

    public void setReservationStartDate(Date reservationStartDate) {
        this.reservationStartDate = reservationStartDate;
    }

    public void setReservationEndDate(Date reservationEndDate) {
        this.reservationEndDate = reservationEndDate;
    }
}
