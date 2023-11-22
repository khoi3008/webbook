package org.weebook.api.service.impl;

import lombok.AllArgsConstructor;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.weebook.api.dto.ProductDto;
import org.weebook.api.dto.mapper.ProductMapper;
import org.weebook.api.entity.Category;
import org.weebook.api.entity.Product;
import org.weebook.api.repository.ProductRepository;
import org.weebook.api.service.ProductService;
import org.weebook.api.util.CriteriaUtility;
import org.weebook.api.web.request.FilterRequest;
import org.weebook.api.web.request.PagingRequest;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import java.util.Optional;

@Service
@AllArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;


    @Override
    public ProductDto saveProduct(ProductDto productDto) {
        Product product = productRepository.save(productMapper.toEntity(productDto));
        return productMapper.toDto(product);
    }

    @Override
    public PageImpl<ProductDto> findAll(PagingRequest pagingRequest) {
        Specification<Product> specification = CriteriaUtility.getSpecification(pagingRequest.getFilters());
        Page<Product> page = productRepository.findAll(specification, buildPageable(pagingRequest));
        return (PageImpl<ProductDto>) page.map(productMapper::toDto);
    }

    @Override
    public ProductDto update(ProductDto productDto, Long id) {
        Optional<Product> existingProduct = productRepository.findById(id);
        if (existingProduct.isPresent()) {
            Product product = existingProduct.get();
            productMapper.partialUpdate(productDto, product);
            return productMapper.toDto(productRepository.save(product));
        }
        return null;
    }

    @Override
    public List<ProductDto> saveListProduct(MultipartFile file) throws IOException {
        List<ProductDto> productDto = readProductsFromCSV(file.getInputStream());
        List<Product> products =  productRepository.saveAll(productMapper.ListToEntity(productDto));
        return productMapper.ListToDto(products);
    }

    private static List<ProductDto> readProductsFromCSV(InputStream inputStream) {
        List<ProductDto> products = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            boolean headerSkipped = false;
            while ((line = br.readLine()) != null) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }
                String[] values = line.split(",");
                ProductDto product = new ProductDto();
                product.setValuesFromArray(values);
                products.add(product);
            }
            return products;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PageImpl<ProductDto> filterProducts(Long id, PagingRequest pagingRequest) {
        FilterRequest filterRequest = new FilterRequest("category.id", id.toString(), "EQ");
        pagingRequest.getFilters().add(filterRequest);
        Specification<Product> specification = CriteriaUtility.getSpecification(pagingRequest.getFilters());
        Page<Product> products = productRepository.findAll(specification, buildPageable(pagingRequest));
        return (PageImpl<ProductDto>) products.map(productMapper::toDto);
    }


    @Override
    public PageImpl<ProductDto> findByName(PagingRequest pagingRequest, String name) {
        Specification<Product> specification = CriteriaUtility.buildFieldLikeAny("name", name);
        Page<Product> products = productRepository.findAll(specification, buildPageable(pagingRequest));
        return (PageImpl<ProductDto>) products.map(productMapper::toDto);
    }

    @Override
    public List<ProductDto> findByNameSuggest(String name) {
        Specification<Product> specification = CriteriaUtility.buildFieldSearch("name", name);
        List<Product> products = productRepository.findAll(specification);
        return productMapper.ListToDto(products);
    }


    private Pageable buildPageable(PagingRequest pagingRequest) {
        return PageRequest.of(
                pagingRequest.getPageNumber() <= 0 ? 0 : pagingRequest.getPageNumber(),
                pagingRequest.getPageSize(),
                CriteriaUtility.buildSort(pagingRequest.getSortBy(), pagingRequest.getSortType())
        );
    }

    @Override
    public void delete(Long id) {
        productRepository.deleteById(id);
    }
}
