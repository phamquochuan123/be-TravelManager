package com.example.travelManager.repository.hotel;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.travelManager.domain.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {

}
