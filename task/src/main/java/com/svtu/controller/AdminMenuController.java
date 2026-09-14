package com.svtu.controller;

import com.svtu.common.Result;
import com.svtu.entity.Menu;
import com.svtu.entity.MenuRole;
import com.svtu.service.AdminMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/menu")
public class AdminMenuController {
    @Autowired
    private AdminMenuService adminMenuService;

    @PreAuthorize("add_Menu")
    @PostMapping("/addMenu")
    public Result<Void> addMenu(@RequestBody Menu menu){
        return adminMenuService.insertMenu(menu);
    }
    @GetMapping("/userAllMenu")//xx
    public Result<List<String>> userAllMenu(@RequestParam int userId){

        return adminMenuService.selectUserAllMenu(userId);
    }
    @GetMapping("/getMenu")//xx
    public Result<Menu> getMenu(@RequestParam int menuId){
        return adminMenuService.getMenu(menuId);
    }
    @DeleteMapping("/deleteMenu")//xx
    public Result<Void> deleteMenu(@RequestParam int menuId){
        return adminMenuService.deleteMenu(menuId);
    }
    @GetMapping("/AllMenu")//xx
    public Result<List<Menu>> selectAllMenu(){
        return adminMenuService.selectAllMenu();
    }
    @PostMapping("/addMenuRole")//xx
    public Result<Void> addMenuRole(@RequestParam MenuRole menuRole){
        return adminMenuService.insertMenuRole(menuRole);
    }
    @PutMapping("/updateMenu")//xx
    public Result<Void> updateMenu(@RequestParam Menu menu){
        return adminMenuService.updateMenu(menu);
    }
    @GetMapping("/selectMenuRole")//xx
    public Result<List<String>> selectMenuRole(@RequestParam int roleId){
        return adminMenuService.selectMenuRole(roleId);
    }
    @DeleteMapping("/deleteMenuRole")//xx
    public Result<Void> deleteMenuRole(@RequestParam MenuRole menuRole){
        return adminMenuService.deleteMenuRole(menuRole);
    }
}
