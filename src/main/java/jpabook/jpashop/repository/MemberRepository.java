package jpabook.jpashop.repository;

import jpabook.jpashop.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    //select m from Member m where m.name = ?
    //메서드 이름으로 정확한 JPQL 쿼리를 실행한다.
    //인터페이스만 만들면, 구현체는 스프링 데이터 JPA가 애플리케이션 실행시점에 주입해준다.
    List<Member> findByName(String name);
}
