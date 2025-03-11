package bio.repository.search;

import bio.domain.BioInvShipping;
import bio.domain.Employees;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class BioInvShippingSearchImpl implements BioInvShippingSearch {
    private final EntityManager entityManager;

    @Override
    public Page<BioInvShipping> searchAll(String[] types, String keyword, String dateKeyword, String shelfLifeKeyword, Pageable pageable) {
        String baseQuery = "SELECT b FROM BioInvShipping b WHERE b.shippingId > 0";
        String countQueryBase = "SELECT COUNT(b) FROM BioInvShipping b WHERE b.shippingId > 0";

        List<String> conditions = new ArrayList<>();
        boolean hasKeyword = keyword != null && !keyword.isEmpty();
        boolean hasDateKeyword = dateKeyword != null && !dateKeyword.isEmpty();
        boolean hasShelfLifeKeyword = shelfLifeKeyword != null && !shelfLifeKeyword.isEmpty();
        boolean hasTypes = types != null && types.length > 0;

        // 일반 검색 (제품코드, 출고수량 등)
        if (hasKeyword && hasTypes) {
            List<String> typeConditions = new ArrayList<>();
            for (String type : types) {
                switch (type) {
                    case "p": typeConditions.add("b.productCode LIKE :keyword"); break;
                    case "q": typeConditions.add("CAST(b.quantity AS string) LIKE :keyword"); break;
                    case "c": typeConditions.add("b.customer LIKE :keyword"); break;
                    case "l": typeConditions.add("b.warehouseLocation LIKE :keyword"); break;
                    case "r": typeConditions.add("b.registeredBy LIKE :keyword"); break;
                }
            }
            if (!typeConditions.isEmpty()) {
                conditions.add("(" + String.join(" OR ", typeConditions) + ")");
            }
        }

        // 출고일자 검색
        if (hasDateKeyword) {
            conditions.add("b.shippingDate = :shippingDate");
        }

        // 유통기한 검색
        if (hasShelfLifeKeyword) {
            conditions.add("b.shelfLife = :shelfLifeDate");
        }

        // WHERE 조건을 동적으로 추가
        String conditionString = conditions.isEmpty() ? "" : " AND " + String.join(" AND ", conditions);
        String finalQuery = baseQuery + conditionString + " ORDER BY b.shippingId DESC";
        String countQueryFinal = countQueryBase + conditionString;

        // JPQL 쿼리 실행
        TypedQuery<BioInvShipping> query = entityManager.createQuery(finalQuery, BioInvShipping.class);
        TypedQuery<Long> countQuery = entityManager.createQuery(countQueryFinal, Long.class);

        // 파라미터 설정
        if (hasKeyword) {
            query.setParameter("keyword", "%" + keyword + "%");
            countQuery.setParameter("keyword", "%" + keyword + "%");
        }

        if (hasDateKeyword) {
            LocalDate date = LocalDate.parse(dateKeyword);
            query.setParameter("shippingDate", date);
            countQuery.setParameter("shippingDate", date);
        }

        if (hasShelfLifeKeyword) {
            LocalDate shelfLifeDate = LocalDate.parse(shelfLifeKeyword);
            query.setParameter("shelfLifeDate", shelfLifeDate);
            countQuery.setParameter("shelfLifeDate", shelfLifeDate);
        }

        // 페이징 처리
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<BioInvShipping> list = query.getResultList();
        long count = countQuery.getSingleResult();

        log.info("Keyword: [" + keyword + "], Date: [" + dateKeyword + "], ShelfLife: [" + shelfLifeKeyword + "]");

        return new PageImpl<>(list, pageable, count);
    }



}



