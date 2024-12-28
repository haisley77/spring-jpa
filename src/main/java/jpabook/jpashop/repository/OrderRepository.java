package jpabook.jpashop.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jpabook.jpashop.domain.*;
import jpabook.jpashop.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final EntityManager em;

    public void save(Order order) {
        em.persist(order);
    }

    public Order findOne(Long id) {
        return em.find(Order.class, id);
    }

    /**
     * JPQL로 처리
     */
    public List<Order> findAllByString(OrderSearch orderSearch) {

        String jpql = "select o From Order o join o.member m";
        boolean isFirstCondition = true;
        //주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.status = :status";
        }
        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " m.name like :name";
        }
        TypedQuery<Order> query = em.createQuery(jpql, Order.class)
                .setMaxResults(1000); //최대 1000건
        if (orderSearch.getOrderStatus() != null) {
            query = query.setParameter("status", orderSearch.getOrderStatus());
        }
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            query = query.setParameter("name", orderSearch.getMemberName());
        }
        return query.getResultList();
    }

    /**
     * JPA Criteria로 처리
     */
    public List<Order> findAllByCriteria(OrderSearch orderSearch) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> o = cq.from(Order.class);
        Join<Order, Member> m = o.join("member", JoinType.INNER); //회원과 조인
        List<Predicate> criteria = new ArrayList<>();
        //주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            Predicate status = cb.equal(o.get("status"),
                    orderSearch.getOrderStatus());
            criteria.add(status);
        }
        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            Predicate name =
                    cb.like(m.<String>get("name"), "%" + orderSearch.getMemberName()
                            + "%");
            criteria.add(name);
        }
        cq.where(cb.and(criteria.toArray(new Predicate[criteria.size()])));
        TypedQuery<Order> query = em.createQuery(cq).setMaxResults(1000); //최대 1000

        return query.getResultList();
    }

    public List<Order> findAll(OrderSearch orderSearch) {
        QOrder order = QOrder.order;
        QMember member = QMember.member;

        JPAQueryFactory query = new JPAQueryFactory(em);
        return query
                .select(order)
                .from(order)
                .join(order.member, member)
                .where(statusEq(orderSearch.getOrderStatus()), nameLike(orderSearch.getMemberName()))
                .limit(1000)
                .fetch();
    }

    private BooleanExpression nameLike(String memberName) {
        if (!StringUtils.hasText(memberName)) {
            return null;
        }
        return QMember.member.name.like(memberName);
    }

    private BooleanExpression statusEq(OrderStatus statusCond) {
        if (statusCond == null) {
            return null;
        }
        return QOrder.order.status.eq(statusCond);
    }

    // 재사용성이 좋다
    public List<Order> findAllWithMemberDelivery() {
        // 페치 조인 (지연로딩 설정이 되어 있더라도, 쿼리를 날릴 때 관련 데이터를 다 가져오도록)
        return em.createQuery(
                "select o from Order o" +
                        " join fetch o.member m" +
                        " join fetch o.delivery d", Order.class)
                .getResultList();
    }


    /**
     * 컬렉션 페치 조인
     * 페이징을 하면 큰일난다 -> 치명적인 단점
     * 1개만 사용할 수 있다. 컬렉션 둘 이상에 페치 조인을 사용하면, 데이터가 부정합하게 조회될 수 있다.
     */
    public List<Order> findAllWithItem() {
        // distinct 추가하더라도 DB 쿼리 결과 자체는 중복 제거가 안될 수 있다.
        // JPA는 엔티티가 중복이면 그 중복을 제거하고 컬렉션에 담아준다.
        // SpringBoot 3 + Hibernate -> distinct가 자동 적용된다고 한다.
        return em.createQuery(
                "select distinct o from Order o" +
                        " join fetch o.member m" +
                        " join fetch o.delivery d" +
                        " join fetch o.orderItems oi" +
                        " join fetch oi.item i", Order.class)
                // DB 쿼리에 limit offset을 적용하더라도 우리가 원하는 데이터 페이징이 안될 수도 있다.
                // 그래서 모든 데이터를 애플리케이션 메모리로 끌어올린 다음, 메모리에서 페이징 처리를 하게 된다.
                // OutOfMemory 에러가 발생할 수 있다.
//                .setFirstResult(1)
//                .setMaxResults(10000)
                .getResultList();
    }


    // ToOne 관계는 페치 조인해도 페이징에 영향을 주지 않는다.
    // 따라서 ToOne 관계는 페치 조인으로 쿼리 수를 줄이고 해결하고,
    // 나머지는 hibernate.default_batch_fetch_size 로 최적화한다.
    // 쿼리 호출 수가 1 + N -> 1 + 1 로 최적화된다.
    // size는 100 ~ 1000 사이가 좋지만, DB와 애플리케이션이 순간 부하를 어느 수준까지 견딜 수 있는 지로 결정하면 된다.
    public List<Order> findAllWithMemberDelivery(int offset, int limit) {
        return em.createQuery(
                        "select o from Order o" +
                                " join fetch o.member m" +
                                " join fetch o.delivery d", Order.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }
}
