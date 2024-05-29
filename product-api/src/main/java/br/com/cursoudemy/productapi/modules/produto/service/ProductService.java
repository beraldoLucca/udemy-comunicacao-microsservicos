package br.com.cursoudemy.productapi.modules.produto.service;

import br.com.cursoudemy.productapi.config.exception.SuccessResponse;
import br.com.cursoudemy.productapi.config.exception.ValidationException;
import br.com.cursoudemy.productapi.modules.category.service.CategoryService;
import br.com.cursoudemy.productapi.modules.produto.dto.rabbitmq.ProductQuantityDTO;
import br.com.cursoudemy.productapi.modules.produto.dto.rabbitmq.ProductStockDTO;
import br.com.cursoudemy.productapi.modules.produto.dto.request.ProductCheckStock;
import br.com.cursoudemy.productapi.modules.produto.dto.request.ProductRequest;
import br.com.cursoudemy.productapi.modules.produto.dto.response.ProductResponse;
import br.com.cursoudemy.productapi.modules.produto.dto.response.ProductSalesResponse;
import br.com.cursoudemy.productapi.modules.produto.model.Product;
import br.com.cursoudemy.productapi.modules.produto.repository.IProductRepository;
import br.com.cursoudemy.productapi.modules.sales.client.SalesClient;
import br.com.cursoudemy.productapi.modules.sales.dto.SalesConfirmationDTO;
import br.com.cursoudemy.productapi.modules.sales.enums.SalesStatus;
import br.com.cursoudemy.productapi.modules.sales.rabbitmq.SalesConfirmationSender;
import br.com.cursoudemy.productapi.modules.supplier.service.SupplierService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static br.com.cursoudemy.productapi.config.RequestUtil.getCurrentRequest;
import static io.micrometer.common.util.StringUtils.isBlank;
import static org.springframework.util.ObjectUtils.isEmpty;

@Slf4j
@Service
@AllArgsConstructor
public class ProductService {

    private static final Integer ZERO = 0;
    private static final String TRANSACTION_ID = "transactionid";
    private static final String SERVICE_ID = "serviceid";
    private static final String AUTHORIZATION = "Authorization";


    private final IProductRepository iProductRepository;

    private final SupplierService supplierService;

    private final CategoryService categoryService;

    private final SalesConfirmationSender salesConfirmationSender;

    private final SalesClient salesClient;

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
        if(isEmpty(request.getQuantityAvailable())){
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

    public void updateProductStock(ProductStockDTO productStockDTO){
        try{
            validateStockUpdateData(productStockDTO);
            updateStock(productStockDTO);
        } catch (Exception ex){
            log.error("Error while trying to update stock for message with error: {}", ex.getMessage(), ex);
            var rejectedMessage = new SalesConfirmationDTO(productStockDTO.getSalesId(),
                    SalesStatus.REJECTED, productStockDTO.getTransactionid());
            salesConfirmationSender.sendSalesConfirmationMessage(rejectedMessage);
        }
    }

    private void updateStock(ProductStockDTO productStockDTO){
        var productsForUpdate = new ArrayList<Product>();

        productStockDTO.getProducts()
                .forEach(salesProduct -> {
                    var existingProduct = findById(salesProduct.getProductId());
                    validateQuantityInStock(salesProduct, existingProduct);
                    existingProduct.updateStock(salesProduct.getQuantity());
                    productsForUpdate.add(existingProduct);
                });
        if(!isEmpty(productsForUpdate)){
            iProductRepository.saveAll(productsForUpdate);
            var approvedMessage = new SalesConfirmationDTO(productStockDTO.getSalesId(), SalesStatus.APPROVED,
                    productStockDTO.getTransactionid());
            salesConfirmationSender.sendSalesConfirmationMessage(approvedMessage);
        }
    }

    @Transactional
    private void validateStockUpdateData(ProductStockDTO productStockDTO){
        if (isEmpty(productStockDTO) || isEmpty(productStockDTO.getSalesId())){
            throw new ValidationException("The product data or sales ID should be informed.");
        }
        if(isEmpty(productStockDTO.getProducts())){
            throw new ValidationException("The sales products should be informed");
        }
        validateQuantityAndProductId(productStockDTO);
    }

    @Transactional
    private void validateQuantityAndProductId(ProductStockDTO product){
        product
                .getProducts()
                .forEach(salesProduct -> {
                    if (isEmpty(salesProduct.getQuantity())
                        || isEmpty(salesProduct.getProductId())){
                        throw new ValidationException("THe productID and the quantity should be informed.");
                    }
                });
    }

    private void validateQuantityInStock(ProductQuantityDTO productQuantityDTO,
                                         Product existingProduct){
        if(productQuantityDTO.getQuantity() > existingProduct.getQuantityAvailable()){
            throw new ValidationException(
                    String.format("The product %s is out of stock.", existingProduct.getId()));
        }
    }

    public ProductSalesResponse findProductSalesById(Integer id){
        var product = findById(id);
        try{
            var currentRequest = getCurrentRequest();
            var token = currentRequest.getHeader(AUTHORIZATION);
            var transactionId = currentRequest.getHeader(TRANSACTION_ID);
            var serviceid = currentRequest.getAttribute(SERVICE_ID);
            log.info("Sending GET Request to orders by productId with data {} | [transactionID: ${} | serviceID: ${}",
                    product, transactionId, serviceid);
            var sales = salesClient.findSalesByProductId(product.getId(), token, transactionId)
                    .orElseThrow(() -> new ValidationException("The sales was not found by this product."));
            log.info("Receiving response from orders by productId with data {} | [transactionID: ${} | serviceID: ${}",
                    new ObjectMapper().writeValueAsString(sales), transactionId, serviceid);
            return ProductSalesResponse.of(product, sales.getSalesId());
        } catch (Exception ex){
            ex.printStackTrace();
            throw new ValidationException("There was an error trying to get the product's sales.");
        }
    }

    public SuccessResponse checkProductsStock(ProductCheckStock productCheckStock){
        try{
            var currentRequest = getCurrentRequest();
            var transactionid = currentRequest.getHeader(TRANSACTION_ID);
            var serviceid = currentRequest.getAttribute(SERVICE_ID);
            log.info("Request to POST product stock with data {} | [transactionID: ${} | serviceID: ${}",
                    new ObjectMapper().writeValueAsString(productCheckStock),
                    transactionid, serviceid);
            if(isEmpty(productCheckStock)){
                throw new ValidationException("The request data and products must be informed.");
            }
            productCheckStock
                    .getProducts()
                    .forEach(this::validateStock);

            var response = SuccessResponse.create("The stock is ok!!");
            log.info("Response to POST product stock with data {} | [transactionID: ${} | serviceID: ${}",
                    new ObjectMapper().writeValueAsString(response),
                    transactionid, serviceid);
            return response;
        } catch (Exception ex){
            throw new ValidationException(ex.getMessage());
        }
    }

    private void validateStock(ProductQuantityDTO productQuantity){
        if(isEmpty(productQuantity.getProductId()) || isEmpty(productQuantity.getQuantity())){
            throw new ValidationException("Product ID and quantity must be informed.");
        }
        var product = findById(productQuantity.getProductId());
        if(productQuantity.getQuantity() > product.getQuantityAvailable()){
            throw new ValidationException(String.format("The product %s is out of stock", product.getId()));
        }
    }
}
