package com.inventory.negosyopro.controller.admin;

import java.sql.SQLException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.inventory.negosyopro.config.CurrentUser;
import com.inventory.negosyopro.dao.UsersDAO;
import com.inventory.negosyopro.model.Users;
import com.inventory.negosyopro.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/users")
public class UserAdminController {

    private final UserService userService;
    private final UsersDAO usersDAO;

    public UserAdminController(UserService userService, UsersDAO usersDAO) {
        this.userService = userService;
        this.usersDAO = usersDAO;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        long bid = CurrentUser.get(session).getBusinessId();
        model.addAttribute("users", usersDAO.getUsersByBusiness(bid));
        model.addAttribute("roles", Users.Role.values()); // for the role dropdown
        return "admin/users";
    }

    @PostMapping
    public String add(@RequestParam(value = "username") String username, @RequestParam(value = "firstName") String firstName,
            @RequestParam(value = "lastName") String lastName, @RequestParam(value = "password") String password, @RequestParam(value = "role") Users.Role role,
            HttpSession session, RedirectAttributes flash) throws SQLException {
        long bid = CurrentUser.get(session).getBusinessId();
        try {
            // businessId comes from the session; the last false is isOwner
            userService.addUser(bid, new Users(0, bid, username, null, firstName, lastName, role, true, false),
                    password);
            flash.addFlashAttribute("success", "User added.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable("id") long id, HttpSession session, RedirectAttributes flash)
            throws SQLException {
        Users me = CurrentUser.get(session);
        if (id == me.getUserId()) {
            flash.addFlashAttribute("error", "You cannot deactivate your own account.");
            return "redirect:/admin/users";
        }
        try {
            boolean ok = userService.deactivateUser(id, me.getBusinessId());
            flash.addFlashAttribute(ok ? "success" : "error", ok ? "User deactivated." : "User not found.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable("id") long id, HttpSession session, RedirectAttributes flash)
            throws SQLException {
        long bid = CurrentUser.get(session).getBusinessId();
        try {
            boolean ok = userService.activateUser(id, bid);
            flash.addFlashAttribute(ok ? "success" : "error", ok ? "User activated." : "User not found.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
