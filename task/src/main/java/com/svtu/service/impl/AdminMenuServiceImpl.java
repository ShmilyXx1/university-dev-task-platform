package com.svtu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.svtu.common.Result;
import com.svtu.entity.Common;
import com.svtu.entity.Menu;
import com.svtu.entity.MenuRole;
import com.svtu.exception.AdminException;
import com.svtu.mapper.AdminMenuMapper;
import com.svtu.mapper.MenuRoleMapper;
import com.svtu.service.AdminMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Transactional
@Service
public class AdminMenuServiceImpl implements AdminMenuService {
    @Autowired
    private AdminMenuMapper adminMenuMapper;
    @Autowired
    private MenuRoleMapper menuRoleMapper;
    @Autowired
    private Common common;

    @Override
    public Result<Void> insertMenu(Menu menu) {
        if (menu==null){
            throw new AdminException(502,"新建的权限为空");
        }
        int rows = adminMenuMapper.insert(menu);
        if (rows<=0){
            throw new AdminException(502,"创建权限失败");
        }
        return Result.success();
    }

    @Override
    public Result<List<String>> selectUserAllMenu(int userId) {
        common.checkUserId(userId);
        List<String> list = adminMenuMapper.selectUserAllMenu(userId);
        if (list==null||list.isEmpty()){
            list = new ArrayList<>();
        }
        return Result.success(list);
    }

    @Override
    public Result<Menu> getMenu(int menuId) {
        if (menuId<=0){
            throw new AdminException(502,"菜单编号错误");
        }
        Menu menu = adminMenuMapper.selectById(menuId);
        if (menu==null){
            throw new AdminException(502,"没有此菜单");
        }
        return Result.success(menu);
    }

    @Override
    public Result<Void> deleteMenu(int menuId) {
        if (menuId<=0){
            throw new AdminException(502,"菜单编号错误");
        }
        Menu menu = adminMenuMapper.selectById(menuId);
        if (menu==null){
            throw new AdminException(502,"没有此菜单");
        }
        menuRoleMapper.deleteById(menuId);    //删除menuRole关联表中的数据
        int rows = adminMenuMapper.deleteById(menuId);
        if (rows<=0){
            throw new AdminException(502,"删除此菜单失败");
        }
        return Result.success() ;
    }

    @Override
    public Result<List<Menu>> selectAllMenu() {
        List<Menu> list = adminMenuMapper.selectList(new QueryWrapper<>());
        if (list==null||list.isEmpty()){
            list=new ArrayList<>();
        }
        return Result.success(list);
    }

    @Override
    public Result<Void> insertMenuRole(MenuRole menuRole) {
        if (menuRole==null||(menuRole.getMenuId()<=0&&menuRole.getRoleId()<=0)){
            throw new AdminException(502,"给角色分配菜单的值错误或为空");
        }
        int insert = menuRoleMapper.insert(menuRole);
        if (insert<=0){
            throw new AdminException(502,"给角色分配菜单失败");
        }
        return Result.success();
    }

    @Override
    public Result<Void> updateMenu(Menu menu) {
        if (menu==null){
            throw new AdminException(502,"菜单内容为空");
        }
        int rows = adminMenuMapper.updateById(menu);
        if (rows<=0){
            throw new AdminException(502,"修改菜单失败");
        }
        return Result.success();
    }

    @Override
    public Result<List<String>> selectMenuRole(int roleId) {
        if (roleId<=0){
            throw new AdminException(502,"角色编号错误");
        }
        List<String> list = menuRoleMapper.selectMenRole(roleId);
        if (list==null||list.isEmpty()){
            list=new ArrayList<>();
        }
        return Result.success();
    }

    @Override
    public Result<Void> deleteMenuRole(MenuRole menuRole) {
        if (menuRole==null){
            throw  new AdminException(502,"删除角色菜单失败");
        }
        int rows = menuRoleMapper.deleteById(menuRole);
        if (rows<0){
            throw new AdminException(502,"删除当前角色菜单失败");
        }
        return Result.success();
    }

}
