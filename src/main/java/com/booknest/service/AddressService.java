package com.booknest.service;

import com.booknest.entity.Address;
import com.booknest.entity.User;
import com.booknest.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for Address operations
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;

    public List<Address> getAddressesByUser(User user) {
        return addressRepository.findByUserId(user.getId());
    }

    @SuppressWarnings("null")
    public Optional<Address> getAddressById(Long id) {
        return addressRepository.findById(id);
    }

    public Optional<Address> getDefaultAddress(User user) {
        return addressRepository.findByUserIdAndDefaultAddressTrue(user.getId());
    }

    public Address saveAddress(User user, Address address) {
        address.setUser(user);

        if (address.getDefaultAddress()) {
            @SuppressWarnings("null")
            List<Address> userAddresses = addressRepository.findByUserId(user.getId());
            userAddresses.stream()
                    .filter(addr -> addr.getDefaultAddress() != null && addr.getDefaultAddress())
                    .forEach(existingDefault -> {
                        existingDefault.setDefaultAddress(false);
                        addressRepository.save(existingDefault);
                    });
        }

        return addressRepository.save(address);
    }

    @SuppressWarnings("null")
    public Address updateAddress(Address address) {
        return addressRepository.save(address);
    }

    @SuppressWarnings("null")
    public void deleteAddress(Long id) {
        addressRepository.deleteById(id);
    }

    @SuppressWarnings("null")
    public void setDefaultAddress(User user, Long addressId) {
        addressRepository.findByUserId(user.getId()).forEach(addr -> {
            addr.setDefaultAddress(false);
            addressRepository.save(addr);
        });

        Address defaultAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        defaultAddress.setDefaultAddress(true);
        addressRepository.save(defaultAddress);
    }
}
