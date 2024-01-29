package br.com.cursoudemy.productapi.modules.supplier.service;

import br.com.cursoudemy.productapi.config.exception.SuccessResponse;
import br.com.cursoudemy.productapi.config.exception.ValidationException;
import br.com.cursoudemy.productapi.modules.produto.service.ProductService;
import br.com.cursoudemy.productapi.modules.supplier.dto.request.SupplierRequest;
import br.com.cursoudemy.productapi.modules.supplier.dto.response.SupplierResponse;
import br.com.cursoudemy.productapi.modules.supplier.model.Supplier;
import br.com.cursoudemy.productapi.modules.supplier.repository.ISupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static io.micrometer.common.util.StringUtils.isBlank;

@Service
public class SupplierService {

    @Autowired
    private ISupplierRepository iSupplierRepository;

    @Autowired
    private ProductService productService;

    public Supplier findById(Integer id){
        validateInformedId(id);
        return iSupplierRepository
                .findById(id)
                .orElseThrow(() -> new ValidationException("There's no supplier for the given ID."));
    }

    public SupplierResponse findByIdResponse(Integer id){
        return SupplierResponse.of(findById(id));
    }

    public List<SupplierResponse> findByName(String name){
        if (isBlank(name)){
            throw new ValidationException("The supplier name should be informed.");
        }
        return iSupplierRepository
                .findByNameIgnoreCaseContaining(name)
                .stream()
                .map(SupplierResponse::of)
                .collect(Collectors.toList());
    }

    public List<SupplierResponse> findAll(){
        return iSupplierRepository
                .findAll()
                .stream()
                .map(SupplierResponse::of)
                .collect(Collectors.toList());
    }

    public SupplierResponse save(SupplierRequest request){
        validateSupplierNameInformed(request);
        var supplier = iSupplierRepository.save(Supplier.of(request));
        return SupplierResponse.of(supplier);
    }

    public SupplierResponse update(SupplierRequest request, Integer id){
        validateSupplierNameInformed(request);
        validateInformedId(id);
        var supplier = Supplier.of(request);
        supplier.setId(id);
        iSupplierRepository.save(supplier);
        return SupplierResponse.of(supplier);
    }

    public SuccessResponse delete(Integer id){
        validateInformedId(id);
        if(productService.existsBySupplierId(id)){
            throw new ValidationException("You cannot delete this supplier because its already defined by a product.");
        }
        iSupplierRepository.deleteById(id);
        return SuccessResponse.create("This supplier was deleted.");
    }

    private void validateInformedId(Integer id){
        if(isBlank(String.valueOf(id))){
            throw new ValidationException("ID should be informed.");
        }
    }

    private void validateSupplierNameInformed(SupplierRequest request){
        if(isBlank(request.getName())){
            throw new ValidationException("The supplier's name was not informed.");
        }
    }
}
