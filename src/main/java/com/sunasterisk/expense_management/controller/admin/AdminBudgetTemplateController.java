package com.sunasterisk.expense_management.controller.admin;

import com.sunasterisk.expense_management.dto.PageResponse;
import com.sunasterisk.expense_management.dto.budgettemplate.BudgetTemplateFilterRequest;
import com.sunasterisk.expense_management.dto.budgettemplate.BudgetTemplateRequest;
import com.sunasterisk.expense_management.dto.budgettemplate.BudgetTemplateResponse;
import com.sunasterisk.expense_management.service.BudgetTemplateService;
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
public class AdminBudgetTemplateController extends BaseAdminController {

    private static final String MODULE = "budget-templates";

    private final BudgetTemplateService budgetTemplateService;
    private final CategoryService categoryService;
    private final CsvExportService csvExportService;

    public AdminBudgetTemplateController(BudgetTemplateService budgetTemplateService,
                                         CategoryService categoryService,
                                         CsvExportService csvExportService,
                                         MessageSource messageSource) {
        super(messageSource);
        this.budgetTemplateService = budgetTemplateService;
        this.categoryService = categoryService;
        this.csvExportService = csvExportService;
    }

    @GetMapping("/budget-templates")
    public String index(Model model,
                        @RequestParam(defaultValue = "0") Integer page,
                        @RequestParam(defaultValue = "20") Integer size) {
        BudgetTemplateFilterRequest filter = BudgetTemplateFilterRequest.builder()
                .page(page)
                .size(size)
                .build();
        PageResponse<BudgetTemplateResponse> response = budgetTemplateService.getBudgetTemplates(filter);
        model.addAttribute("activeMenu", MODULE);
        model.addAttribute("templates", response.getContent());
        return viewIndex(MODULE);
    }

    @GetMapping("/budget-templates/new")
    public String newBudgetTemplate(Model model) {
        model.addAttribute("activeMenu", MODULE);
        if (!model.containsAttribute("template")) {
            model.addAttribute("template", new BudgetTemplateRequest());
        }
        // Load active categories for dropdown
        model.addAttribute("categories", categoryService.getActiveCategories());
        return viewForm(MODULE);
    }

    @GetMapping("/budget-templates/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("activeMenu", MODULE);
            BudgetTemplateResponse template = budgetTemplateService.getBudgetTemplateById(id);
            model.addAttribute("template", template);
            return viewDetail(MODULE);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return redirectToIndex(MODULE);
        }
    }

    @GetMapping("/budget-templates/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("activeMenu", MODULE);
            if (!model.containsAttribute("template")) {
                BudgetTemplateResponse template = budgetTemplateService.getBudgetTemplateById(id);
                BudgetTemplateRequest request = BudgetTemplateRequest.builder()
                        .name(template.getName())
                        .description(template.getDescription())
                        .active(template.getActive())
                        .items(template.getItems())
                        .build();
                model.addAttribute("template", request);
            }
            // Always set templateId for edit mode
            if (!model.containsAttribute("templateId")) {
                model.addAttribute("templateId", id);
            }
            // Load active categories for dropdown
            model.addAttribute("categories", categoryService.getActiveCategories());
            return viewForm(MODULE);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return redirectToIndex(MODULE);
        }
    }

    @PostMapping("/budget-templates")
    public String create(@Valid @ModelAttribute("template") BudgetTemplateRequest template,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", MODULE);
            model.addAttribute("categories", categoryService.getActiveCategories());
            return viewForm(MODULE);
        }
        try {
            budgetTemplateService.createBudgetTemplate(template);
            redirectAttributes.addFlashAttribute("success",
                    getMessage("admin.budget.template.created.success"));
            return redirectToIndex(MODULE);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("template", template);
            return REDIRECT_PREFIX + MODULE + "/new";
        }
    }

    @PutMapping("/budget-templates/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("template") BudgetTemplateRequest template,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", MODULE);
            model.addAttribute("categories", categoryService.getActiveCategories());
            model.addAttribute("templateId", id);
            return viewForm(MODULE);
        }
        try {
            budgetTemplateService.updateBudgetTemplate(id, template);
            redirectAttributes.addFlashAttribute("success",
                    getMessage("admin.budget.template.updated.success"));
            return redirectToIndex(MODULE);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("template", template);
            redirectAttributes.addFlashAttribute("templateId", id);
            return redirectToEdit(MODULE, id);
        }
    }

    @DeleteMapping("/budget-templates/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            budgetTemplateService.deleteBudgetTemplate(id);
            redirectAttributes.addFlashAttribute("success",
                    getMessage("admin.budget.template.deleted.success"));
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return redirectToIndex(MODULE);
    }

    /**
     * Export budgets to CSV
     */
    @GetMapping("/budgets/export")
    public void exportBudgets(HttpServletResponse response) {
        try {
            csvExportService.exportBudgets(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export budgets: " + e.getMessage(), e);
        }
    }
}
