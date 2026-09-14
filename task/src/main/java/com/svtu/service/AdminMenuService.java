package com.svtu.service;

import com.svtu.common.Result;
import com.svtu.entity.Menu;
import com.svtu.entity.MenuRole;

import java.util.List;

public interface AdminMenuService {
    Result<Void> insertMenu(Menu menu);
    Result<List<String>> selectUserAllMenu(int userId);
    Result<Menu> getMenu(int menuId);
    Result<Void> deleteMenu(int menuId);
    Result<List<Menu>> selectAllMenu();
    Result<Void> insertMenuRole(MenuRole menuRole);
    Result<Void> updateMenu(Menu menu);
    Result<List<String>> selectMenuRole(int roleId);
    Result<Void> deleteMenuRole(MenuRole menuRole);
}
