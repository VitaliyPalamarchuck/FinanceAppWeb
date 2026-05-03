package org.example.financeapp.config;

import org.example.financeapp.model.Category;
import org.example.financeapp.model.User;
import org.example.financeapp.repository.CategoryRepository;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {

    private final CategoryRepository categoryRepository;

    public DataInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void ensureDefaultCategories(User user) {
        createIfMissing(user, "Salary", Category.CategoryType.INCOME, "fa-solid fa-wallet", "#22c55e");
        createIfMissing(user, "Freelance", Category.CategoryType.INCOME, "fa-solid fa-briefcase", "#10b981");
        createIfMissing(user, "Investments", Category.CategoryType.INCOME, "fa-solid fa-chart-line", "#14b8a6");
        createIfMissing(user, "Bonus", Category.CategoryType.INCOME, "fa-solid fa-circle-dollar-to-slot", "#06b6d4");
        createIfMissing(user, "Gifts", Category.CategoryType.INCOME, "fa-solid fa-gift", "#0ea5e9");
        createIfMissing(user, "Refunds", Category.CategoryType.INCOME, "fa-solid fa-rotate-left", "#3b82f6");
        createIfMissing(user, "Rent income", Category.CategoryType.INCOME, "fa-solid fa-house", "#8b5cf6");
        createIfMissing(user, "Interest", Category.CategoryType.INCOME, "fa-solid fa-coins", "#6366f1");
        createIfMissing(user, "Part-time job", Category.CategoryType.INCOME, "fa-solid fa-user-tie", "#84cc16");
        createIfMissing(user, "Other income", Category.CategoryType.INCOME, "fa-solid fa-plus", "#34d399");

        createIfMissing(user, "Groceries", Category.CategoryType.EXPENSE, "fa-solid fa-cart-shopping", "#f97316");
        createIfMissing(user, "Transport", Category.CategoryType.EXPENSE, "fa-solid fa-car", "#fb7185");
        createIfMissing(user, "Rent", Category.CategoryType.EXPENSE, "fa-solid fa-house", "#f59e0b");
        createIfMissing(user, "Utilities", Category.CategoryType.EXPENSE, "fa-solid fa-bolt", "#eab308");
        createIfMissing(user, "Internet", Category.CategoryType.EXPENSE, "fa-solid fa-wifi", "#a855f7");
        createIfMissing(user, "Food delivery", Category.CategoryType.EXPENSE, "fa-solid fa-burger", "#ef4444");
        createIfMissing(user, "Entertainment", Category.CategoryType.EXPENSE, "fa-solid fa-film", "#ec4899");
        createIfMissing(user, "Health", Category.CategoryType.EXPENSE, "fa-solid fa-heart-pulse", "#06b6d4");
        createIfMissing(user, "Education", Category.CategoryType.EXPENSE, "fa-solid fa-book", "#3b82f6");
        createIfMissing(user, "Other expense", Category.CategoryType.EXPENSE, "fa-solid fa-ellipsis", "#94a3b8");
    }

    private void createIfMissing(User user, String name, Category.CategoryType type, String iconClass, String colorHex) {
        boolean exists = categoryRepository.findByUserId(user.getId()).stream()
                .anyMatch(category -> category.getName().equalsIgnoreCase(name) && category.getType() == type);

        if (!exists) {
            Category category = new Category();
            category.setUser(user);
            category.setName(name);
            category.setType(type);
            category.setIcon_class(iconClass);
            category.setColor_hex(colorHex);
            categoryRepository.save(category);
        }
    }
}