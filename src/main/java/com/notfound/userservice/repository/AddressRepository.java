package com.notfound.userservice.repository;

import com.notfound.userservice.model.entity.Address;
import com.notfound.userservice.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {
    List<Address> findByUser(User user);
    List<Address> findByUserId(UUID userId);
}
