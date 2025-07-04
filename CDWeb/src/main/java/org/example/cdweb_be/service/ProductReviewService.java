package org.example.cdweb_be.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cdweb_be.component.MessageProvider;
import org.example.cdweb_be.dto.request.ProductReviewCreateRequest;
import org.example.cdweb_be.dto.response.OrderUser;
import org.example.cdweb_be.dto.response.PagingResponse;
import org.example.cdweb_be.dto.response.ProductReviewResponse;
import org.example.cdweb_be.entity.Order;
import org.example.cdweb_be.entity.OrderItem;
import org.example.cdweb_be.entity.Product;
import org.example.cdweb_be.entity.ProductReview;
import org.example.cdweb_be.enums.OrderStatus;
import org.example.cdweb_be.exception.AppException;
import org.example.cdweb_be.exception.ErrorCode;
import org.example.cdweb_be.respository.OrderItemRepository;
import org.example.cdweb_be.respository.OrderRepository;
import org.example.cdweb_be.respository.ProductRepository;
import org.example.cdweb_be.respository.ProductReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class ProductReviewService {
    MessageProvider messageProvider;
    ProductReviewRepository productReviewRepository;
    OrderItemRepository orderItemRepository;
    OrderRepository orderRepository;
    ProductRepository productRepository;
    AuthenticationService authenticationService;

    @PreAuthorize("isAuthenticated()")
    public ProductReviewResponse add(String token, ProductReviewCreateRequest request) {
        long userId = authenticationService.getUserId(token);
        Order order = orderRepository.findById(request.getOrderId()).orElseThrow(() ->
                new AppException(messageProvider,ErrorCode.ORDER_NOT_EXISTS));
        if(order.getStatus() != OrderStatus.ST_GIAO_THANH_CONG && order.getStatus() != OrderStatus.ST_YC_TRA_HANG&& order.getStatus() != OrderStatus.ST_DA_TRA_HANG)
            throw new AppException(messageProvider,ErrorCode.PRODUCT_REVIEW_STATUS_INVALID);
        Product product = productRepository.findById(request.getProductId()).orElseThrow(() ->
                new AppException(messageProvider,ErrorCode.PRODUCT_NOT_EXISTS));
        OrderItem orderItem = orderItemRepository.findByOrderIdAndProductId(order.getId(), product.getId()).orElseThrow(() ->
                new AppException(messageProvider,ErrorCode.PRODUCT_REVIEW_NOT_EXIST));
        if(order.getUser().getId() != userId) throw new AppException(messageProvider,ErrorCode.PRODUCT_REVIEW_UNAUTH);
        Optional<ProductReview> productReviewOptional = productReviewRepository.findByOrderIdAndProductId(order.getId(), product.getId());
        if(productReviewOptional.isPresent()) throw new AppException(messageProvider,ErrorCode.PRODUCT_REVIEW_EXISTED);
        if(request.getRatingScore() <1 || request.getRatingScore()>5) throw new AppException(messageProvider,ErrorCode.RATING_SCORE_INVALID);
        ProductReview productReview = ProductReview.builder()
                .product(product)
                .order(order)
                .content(request.getContent())
                .isShow(true)
                .ratingScore(request.getRatingScore())
                .images(request.getImages())
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build();
        productReview = productReviewRepository.save(productReview);
        ProductReviewResponse result = converToProductReviewResponse(productReview);

        return result;
    }

    public PagingResponse getByProductId(long productId, int page, int size) {
        Pageable pageable = PageRequest.of(page-1, size);
        Page<ProductReview> productReviews = productReviewRepository.findByProductId(productId, pageable);
        List<ProductReviewResponse> productReviewResponses = productReviews.stream().map(productReview ->
                converToProductReviewResponse(productReview)).collect(Collectors.toList());
        return PagingResponse.<ProductReviewResponse>builder()
                .page(page)
                .size(size)
                .totalItem(productReviewRepository.count())
                .data(productReviewResponses)
                .build();
    }


    public ProductReviewResponse converToProductReviewResponse(ProductReview productReview) {
        OrderUser orderUser = new OrderUser(productReview.getOrder().getUser());
        ProductReviewResponse productReviewResponse = ProductReviewResponse.builder()
                .id(productReview.getId())
                .orderUser(orderUser)
                .productId(productReview.getProduct().getId())
                .productName(productReview.getProduct().getName())
                .ratingScore(productReview.getRatingScore())
                .content(productReview.getContent())
                .images(productReview.getImages())
                .createdAt(productReview.getCreatedAt())
                .updatedAt(productReview.getUpdatedAt())
                .build();
        return productReviewResponse;
    }
}
