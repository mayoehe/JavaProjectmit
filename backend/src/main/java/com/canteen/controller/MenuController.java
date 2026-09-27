package com.canteen.controller;

import com.canteen.entity.MenuItem;
import com.canteen.repository.MenuItemRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/menu")
@CrossOrigin
public class MenuController {

  private final MenuItemRepository menu;

  public MenuController(MenuItemRepository menu) {
    this.menu = menu;
  }

  /** Today's menu — students see available items; admins see everything. */
  @GetMapping
  public List<MenuItem> list(@RequestParam(defaultValue = "false") boolean all) {
    if (all) return menu.findAllByOrderByCategoryAscNameAsc();
    return menu.findByAvailableTrueOrderByCategoryAscNameAsc();
  }

  @GetMapping("/{id}")
  public MenuItem get(@PathVariable Long id) {
    return menu.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public MenuItem create(@Valid @RequestBody MenuItem body) {
    body.setId(null);
    return menu.save(body);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{id}")
  public MenuItem update(@PathVariable Long id, @RequestBody MenuItem body) {
    MenuItem mi = menu.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
    if (body.getName() != null) mi.setName(body.getName());
    if (body.getPrice() != 0) mi.setPrice(body.getPrice());
    if (body.getDescription() != null) mi.setDescription(body.getDescription());
    if (body.getCategory() != null) mi.setCategory(body.getCategory());
    mi.setAvailable(body.isAvailable());
    if (body.getImageUrl() != null) mi.setImageUrl(body.getImageUrl());
    return menu.save(mi);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping("/{id}/availability")
  public MenuItem availability(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
    MenuItem mi = menu.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
    mi.setAvailable(Boolean.TRUE.equals(body.get("available")));
    return menu.save(mi);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    if (!menu.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found");
    }
    menu.deleteById(id);
  }
}
