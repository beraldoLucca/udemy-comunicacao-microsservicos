package br.com.cursoudemy.productapi.modules.category.controller;

import br.com.cursoudemy.productapi.config.exception.SuccessResponse;
import br.com.cursoudemy.productapi.modules.category.dto.request.CategoryRequest;
import br.com.cursoudemy.productapi.modules.category.dto.response.CategoryResponse;
import br.com.cursoudemy.productapi.modules.category.service.CategoryService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category")
@AllArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping("")
    public CategoryResponse save(@RequestBody CategoryRequest request){
        return categoryService.save(request);
    }

    @GetMapping
    public List<CategoryResponse> findAll(){
        return categoryService.findAll();
    }

    @GetMapping("{id}")
    public CategoryResponse findById(@PathVariable Integer id){
        return categoryService.findByIdResponse(id);
    }

    @GetMapping("description/{description}")
    public List<CategoryResponse> findByDescription(@PathVariable String description){
        return categoryService.findByDescription(description);
    }

    @DeleteMapping("{id}")
    public SuccessResponse delete(@PathVariable Integer id){
        return categoryService.delete(id);
    }

    @PutMapping("{id}")
    public CategoryResponse update(@RequestBody CategoryRequest request, @PathVariable Integer id){
        return categoryService.update(request, id);
    }
}
