package org.example.financeapp.controller;

import org.example.financeapp.model.Category;
import org.example.financeapp.model.User;
import org.example.financeapp.repository.CategoryRepository;
import org.example.financeapp.repository.UserRepository;
import org.example.financeapp.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public SettingsController(CategoryRepository categoryRepository,
                              UserRepository userRepository,
                              UserService userService) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping
    public String settings(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        model.addAttribute("categories", categoryRepository.findByUserId(user.getId()));
        model.addAttribute("newCategory", new Category());
        model.addAttribute("userCurrency", user.getCurrency());
        return "settings";
    }

    @PostMapping("/categories")
    public String addCategory(Category newCategory, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        newCategory.setUser(user);
        categoryRepository.save(newCategory);
        return "redirect:/settings";
    }

    @PostMapping("/update-currency")
    public String updateCurrency(@RequestParam("currency") String currency,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        userService.updateUserCurrency(userDetails.getUsername(), currency);
        return "redirect:/settings?success";
    }
}
