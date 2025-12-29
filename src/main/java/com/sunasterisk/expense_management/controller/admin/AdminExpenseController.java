package com.sunasterisk.expense_management.controller.admin;

import com.sunasterisk.expense_management.dto.PageResponse;
import com.sunasterisk.expense_management.dto.expense.AdminExpenseFilterRequest;
import com.sunasterisk.expense_management.dto.expense.ExpenseRequest;
import com.sunasterisk.expense_management.dto.expense.ExpenseResponse;
import com.sunasterisk.expense_management.service.admin.AdminExpenseService;
import com.sunasterisk.expense_management.service.CategoryService;
import com.sunasterisk.expense_management.service.CsvExportService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.time.LocalDate;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminExpenseController extends BaseAdminController {

    private static final String MODULE = "expenses";

    private final AdminExpenseService adminExpenseService;
    private final CategoryService categoryService;
    private final CsvExportService csvExportService;

    public AdminExpenseController(AdminExpenseService adminExpenseService,
                                  CategoryService categoryService,
                                  CsvExportService csvExportService,
                                  MessageSource messageSource) {
        super(messageSource);
        this.adminExpenseService = adminExpenseService;
        this.categoryService = categoryService;
        this.csvExportService = csvExportService;
    }

    @GetMapping("/expenses")
    public String index(Model model,
                        @RequestParam(required = false) String name,
                        @RequestParam(required = false) Long userId,
                        @RequestParam(required = false) Long categoryId,
                        @RequestParam(required = false) String startDate,
                        @RequestParam(required = false) String endDate,
                        @RequestParam(defaultValue = "0") Integer page,
                        @RequestParam(defaultValue = "20") Integer size) {
        AdminExpenseFilterRequest.AdminExpenseFilterRequestBuilder filterBuilder = AdminExpenseFilterRequest.builder()
                .name(name)
                .userId(userId)
                .categoryId(categoryId)
                .page(page)
                .size(size);

        LocalDate parsedStartDate = parseLocalDate(startDate);
        LocalDate parsedEndDate = parseLocalDate(endDate);

        if (parsedStartDate != null) {
            filterBuilder.startDate(parsedStartDate);
        }
        if (parsedEndDate != null) {
            filterBuilder.endDate(parsedEndDate);
        }

        AdminExpenseFilterRequest filter = filterBuilder.build();
        PageResponse<ExpenseResponse> response = adminExpenseService.getAllExpenses(filter);

        model.addAttribute("activeMenu", "expenses");
        model.addAttribute("expenses", response.getContent());
        model.addAttribute("currentPage", response.getPageNumber());
        model.addAttribute("totalPages", response.getTotalPages());
        model.addAttribute("totalElements", response.getTotalElements());
        model.addAttribute("filter", filter);

        return viewIndex(MODULE);
    }

    @GetMapping("/expenses/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("activeMenu", MODULE);
            ExpenseResponse expense = adminExpenseService.getExpenseById(id);
            model.addAttribute("expense", expense);
            return viewDetail(MODULE);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return redirectToIndex(MODULE);
        }
    }

    @GetMapping("/expenses/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("activeMenu", MODULE);
            ExpenseResponse expense = adminExpenseService.getExpenseById(id);

            if (!model.containsAttribute("expenseRequest")) {
                ExpenseRequest expenseRequest = new ExpenseRequest();
                expenseRequest.setId(expense.getId());
                expenseRequest.setName(expense.getName());
                expenseRequest.setAmount(expense.getAmount());
                expenseRequest.setExpenseDate(expense.getExpenseDate());
                expenseRequest.setCategoryId(expense.getCategoryId());
                expenseRequest.setNote(expense.getNote());
                expenseRequest.setLocation(expense.getLocation());
                expenseRequest.setPaymentMethod(expense.getPaymentMethod());
                expenseRequest.setIsRecurring(expense.getIsRecurring());
                expenseRequest.setRecurringType(expense.getRecurringType());
                model.addAttribute("expenseRequest", expenseRequest);
            }

            model.addAttribute("categories", categoryService.getActiveExpenseCategories());
            return viewForm(MODULE);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return redirectToIndex(MODULE);
        }
    }

    @PutMapping("/expenses/{id}")
    public String update(@PathVariable Long id,
                        @Valid @ModelAttribute("expenseRequest") ExpenseRequest expenseRequest,
                        BindingResult bindingResult,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", MODULE);
            model.addAttribute("categories", categoryService.getActiveExpenseCategories());
            return viewForm(MODULE);
        }

        try {
            adminExpenseService.updateExpense(id, expenseRequest);
            redirectAttributes.addFlashAttribute("success",
                    getMessage("admin.expense.updated.success"));
            return redirectToIndex(MODULE);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("expenseRequest", expenseRequest);
            return redirectToEdit(MODULE, id);
        }
    }

    @DeleteMapping("/expenses/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminExpenseService.deleteExpense(id);
            redirectAttributes.addFlashAttribute("success",
                    getMessage("admin.expense.deleted.success"));
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return redirectToIndex(MODULE);
    }

    /**
     * Export expenses to CSV
     */
    @GetMapping("/expenses/export")
    public void exportExpenses(HttpServletResponse response) {
        try {
            csvExportService.exportExpenses(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export expenses: " + e.getMessage(), e);
        }
    }
}
