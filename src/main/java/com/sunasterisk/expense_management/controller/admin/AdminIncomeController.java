package com.sunasterisk.expense_management.controller.admin;

import com.sunasterisk.expense_management.dto.PageResponse;
import com.sunasterisk.expense_management.dto.income.AdminIncomeFilterRequest;
import com.sunasterisk.expense_management.dto.income.IncomeRequest;
import com.sunasterisk.expense_management.dto.income.IncomeResponse;
import com.sunasterisk.expense_management.service.admin.AdminIncomeService;
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
public class AdminIncomeController extends BaseAdminController {

    private static final String MODULE = "incomes";

    private final CategoryService categoryService;
    private final AdminIncomeService adminIncomeService;
    private final CsvExportService csvExportService;

    public AdminIncomeController(CategoryService categoryService,
                                 AdminIncomeService adminIncomeService,
                                 CsvExportService csvExportService,
                                 MessageSource messageSource) {
        super(messageSource);
        this.categoryService = categoryService;
        this.adminIncomeService = adminIncomeService;
        this.csvExportService = csvExportService;
    }

    @GetMapping("/incomes")
    public String index(Model model,
                        @RequestParam(required = false) String name,
                        @RequestParam(required = false) Long userId,
                        @RequestParam(required = false) Long categoryId,
                        @RequestParam(required = false) String startDate,
                        @RequestParam(required = false) String endDate,
                        @RequestParam(defaultValue = "0") Integer page,
                        @RequestParam(defaultValue = "20") Integer size) {
        AdminIncomeFilterRequest.AdminIncomeFilterRequestBuilder filterBuilder = AdminIncomeFilterRequest.builder()
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

        AdminIncomeFilterRequest filter = filterBuilder.build();
        PageResponse<IncomeResponse> response = adminIncomeService.getAllIncomes(filter);

        model.addAttribute("activeMenu", "incomes");
        model.addAttribute("incomes", response.getContent());
        model.addAttribute("currentPage", response.getPageNumber());
        model.addAttribute("totalPages", response.getTotalPages());
        model.addAttribute("totalElements", response.getTotalElements());
        model.addAttribute("filter", filter);

        return viewIndex(MODULE);
    }

    @GetMapping("/incomes/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("activeMenu", MODULE);
            IncomeResponse income = adminIncomeService.getIncomeById(id);
            model.addAttribute("income", income);
            return viewDetail(MODULE);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return redirectToIndex(MODULE);
        }
    }

    @GetMapping("/incomes/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("activeMenu", MODULE);
            IncomeResponse income = adminIncomeService.getIncomeById(id);

            if (!model.containsAttribute("incomeRequest")) {
                IncomeRequest incomeRequest = new IncomeRequest();
                incomeRequest.setId(income.getId());
                incomeRequest.setName(income.getName());
                incomeRequest.setAmount(income.getAmount());
                incomeRequest.setIncomeDate(income.getIncomeDate());
                incomeRequest.setCategoryId(income.getCategoryId());
                incomeRequest.setNote(income.getNote());
                incomeRequest.setSource(income.getSource());
                incomeRequest.setIsRecurring(income.getIsRecurring());
                incomeRequest.setRecurringType(income.getRecurringType());
                model.addAttribute("incomeRequest", incomeRequest);
            }

            model.addAttribute("categories", categoryService.getActiveIncomeCategories());
            return viewForm(MODULE);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return redirectToIndex(MODULE);
        }
    }

    @PutMapping("/incomes/{id}")
    public String update(@PathVariable Long id,
                        @Valid @ModelAttribute("incomeRequest") IncomeRequest incomeRequest,
                        BindingResult bindingResult,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", MODULE);
            model.addAttribute("categories", categoryService.getActiveIncomeCategories());
            return viewForm(MODULE);
        }

        try {
            adminIncomeService.updateIncome(id, incomeRequest);
            redirectAttributes.addFlashAttribute("success",
                    getMessage("admin.income.updated.success"));
            return redirectToIndex(MODULE);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("incomeRequest", incomeRequest);
            return redirectToEdit(MODULE, id);
        }
    }

    @DeleteMapping("/incomes/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminIncomeService.deleteIncome(id);
            redirectAttributes.addFlashAttribute("success",
                    getMessage("admin.income.deleted.success"));
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return redirectToIndex(MODULE);
    }

    /**
     * Export incomes to CSV
     */
    @GetMapping("/incomes/export")
    public void exportIncomes(HttpServletResponse response) {
        try {
            csvExportService.exportIncomes(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export incomes: " + e.getMessage(), e);
        }
    }
}
