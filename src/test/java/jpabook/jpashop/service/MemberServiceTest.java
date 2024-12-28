package jpabook.jpashop.service;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepositoryOld;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberServiceTest {

    @Autowired
    MemberService memberService;
    @Autowired
    MemberRepositoryOld memberRepository;

    @Test
    @DisplayName("회원 가입한다.")
    public void 회원가입() throws Exception {
        // given
        Member member = new Member();
        member.setName("kim");
        
        // when
        Long savedId = memberService.join(member);

        //then
        assertEquals(member, memberRepository.findOne(savedId));
    }
    
    @Test
    @DisplayName("이름이 중복된 회원은 가입할 수 없다.")
    public void 중복_회원_예외() throws Exception {
        // given
        Member member1 = new Member();
        member1.setName("kim");

        Member member2 = new Member();
        member2.setName("kim");

        // when
        memberService.join(member1);

        //then
        assertThrows(IllegalStateException.class, () -> {
            memberService.join(member2);
        });

    }
    

}