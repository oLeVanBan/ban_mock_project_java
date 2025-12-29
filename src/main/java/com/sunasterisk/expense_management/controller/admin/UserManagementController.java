package com.sunasterisk.expense_management.controller.admin;

import com.sunasterisk.expense_management.dto.UserDto;
import com.sunasterisk.expense_management.service.CsvExportService;
import com.sunasterisk.expense_management.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for User Management in Admin Panel
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserService userService;
    private final MessageSource messageSource;
    private final CsvExportService csvExportService;

    /**
     * List all users
     */
    @GetMapping("/users")
    public String index(Model model) {
        model.addAttribute("activeMenu", "users");
        model.addAttribute("users", userService.getAllUsers());
        return "admin/users/index";
    }

    /**
     * View user detail
     */
    @GetMapping("/users/{id}")
    public String detail(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("activeMenu", "users");
            UserDto user = userService.getUserById(id);
            model.addAttribute("user", user);
            return "admin/users/detail";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users";
        }
    }

    /**
     * Show create user form
     */
    @GetMapping("/users/new")
    public String newUser(Model model) {
        model.addAttribute("activeMenu", "users");
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new UserDto());
        }
        return "admin/users/form";
    }

    /**
     * Show edit user form
     */
    @GetMapping("/users/{id}/edit")
    public String edit(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("activeMenu", "users");
            if (!model.containsAttribute("user")) {
                UserDto user = userService.getUserById(id);
                model.addAttribute("user", user);
            }
            return "admin/users/form";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users";
        }
    }

    /**
     * Create new user
     */
    @PostMapping("/users")
    public String create(@Valid @ModelAttribute("user") UserDto user, BindingResult bindingResult,
                        Model model, RedirectAttributes redirectAttributes) {
        // Check email exists
        if (userService.existsByEmail(user.getEmail())) {
            bindingResult.rejectValue("email", "admin.user.email.exists",
                    messageSource.getMessage("admin.user.email.exists", null, LocaleContextHolder.getLocale()));
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "users");
            return "admin/users/form";
        }
        try {
            userService.createUser(user);
            redirectAttributes.addFlashAttribute("success",
                    messageSource.getMessage("admin.user.created.success", null, LocaleContextHolder.getLocale()));
            return "redirect:/admin/users";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/admin/users/new";
        }
    }

    /**
     * Update existing user
     */
    @PutMapping("/users/{id}")
    public String update(@PathVariable("id") Long id, @Valid @ModelAttribute("user") UserDto user,
                        BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        // Check email exists (except current user)
        UserDto existingUser = userService.getUserById(id);
        if (!existingUser.getEmail().equals(user.getEmail()) && userService.existsByEmail(user.getEmail())) {
            bindingResult.rejectValue("email", "admin.user.email.exists",
                    messageSource.getMessage("admin.user.email.exists", null, LocaleContextHolder.getLocale()));
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "users");
            user.setId(id);
            return "admin/users/form";
        }
        try {
            userService.updateUser(id, user);
            redirectAttributes.addFlashAttribute("success",
                    messageSource.getMessage("admin.user.updated.success", null, LocaleContextHolder.getLocale()));
            return "redirect:/admin/users";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/admin/users/" + id + "/edit";
        }
    }

    /**
     * Delete user
     */
    @DeleteMapping("/users/{id}")
    public String deleteUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success",
                    messageSource.getMessage("admin.user.deleted.success", null, LocaleContextHolder.getLocale()));
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Export users to CSV
     */
    @GetMapping("/users/export")
    public void exportUsers(HttpServletResponse response) {
        try {
            csvExportService.exportUsers(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export users: " + e.getMessage(), e);
        }
    }
}
