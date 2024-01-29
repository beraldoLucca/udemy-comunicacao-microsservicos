package br.com.cursoudemy.productapi.modules.produto.service;

import br.com.cursoudemy.productapi.config.exception.SuccessResponse;
import br.com.cursoudemy.productapi.config.exception.ValidationException;
import br.com.cursoudemy.productapi.modules.category.service.CategoryService;
import br.com.cursoudemy.productapi.modules.produto.dto.request.ProductRequest;
import br.com.cursoudemy.productapi.modules.produto.dto.response.ProductResponse;
import br.com.cursoudemy.productapi.modules.produto.model.Product;
import br.com.cursoudemy.productapi.modules.produto.repository.IProductRepository;
import br.com.cursoudemy.productapi.modules.supplier.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static io.micrometer.common.util.StringUtils.isBlank;
import static org.hibernate.type.descriptor.java.IntegerJavaType.ZERO;

@Service
public class ProductService {

    @Autowired
    private IProductRepository iProductRepository;
    @Autowired
    private SupplierService supplierService;
    @Autowired
    private CategoryService categoryService;

    public Product findById(Integer id){
        validateInformedId(id);
        return iProductRepository
                .findById(id)
                .orElseThrow(() -> new ValidationException("There's no product for the given ID."));
    }

    public ProductResponse findByIdResponse(Integer id){
        return ProductResponse.of(findById(id));
    }

    public List<ProductResponse> findByName(String name){
        if (isBlank(name)){
            throw new ValidationException("The product name should be informed.");
        }
        return iProductRepository
                .findByNameIgnoreCaseContaining(name)
                .stream()
                .map(ProductResponse::of)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> findBySupplierId(Integer supplierId){
        validateInformedId(supplierId);
        return iProductRepository
                .findBySupplierId(supplierId)
                .stream()
                .map(ProductResponse::of)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> findByCategoryId(Integer categoryId){
        validateInformedId(categoryId);
        return iProductRepository
                .findBySupplierId(categoryId)
                .stream()
                .map(ProductResponse::of)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> findAll(){
        return iProductRepository
                .findAll()
                .stream()
                .map(ProductResponse::of)
                .collect(Collectors.toList());
    }

    public ProductResponse save(ProductRequest request){
        validateProductDataInformed(request);
        validateCategoryAndSupplierInformed(request);
        var category = categoryService.findById(request.getCategoryId());
        var supplier = supplierService.findById(request.getSupplierId());
        var product = iProductRepository.save(Product.of(request, supplier, category));
        return ProductResponse.of(product);
    }

    public ProductResponse update(ProductRequest request, Integer id){
        validateProductDataInformed(request);
        validateInformedId(id);
        validateCategoryAndSupplierInformed(request);
        var category = categoryService.findById(request.getCategoryId());
        var supplier = supplierService.findById(request.getSupplierId());
        var product = Product.of(request, supplier, category);
        product.setId(id);
        iProductRepository.save(product);
        return ProductResponse.of(product);
    }

    public Boolean existsByCategoryId(Integer categoryId){
        return iProductRepository.existsByCategoryId(categoryId);
    }

    public Boolean existsBySupplierId(Integer supplierId){
        return iProductRepository.existsBySupplierId(supplierId);
    }

    public SuccessResponse delete(Integer id){
        validateInformedId(id);
        iProductRepository.deleteById(id);
        return SuccessResponse.create("This product was deleted.");
    }

    private void validateInformedId(Integer id){
        if(isBlank(String.valueOf(id))){
            throw new ValidationException("ID should be informed.");
        }
    }

    private void validateProductDataInformed(ProductRequest request){
        if(isBlank(request.getName())){
            throw new ValidationException("The product's name was not informed.");
        }
        if(isBlank(String.valueOf(request.getQuantityAvailable()))){
            throw new ValidationException("The product's quantity available was not informed.");
        }
        if(request.getQuantityAvailable() <= ZERO){
            throw new ValidationException("The quantity should not be less or equal to zero.");
        }
    }

    private void validateCategoryAndSupplierInformed(ProductRequest request){
        if(isBlank(String.valueOf(request.getCategoryId()))){
            throw new ValidationException("The category Id name was not informed.");
        }
        if(isBlank(String.valueOf(request.getSupplierId()))){
            throw new ValidationException("The supplier Id name was not informed.");
        }
    }
}
