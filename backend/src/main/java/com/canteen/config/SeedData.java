package com.canteen.config;

import com.canteen.entity.MenuItem;
import com.canteen.entity.User;
import com.canteen.repository.MenuItemRepository;
import com.canteen.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds a demo admin + sample menu on first startup so the app is
 * immediately usable. Safe to re-run (checks existence first).
 */
@Configuration
public class SeedData {

  @Bean
  CommandLineRunner seed(UserRepository users, MenuItemRepository menu,
      PasswordEncoder encoder,
      @Value("${app.admin.id:admin}") String adminId,
      @Value("${app.admin.password:admin123}") String adminPassword,
      @Value("${app.admin.name:Canteen Admin}") String adminName,
      @Value("${app.admin.email:admin@college.edu}") String adminEmail) {
    return args -> {
      if (!users.existsByStudentId(adminId)) {
        User admin = new User();
        admin.setStudentId(adminId);
        admin.setName(adminName);
        admin.setEmail(adminEmail);
        admin.setPasswordHash(encoder.encode(adminPassword));
        admin.setRole("ADMIN");
        users.save(admin);
        System.out.println("Seeded admin: " + adminId);
      }
      if (menu.count() == 0) {
        seedDish(menu, "Masala Dosa", 60, "Crispy dosa with potato masala + chutney & sambar", "Breakfast", null);
        seedDish(menu, "Veg Pulao + Raita", 80, "Fragrant veg pulao served with cool raita", "Lunch", null);
        seedDish(menu, "Paneer Butter Masala + 3 Roti", 110, "Creamy paneer curry with fresh rotis", "Lunch", null);
        seedDish(menu, "Veg Hakka Noodles", 75, "Wok-tossed noodles with crunchy veggies", "Snacks", null);
        seedDish(menu, "Samosa (2 pc) + Chutney", 30, "Golden fried samosas with tangy chutney", "Snacks", null);
        seedDish(menu, "Cold Coffee", 50, "Thick blended cold coffee", "Beverages", null);
        seedDish(menu, "Masala Chai", 20, "Classic kadak masala chai", "Beverages", null);
        seedDish(menu, "Veg Sandwich (Grilled)", 45, "Triple-layer grilled veg sandwich", "Snacks", null);
        System.out.println("Seeded sample menu (8 items)");
      }
    };
  }

  private void seedDish(MenuItemRepository menu, String name, double price,
      String desc, String category, String imageUrl) {
    MenuItem mi = new MenuItem();
    mi.setName(name);
    mi.setPrice(price);
    mi.setDescription(desc);
    mi.setCategory(category);
    mi.setAvailable(true);
    mi.setImageUrl(imageUrl);
    menu.save(mi);
  }
}
