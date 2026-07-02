package com.openbake.server.config;

import com.openbake.server.entity.Address;
import com.openbake.server.entity.Category;
import com.openbake.server.entity.Coupon;
import com.openbake.server.entity.Product;
import com.openbake.server.entity.ProductVariant;
import com.openbake.server.entity.User;
import com.openbake.server.repository.AddressRepository;
import com.openbake.server.repository.CategoryRepository;
import com.openbake.server.repository.CouponRepository;
import com.openbake.server.repository.ProductRepository;
import com.openbake.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Ports backend/app/seed.py: categories, products+variants, coupons, admin + test-customer bootstrap. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public DataSeeder(CategoryRepository categoryRepository, ProductRepository productRepository,
                       CouponRepository couponRepository, UserRepository userRepository,
                       AddressRepository addressRepository, PasswordEncoder passwordEncoder,
                       AppProperties appProperties) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.couponRepository = couponRepository;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    @Override
    public void run(String... args) {
        seed();
    }

    @Transactional
    public String seed() {
        if (categoryRepository.count() > 0 && productRepository.count() > 0) {
            String msg = "Database already seeded with products and categories. Skipping.";
            log.info(msg);
            return msg;
        }

        log.info("Seeding database...");
        seedUsers();
        Map<String, Category> categories = seedCategories();
        seedProducts(categories);
        seedCoupons();

        String msg = "Database seeded successfully!";
        log.info(msg);
        return msg;
    }

    private void seedUsers() {
        userRepository.findByEmail(appProperties.getAdmin().getEmail()).orElseGet(() -> {
            User admin = new User();
            admin.setName("Admin");
            admin.setEmail(appProperties.getAdmin().getEmail());
            admin.setPasswordHash(passwordEncoder.encode(appProperties.getAdmin().getPassword()));
            admin.setAuthProvider("email");
            admin.setRole("admin");
            return userRepository.save(admin);
        });

        userRepository.findByEmail("customer@openbake.com").orElseGet(() -> {
            User customer = new User();
            customer.setName("Test Customer");
            customer.setEmail("customer@openbake.com");
            customer.setPhone("9876543210");
            customer.setPasswordHash(passwordEncoder.encode("Customer@123"));
            customer.setAuthProvider("email");
            customer.setRole("customer");
            User saved = userRepository.save(customer);

            Address address = new Address();
            address.setUser(saved);
            address.setLabel("Home");
            address.setFullAddress("123, Bakery Lane, Banjara Hills");
            address.setCity("Hyderabad");
            address.setPincode("500034");
            address.setDefault(true);
            addressRepository.save(address);

            return saved;
        });
    }

    private Map<String, Category> seedCategories() {
        record CategoryData(String name, String imageUrl) {}
        List<CategoryData> data = List.of(
                new CategoryData("Cakes", "https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=400"),
                new CategoryData("Pastries", "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=400"),
                new CategoryData("Breads", "https://images.unsplash.com/photo-1549931319-a545753d62ce?w=400"),
                new CategoryData("Cookies", "https://images.unsplash.com/photo-1499636136210-6f4ee915583e?w=400"),
                new CategoryData("Cupcakes", "https://images.unsplash.com/photo-1587668178277-295251f900ce?w=400"),
                new CategoryData("Beverages", "https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=400")
        );

        Map<String, Category> categories = new LinkedHashMap<>();
        for (CategoryData c : data) {
            Category category = categoryRepository.findByName(c.name()).orElseGet(() -> {
                Category cat = new Category();
                cat.setName(c.name());
                cat.setImageUrl(c.imageUrl());
                return categoryRepository.save(cat);
            });
            categories.put(c.name(), category);
        }
        return categories;
    }

    private record VariantData(String type, String value, double extraPrice) {}

    private record ProductData(String category, String name, String description, double price, int stockCount,
                                boolean eggless, boolean customizable, double rating, List<String> images,
                                List<VariantData> variants) {}

    private void seedProducts(Map<String, Category> categories) {
        List<ProductData> products = List.of(
                new ProductData("Cakes", "Classic Chocolate Cake",
                        "Rich, moist chocolate cake layered with velvety chocolate ganache. Perfect for celebrations or as a sweet treat any day.",
                        599, 25, true, true, 4.7,
                        List.of("https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=600"),
                        List.of(new VariantData("size", "0.5 kg", 0), new VariantData("size", "1 kg", 350),
                                new VariantData("size", "2 kg", 800), new VariantData("flavor", "Dark Chocolate", 0),
                                new VariantData("flavor", "Truffle", 150))),
                new ProductData("Cakes", "Red Velvet Cake",
                        "Stunning red velvet layers with cream cheese frosting. A showstopper for special occasions.",
                        699, 15, true, true, 4.8,
                        List.of("https://images.unsplash.com/photo-1616541823729-00fe0aacd32c?w=600"),
                        List.of(new VariantData("size", "0.5 kg", 0), new VariantData("size", "1 kg", 400))),
                new ProductData("Cakes", "Butterscotch Crunch Cake",
                        "Soft butterscotch sponge loaded with crunchy caramel bits and butterscotch sauce.",
                        549, 20, true, true, 4.5,
                        List.of("https://images.unsplash.com/photo-1621303837174-89787a7d4729?w=600"),
                        List.of(new VariantData("size", "0.5 kg", 0), new VariantData("size", "1 kg", 300))),
                new ProductData("Cakes", "Mango Delight Cake",
                        "Fresh mango cream cake made with real Alphonso mangoes. A seasonal favorite.",
                        749, 10, true, true, 4.9,
                        List.of("https://images.unsplash.com/photo-1565958011703-44f9829ba187?w=600"),
                        List.of(new VariantData("size", "0.5 kg", 0), new VariantData("size", "1 kg", 500))),
                new ProductData("Pastries", "Chocolate Éclair",
                        "Choux pastry filled with silky pastry cream and topped with chocolate glaze.",
                        120, 50, false, false, 4.3,
                        List.of("https://images.unsplash.com/photo-1509440159596-0249088772ff?w=600"), List.of()),
                new ProductData("Pastries", "Fruit Danish",
                        "Flaky puff pastry with custard cream and seasonal fresh fruits.",
                        150, 40, false, false, 4.4,
                        List.of("https://images.unsplash.com/photo-1517433670267-08bbd4be890f?w=600"), List.of()),
                new ProductData("Pastries", "Almond Croissant",
                        "Buttery croissant filled with almond frangipane and topped with sliced almonds.",
                        180, 35, false, false, 4.6,
                        List.of("https://images.unsplash.com/photo-1530610476181-d83430b64dcd?w=600"), List.of()),
                new ProductData("Breads", "Sourdough Loaf",
                        "Artisan sourdough bread with a crispy crust and tangy, chewy interior. 72-hour ferment.",
                        250, 30, true, false, 4.8,
                        List.of("https://images.unsplash.com/photo-1589367920969-ab8e050bbb04?w=600"), List.of()),
                new ProductData("Breads", "Garlic Focaccia",
                        "Italian-style flatbread infused with roasted garlic, olive oil, and rosemary.",
                        199, 25, true, false, 4.5,
                        List.of("https://images.unsplash.com/photo-1586444248902-2367d1a49ef3?w=600"), List.of()),
                new ProductData("Breads", "Multigrain Bread",
                        "Healthy multigrain bread packed with oats, flax, sunflower seeds, and whole wheat.",
                        149, 40, true, false, 4.2,
                        List.of("https://images.unsplash.com/photo-1509440159596-0249088772ff?w=600"), List.of()),
                new ProductData("Cookies", "Chocolate Chip Cookies (6 pcs)",
                        "Classic chewy cookies loaded with premium dark chocolate chips.",
                        199, 60, true, false, 4.6,
                        List.of("https://images.unsplash.com/photo-1499636136210-6f4ee915583e?w=600"), List.of()),
                new ProductData("Cookies", "Almond Biscotti (8 pcs)",
                        "Crunchy Italian-style biscotti with toasted almonds. Perfect with coffee.",
                        249, 45, true, false, 4.4,
                        List.of("https://images.unsplash.com/photo-1590080875515-8a3a8dc5735e?w=600"), List.of()),
                new ProductData("Cupcakes", "Vanilla Rainbow Cupcakes (4 pcs)",
                        "Fluffy vanilla cupcakes with rainbow buttercream swirl and sprinkles.",
                        349, 30, true, true, 4.7,
                        List.of("https://images.unsplash.com/photo-1587668178277-295251f900ce?w=600"),
                        List.of(new VariantData("flavor", "Vanilla", 0), new VariantData("flavor", "Chocolate", 50),
                                new VariantData("flavor", "Red Velvet", 80))),
                new ProductData("Cupcakes", "Salted Caramel Cupcakes (4 pcs)",
                        "Moist caramel cupcakes with salted caramel buttercream and a drizzle of caramel sauce.",
                        399, 20, true, false, 4.8,
                        List.of("https://images.unsplash.com/photo-1563729784474-d77dbb933a9e?w=600"), List.of()),
                new ProductData("Beverages", "Hot Chocolate",
                        "Rich and creamy Belgian hot chocolate topped with whipped cream.",
                        199, 100, true, false, 4.5,
                        List.of("https://images.unsplash.com/photo-1542990253-0d0f5be5f0ed?w=600"), List.of()),
                new ProductData("Beverages", "Iced Matcha Latte",
                        "Premium Japanese matcha whisked with chilled milk. Smooth and refreshing.",
                        249, 80, true, false, 4.3,
                        List.of("https://images.unsplash.com/photo-1515823064-d6e0c04616a7?w=600"), List.of())
        );

        for (ProductData p : products) {
            if (productRepository.findByName(p.name()).isPresent()) {
                continue;
            }
            Product product = new Product();
            product.setCategory(categories.get(p.category()));
            product.setName(p.name());
            product.setDescription(p.description());
            product.setPrice(p.price());
            product.setStockCount(p.stockCount());
            product.setEgglessAvailable(p.eggless());
            product.setCustomizable(p.customizable());
            product.setRating(p.rating());
            product.setImages(p.images());
            Product saved = productRepository.save(product);

            for (VariantData v : p.variants()) {
                ProductVariant variant = new ProductVariant();
                variant.setProduct(saved);
                variant.setVariantType(v.type());
                variant.setValue(v.value());
                variant.setExtraPrice(v.extraPrice());
                saved.getVariants().add(variant);
            }
            productRepository.save(saved);
        }
    }

    private void seedCoupons() {
        LocalDate today = LocalDate.now();
        record CouponData(String code, String type, double value, double minOrder, Integer maxUses, long validDays) {}
        List<CouponData> data = List.of(
                new CouponData("WELCOME10", "percent", 10, 200, null, 90),
                new CouponData("FLAT50", "flat", 50, 300, null, 60),
                new CouponData("BAKE20", "percent", 20, 500, 50, 30)
        );
        for (CouponData c : data) {
            if (couponRepository.findByCode(c.code()).isPresent()) {
                continue;
            }
            Coupon coupon = new Coupon();
            coupon.setCode(c.code());
            coupon.setDiscountType(c.type());
            coupon.setDiscountValue(c.value());
            coupon.setMinOrderValue(c.minOrder());
            if (c.maxUses() != null) {
                coupon.setMaxUses(c.maxUses());
            }
            coupon.setValidFrom(today);
            coupon.setValidUntil(today.plusDays(c.validDays()));
            couponRepository.save(coupon);
        }
    }
}
