package com.example.receptor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class AppRepository {

    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final Locale RU_LOCALE = new Locale("ru");
    private static AppRepository instance;

    private final SimpleDateFormat apiDateFormat = new SimpleDateFormat(DATE_PATTERN, Locale.US);
    private final SimpleDateFormat uiDateFormat = new SimpleDateFormat("d MMM yyyy", RU_LOCALE);

    private final List<Category> baseCategories = new ArrayList<>();
    private final List<Category> customCategories = new ArrayList<>();
    private final List<Product> products = new ArrayList<>();

    private final int expiryWarningDays = 3;
    private int unitCounter = 1;

    private AppRepository() {
        seedCategories();
        seedProducts();
    }

    public static synchronized AppRepository getInstance() {
        if (instance == null) {
            instance = new AppRepository();
        }
        return instance;
    }

    public synchronized List<Category> getVisibleCategories() {
        List<Category> result = new ArrayList<>();
        for (Category category : baseCategories) {
            if (!"other".equals(category.id)) {
                result.add(category.copy());
            }
        }
        for (Category category : customCategories) {
            result.add(category.copy());
        }
        return result;
    }

    public synchronized Category getCategoryById(String categoryId) {
        for (Category category : baseCategories) {
            if (category.id.equals(categoryId)) {
                return category.copy();
            }
        }
        for (Category category : customCategories) {
            if (category.id.equals(categoryId)) {
                return category.copy();
            }
        }
        return null;
    }

    public synchronized int getInStockCategoryCount(String categoryId) {
        int count = 0;
        for (Product product : products) {
            if (product.categoryId.equals(categoryId) && !product.units.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public synchronized List<Product> getProductsByCategory(String categoryId) {
        List<Product> result = new ArrayList<>();
        for (Product product : products) {
            if (product.categoryId.equals(categoryId)) {
                result.add(product.copy());
            }
        }
        return result;
    }

    public synchronized int getExpiryWarningDays() {
        return expiryWarningDays;
    }

    public synchronized void addUnit(String productId, String expiryDate) {
        for (Product product : products) {
            if (product.id.equals(productId)) {
                product.units.add(new ProductUnit(newUnitId(), expiryDate));
                return;
            }
        }
    }

    public synchronized void removeUnit(String productId) {
        for (Product product : products) {
            if (product.id.equals(productId) && !product.units.isEmpty()) {
                product.units.remove(product.units.size() - 1);
                return;
            }
        }
    }

    public synchronized void addCategory(String name, String emoji, String bgColor, String textColor) {
        String categoryId = "cat_" + System.currentTimeMillis();
        customCategories.add(new Category(categoryId, name, emoji, bgColor, textColor));
    }

    public synchronized void addProductManual(String categoryId, String productName, int quantity, String expiryDate) {
        if (quantity <= 0) {
            return;
        }
        Product existing = null;
        for (Product product : products) {
            if (product.categoryId.equals(categoryId) && product.name.equalsIgnoreCase(productName)) {
                existing = product;
                break;
            }
        }

        if (existing == null) {
            existing = new Product("p_custom_" + System.currentTimeMillis(), productName, categoryId, new ArrayList<>());
            products.add(existing);
        }

        for (int i = 0; i < quantity; i++) {
            existing.units.add(new ProductUnit(newUnitId(), expiryDate));
        }
    }

    public String getDefaultExpiryDateForNewUnit() {
        return addDaysFromToday(7);
    }

    public String formatExpiryDate(String expiryDate) {
        Date date = parseDate(expiryDate);
        return uiDateFormat.format(date);
    }

    public ExpiryStatus getExpiryStatus(String expiryDate) {
        Date today = startOfDay(new Date());
        Date expiry = startOfDay(parseDate(expiryDate));
        long diffDays = (expiry.getTime() - today.getTime()) / 86400000L;
        if (diffDays < 0) {
            return ExpiryStatus.EXPIRED;
        }
        if (diffDays <= expiryWarningDays) {
            return ExpiryStatus.EXPIRING;
        }
        return ExpiryStatus.FRESH;
    }

    public String toApiDate(Date date) {
        return apiDateFormat.format(date);
    }

    public Date parseApiDate(String value) {
        return parseDate(value);
    }

    private void seedCategories() {
        baseCategories.add(new Category("vegetables", "Овощи", "🥦", "#D5EDD8", "#2D6E35"));
        baseCategories.add(new Category("fruits", "Фрукты", "🍎", "#FDE8D2", "#A04010"));
        baseCategories.add(new Category("meat", "Мясо и птица", "🥩", "#FDD8E0", "#9E3050"));
        baseCategories.add(new Category("dairy", "Молочные", "🥛", "#D8EEF8", "#1A5C82"));
        baseCategories.add(new Category("grains", "Крупы и зерновые", "🌾", "#F5E8CC", "#7A5010"));
        baseCategories.add(new Category("seafood", "Морепродукты", "🐟", "#CCE4F5", "#1A4A82"));
        baseCategories.add(new Category("condiments", "Соусы и специи", "🧂", "#EDD5F5", "#6A2A82"));
        baseCategories.add(new Category("beverages", "Напитки", "🧃", "#D5F5E3", "#1A6A40"));
        baseCategories.add(new Category("frozen", "Заморозка", "🧊", "#D5F0F5", "#1A6A7A"));
        baseCategories.add(new Category("other", "Прочее", "📦", "#F5F0D5", "#6A5A10"));
    }

    private void seedProducts() {
        products.add(new Product("p1", "Помидоры", "vegetables", units(3, -1)));
        products.add(new Product("p2", "Огурцы", "vegetables", units(7)));
        products.add(new Product("p3", "Морковь", "vegetables", units(14, 14)));
        products.add(new Product("p4", "Лук репчатый", "vegetables", units(30)));
        products.add(new Product("p5", "Перец болгарский", "vegetables", units(5)));
        products.add(new Product("p6", "Чеснок", "vegetables", units(60)));
        products.add(new Product("p7", "Шпинат", "vegetables", new ArrayList<>()));
        products.add(new Product("p8", "Брокколи", "vegetables", new ArrayList<>()));
        products.add(new Product("p9", "Яблоки", "fruits", units(10, 10)));
        products.add(new Product("p10", "Бананы", "fruits", units(2)));
        products.add(new Product("p11", "Лимон", "fruits", units(15)));
        products.add(new Product("p12", "Курица", "meat", units(3)));
        products.add(new Product("p13", "Говядина", "meat", units(-2)));
        products.add(new Product("p14", "Свинина", "meat", new ArrayList<>()));
        products.add(new Product("p15", "Молоко", "dairy", units(5)));
        products.add(new Product("p16", "Яйца", "dairy", units(21, 21)));
        products.add(new Product("p17", "Сыр", "dairy", units(14)));
        products.add(new Product("p18", "Сметана", "dairy", units(7)));
        products.add(new Product("p19", "Макароны", "grains", units(180)));
        products.add(new Product("p20", "Рис", "grains", units(365)));
        products.add(new Product("p21", "Гречка", "grains", units(270)));
        products.add(new Product("p22", "Лосось", "seafood", units(2)));
        products.add(new Product("p23", "Оливковое масло", "condiments", units(180)));
        products.add(new Product("p24", "Томатная паста", "condiments", units(90)));
        products.add(new Product("p25", "Базилик свежий", "condiments", units(4)));
    }

    private List<ProductUnit> units(int... days) {
        List<ProductUnit> result = new ArrayList<>();
        for (int day : days) {
            result.add(new ProductUnit(newUnitId(), addDaysFromToday(day)));
        }
        return result;
    }

    private String addDaysFromToday(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, days);
        return apiDateFormat.format(calendar.getTime());
    }

    private String newUnitId() {
        return "u" + unitCounter++;
    }

    private Date parseDate(String value) {
        try {
            Date parsed = apiDateFormat.parse(value);
            return parsed != null ? parsed : new Date();
        } catch (ParseException ignored) {
            return new Date();
        }
    }

    private Date startOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public enum ExpiryStatus {
        FRESH,
        EXPIRING,
        EXPIRED
    }

    public static final class Category {
        public final String id;
        public final String name;
        public final String emoji;
        public final String bgColor;
        public final String textColor;

        public Category(String id, String name, String emoji, String bgColor, String textColor) {
            this.id = id;
            this.name = name;
            this.emoji = emoji;
            this.bgColor = bgColor;
            this.textColor = textColor;
        }

        public Category copy() {
            return new Category(id, name, emoji, bgColor, textColor);
        }
    }

    public static final class ProductUnit {
        public final String id;
        public final String expiryDate;

        public ProductUnit(String id, String expiryDate) {
            this.id = id;
            this.expiryDate = expiryDate;
        }

        public ProductUnit copy() {
            return new ProductUnit(id, expiryDate);
        }
    }

    public static final class Product {
        public final String id;
        public final String name;
        public final String categoryId;
        public final List<ProductUnit> units;

        public Product(String id, String name, String categoryId, List<ProductUnit> units) {
            this.id = id;
            this.name = name;
            this.categoryId = categoryId;
            this.units = units;
        }

        public Product copy() {
            List<ProductUnit> unitCopies = new ArrayList<>();
            for (ProductUnit unit : units) {
                unitCopies.add(unit.copy());
            }
            return new Product(id, name, categoryId, unitCopies);
        }
    }
}