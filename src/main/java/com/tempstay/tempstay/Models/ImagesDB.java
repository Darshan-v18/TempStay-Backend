package com.tempstay.tempstay.Models;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class ImagesDB {
    @Id
    private UUID imageId;

    @Column(length = 400)
    private String imageURL;

    private UUID hotelownId;

    private LocalDate dateOfGenration;
}