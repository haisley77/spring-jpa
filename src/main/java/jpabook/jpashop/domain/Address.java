package jpabook.jpashop.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;


@Embeddable
@Getter
public class Address {
    private String city;
    private String street;
    private String zipcode;

    // JPA 스펙에서 기본 생성자 필요 -> protected로 지정하면 호출될 위험을 줄일 수 있음
    protected Address() {
    }

    // 값 타입 -> 변경이 불가능 -> 처음 생성할 때에만 값 넣어주도록 설계
    public Address(String city, String street, String zipcode) {
        this.city = city;
        this.street = street;
        this.zipcode = zipcode;
    }
}
