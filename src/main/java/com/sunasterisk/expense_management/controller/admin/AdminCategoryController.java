package com.sunasterisk.expense_management.controller.admin;

import com.sunasterisk.expense_management.dto.PageResponse;
import com.sunasterisk.expense_management.dto.category.CategoryFilterRequest;
import com.sunasterisk.expense_management.dto.CategoryDto;
import com.sunasterisk.expense_management.dto.category.CategoryRequest;
import com.sunasterisk.expense_management.dto.category.CategoryResponse;
import com.sunasterisk.expense_management.service.CategoryService;
import com.sunasterisk.expense_management.service.CsvExportService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminCategoryController extends BaseAdminController {

    private static final String MODULE = "categories";

    private final CategoryService categoryService;
    private final CsvExportService csvExportService;

    public AdminCategoryController(CategoryService categoryService,
                                   CsvExportService csvExportService,
                                   MessageSource messageSource) {
        super(messageSource);
        this.categoryService = categoryService;
        this.csvExportService = csvExportService;
    }

    @GetMapping("/categories")
    public String index(Model model,
                        @RequestParam(defaultValue = "0") Integer page,
                        @RequestParam(defaultValue = "20") Integer size) {
        CategoryFilterRequest filter = CategoryFilterRequest.builder()
                .page(page)
                .size(size)
                .build();
        PageResponse<CategoryResponse> response = categoryService.getCategories(filter);
        model.addAttribute("activeMenu", MODULE);
        model.addAttribute("categories", response.getContent());
        return viewIndex(MODULE);
    }

    @GetMapping("/categories/new")
    public String newCategory(Model model) {
        model.addAttribute("activeMenu", MODULE);
        if (!model.containsAttribute("category")) {
            model.addAttribute("category", new CategoryDto());
        }
        return viewForm(MODULE);
    }

    @GetMapping("/categories/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("activeMenu", MODULE);
            CategoryResponse category = categoryService.getCategoryById(id);
            model.addAttribute("category", category);
            return viewDetail(MODULE);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return redirectToIndex(MODULE);
        }
    }

    @GetMapping("/categories/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("activeMenu", MODULE);
            if (!model.containsAttribute("category")) {
                CategoryResponse category = categoryService.getCategoryById(id);
                CategoryDto dto = CategoryDto.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .description(category.getDescription())
                        .icon(category.getIcon())
                        .color(category.getColor())
                        .type(category.getType())
                        .active(category.getActive())
                        .isDefault(category.getIsDefault())
                        .build();
                model.addAttribute("category", dto);
            }
            return viewForm(MODULE);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return redirectToIndex(MODULE);
        }
    }

    @PostMapping("/categories")
    public String create(@Valid @ModelAttribute("category") CategoryDto category, BindingResult bindingResult,
                         Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", MODULE);
            return viewForm(MODULE);
        }
        try {
            CategoryRequest request = CategoryRequest.builder()
                    .name(category.getName())
                    .description(category.getDescription())
                    .icon(category.getIcon())
                    .color(category.getColor())
                    .type(category.getType())
                    .active(category.getActive())
                    .build();
            categoryService.createCategory(request);
            redirectAttributes.addFlashAttribute("success",
                    getMessage("admin.category.created.success"));
            return redirectToIndex(MODULE);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("category", category);
            return REDIRECT_PREFIX + MODULE + "/new";
        }
    }

    @PutMapping("/categories/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("category") CategoryDto category,
                         BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", MODULE);
            return viewForm(MODULE);
        }
        try {
            CategoryRequest request = CategoryRequest.builder()
                    .name(category.getName())
                    .description(category.getDescription())
                    .icon(category.getIcon())
                    .color(category.getColor())
                    .type(category.getType())
                    .active(category.getActive())
                    .build();
            categoryService.updateCategory(id, request);
            redirectAttributes.addFlashAttribute("success",
                    getMessage("admin.category.updated.success"));
            return redirectToIndex(MODULE);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("category", category);
            return redirectToEdit(MODULE, id);
        }
    }

    @DeleteMapping("/categories/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("success",
                    getMessage("admin.category.deleted.success"));
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return redirectToIndex(MODULE);
    }

    /**
     * Export categories to CSV
     */
    @GetMapping("/categories/export")
    public void exportCategories(HttpServletResponse response) {
        try {
            csvExportService.exportCategories(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export categories: " + e.getMessage(), e);
        }
    }
}
