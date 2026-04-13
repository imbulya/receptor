package com.example.receptor;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    private static final String PREFS_NAME = "receptor_repo";
    private static final String OTHER_CATEGORY_ID = "other";
    private static final String KEY_CUSTOM_CATEGORIES = "custom_categories";
    private static final String KEY_PRODUCTS = "products";
    private static final String KEY_EXPIRY_DAYS = "expiry_days";
    private static final String KEY_UNIT_COUNTER = "unit_counter";
    private static final int MAX_EXPIRY_WARNING_DAYS = 60;
    private static AppRepository instance;

    private final SimpleDateFormat apiDateFormat = new SimpleDateFormat(DATE_PATTERN, Locale.US);
    private final SimpleDateFormat uiDateFormat = new SimpleDateFormat("d MMM yyyy", RU_LOCALE);

    private final List<Category> baseCategories = new ArrayList<>();
    private final List<Category> customCategories = new ArrayList<>();
    private final List<Product> products = new ArrayList<>();
    private final SharedPreferences prefs;

    private int expiryWarningDays = 3;
    private int unitCounter = 1;

    private AppRepository(Context context) {
        prefs = context == null
                ? null
                : context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        seedCategories();
        loadFromStorage();
    }

    public static synchronized AppRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AppRepository(context);
        }
        return instance;
    }

    public static synchronized AppRepository getInstance() {
        return getInstance(null);
    }

    public synchronized List<Category> getVisibleCategories() {
        List<Category> result = new ArrayList<>();
        for (Category category : baseCategories) {
            if (!OTHER_CATEGORY_ID.equals(category.id)) {
                result.add(category.copy());
            }
        }
        for (Category category : customCategories) {
            result.add(category.copy());
        }
        return result;
    }

    public synchronized Category getCategoryById(String categoryId) {
        Category category = findCategoryInternal(categoryId);
        return category == null ? null : category.copy();
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

    public synchronized List<Product> getAllProducts() {
        List<Product> result = new ArrayList<>();
        for (Product product : products) {
            result.add(product.copy());
        }
        return result;
    }

    public synchronized int getExpiryWarningDays() {
        return expiryWarningDays;
    }

    public synchronized void setExpiryWarningDays(int days) {
        if (days < 1) {
            expiryWarningDays = 1;
        } else {
            expiryWarningDays = Math.min(days, MAX_EXPIRY_WARNING_DAYS);
        }
        persist();
    }

    public synchronized void addUnit(String productId, String expiryDate) {
        Product product = findProductInternalById(productId);
        if (product != null) {
            product.units.add(new ProductUnit(newUnitId(), expiryDate));
            persist();
        }
    }

    public synchronized void removeUnit(String productId) {
        Product product = findProductInternalById(productId);
        if (product != null && !product.units.isEmpty()) {
            product.units.remove(product.units.size() - 1);
            persist();
        }
    }

    public synchronized Category addCategory(String name, String emoji, String bgColor, String textColor) {
        String categoryId = "cat_" + System.currentTimeMillis();
        Category category = new Category(categoryId, name, emoji, bgColor, textColor);
        customCategories.add(category);
        persist();
        return category;
    }

    public synchronized boolean isSystemCategory(String categoryId) {
        if (categoryId == null) {
            return false;
        }
        for (Category category : baseCategories) {
            if (category.id.equals(categoryId)) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean removeCategory(String categoryId) {
        if (categoryId == null || categoryId.trim().isEmpty()) {
            return false;
        }
        for (Category category : baseCategories) {
            if (category.id.equals(categoryId)) {
                return false;
            }
        }
        if (OTHER_CATEGORY_ID.equals(categoryId)) {
            return false;
        }
        boolean removed = false;
        for (int i = 0; i < customCategories.size(); i++) {
            if (customCategories.get(i).id.equals(categoryId)) {
                customCategories.remove(i);
                removed = true;
                break;
            }
        }
        if (!removed) {
            return false;
        }
        for (int i = products.size() - 1; i >= 0; i--) {
            if (categoryId.equals(products.get(i).categoryId)) {
                products.remove(i);
            }
        }
        persist();
        return true;
    }

    public synchronized void addProductManual(String categoryId, String productName, int quantity, String expiryDate) {
        if (quantity <= 0) {
            return;
        }
        Product existing = findProductInternalByName(categoryId, productName);

        if (existing == null) {
            existing = new Product("p_custom_" + System.currentTimeMillis(), productName, categoryId, new ArrayList<>());
            products.add(existing);
        }

        for (int i = 0; i < quantity; i++) {
            existing.units.add(new ProductUnit(newUnitId(), expiryDate));
        }
        persist();
    }

    public synchronized boolean addProductNameOnly(String categoryId, String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            return false;
        }
        for (Product product : products) {
            if (product.categoryId.equals(categoryId) && product.name.equalsIgnoreCase(productName.trim())) {
                return false;
            }
        }
        products.add(new Product("p_custom_" + System.currentTimeMillis(), productName.trim(), categoryId, new ArrayList<>()));
        persist();
        return true;
    }

    public synchronized boolean removeUnitById(String productId, String unitId) {
        if (unitId == null) {
            return false;
        }
        Product product = findProductInternalById(productId);
        if (product == null) {
            return false;
        }
        for (int i = 0; i < product.units.size(); i++) {
            if (unitId.equals(product.units.get(i).id)) {
                product.units.remove(i);
                persist();
                return true;
            }
        }
        return false;
    }

    public synchronized boolean removeProduct(String productId) {
        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).id.equals(productId)) {
                products.remove(i);
                persist();
                return true;
            }
        }
        return false;
    }

    public synchronized void resetInventory() {
        customCategories.clear();
        products.clear();
        unitCounter = 1;
        seedProducts();
        persist();
    }

    private void loadFromStorage() {
        if (prefs == null) {
            seedProducts();
            return;
        }

        expiryWarningDays = prefs.getInt(KEY_EXPIRY_DAYS, expiryWarningDays);

        String customJson = prefs.getString(KEY_CUSTOM_CATEGORIES, null);
        if (customJson != null) {
            loadCustomCategories(customJson);
        }

        String productsJson = prefs.getString(KEY_PRODUCTS, null);
        if (productsJson != null) {
            loadProducts(productsJson);
        } else {
            seedProducts();
        }

        if (prefs.contains(KEY_UNIT_COUNTER)) {
            unitCounter = prefs.getInt(KEY_UNIT_COUNTER, unitCounter);
        } else {
            unitCounter = computeNextUnitCounter();
        }
    }

    private void loadCustomCategories(String json) {
        customCategories.clear();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                customCategories.add(new Category(
                        obj.optString("id"),
                        obj.optString("name"),
                        obj.optString("emoji"),
                        obj.optString("bgColor"),
                        obj.optString("textColor")
                ));
            }
        } catch (JSONException ignored) {
            customCategories.clear();
        }
    }

    private void loadProducts(String json) {
        products.clear();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                List<ProductUnit> units = new ArrayList<>();
                JSONArray unitsArray = obj.optJSONArray("units");
                if (unitsArray != null) {
                    for (int j = 0; j < unitsArray.length(); j++) {
                        JSONObject unitObj = unitsArray.getJSONObject(j);
                        units.add(new ProductUnit(
                                unitObj.optString("id"),
                                unitObj.optString("expiryDate")
                        ));
                    }
                }
                products.add(new Product(
                        obj.optString("id"),
                        obj.optString("name"),
                        obj.optString("categoryId"),
                        units
                ));
            }
        } catch (JSONException ignored) {
            products.clear();
            seedProducts();
        }
    }

    private void persist() {
        if (prefs == null) {
            return;
        }
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_EXPIRY_DAYS, expiryWarningDays);
        editor.putInt(KEY_UNIT_COUNTER, unitCounter);
        editor.putString(KEY_CUSTOM_CATEGORIES, categoriesToJson(customCategories).toString());
        editor.putString(KEY_PRODUCTS, productsToJson(products).toString());
        editor.apply();
    }

    private JSONArray categoriesToJson(List<Category> categories) {
        JSONArray array = new JSONArray();
        for (Category category : categories) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", category.id);
                obj.put("name", category.name);
                obj.put("emoji", category.emoji);
                obj.put("bgColor", category.bgColor);
                obj.put("textColor", category.textColor);
                array.put(obj);
            } catch (JSONException ignored) {
            }
        }
        return array;
    }

    private JSONArray productsToJson(List<Product> items) {
        JSONArray array = new JSONArray();
        for (Product product : items) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", product.id);
                obj.put("name", product.name);
                obj.put("categoryId", product.categoryId);
                JSONArray unitsArray = new JSONArray();
                for (ProductUnit unit : product.units) {
                    JSONObject unitObj = new JSONObject();
                    unitObj.put("id", unit.id);
                    unitObj.put("expiryDate", unit.expiryDate);
                    unitsArray.put(unitObj);
                }
                obj.put("units", unitsArray);
                array.put(obj);
            } catch (JSONException ignored) {
            }
        }
        return array;
    }

    private int computeNextUnitCounter() {
        int max = 0;
        for (Product product : products) {
            for (ProductUnit unit : product.units) {
                int value = parseUnitCounterValue(unit.id);
                if (value > max) {
                    max = value;
                }
            }
        }
        return Math.max(1, max + 1);
    }

    public String getDefaultExpiryDateForNewUnit() {
        return addDaysFromToday(7);
    }

    public synchronized String formatExpiryDate(String expiryDate) {
        Date date = parseDate(expiryDate);
        return uiDateFormat.format(date);
    }

    public synchronized ExpiryStatus getExpiryStatus(String expiryDate) {
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

    public synchronized String toApiDate(Date date) {
        return apiDateFormat.format(date);
    }

    public synchronized Date parseApiDate(String value) {
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
        baseCategories.add(new Category(OTHER_CATEGORY_ID, "Прочее", "📦", "#F5F0D5", "#6A5A10"));
    }

    private void seedProducts() {
    }

    private Category findCategoryInternal(String categoryId) {
        if (categoryId == null) {
            return null;
        }
        for (Category category : baseCategories) {
            if (category.id.equals(categoryId)) {
                return category;
            }
        }
        for (Category category : customCategories) {
            if (category.id.equals(categoryId)) {
                return category;
            }
        }
        return null;
    }

    private Product findProductInternalById(String productId) {
        if (productId == null) {
            return null;
        }
        for (Product product : products) {
            if (product.id.equals(productId)) {
                return product;
            }
        }
        return null;
    }

    private Product findProductInternalByName(String categoryId, String productName) {
        if (categoryId == null || productName == null) {
            return null;
        }
        for (Product product : products) {
            if (product.categoryId.equals(categoryId) && product.name.equalsIgnoreCase(productName)) {
                return product;
            }
        }
        return null;
    }

    private int parseUnitCounterValue(String unitId) {
        String id = unitId == null ? "" : unitId;
        int value = 0;
        int idx = id.startsWith("u") ? 1 : 0;
        for (int i = idx; i < id.length(); i++) {
            char c = id.charAt(i);
            if (!Character.isDigit(c)) {
                return 0;
            }
            value = value * 10 + (c - '0');
        }
        return value;
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
