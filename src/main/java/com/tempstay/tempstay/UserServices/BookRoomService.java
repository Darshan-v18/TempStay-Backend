package com.tempstay.tempstay.UserServices;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.tempstay.tempstay.Models.BookRoomHOModel;
import com.tempstay.tempstay.Models.HotelsDB;
import com.tempstay.tempstay.Models.ResponseBooking;
import com.tempstay.tempstay.Models.ServiceProviderModel;
import com.tempstay.tempstay.Models.UserModel;
import com.tempstay.tempstay.Repository.BookRoomRepo;
import com.tempstay.tempstay.Repository.HotelDBRepo;
import com.tempstay.tempstay.Repository.ServiceProviderRepository;
import com.tempstay.tempstay.Repository.UserRepository;

@Service
public class BookRoomService {

    @Autowired
    private BookRoomRepo bookRoomRepo;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceProviderRepository serviceProviderRepository;

    @Autowired
    private ResponseBooking responseBooking;

    @Autowired
    private HotelDBRepo hotelDBRepo;

    public ResponseEntity<ResponseBooking> checkRoom(UUID roomId, UUID hotelownId, Date checkinDate, Date checkouDate) {
        try {
           
            ArrayList<BookRoomHOModel> bookedrooms = new ArrayList<>();
            bookedrooms = bookRoomRepo.findRoomBookingExists(hotelownId, roomId, checkinDate, checkouDate);
            
           
            if (bookedrooms.size() != 0) {
                HotelsDB hotel_ob = hotelDBRepo.findByHotelownIdAndRoomId(hotelownId, roomId);
                
                int no_of_rooms = hotel_ob.getNumberOfRooms();
                int availableRooms=no_of_rooms;
                Iterator<BookRoomHOModel> iterator = bookedrooms.iterator();
                while (iterator.hasNext()) {
                    BookRoomHOModel element = iterator.next();
                  
                    availableRooms=availableRooms-element.getNumberOfRooms();
                    
                }
              
               
                    if(availableRooms<=0){
                        responseBooking.setSuccess(false);
                        responseBooking.setMessage("no Rooms available");
                        
                        responseBooking.setAvailableRooms(0);
                       
                        
                        return ResponseEntity.ok().body(responseBooking);
                    }
                    else{
                        responseBooking.setSuccess(true);
                        responseBooking.setMessage("Rooms available");
                        
                        responseBooking.setAvailableRooms(availableRooms);
                       
                        
                        return ResponseEntity.ok().body(responseBooking);
                    }

                    
                    
                }
            else {
                HotelsDB hotel_ob = hotelDBRepo.findByHotelownIdAndRoomId(hotelownId, roomId);
                
                int no_of_rooms = hotel_ob.getNumberOfRooms();
                responseBooking.setSuccess(true);
                responseBooking.setMessage("Rooms available");
                
                responseBooking.setAvailableRooms(no_of_rooms);
                
                
                return ResponseEntity.ok().body(responseBooking);
                
            }

           
        } catch (Exception e) {
            responseBooking.setSuccess(false);
            responseBooking
                    .setMessage("Internal Server Error inside BookRoomServce.java Method:checkRoom " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseBooking);
        }
    }

    // 

    public ResponseEntity<ResponseBooking> userRoomBookService(BookRoomHOModel bookRoomHOModelReq, String token,
            String role) {
        try {
           
            ResponseEntity<ResponseBooking> messageFromCheckRoom = checkRoom(bookRoomHOModelReq.getRoomId(),
                    bookRoomHOModelReq.getHotelownId(), bookRoomHOModelReq.getCheckinDate(),
                    bookRoomHOModelReq.getCheckoutDate());
            if (messageFromCheckRoom.getBody().getSuccess()) {
               
                HotelsDB hotel_ob = hotelDBRepo.findByRoomId(bookRoomHOModelReq.getRoomId());

                // Calculate the available rooms
                int availableRooms = messageFromCheckRoom.getBody().getAvailableRooms();

                // Check if the requested number of rooms is available
                if (bookRoomHOModelReq.getNumberOfRooms() <= availableRooms) {
                    BookRoomHOModel bookRoomHOModel = new BookRoomHOModel();

                    bookRoomHOModel.setHotelownId(bookRoomHOModelReq.getHotelownId());

                    String email = authService.verifyToken(token);

                    UserModel user = userRepository.findByEmail(email);

                    bookRoomHOModel.setUserId(user.getId());

                    bookRoomHOModel.setHotelownId((bookRoomHOModelReq.getHotelownId()));

                    bookRoomHOModel.setCheckinDate(bookRoomHOModelReq.getCheckinDate());

                    bookRoomHOModel.setRoomId(bookRoomHOModelReq.getRoomId());

                    bookRoomHOModel.setCheckoutDate(bookRoomHOModelReq.getCheckoutDate());

                    LocalDate checkOut = bookRoomHOModelReq.getCheckoutDate().toLocalDate();
                    LocalDate checkIn = bookRoomHOModelReq.getCheckinDate().toLocalDate();

                    long daysDifference = ChronoUnit.DAYS.between(checkIn, checkOut);

                    bookRoomHOModel.setNumberOfDaysToStay((int) daysDifference);

                    bookRoomHOModel.setNumberOfRooms(bookRoomHOModelReq.getNumberOfRooms());

                    int total_price = (int) (hotel_ob.getPricePerDay() * daysDifference
                            * bookRoomHOModelReq.getNumberOfRooms());

                    bookRoomHOModel.setPriceToBePaid(total_price);

                    Optional<ServiceProviderModel> serviceProviderObject = serviceProviderRepository
                            .findById(bookRoomHOModelReq.getHotelownId());

                    String hotelName = serviceProviderObject.get().getHotelName();

                    bookRoomHOModel.setHotelName(hotelName);

                    bookRoomRepo.save(bookRoomHOModel);

                    // // Update the number of available rooms
                    // hotel_ob.setNumberOfRooms(availableRooms - bookRoomHOModelReq.getNumberOfRooms());
                    // hotelDBRepo.save(hotel_ob);

                    responseBooking.setSuccess(true);
                    responseBooking.setMessage("Room booked.");
                    responseBooking.setPriceToBePaid(total_price);
                    responseBooking.setAvailableRooms(availableRooms-bookRoomHOModel.getNumberOfRooms());

                    return ResponseEntity.ok().body(responseBooking);
                } else {
                    // Not enough rooms available
                    responseBooking.setSuccess(false);
                    responseBooking.setMessage("Not enough rooms available.");
                    responseBooking.setPriceToBePaid(0);
                    responseBooking.setAvailableRooms(availableRooms);
                    return ResponseEntity.badRequest().body(responseBooking);

                }
            } else {
                return messageFromCheckRoom;
            }
        } catch (Exception e) {

            responseBooking.setSuccess(false);
            responseBooking.setMessage("An error occurred while processing your request.");
            responseBooking.setPriceToBePaid(0);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseBooking);
        }
    }

}