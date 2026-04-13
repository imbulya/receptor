package com.example.receptor;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DataClient {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private DataClient() {
    }

    public static void fetchRecipes(Callback<List<RecipeStore.Recipe>> callback) {
        runOn(EXECUTOR, callback, () -> RecipeStore.getAllRecipes(requireContext()));
    }

    public static void fetchRecipeDetail(String id, Callback<RecipeStore.Recipe> callback) {
        runOn(EXECUTOR, callback, () -> {
            Context context = requireContext();
            RecipeStore.Recipe recipe = findRecipeById(context, id);
            if (recipe == null) {
                throw new IOException("Recipe not found");
            }
            return recipe;
        });
    }

    public static void fetchProductCategories(Callback<List<CategoryInfo>> callback) {
        runOn(EXECUTOR, callback, () -> {
            AppRepository repository = AppRepository.getInstance(requireContext());
            List<AppRepository.Category> categories = repository.getVisibleCategories();
            List<CategoryInfo> result = new ArrayList<>();
            for (AppRepository.Category category : categories) {
                int count = repository.getInStockCategoryCount(category.id);
                boolean isSystem = repository.isSystemCategory(category.id);
                result.add(new CategoryInfo(category, count, isSystem));
            }
            return result;
        });
    }

    public static void fetchProductCategory(String id, Callback<CategoryInfo> callback) {
        runOn(EXECUTOR, callback, () -> {
            AppRepository repository = AppRepository.getInstance(requireContext());
            AppRepository.Category category = repository.getCategoryById(id);
            if (category == null) {
                throw new IOException("Category not found");
            }
            int count = repository.getInStockCategoryCount(id);
            boolean isSystem = repository.isSystemCategory(id);
            return new CategoryInfo(category, count, isSystem);
        });
    }

    public static void createProductCategory(String name, String emoji, String bgColor, String textColor, Callback<CategoryInfo> callback) {
        runOn(EXECUTOR, callback, () -> {
            AppRepository repository = AppRepository.getInstance(requireContext());
            AppRepository.Category category = repository.addCategory(name, emoji, bgColor, textColor);
            if (category == null) {
                throw new IOException("Category create failed");
            }
            return new CategoryInfo(category, 0, false);
        });
    }

    public static void deleteProductCategory(String id, Callback<Boolean> callback) {
        runOn(EXECUTOR, callback, () -> {
            AppRepository repository = AppRepository.getInstance(requireContext());
            boolean removed = repository.removeCategory(id);
            if (!removed) {
                throw new IOException("Category delete failed");
            }
            return true;
        });
    }

    public static void fetchProductsByCategory(String categoryId, Callback<List<AppRepository.Product>> callback) {
        runOn(EXECUTOR, callback, () -> {
            AppRepository repository = AppRepository.getInstance(requireContext());
            return repository.getProductsByCategory(categoryId);
        });
    }

    public static void fetchAllProducts(Callback<List<AppRepository.Product>> callback) {
        runOn(EXECUTOR, callback, () -> {
            AppRepository repository = AppRepository.getInstance(requireContext());
            return repository.getAllProducts();
        });
    }

    public static void addProduct(String categoryId, String name, Callback<AppRepository.Product> callback) {
        runOn(EXECUTOR, callback, () -> {
            AppRepository repository = AppRepository.getInstance(requireContext());
            boolean added = repository.addProductNameOnly(categoryId, name);
            if (!added) {
                throw new IOException("Product already exists");
            }
            AppRepository.Product product = findProductByName(repository, categoryId, name);
            if (product == null) {
                throw new IOException("Product not found after insert");
            }
            return product;
        });
    }

    public static void addProductManual(String categoryId, String name, int quantity, String expiryDate, Callback<Boolean> callback) {
        runOn(EXECUTOR, callback, () -> {
            AppRepository repository = AppRepository.getInstance(requireContext());
            repository.addProductManual(categoryId, name, quantity, expiryDate);
            return true;
        });
    }

    public static void addProductUnit(String productId, String expiryDate, Callback<AppRepository.ProductUnit> callback) {
        runOn(EXECUTOR, callback, () -> {
            AppRepository repository = AppRepository.getInstance(requireContext());
            repository.addUnit(productId, expiryDate);
            AppRepository.Product product = findProductById(repository, productId);
            if (product == null || product.units.isEmpty()) {
                throw new IOException("Product unit add failed");
            }
            return product.units.get(product.units.size() - 1);
        });
    }

    public static void deleteProductUnit(String productId, String unitId, Callback<Boolean> callback) {
        runOn(EXECUTOR, callback, () -> {
            AppRepository repository = AppRepository.getInstance(requireContext());
            boolean removed = repository.removeUnitById(productId, unitId);
            if (!removed) {
                throw new IOException("Product unit delete failed");
            }
            return true;
        });
    }

    public static void deleteProduct(String productId, Callback<Boolean> callback) {
        runOn(EXECUTOR, callback, () -> {
            AppRepository repository = AppRepository.getInstance(requireContext());
            boolean removed = repository.removeProduct(productId);
            if (!removed) {
                throw new IOException("Product delete failed");
            }
            return true;
        });
    }

    public static void fetchShoppingItems(Callback<List<ShoppingItem>> callback) {
        runOn(EXECUTOR, callback, () -> ShoppingStorage.load(requireContext()));
    }

    public static void addShoppingItem(String name, String quantity, Callback<ShoppingItem> callback) {
        runOn(EXECUTOR, callback, () -> {
            Context context = requireContext();
            List<ShoppingItem> items = ShoppingStorage.load(context);
            ShoppingItem item = new ShoppingItem("s" + System.currentTimeMillis(), name, quantity, false);
            items.add(item);
            ShoppingStorage.persist(context, items);
            return item;
        });
    }

    public static void updateShoppingItem(String id, String name, String quantity, Boolean isChecked, Callback<ShoppingItem> callback) {
        runOn(EXECUTOR, callback, () -> {
            Context context = requireContext();
            List<ShoppingItem> items = ShoppingStorage.load(context);
            ShoppingItem updated = null;
            for (int i = 0; i < items.size(); i++) {
                ShoppingItem item = items.get(i);
                if (item.id.equals(id)) {
                    String newName = name == null ? item.name : name;
                    String newQuantity = quantity == null ? item.quantity : quantity;
                    boolean newChecked = isChecked == null ? item.isChecked : isChecked;
                    updated = new ShoppingItem(item.id, newName, newQuantity, newChecked);
                    items.set(i, updated);
                    break;
                }
            }
            if (updated == null) {
                throw new IOException("Shopping item not found");
            }
            ShoppingStorage.persist(context, items);
            return updated;
        });
    }

    public static void deleteShoppingItem(String id, Callback<Boolean> callback) {
        runOn(EXECUTOR, callback, () -> {
            Context context = requireContext();
            List<ShoppingItem> items = ShoppingStorage.load(context);
            boolean removed = items.removeIf(item -> item.id.equals(id));
            if (!removed) {
                throw new IOException("Shopping item delete failed");
            }
            ShoppingStorage.persist(context, items);
            return true;
        });
    }

    public static void clearShoppingList(Context context) {
        Context safe = context == null ? ReceptorApp.getContext() : context;
        if (safe == null) {
            return;
        }
        ShoppingStorage.clear(safe);
    }

    private static Context requireContext() {
        Context context = ReceptorApp.getContext();
        if (context == null) {
            throw new IllegalStateException("App context not ready");
        }
        return context;
    }

    private static RecipeStore.Recipe findRecipeById(Context context, String id) {
        if (id == null) {
            return null;
        }
        for (RecipeStore.Recipe recipe : RecipeStore.getAllRecipes(context)) {
            if (id.equals(recipe.id)) {
                return recipe;
            }
        }
        return null;
    }

    private static AppRepository.Product findProductById(AppRepository repository, String productId) {
        if (productId == null) {
            return null;
        }
        for (AppRepository.Product product : repository.getAllProducts()) {
            if (product.id.equals(productId)) {
                return product;
            }
        }
        return null;
    }

    private static AppRepository.Product findProductByName(AppRepository repository, String categoryId, String name) {
        if (categoryId == null || name == null) {
            return null;
        }
        for (AppRepository.Product product : repository.getProductsByCategory(categoryId)) {
            if (product.name.equalsIgnoreCase(name)) {
                return product;
            }
        }
        return null;
    }

    private interface Task<T> {
        T run() throws Exception;
    }

    private static <T> void runOn(ExecutorService executor, Callback<T> callback, Task<T> task) {
        executor.execute(() -> {
            try {
                postSuccess(callback, task.run());
            } catch (Exception e) {
                postError(callback, e);
            }
        });
    }

    private static final class ShoppingStorage {

        private static final String FILE_NAME = "shopping_list.json";

        private ShoppingStorage() {
        }

        static List<ShoppingItem> load(Context context) throws IOException {
            File file = new File(context.getFilesDir(), FILE_NAME);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            String json;
            try (FileInputStream inputStream = new FileInputStream(file)) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                json = output.size() > 0 ? output.toString(StandardCharsets.UTF_8.name()) : "[]";
            }
            if (!json.isEmpty() && json.charAt(0) == '\uFEFF') {
                json = json.substring(1);
            }
            JSONArray array;
            try {
                array = new JSONArray(json);
            } catch (JSONException ignored) {
                return new ArrayList<>();
            }
            List<ShoppingItem> result = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj == null) {
                    continue;
                }
                ShoppingItem item = parseObject(obj);
                if (item != null) {
                    result.add(item);
                }
            }
            return result;
        }

        static void persist(Context context, List<ShoppingItem> items) throws IOException {
            JSONArray array = new JSONArray();
            for (ShoppingItem item : items) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put("id", item.id);
                    obj.put("name", item.name);
                    obj.put("quantity", item.quantity);
                    obj.put("isChecked", item.isChecked);
                } catch (JSONException ignored) {
                }
                array.put(obj);
            }
            File file = new File(context.getFilesDir(), FILE_NAME);
            File tmp = new File(context.getFilesDir(), FILE_NAME + ".tmp");
            try (FileOutputStream outputStream = new FileOutputStream(tmp, false)) {
                outputStream.write(array.toString().getBytes(StandardCharsets.UTF_8));
            }
            if (file.exists() && !file.delete()) {
                throw new IOException("Failed to replace shopping list");
            }
            if (!tmp.renameTo(file)) {
                throw new IOException("Failed to persist shopping list");
            }
        }

        static void clear(Context context) {
            File file = new File(context.getFilesDir(), FILE_NAME);
            if (file.exists()) {
                file.delete();
            }
        }

        private static ShoppingItem parseObject(JSONObject obj) {
            String id = obj.optString("id", "");
            String name = obj.optString("name", "");
            String quantity = obj.optString("quantity", "");
            boolean isChecked = obj.optBoolean("isChecked", false);
            if (id.isEmpty() || name.isEmpty()) {
                return null;
            }
            return new ShoppingItem(id, name, quantity, isChecked);
        }
    }

    private static <T> void postSuccess(Callback<T> callback, T data) {
        MAIN.post(() -> callback.onSuccess(data));
    }

    private static void postError(Callback<?> callback, Exception error) {
        MAIN.post(() -> callback.onError(error));
    }

    public interface Callback<T> {
        void onSuccess(T data);

        void onError(Exception error);
    }

    public static final class CategoryInfo {
        public final AppRepository.Category category;
        public final int inStockCount;
        public final boolean isSystem;

        public CategoryInfo(AppRepository.Category category, int inStockCount, boolean isSystem) {
            this.category = category;
            this.inStockCount = inStockCount;
            this.isSystem = isSystem;
        }
    }

    public static final class ShoppingItem {
        public final String id;
        public final String name;
        public final String quantity;
        public final boolean isChecked;

        public ShoppingItem(String id, String name, String quantity, boolean isChecked) {
            this.id = id;
            this.name = name;
            this.quantity = quantity;
            this.isChecked = isChecked;
        }
    }
}
