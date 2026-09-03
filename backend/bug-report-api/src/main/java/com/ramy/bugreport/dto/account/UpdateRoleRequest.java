package com.ramy.bugreport.dto.account;

import com.ramy.bugreport.domain.EUserRole;

import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull EUserRole role) {

}
