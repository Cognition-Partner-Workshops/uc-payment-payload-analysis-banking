package com.cbarkinozer.onlinebankingrestapi.app.cus.service;

import com.cbarkinozer.onlinebankingrestapi.app.cus.mapper.CusCustomerMapper;
import com.cbarkinozer.onlinebankingrestapi.app.cus.dto.CusCustomerDto;
import com.cbarkinozer.onlinebankingrestapi.app.cus.dto.CusCustomerPasswordUpdateDto;
import com.cbarkinozer.onlinebankingrestapi.app.cus.dto.CusCustomerSaveDto;
import com.cbarkinozer.onlinebankingrestapi.app.cus.dto.CusCustomerUpdateDto;
import com.cbarkinozer.onlinebankingrestapi.app.cus.entity.CusCustomer;
import com.cbarkinozer.onlinebankingrestapi.app.cus.enums.CusCustomerRoleType;
import com.cbarkinozer.onlinebankingrestapi.app.cus.enums.CusErrorMessage;
import com.cbarkinozer.onlinebankingrestapi.app.cus.service.entityservice.CusCustomerEntityService;
import com.cbarkinozer.onlinebankingrestapi.app.gen.exceptions.IllegalFieldException;
import com.cbarkinozer.onlinebankingrestapi.app.gen.exceptions.ItemNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CusCustomerService {

    public final CusCustomerEntityService cusCustomerEntityService;
    public final CusCustomerValidationService cusCustomerValidationService;
    private final PasswordEncoder passwordEncoder;

    public List<CusCustomerDto> findAllCustomers(){

        controlIsCurrentCustomerAdmin();

        List<CusCustomer> cusCustomerList = cusCustomerEntityService.findAllCustomers();

        List<CusCustomerDto> cusCustomerDtoList = CusCustomerMapper.INSTANCE.convertToCusCustomerDtoList(cusCustomerList);

        return cusCustomerDtoList;
    }

    public CusCustomerDto findCustomerById(Long id) {

        boolean isOwnRecord = id != null && id.equals(cusCustomerEntityService.getCurrentCustomerId());
        if (!isOwnRecord && !cusCustomerEntityService.isCurrentCustomerAdmin()) {
            throw new ItemNotFoundException(CusErrorMessage.CUSTOMER_NOT_FOUND);
        }

        CusCustomer cusCustomer = cusCustomerEntityService.getByIdWithControl(id);

        CusCustomerDto cusCustomerDto = CusCustomerMapper.INSTANCE.convertToCusCustomerDto(cusCustomer);

        return cusCustomerDto;
    }

    public CusCustomerDto saveCustomer(CusCustomerSaveDto cusCustomerSaveDto) {

        CusCustomer cusCustomer = CusCustomerMapper.INSTANCE.convertToCusCustomer(cusCustomerSaveDto);

        String password = passwordEncoder.encode(cusCustomer.getPassword());
        cusCustomer.setPassword(password);
        cusCustomer.setRoleType(CusCustomerRoleType.USER);

        cusCustomerValidationService.controlAreFieldsNonNull(cusCustomer);
        cusCustomerValidationService.controlIsIdentityNoUnique(cusCustomer);

        cusCustomer = cusCustomerEntityService.saveCustomer(cusCustomer);

        CusCustomerDto cusCustomerDto = CusCustomerMapper.INSTANCE.convertToCusCustomerDto(cusCustomer);

        return cusCustomerDto;
    }

    public CusCustomerDto updateCustomer(CusCustomerUpdateDto cusCustomerUpdateDto) {

        CusCustomer cusCustomer = getCurrentCustomerOrThrow();

        cusCustomer.setName(cusCustomerUpdateDto.getName());
        cusCustomer.setSurname(cusCustomerUpdateDto.getSurname());

        cusCustomerValidationService.controlAreFieldsNonNull(cusCustomer);

        cusCustomer = cusCustomerEntityService.saveCustomer(cusCustomer);

        CusCustomerDto cusCustomerDto = CusCustomerMapper.INSTANCE.convertToCusCustomerDto(cusCustomer);

        return cusCustomerDto;
    }

    public void changePassword(CusCustomerPasswordUpdateDto cusCustomerPasswordUpdateDto) {

        String currentPassword = cusCustomerPasswordUpdateDto.getCurrentPassword();
        String newPassword = cusCustomerPasswordUpdateDto.getNewPassword();

        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalFieldException(CusErrorMessage.FIELD_CANNOT_BE_NULL);
        }

        CusCustomer cusCustomer = getCurrentCustomerOrThrow();

        if (currentPassword == null || !passwordEncoder.matches(currentPassword, cusCustomer.getPassword())) {
            throw new IllegalFieldException(CusErrorMessage.CURRENT_PASSWORD_IS_WRONG);
        }

        cusCustomer.setPassword(passwordEncoder.encode(newPassword));
        cusCustomerEntityService.saveCustomer(cusCustomer);
    }

    public void deleteCustomer(Long id) {

        controlIsCurrentCustomerAdmin();

        CusCustomer cusCustomer = cusCustomerEntityService.getByIdWithControl(id);

        cusCustomerEntityService.delete(cusCustomer);
    }

    private CusCustomer getCurrentCustomerOrThrow() {

        Long currentCustomerId = cusCustomerEntityService.getCurrentCustomerId();
        if (currentCustomerId == null) {
            throw new ItemNotFoundException(CusErrorMessage.CUSTOMER_NOT_FOUND);
        }

        return cusCustomerEntityService.getByIdWithControl(currentCustomerId);
    }

    private void controlIsCurrentCustomerAdmin() {

        if (!cusCustomerEntityService.isCurrentCustomerAdmin()) {
            throw new AccessDeniedException("Administrator role is required");
        }
    }
}
