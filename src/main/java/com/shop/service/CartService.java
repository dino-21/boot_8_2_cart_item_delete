package com.shop.service;


import com.shop.dto.CartDetailDto;
import com.shop.dto.CartItemDto;
import com.shop.entity.Cart;
import com.shop.entity.CartItem;
import com.shop.entity.Item;
import com.shop.entity.Member;
import com.shop.repository.CartItemRepository;
import com.shop.repository.CartRepository;
import com.shop.repository.ItemRepository;
import com.shop.repository.MemberRepository;
import groovy.util.logging.Log4j2;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional
public class CartService {
    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    // 장바구니에 상품 추가하는 메서드
    public Long addCart(CartItemDto cartItemDto, String email){
        //1 주어진 상품 ID로 상품을 찾음, 없으면 예외 발생
        Item item = itemRepository.findById(cartItemDto.getItemId())
                .orElseThrow(EntityNotFoundException::new);

        // 2 주어진 이메일로 회원을 찾음
        Member member = memberRepository.findByEmail(email);

        // 3 회원의 장바구니를 찾음
        Cart cart = cartRepository.findByMemberId(member.getId());

        // 4 email(사용자)에게 카트(장바구니)가 없다면 카트를 생성
        if(cart == null){
            cart = Cart.createCart(member); // 회원의 장바구니 생성
            cartRepository.save(cart);  // 생성된 장바구니 저장
        }

        //5 카트안에 같은 상품이 있는지 확인한다.
        CartItem savedCartItem =
                cartItemRepository.findByCartIdAndItemId(cart.getId(), item.getId());

        //카트안에 상품이 있다면 기존 카트(장바구니)에 추가한다.
        if(savedCartItem != null){
            savedCartItem.addCount(cartItemDto.getCount());  // 6수량 추가
            return savedCartItem.getId();  // 장바구니 항목 ID 반환
        }else{ // 장바구니에 같은 상품이 없으면 새로운 장바구니 항목 생성
            // 7새로운 장바구니 항목 생성
            CartItem cartItem = CartItem.createCartItem(cart, item, cartItemDto.getCount());
            cartItemRepository.save(cartItem);  //8 생성된 장바구니 항목 저장
            return cartItem.getId();  // 장바구니 항목 ID 반환
        }
    }


    // 장바구니 목록을 가져오는 메서드
    @Transactional(readOnly = true)
    public List<CartDetailDto> getCartList(String email){

        // 장바구니 상세 정보 DTO 리스트 생성
        List<CartDetailDto> cartDetailDtoList = new ArrayList<>();

        // 이메일을 기반으로 회원 조회
        Member member = memberRepository.findByEmail(email);

        // 1 현재 로그인한 회원의 장바구니 엔티티를 조회
        Cart cart = cartRepository.findByMemberId(member.getId());

        // 2 장바구니에 상품을 한번도 안 담았을 경우 장바구니 엔티티가 없으므로 빈 리스트를 반환한다.
        if(cart == null){
            return cartDetailDtoList;
        }

        //3 장바구니에 담겨있는 상품 정보를 조회한다.
        cartDetailDtoList = cartItemRepository.findCartDetailDtoList(cart.getId());
        return cartDetailDtoList; // 조회된 장바구니 상세 정보 DTO 리스트 반환
    }


    // 장바구니 항목의 소유자 여부를 확인하는 메서드
    @Transactional(readOnly = true)
    public boolean validateCartItem(Long cartItemId, String email){
        // 1 현재 사용자를 이메일을 기반으로 조회
        Member curMember = memberRepository.findByEmail(email);

        // 주어진 cartItemId로 장바구니 항목 조회
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(EntityNotFoundException::new);

        // 2 해당 장바구니 항목의 소유자 조회
        Member savedMember = cartItem.getCart().getMember();

        // 3 현재 사용자와 장바구니 항목의 소유자가 다를 경우 false 반환
        if(!StringUtils.equals(curMember.getEmail(), savedMember.getEmail())){
            return false;
        }
        // 4 현재 사용자가 장바구니 항목의 소유자일 경우 true 반환
        return true;
    }


    // 5 장바구니 상품의 수량을 업데이트하는 메서드
    public void updateCartItemCount(Long cartItemId, int count){
        // 주어진 cartItemId로 장바구니 항목 조회
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(EntityNotFoundException::new);
        // 장바구니 항목의 수량 업데이트
        cartItem.updateCount(count);
    }


   // 장바구니 항목을 삭제하는 메서드
    public void deleteCartItem(Long cartItemId) {
        // 주어진 cartItemId로 장바구니 항목 조회
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(EntityNotFoundException::new);
        // 조회된 장바구니 항목 삭제
        cartItemRepository.delete(cartItem);
    }


}
