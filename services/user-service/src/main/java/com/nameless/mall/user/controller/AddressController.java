package com.nameless.mall.user.controller;

import com.nameless.mall.core.domain.Result;
import com.nameless.mall.user.api.dto.AddressCreateDTO;
import com.nameless.mall.user.api.dto.AddressDTO;
import com.nameless.mall.user.api.vo.AddressVO;
import com.nameless.mall.user.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用戶收貨地址 Controller
 */
@RestController
@RequestMapping("/users/me/addresses")
@RequiredArgsConstructor
@Tag(name = "Address", description = "收貨地址管理 API")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(summary = "獲取地址列表", description = "獲取當前用戶的所有收貨地址")
    public Result<List<AddressVO>> list(@RequestHeader("X-User-Id") Long userId) {
        List<AddressDTO> dtos = addressService.listByUserId(userId);
        return Result.ok(toVOList(dtos));
    }

    @GetMapping("/{id}")
    @Operation(summary = "獲取地址詳情", description = "根據 ID 獲取地址詳情")
    public Result<AddressVO> getById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        AddressDTO dto = addressService.getById(id, userId);
        return Result.ok(toVO(dto));
    }

    @GetMapping("/default")
    @Operation(summary = "獲取預設地址", description = "獲取當前用戶的預設收貨地址")
    public Result<AddressVO> getDefault(@RequestHeader("X-User-Id") Long userId) {
        AddressDTO dto = addressService.getDefaultAddress(userId);
        return Result.ok(toVO(dto));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新增地址", description = "新增一個收貨地址")
    public Result<AddressVO> create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AddressCreateDTO dto) {
        AddressDTO resultDto = addressService.create(userId, dto);
        return Result.ok(toVO(resultDto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新地址", description = "更新指定的收貨地址")
    public Result<AddressVO> update(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AddressCreateDTO dto) {
        AddressDTO resultDto = addressService.update(id, userId, dto);
        return Result.ok(toVO(resultDto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除地址", description = "刪除指定的收貨地址")
    public Result<Void> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        addressService.delete(id, userId);
        return Result.ok();
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "設為預設地址", description = "將指定地址設為預設收貨地址")
    public Result<Void> setDefault(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        addressService.setDefault(id, userId);
        return Result.ok();
    }

    // --- 輔助方法 ---
    private AddressVO toVO(AddressDTO dto) {
        if (dto == null)
            return null;
        AddressVO vo = new AddressVO();
        BeanUtils.copyProperties(dto, vo);
        return vo;
    }

    private List<AddressVO> toVOList(List<AddressDTO> dtos) {
        if (dtos == null)
            return null;
        return dtos.stream().map(this::toVO).collect(Collectors.toList());
    }
}
