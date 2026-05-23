package io.weblinkpilot.analytics.repository;

import io.weblinkpilot.analytics.domain.ClickEvent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    long countByShortCode(String shortCode);

    @Query("select count(distinct c.ipAddress) from ClickEvent c where c.shortCode = :shortCode and c.ipAddress is not null")
    long countDistinctIpAddressByShortCode(@Param("shortCode") String shortCode);

    Optional<ClickEvent> findFirstByShortCodeOrderByClickedAtDesc(String shortCode);

    @Query(value = """
            select coalesce(nullif(country, ''), 'UNKNOWN') as country,
                   count(*) as clicks
            from click_events
            where short_code = :shortCode
            group by coalesce(nullif(country, ''), 'UNKNOWN')
            order by clicks desc, country asc
            """, nativeQuery = true)
    List<CountryClicksView> findTopCountriesByShortCode(@Param("shortCode") String shortCode);
}
