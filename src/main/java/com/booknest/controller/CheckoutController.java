package com.booknest.controller;

import com.booknest.entity.*;
import com.booknest.service.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for checkout operations
 */
@Controller
@RequiredArgsConstructor
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);
    private static final String ORDER_FAILED_MESSAGE = "Unable to place your order right now. Please try again.";

    private final UserService userService;
    private final CartService cartService;
    private final AddressService addressService;
    private final OrderService orderService;

    @GetMapping("/checkout")
    public String checkout(Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        var cart = cartService.getCartByUser(currentUser);
        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            return "redirect:/cart";
        }

        List<Address> addresses = addressService.getAddressesByUser(currentUser);
        Address defaultAddress = addressService.getDefaultAddress(currentUser).orElse(null);
        if (defaultAddress == null && !addresses.isEmpty()) {
            defaultAddress = addresses.get(0);
        }

        model.addAttribute("cart", cart);
        model.addAttribute("addresses", addresses);
        model.addAttribute("defaultAddress", defaultAddress);
        model.addAttribute("currentUser", currentUser);

        return "checkout";
    }

    @PostMapping("/checkout/place-order")
    public String placeOrder(@RequestParam(value = "addressId", required = false) Long addressId,
                            @RequestParam(value = "addressLine1", required = false) String addressLine1,
                            @RequestParam(value = "addressLine2", required = false) String addressLine2,
                            @RequestParam(value = "city", required = false) String city,
                            @RequestParam(value = "state", required = false) String state,
                            @RequestParam(value = "postalCode", required = false) String postalCode,
                            @RequestParam(value = "country", required = false) String country,
                            @RequestParam(value = "phone", required = false) String phone,
                            @RequestParam(value = "deliveryOption", defaultValue = "STANDARD") String deliveryOption,
                            @RequestParam String paymentMethod,
                            @RequestParam(value = "notes", required = false) String notes,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Address address = null;

        if (addressId != null) {
            address = addressService.getAddressById(addressId)
                    .filter(a -> a.getUser() != null && currentUser.getId().equals(a.getUser().getId()))
                    .orElse(null);
            if (address != null && phone != null && !phone.isBlank()) {
                address.setPhone(phone);
                addressService.saveAddress(currentUser, address);
            }
        }

        if (address == null && addressLine1 != null && !addressLine1.isBlank()) {
            address = new Address();
            address.setAddressLine1(addressLine1);
            address.setAddressLine2(addressLine2);
            address.setCity(city != null ? city : "");
            address.setState(state != null ? state : "");
            address.setPostalCode(postalCode != null ? postalCode : "");
            address.setCountry(country != null ? country : "India");
            address.setPhone(phone != null ? phone : (currentUser.getPhone() != null ? currentUser.getPhone() : ""));
            address.setDefaultAddress(true);
            address = addressService.saveAddress(currentUser, address);
        }

        if (address == null) {
            redirectAttributes.addFlashAttribute("error", "Please select or provide a valid shipping address");
            return "redirect:/checkout";
        }

        try {
            Order order = orderService.createOrder(currentUser, address, paymentMethod, deliveryOption, notes);
            return "redirect:/order/success/" + order.getId();
        } catch (OrderPlacementException e) {
            // An empty cart right after an order means Place Order was submitted
            // twice; the first submission succeeded, so show its confirmation.
            if (e.isEmptyCart()) {
                Order recent = orderService.findRecentOrder(currentUser, 60).orElse(null);
                if (recent != null) {
                    return "redirect:/order/success/" + recent.getId();
                }
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return "redirect:/cart";
            }
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/checkout";
        } catch (RuntimeException e) {
            log.error("Failed to place order for user {}", currentUser.getId(), e);
            redirectAttributes.addFlashAttribute("error", ORDER_FAILED_MESSAGE);
            return "redirect:/checkout";
        }
    }

    @GetMapping("/order/success/{id}")
    public String orderSuccess(@PathVariable Long id, Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Order order = orderService.getOrderById(id);
        if (order == null || !order.getUser().getId().equals(currentUser.getId())) {
            return "redirect:/";
        }

        model.addAttribute("order", order);
        model.addAttribute("currentUser", currentUser);

        return "order-success";
    }

    @GetMapping("/order/invoice/{id}")
    public String downloadInvoice(@PathVariable Long id, Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Order order = orderService.getOrderById(id);
        if (order == null || !order.getUser().getId().equals(currentUser.getId())) {
            return "redirect:/";
        }

        model.addAttribute("order", order);
        model.addAttribute("currentUser", currentUser);

        return "invoice";
    }
}
