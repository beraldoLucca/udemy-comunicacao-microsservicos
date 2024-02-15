package br.com.cursoudemy.productapi.modules.produto.dto.request;

import br.com.cursoudemy.productapi.modules.produto.dto.rabbitmq.ProductQuantityDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCheckStock {

    private List<ProductQuantityDTO> products;
}
