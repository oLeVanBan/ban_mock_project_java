package com.sunasterisk.expense_management.service;

import com.sunasterisk.expense_management.entity.*;
import com.sunasterisk.expense_management.repository.*;
import com.sunasterisk.expense_management.util.MessageUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for exporting data to CSV format
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CsvExportService {

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;
    private final MessageUtil messageUtil;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Export all users to CSV
     */
    public void exportUsers(HttpServletResponse response) throws IOException {
        List<User> users = userRepository.findAll();

        // Cache expenses and incomes to avoid repeated database queries
        List<Expense> allExpenses = expenseRepository.findAll();
        List<Income> allIncomes = incomeRepository.findAll();

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"users.csv\"");

        try (PrintWriter writer = response.getWriter()) {
            // Write BOM for UTF-8
            writer.write('\ufeff');

            // Header
            writer.println(String.join(",",
                    escapeCSV(messageUtil.getMessage("csv.user.id")),
                    escapeCSV(messageUtil.getMessage("csv.user.name")),
                    escapeCSV(messageUtil.getMessage("csv.user.email")),
                    escapeCSV(messageUtil.getMessage("csv.user.phone")),
                    escapeCSV(messageUtil.getMessage("csv.user.role")),
                    escapeCSV(messageUtil.getMessage("csv.user.active")),
                    escapeCSV(messageUtil.getMessage("csv.user.total.expenses")),
                    escapeCSV(messageUtil.getMessage("csv.user.total.incomes")),
                    escapeCSV(messageUtil.getMessage("csv.user.balance")),
                    escapeCSV(messageUtil.getMessage("csv.user.created.at"))
            ));

            // Data
            for (User user : users) {
                BigDecimal totalExpenses = allExpenses.stream()
                        .filter(expense -> expense.getUser() != null && expense.getUser().getId().equals(user.getId()))
                        .map(Expense::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalIncomes = allIncomes.stream()
                        .filter(income -> income.getUser() != null && income.getUser().getId().equals(user.getId()))
                        .map(Income::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal balance = totalIncomes.subtract(totalExpenses);

                writer.println(String.join(",",
                        escapeCSV(user.getId().toString()),
                        escapeCSV(user.getName()),
                        escapeCSV(user.getEmail()),
                        escapeCSV(user.getPhone() != null ? user.getPhone() : ""),
                        escapeCSV(user.getRole().name()),
                        escapeCSV(user.getActive().toString()),
                        escapeCSV(totalExpenses.toString()),
                        escapeCSV(totalIncomes.toString()),
                        escapeCSV(balance.toString()),
                        escapeCSV(user.getCreatedAt() != null ? user.getCreatedAt().format(DATETIME_FORMATTER) : "")
                ));
            }
        }

        log.info("Exported {} users to CSV", users.size());
    }

    /**
     * Export all expenses to CSV
     */
    public void exportExpenses(HttpServletResponse response) throws IOException {
        List<Expense> expenses = expenseRepository.findAll();

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"expenses.csv\"");

        try (PrintWriter writer = response.getWriter()) {
            writer.write('\ufeff');

            // Header
            writer.println(String.join(",",
                    escapeCSV(messageUtil.getMessage("csv.expense.id")),
                    escapeCSV(messageUtil.getMessage("csv.expense.name")),
                    escapeCSV(messageUtil.getMessage("csv.expense.amount")),
                    escapeCSV(messageUtil.getMessage("csv.expense.date")),
                    escapeCSV(messageUtil.getMessage("csv.expense.category")),
                    escapeCSV(messageUtil.getMessage("csv.expense.note")),
                    escapeCSV(messageUtil.getMessage("csv.expense.user")),
                    escapeCSV(messageUtil.getMessage("csv.expense.created.at"))
            ));

            // Data
            for (Expense expense : expenses) {
                writer.println(String.join(",",
                        escapeCSV(expense.getId().toString()),
                        escapeCSV(expense.getName()),
                        escapeCSV(expense.getAmount().toString()),
                        escapeCSV(expense.getExpenseDate().format(DATE_FORMATTER)),
                        escapeCSV(expense.getCategory() != null ? expense.getCategory().getName() : ""),
                        escapeCSV(expense.getNote() != null ? expense.getNote() : ""),
                        escapeCSV(expense.getUser() != null ? expense.getUser().getName() : ""),
                        escapeCSV(expense.getCreatedAt() != null ? expense.getCreatedAt().format(DATETIME_FORMATTER) : "")
                ));
            }
        }

        log.info("Exported {} expenses to CSV", expenses.size());
    }

    /**
     * Export all incomes to CSV
     */
    public void exportIncomes(HttpServletResponse response) throws IOException {
        List<Income> incomes = incomeRepository.findAll();

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"incomes.csv\"");

        try (PrintWriter writer = response.getWriter()) {
            writer.write('\ufeff');

            // Header
            writer.println(String.join(",",
                    escapeCSV(messageUtil.getMessage("csv.income.id")),
                    escapeCSV(messageUtil.getMessage("csv.income.name")),
                    escapeCSV(messageUtil.getMessage("csv.income.amount")),
                    escapeCSV(messageUtil.getMessage("csv.income.date")),
                    escapeCSV(messageUtil.getMessage("csv.income.category")),
                    escapeCSV(messageUtil.getMessage("csv.income.note")),
                    escapeCSV(messageUtil.getMessage("csv.income.user")),
                    escapeCSV(messageUtil.getMessage("csv.income.created.at"))
            ));

            // Data
            for (Income income : incomes) {
                writer.println(String.join(",",
                        escapeCSV(income.getId().toString()),
                        escapeCSV(income.getName()),
                        escapeCSV(income.getAmount().toString()),
                        escapeCSV(income.getIncomeDate().format(DATE_FORMATTER)),
                        escapeCSV(income.getCategory() != null ? income.getCategory().getName() : ""),
                        escapeCSV(income.getNote() != null ? income.getNote() : ""),
                        escapeCSV(income.getUser() != null ? income.getUser().getName() : ""),
                        escapeCSV(income.getCreatedAt() != null ? income.getCreatedAt().format(DATETIME_FORMATTER) : "")
                ));
            }
        }

        log.info("Exported {} incomes to CSV", incomes.size());
    }

    /**
     * Export all categories to CSV
     */
    public void exportCategories(HttpServletResponse response) throws IOException {
        List<Category> categories = categoryRepository.findAll();

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"categories.csv\"");

        try (PrintWriter writer = response.getWriter()) {
            writer.write('\ufeff');

            // Header
            writer.println(String.join(",",
                    escapeCSV(messageUtil.getMessage("csv.category.id")),
                    escapeCSV(messageUtil.getMessage("csv.category.name")),
                    escapeCSV(messageUtil.getMessage("csv.category.description")),
                    escapeCSV(messageUtil.getMessage("csv.category.type")),
                    escapeCSV(messageUtil.getMessage("csv.category.icon")),
                    escapeCSV(messageUtil.getMessage("csv.category.created.at"))
            ));

            // Data
            for (Category category : categories) {
                writer.println(String.join(",",
                        escapeCSV(category.getId().toString()),
                        escapeCSV(category.getName()),
                        escapeCSV(category.getDescription() != null ? category.getDescription() : ""),
                        escapeCSV(category.getType() != null ? category.getType().name() : ""),
                        escapeCSV(category.getIcon() != null ? category.getIcon() : ""),
                        escapeCSV(category.getCreatedAt() != null ? category.getCreatedAt().format(DATETIME_FORMATTER) : "")
                ));
            }
        }

        log.info("Exported {} categories to CSV", categories.size());
    }

    /**
     * Export all budgets to CSV
     */
    public void exportBudgets(HttpServletResponse response) throws IOException {
        List<Budget> budgets = budgetRepository.findAll();

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"budgets.csv\"");

        try (PrintWriter writer = response.getWriter()) {
            writer.write('\ufeff');

            // Header
            writer.println(String.join(",",
                    escapeCSV(messageUtil.getMessage("csv.budget.id")),
                    escapeCSV(messageUtil.getMessage("csv.budget.name")),
                    escapeCSV(messageUtil.getMessage("csv.budget.amount")),
                    escapeCSV(messageUtil.getMessage("csv.budget.spent")),
                    escapeCSV(messageUtil.getMessage("csv.budget.remaining")),
                    escapeCSV(messageUtil.getMessage("csv.budget.month")),
                    escapeCSV(messageUtil.getMessage("csv.budget.category")),
                    escapeCSV(messageUtil.getMessage("csv.budget.user")),
                    escapeCSV(messageUtil.getMessage("csv.budget.created.at"))
            ));

            // Data
            for (Budget budget : budgets) {
                BigDecimal spent = budget.getSpentAmount() != null ? budget.getSpentAmount() : BigDecimal.ZERO;
                BigDecimal remaining = budget.getAmountLimit().subtract(spent);
                String monthStr = String.format("%04d-%02d", budget.getYear(), budget.getMonth());

                writer.println(String.join(",",
                        escapeCSV(budget.getId().toString()),
                        escapeCSV(budget.getName()),
                        escapeCSV(budget.getAmountLimit().toString()),
                        escapeCSV(spent.toString()),
                        escapeCSV(remaining.toString()),
                        escapeCSV(monthStr),
                        escapeCSV(budget.getCategory() != null ? budget.getCategory().getName() : ""),
                        escapeCSV(budget.getUser() != null ? budget.getUser().getName() : ""),
                        escapeCSV(budget.getCreatedAt() != null ? budget.getCreatedAt().format(DATETIME_FORMATTER) : "")
                ));
            }
        }

        log.info("Exported {} budgets to CSV", budgets.size());
    }

    /**
     * Escape CSV values to handle commas, quotes, and newlines
     */
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }

        // If value contains comma, quote, or newline, wrap in quotes and escape existing quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }
}
