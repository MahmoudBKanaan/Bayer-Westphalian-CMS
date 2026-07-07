package com.bayerwestphalian.campaign.customer;

import com.bayerwestphalian.campaign.common.api.ApiResponse;
import com.bayerwestphalian.campaign.common.api.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CustomerView>>> searchCustomers(
            @RequestParam(required = false) String term,
            @RequestParam(required = false) CustomerType customerType,
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Boolean contactable,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        CustomerSearchCriteria criteria =
                new CustomerSearchRequest(term, customerType, status, city, country, contactable)
                        .toCriteria();
        PageResponse<CustomerView> customers =
                customerService.searchCustomers(criteria, page, size);

        return ResponseEntity.ok(ApiResponse.success("Customers loaded", customers));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerView>> getCustomer(@PathVariable UUID id) {
        CustomerView customer = customerService.findById(id);

        return ResponseEntity.ok(ApiResponse.success("Customer loaded", customer));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerView>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        CustomerView customer = customerService.createCustomer(request.toCommand());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created", customer));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CustomerImportResult>> importCustomers(
            @RequestParam("file") MultipartFile file) {
        CustomerImportResult result = customerService.importCustomers(file);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customers imported", result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerView>> updateCustomer(
            @PathVariable UUID id, @Valid @RequestBody UpdateCustomerRequest request) {
        CustomerView customer = customerService.updateCustomer(id, request.toCommand());

        return ResponseEntity.ok(ApiResponse.success("Customer updated", customer));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerView>> deleteCustomer(@PathVariable UUID id) {
        CustomerView customer = customerService.softDeleteCustomer(id);

        return ResponseEntity.ok(ApiResponse.success("Customer deleted", customer));
    }
}
