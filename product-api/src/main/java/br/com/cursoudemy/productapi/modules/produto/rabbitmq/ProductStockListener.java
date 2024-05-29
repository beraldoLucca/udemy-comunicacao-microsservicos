package br.com.cursoudemy.productapi.modules.produto.rabbitmq;

import br.com.cursoudemy.productapi.modules.produto.dto.rabbitmq.ProductStockDTO;
import br.com.cursoudemy.productapi.modules.produto.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductStockListener {

    private final ProductService productService;

    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${app-config.rabbit.queue.product-stock}")
    public void receiveProductStockMessage(ProductStockDTO productStockDTO) throws JsonProcessingException {
        log.info("Receiving message with data: {} and TransactionID: {}", objectMapper
                .writeValueAsString(productStockDTO),
                productStockDTO.getTransactionid());
        productService.updateProductStock(productStockDTO);
    }
}
